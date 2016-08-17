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
  "Obtain an appropriately positioned kafka consumer that is ready to be polled"
  ([topic]
   (topic-consumer topic CONSUME_LATEST))
  ([topic offset]
   {:pre [(or (= offset CONSUME_LATEST) (>= offset 0))]}
   (let [group-id {"group.id" (str proxy-group "-" (rand))}
         consumer (KafkaConsumer. (merge brokers marshalling-options subscriber-options group-id))]

     (if (= offset CONSUME_LATEST)
       (.subscribe consumer [topic])
       (let [partition (TopicPartition. topic 0)]
         (.assign consumer [partition])
         (.seek consumer partition offset)))

     consumer)))

(defn kafka-proxy-handler
  [{:keys [params]}]
  (let [consumer (topic-consumer "simple-proxy-topic" 0)    ; get offset
        kafka-ch (chan)]
    (go-loop []
      (if-let [records (.poll consumer 100)]
        (doseq [record records]
          (>! kafka-ch (str "id: " (.key record) "\ndata:" (.value record) "\n\n"))))
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

;(def server (http/start-server handler {:port 10000}))


;------------------------------------------------------------------------
; Short hand for experimentation in the REPL
;------------------------------------------------------------------------

(comment

  (defonce server (http/start-server handler {:port 8080}))

  (def latest-data-consumer (topic-consumer "simple-proxy-topic"))

  ;  (def ldc-dc (chan))

  ;  (consume-data ldc-dc)

  (topic-into-channel latest-data-consumer kafka-ch)        ; should see no topic entries


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

