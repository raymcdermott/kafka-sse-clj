(ns kafka-proxy.kafka-sse
  (:require [clojure.core.async :as async :refer [>! <! go-loop chan close! timeout]]
            [clojure.string :as str]
            [kafka-proxy.config :as config]
            [kafka-proxy.kafka :as kafka]))

(def ^:private POLL_TIMEOUT_MILLIS (config/env-or-default :sse-proxy-poll-timeeout-millis 100))

(def ^:private BUFFER_SIZE (config/env-or-default :sse-proxy-buffer-size 512))

(def ^:private KEEP_ALIVE_MILLIS (config/env-or-default :sse-proxy-keep-alive-millis (* 20 1000)))

(defn consumer-record->sse
  "Kakfa Java API ConsumerRecord to the standard SSE data record format"
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

(defn kafka->sse-ch
  "Creates a channel that produces SSE data from the Kafka consumer"
  ([request topic]
   (let [event-filter (or (get (:params request) "filter[event]") ".*")]
     (kafka->sse-ch request topic (comp (filter #(name-matches? event-filter (.key %)))
                                        (map consumer-record->sse)))))

  ([request topic transducer]
   (let [offset (or (get (:headers request) "last-event-id") kafka/CONSUME_LATEST)
         consumer (kafka/consumer topic offset)
         timeout-ch (chan)
         kafka-ch (chan BUFFER_SIZE transducer)]
     (go-loop []
       (let [_ (<! (timeout KEEP_ALIVE_MILLIS))]
         (>! timeout-ch ":\n")
         (recur)))

     (go-loop []
       (if-let [records (.poll consumer POLL_TIMEOUT_MILLIS)]
         (doseq [record records]
           (>! kafka-ch record)))
       (recur))

     (async/merge [kafka-ch timeout-ch]))))
