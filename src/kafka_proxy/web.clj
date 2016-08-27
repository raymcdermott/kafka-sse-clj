(ns kafka-proxy.web
  (:require [aleph.http :as http]
            [compojure.route :as route]
            [compojure.core :as compojure :refer [GET]]
            [ring.middleware.params :as params]
            [kafka-proxy.kafka-sse :as sse]
            [kafka-proxy.kafka :as kafka]
            [kafka-proxy.config :as config])
  (:gen-class))

(def ^:private TOPIC (config/env-or-default :sse-proxy-topic "simple-proxy-topic"))

(defn my-sse-handler-with-options
  "Setup to stream SSE data from the Kafka topic"
  [request]
  (let [offset (or (get (:headers request) "last-event-id") kafka/CONSUME_LATEST)
        topic (or (get (:params request) "topic") TOPIC)
        event-filter (or (get (:params request) "filter[event]") ".*")
        my-subscriber-options {"session.timeout.ms", "30000"}
        consumer (kafka/consumer topic offset my-subscriber-options)]
    (sse/kafka->sse-handler event-filter consumer)))


(def web-handler-my-options
  (params/wrap-params
    (compojure/routes
      (GET "/kafka-sse" [] my-sse-handler-with-options)
      (route/not-found "No such page."))))


(defn my-sse-handler-defaults
  "Setup to stream SSE data from the Kafka topic"
  [request]
  (let [offset (or (get (:headers request) "last-event-id") kafka/CONSUME_LATEST)
        topic (or (get (:params request) "topic") TOPIC)
        event-filter (or (get (:params request) "filter[event]") ".*")
        consumer (kafka/consumer topic offset)]
    (sse/kafka->sse-handler event-filter consumer)))


(def web-handler-defaults
  (params/wrap-params
    (compojure/routes
      (GET "/kafka-sse" [] my-sse-handler-defaults)
      (route/not-found "No such page."))))

; TODO: Herokufy the server start
(defn- start-server [handler]
  (http/start-server handler {:port 10000}))


