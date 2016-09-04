(ns kafka-proxy.producer
  (:require [clojure.core.async :as async :refer [>! <! go-loop chan close! timeout]])
  (:import (org.apache.kafka.clients.producer ProducerRecord KafkaProducer)
           (org.apache.kafka.common.serialization StringDeserializer StringSerializer)))

(def ^:private marshalling-config {"key.serializer"     StringSerializer
                                   "value.serializer"   StringSerializer
                                   "key.deserializer"   StringDeserializer
                                   "value.deserializer" StringDeserializer})

(defn producer [kafka-brokers]
  (KafkaProducer. (merge kafka-brokers marshalling-config)))

(defn kafka-produce
  [producer topic k v]
  (.send producer (ProducerRecord. topic k v)))


(defn produce-constantly!
  [brokers topic]
  (let [producer (producer brokers)
        keep-alive-millis 1000]

    (go-loop [rando 0]
      (let [_ (<! (timeout keep-alive-millis))]
        (kafka-produce producer topic "rando-event" (str "{\"id\" " rando " \"message\" \"Hello SSE\"}"))
        (recur (rand-int 1000))))))