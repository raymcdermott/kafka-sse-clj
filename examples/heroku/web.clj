(ns kafka-proxy.web
  (:require [aleph.http :as http]
            [compojure.route :as route]
            [compojure.core :as compojure :refer [GET]]
            [ring.middleware.params :as params]
            [manifold.stream :as s]
            [kafka-proxy.config :as config]
            [kafka-proxy.heroku-kafka :as heroku])
  (:gen-class))

(def ^:private TOPIC (config/env-or-default :sse-proxy-topic "simple-proxy-topic"))

(defn sse-handler-using-heroku
  "Stream SSE data from the Kafka topic"
  [request]
  (let [topic (get (:params request) "topic" TOPIC)
        offset (get (:headers request) "last-event-id" config/CONSUME_LATEST)
        event-filter (get (:params request) "filter[event]" ".*")
        ch (heroku/heroku-kafka->sse-ch topic offset event-filter)]
    {:status  200
     :headers {"Content-Type"  "text/event-stream;charset=UTF-8"
               "Cache-Control" "no-cache"}
     :body    (s/->source ch)}))


(def handler
  (params/wrap-params
    (compojure/routes
      (GET "/kafka-sse" [] sse-handler-using-heroku)
      (route/not-found "No such page."))))

; TODO heroku-ify
;(defonce server (http/start-server handler {:port 10000}))
