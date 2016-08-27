(ns kafka-proxy.web
  (:require [aleph.http :as http]
            [compojure.route :as route]
            [compojure.core :as compojure :refer [GET]]
            [ring.middleware.params :as params]
            [kafka-proxy.kafka-sse :as sse]
            [kafka-proxy.config :as config]
            [manifold.stream :as s])
  (:gen-class))

(def ^:private TOPIC (config/env-or-default :sse-proxy-topic "simple-proxy-topic"))

(defn sse-handler-using-defaults
  "Stream SSE data from the Kafka topic"
  [request]
  (let [topic (get (:params request) "topic" TOPIC)
        ch (sse/kafka->sse-ch request topic)]
    {:status  200
     :headers {"Content-Type"  "text/event-stream;charset=UTF-8"
               "Cache-Control" "no-cache"}
     :body    (s/->source ch)}))


(def handler
  (params/wrap-params
    (compojure/routes
      (GET "/kafka-sse" [] sse-handler-using-defaults)
      (route/not-found "No such page."))))


(comment

  (def server (http/start-server handler {:port 10000}))

  )
