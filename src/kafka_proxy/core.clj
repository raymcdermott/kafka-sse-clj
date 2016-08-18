(ns kafka-proxy.core
  (:require [aleph.http :as http]
            [compojure.core :as compojure :refer [GET]]
            [compojure.route :as route]
            [manifold.stream :as s]
            [ring.middleware.params :as params]
            [clojure.core.async :as async :refer [>! <! go-loop chan close! timeout]])
  (:gen-class)
  (:import (org.apache.kafka.common.serialization StringSerializer StringDeserializer
                                                  IntegerSerializer IntegerDeserializer)
           (org.apache.kafka.clients.producer KafkaProducer ProducerRecord)
           (org.apache.kafka.clients.consumer KafkaConsumer)
           (org.apache.kafka.common TopicPartition)))

(def brokers {"bootstrap.servers" "localhost:9092"})        ; TODO: env


(def marshalling-options {"key.serializer"     IntegerSerializer
                          "value.serializer"   StringSerializer
                          "key.deserializer"   IntegerDeserializer
                          "value.deserializer" StringDeserializer}) ; TODO: use Byte serialization


(def subscriber-options {"enable.auto.commit" "false"})

(def CONSUME_LATEST -1)
(def CONSUME_POLLING_TIMEOUT 100)                           ; TODO enable env parameter (> 0 && <= 1000)
(def KEEP_ALIVE_TIMEOUT 30)                                 ; TODO enable env parameter (> 0 && <= 60)

(def CLIENT_RECONNECTION_TIME 10)                           ; TODO enable env parameter (> 0 && <= 30)

(def proxy-group (str "kafka-proxy-" (java.util.UUID/randomUUID)))

(defn topic-consumer
  "Obtain an appropriately positioned kafka consumer that is ready to be polled"
  ([topic]
   (topic-consumer topic CONSUME_LATEST))
  ([topic offset]
   {:pre [(or (= offset CONSUME_LATEST) (>= offset 0))]}
   (let [group-id {"group.id" (str proxy-group "-" (rand))}
         consumer (KafkaConsumer. (merge brokers marshalling-options subscriber-options group-id))]
     ; TODO handle connection fail (hystrix?)

     (if (= offset CONSUME_LATEST)
       (.subscribe consumer [topic])
       (let [partition (TopicPartition. topic 0)]
         (.assign consumer [partition])
         (.seek consumer partition offset)))

     consumer)))

(defn kafka-proxy-handler
  [request]
  (let [offset (or (get (:headers request) "last-event-id") CONSUME_LATEST)
        consumer (topic-consumer "simple-proxy-topic" offset)
        kafka-ch (chan)]
    (go-loop []
      (let [_ (<! (timeout CLIENT_RECONNECTION_TIME))]
        (>! kafka-ch ":\n")
        (recur)))
    (go-loop []
      (if-let [records (.poll consumer CONSUME_POLLING_TIMEOUT)]
        (doseq [record records]
          (>! kafka-ch (str "id: " (.offset record) "\n"
                            "retry: " CLIENT_RECONNECTION_TIME "\n"
                            "data: key " (.key record) "value" (.value record) "\n\n"))))
      (recur))
    {:status  200
     :headers {"Content-Type"  "text/event-stream;charset=UTF-8"
               "Cache-Control" "no-cache"}
     :body    (s/->source kafka-ch)}))

(def handler
  (params/wrap-params
    (compojure/routes
      (GET "/kafka-sse" [] kafka-proxy-handler)
      (route/not-found "No such page."))))



;------------------------------------------------------------------------
; Short hand for experimentation in the REPL
;------------------------------------------------------------------------

; TODO write tests to check the basics

(comment

  ; REPL 1
  (def server (http/start-server handler {:port 10000}))

  (def latest-data-consumer (topic-consumer "simple-proxy-topic"))

  (def ldc-dc (chan))

  (consume-data ldc-dc)

  (topic-into-channel latest-data-consumer ldc-dc)          ; should see no topic entries


  ; REPL 2
  (def earliest-data-consumer (topic-consumer "simple-proxy-topic" 0))

  (def edc-dc (chan))

  (consume-data edc-dc)

  (topic-into-channel earliest-data-consumer edc-dc)        ; should see all topic entries


  ; REPL 1
  (def producer (KafkaProducer. (merge brokers marshalling-options)))

  (defn produce
    [topic k v]
    (.send producer (ProducerRecord. topic k v)))

  (let [rando (rand-int 1000)]
    (produce "simple-proxy-topic" rando (str "Message for KEY: " rando)))

  ; both consumers should report a new item

  )

