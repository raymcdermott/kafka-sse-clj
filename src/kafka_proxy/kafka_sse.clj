(ns kafka-proxy.kafka-sse
  (:require [manifold.stream :as s]
            [clojure.core.async :as async :refer [>! <! go-loop chan close! timeout]]
            [clojure.string :as str]
            [kafka-proxy.config :as config]))

(def ^:private POLL_TIMEOUT (config/env-or-default :sse-proxy-poll-timeeout 100))

(def ^:private BUFFER_SIZE (config/env-or-default :sse-proxy-buffer-size 512))

(def ^:private KEEP_ALIVE (config/env-or-default :sse-proxy-keep-alive (* 20 1000)))

(def ^:private RETRY (config/env-or-default :sse-proxy-retry (* 5 1000)))

(defn kafka-record->sse [kafka-record]
  (str "id: " (.offset kafka-record) "\n"
       "event: " (.key kafka-record) "\n"
       "retry: " RETRY "\n"
       "data: " (.value kafka-record) "\n\n"))

(defn event-name-filter
  "Filter event types using one or more comma seperated regexes"
  [event-filter event-name]
  (let [rxs (map #(re-pattern %) (str/split event-filter #","))
        found (filter #(re-find % event-name) rxs)]
    (> (count found) 0)))

(defn kafka->sse-handler
  "Stream SSE data from the Kafka consumer"
  [event-filter consumer]
  (let [timeout-ch (chan)
        kafka-ch (chan BUFFER_SIZE (comp (filter #(event-name-filter event-filter (.key %)))
                                         (map kafka-record->sse)))]
    (go-loop []
      (let [_ (<! (timeout KEEP_ALIVE))]
        (>! timeout-ch ":\n")
        (recur)))
    (go-loop []
      (if-let [records (.poll consumer POLL_TIMEOUT)]
        (doseq [record records]
          (>! kafka-ch record)))
      (recur))
    {:status  200
     :headers {"Content-Type"  "text/event-stream;charset=UTF-8"
               "Cache-Control" "no-cache"}
     :body    (s/->source (async/merge [kafka-ch timeout-ch]))}))
