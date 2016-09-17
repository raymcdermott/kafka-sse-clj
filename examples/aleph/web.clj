(ns kafka-proxy.web
  (:require [aleph.http :as http]
            [environ.core :refer [env]]
            [compojure.route :as route]
            [compojure.core :as compojure :refer [GET]]
            [ring.middleware.params :as params]
            [kafka-proxy.kafka-sse :as ks]
            [kafka-proxy.config :as config]
            [manifold.stream :as s]
            [kafka-proxy.producer :as producer])
  (:gen-class))

(def ^:private TOPIC (config/env-or-default :sse-proxy-topic "simple-proxy-topic"))

(def ^:private kafka-brokers (if-let [env-brokers (env :kafka-proxy-broker-url)]
                               {"bootstrap.servers" env-brokers}
                               {"bootstrap.servers" "localhost:9092"}))

(defn sse-handler-using-defaults
  "Stream SSE data from the Kafka topic"
  [request]
  (let [topic-name (get (:params request) "topic" TOPIC)
        offset (get (:headers request) "last-event-id" config/CONSUME_LATEST)
        event-filter-regex (get (:params request) "filter[event]" ".*")
        _ (producer/produce-constantly! kafka-brokers topic-name) ; not normal, just for demo - also produce!!
        ch (ks/kafka->sse-ch topic-name offset event-filter-regex)]
    {:status  200
     :headers {"Content-Type"  "text/event-stream;charset=UTF-8"
               "Cache-Control" "no-cache"}
     :body    (s/->source ch)}))


(def handler
  (params/wrap-params
    (compojure/routes
      (GET "/kafka-sse" [] sse-handler-using-defaults)
      (route/not-found "No such page."))))


;(defonce server (http/start-server handler {:port 10000}))
