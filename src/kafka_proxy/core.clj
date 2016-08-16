(ns kafka-proxy.core
  (:require [clojure.core.async :as async :refer [>! <! >!! <!! alts! go go-loop chan onto-chan put! timeout]])
  (:gen-class)
  (:import (org.apache.kafka.common.serialization StringSerializer StringDeserializer IntegerSerializer IntegerDeserializer)
           (org.apache.kafka.clients.producer KafkaProducer ProducerRecord)
           (org.apache.kafka.clients.consumer KafkaConsumer)
           (org.apache.kafka.common TopicPartition)))

; TODO: env
(def brokers {"bootstrap.servers" "localhost:9092"})

; TODO: use Byte serialization
(def marshalling-options {"key.serializer"     IntegerSerializer
                          "value.serializer"   StringSerializer
                          "key.deserializer"   IntegerDeserializer
                          "value.deserializer" StringDeserializer})

(def subscriber-options {"enable.auto.commit" "false"})

(def CONSUME_LATEST -1)

(def proxy-group (str "kafka-proxy-" (java.util.UUID/randomUUID)))

(defn topic-consumer
  "Obtain an appropriately configured kafka consumer that is ready to poll"
  ([topic]
   (topic-consumer topic CONSUME_LATEST))
  ([topic offset]
   {:pre [(or (= offset CONSUME_LATEST) (>= offset 0))]}
   (let [group-id {"group.id" (str proxy-group "-" (rand))}
         consumer (KafkaConsumer. (merge brokers marshalling-options subscriber-options group-id))]

     (if (= offset CONSUME_LATEST)
       (.subscribe consumer [topic])
       (let [partition (TopicPartition. topic 0)]
         (.assign consumer [partition])                     ; TODO: need to override the group.id here?
         (.seek consumer partition offset)))

     consumer)))

(defn topic-into-channel
  "Place data from the topic into a channel"
  [consumer ch]
  (go-loop
    []
    (if-let [records (.poll consumer 100)]
      (doseq [record records]
        (>! ch record)))
    (recur)))

(defn consume-data
  [ch]
  (go-loop
    []
    (if-let [data (<! ch)]
      (do (clojure.pprint/pprint data)
          (recur)))))

;------------------------------------------------------------------------
; Short hand for experimentation in the REPL
;------------------------------------------------------------------------

(comment
  
  (def latest-data-consumer (topic-consumer "simple-proxy-topic"))

  (def ldc-dc (chan))

  (consume-data ldc-dc)

  (topic-into-channel latest-data-consumer ldc-dc)          ; should see no topic entries


  (def earliest-data-consumer (topic-consumer "simple-proxy-topic" 0))

  (def edc-dc (chan))

  (consume-data edc-dc)

  (topic-into-channel earliest-data-consumer edc-dc)        ; should see all topic entries


  (def producer (KafkaProducer. (merge brokers marshalling-options)))

  (defn produce
    [topic k v]
    (.send producer (ProducerRecord. topic k v)))

  (let [rando (rand-int 1000)]
    (produce "simple-proxy-topic" rando (str "Message for KEY: " rando)))

  ; both consumers should report a new item

  )

