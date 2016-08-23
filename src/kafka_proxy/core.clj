(ns kafka-proxy.core
  (:require [aleph.http :as http]
            [compojure.core :as compojure :refer [GET]]
            [compojure.route :as route]
            [manifold.stream :as s]
            [clojure.core.async :refer [>! <! go-loop chan close! timeout]]
            [clojure.string :as str])
  (:gen-class)
  (:import (org.apache.kafka.common.serialization StringSerializer StringDeserializer)
           (org.apache.kafka.clients.producer KafkaProducer ProducerRecord)
           (org.apache.kafka.clients.consumer KafkaConsumer)
           (org.apache.kafka.common TopicPartition)))

(def brokers {"bootstrap.servers" "localhost:9092"})        ; TODO: env


(def marshalling-options {"key.serializer"     StringSerializer
                          "value.serializer"   StringSerializer
                          "key.deserializer"   StringDeserializer
                          "value.deserializer" StringDeserializer}) ; TODO: use Byte serialization


(def subscriber-options {"enable.auto.commit" "false"})

(def CONSUME_LATEST -1)
(def CONSUME_POLLING_TIMEOUT 100)                           ; TODO enable env parameter (> 0 && <= 1000)
(def CONSUME_DEFAULT_TOPIC "simple-proxy-topic")            ; TODO enable env parameter
(def CONSUME_DEFAULT_BUFFER 512)                            ; TODO enable env parameter (> 0 && <=1024)

(def CLIENT_KEEP_ALIVE_TIMEOUT (* 20 1000))                 ; TODO enable env parameter (> 0 && <= 60)
(def CLIENT_RECONNECTION_TIME (* 3 1000))                   ; TODO enable env parameter (> 0 && <= 30)

(def proxy-group (str "kafka-proxy-" (java.util.UUID/randomUUID)))

(defn topic-consumer
  "Obtain an appropriately positioned kafka consumer that is ready to be polled"
  [topic offset]
  {:pre [(or (= offset CONSUME_LATEST) (>= offset 0))]}
  (let [group-id {"group.id" (str proxy-group "-" (rand))}
        consumer (KafkaConsumer. (merge brokers marshalling-options subscriber-options group-id))]
    ; TODO handle connection fail (hystrix?)

    (if (= offset CONSUME_LATEST)
      (.subscribe consumer [topic])
      (let [partition (TopicPartition. topic 0)]
        (.assign consumer [partition])
        (.seek consumer partition offset)))

    consumer))

(defn sse-data [kafka-record]
  (str "id: " (.offset kafka-record) "\n"
       "event: " (.key kafka-record) "\n"
       "retry: " CLIENT_RECONNECTION_TIME "\n"
       "data: " (.value kafka-record) "\n\n"))

(defn transformer-fn
  "Filter event types using one or more comma seperated regexes, maps Kafka ConsumerRecord to SSE format"
  [client-event-filter]
  (let [rxs (map #(re-pattern %) (str/split client-event-filter #","))]
    (comp (map #(sse-data %))
          (filter (fn [kafka-record]
                    (let [event (.key kafka-record)
                          found (mapcat #(re-find % event) rxs)]
                      (> (count found) 0)))))))

(defn kafka-proxy-handler
  "Stream SSE data from the Kafka topic"
  [request]
  (let [offset (or (get (:headers request) "last-event-id") CONSUME_LATEST)
        topic (or (get (:params request) "topic") CONSUME_DEFAULT_TOPIC)
        client-filter (or (get (:params request) "filter") ".*")
        consumer (topic-consumer topic offset)
        kafka-transformed-ch (chan CONSUME_DEFAULT_BUFFER (transformer-fn client-filter))]
    (go-loop []
      (let [_ (<! (timeout CLIENT_KEEP_ALIVE_TIMEOUT))]
        (>! kafka-transformed-ch ":\n")
        (recur)))
    (go-loop []
      (if-let [records (.poll consumer CONSUME_POLLING_TIMEOUT)]
        (doseq [record records]
          (>! kafka-transformed-ch record)))
      (recur))
    {:status  200
     :headers {"Content-Type"  "text/event-stream;charset=UTF-8"
               "Cache-Control" "no-cache"}
     :body    (s/->source kafka-transformed-ch)}))

(def handler
  (compojure/routes
    (GET "/kafka-sse" [] kafka-proxy-handler)
    ; TODO POST for sending on the topic
    (route/not-found "No such page.")))



;------------------------------------------------------------------------
; Short hand for experimentation in the REPL
;------------------------------------------------------------------------

; TODO write tests to check the basics

(comment

  (def server (http/start-server handler {:port 10000}))

  (def producer (KafkaProducer. (merge brokers marshalling-options)))

  (defn produce
    [topic k v]
    (.send producer (ProducerRecord. topic k v)))

  (let [rando (rand-int 1000)]
    (produce "simple-proxy-topic" "rando-event" (str "{\"id\" " rando " \"message\" \"Hello SSE\"}")))

  )

