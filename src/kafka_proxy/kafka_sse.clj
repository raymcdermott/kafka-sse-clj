(ns kafka-proxy.kafka-sse
  (:require [clojure.core.async :as async :refer [>! <! go-loop chan close! timeout]]
            [clojure.string :as str]
            [kafka-proxy.config :as config]
            [kafka-proxy.kafka :as kafka]))

(def ^:private poll-timeout-millis (config/env-or-default :sse-proxy-poll-timeout-millis 100))

(def ^:private buffer-size (config/env-or-default :sse-proxy-buffer-size 512))

(def ^:private keep-alive-millis (config/env-or-default :sse-proxy-keep-alive-millis (* 5 1000)))

(defn consumer-record->sse
  "Convert a Kakfa Java API ConsumerRecord to the HTML5 EventSource format"
  [consumer-record]
  (str "id: " (.offset consumer-record) "\n"
       "event: " (.key consumer-record) "\n"
       "data: " (.value consumer-record) "\n\n"))

(defn name-matches?
  "Match name with the regexes in a comma seperated string"
  [regex-str name]
  (let [rxs (map #(re-pattern %) (str/split regex-str #","))
        found (filter #(re-find % name) rxs)]
    (> (count found) 0)))

(defn kafka-consumer->sse-ch
  [consumer transducer]
  (let [keep-alive-ch (chan)
        kafka-ch (chan buffer-size transducer)]

    (go-loop []
      (let [_ (<! (timeout keep-alive-millis))]
        (>! keep-alive-ch ":\n")
        (recur)))

    (go-loop []
      (if-let [records (.poll consumer poll-timeout-millis)]
        (doseq [record records]
          (>! kafka-ch record)))
      (recur))

    (async/merge [kafka-ch keep-alive-ch])))

(defn kafka->sse-ch
  "Creates a channel that filters and maps data from a Kafka topic to the HTML5 EventSource format"
  [request topic-name]
  (let [offset (get (:headers request) "last-event-id" kafka/CONSUME_LATEST)
        event-filter (get (:params request) "filter[event]" ".*")
        consumer (kafka/sse-consumer topic-name offset)]
    (kafka-consumer->sse-ch consumer (comp (filter #(name-matches? event-filter (.key %)))
                                           (map consumer-record->sse)))))
