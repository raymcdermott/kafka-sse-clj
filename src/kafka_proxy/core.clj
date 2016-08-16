(ns kafka-proxy.core
  (:require [clojure.core.async :as async :refer [>! <! >!! <!! alts! go go-loop chan onto-chan put! timeout]])
  (:gen-class)
  (:import (org.apache.kafka.common.serialization StringSerializer StringDeserializer IntegerSerializer IntegerDeserializer)
           (org.apache.kafka.clients.producer KafkaProducer ProducerRecord)
           (org.apache.kafka.clients.consumer KafkaConsumer)
           (org.apache.kafka.common TopicPartition)))

; TODO: env
(def brokers {"bootstrap.servers" "localhost:9092"})

(def marshalling-options {"key.serializer"     IntegerSerializer
                          "value.serializer"   StringSerializer
                          "key.deserializer"   IntegerDeserializer
                          "value.deserializer" StringDeserializer})

(def subscriber-options {"group.id",          "kafka-proxy"
                         "enable.auto.commit" "false"})

(defn kafka-topic-consumer
  [topic]
  (doto (KafkaConsumer. (merge brokers marshalling-options subscriber-options))
    (.subscribe [topic])))

(defn topic-into-channel
  "Place data from the topic into a channel"
  ([topic ch]
   (topic-into-channel topic 0 ch))
  ([topic offset ch]
   (let [consumer (kafka-topic-consumer topic)
         partition (TopicPartition. topic 0)
         maybe-data (.poll consumer 100)                    ; assigns the partitions to the subscriber
         seek-effect (.seek consumer partition offset)]     ; position to the offset provided

     (go-loop
       []
       (if-let [records (.poll consumer 100)]
         (doseq [record records]
           (>! ch record)))
       (recur)))))

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
(comment def producer (KafkaProducer. (merge brokers marshalling-options)))

(comment defn produce
         [topic k v]
         (.send producer (ProducerRecord. topic k v)))

