(ns kafka-proxy.kafka
  (:require [environ.core :refer [env]])
  (:import (org.apache.kafka.common.serialization StringSerializer StringDeserializer)
           (org.apache.kafka.clients.consumer KafkaConsumer)
           (org.apache.kafka.common TopicPartition)
           (java.util UUID)))

(def ^:private local-brokers {"bootstrap.servers" "localhost:9092"})

(def ^:private brokers-from-env (if-let [u (env :sse-proxy-kafka-broker-url)]
                                  {"bootstrap.servers" u}))

(def ^:private kafka-brokers (or brokers-from-env local-brokers))

(def ^:private marshalling-config {"key.serializer"     StringSerializer
                                   "value.serializer"   StringSerializer
                                   "key.deserializer"   StringDeserializer
                                   "value.deserializer" StringDeserializer})

(def ^:private autocommit-config {"enable.auto.commit" "false"})

(def ^:private proxy-group (str "kafka-proxy-" (UUID/randomUUID)))

(def CONSUME_LATEST -1)

(defn sse-consumer
  "Obtain an appropriately positioned kafka consumer that is ready to be polled"
  ([topic offset]
   (sse-consumer topic offset autocommit-config))

  ([topic offset options]
   (sse-consumer topic offset options kafka-brokers))

  ([topic offset options brokers]
   (sse-consumer topic offset options brokers marshalling-config))

  ([topic offset options brokers marshallers]
   {:pre [(or (= offset CONSUME_LATEST) (>= offset 0))]}
   (let [consumer-group {"group.id" (str proxy-group "-" (rand))}
         merged-options (merge options autocommit-config consumer-group)
         consumer (KafkaConsumer. (merge brokers marshallers merged-options))]

     (if (= offset CONSUME_LATEST)
       (.subscribe consumer [topic])
       (let [partition (TopicPartition. topic 0)]
         (.assign consumer [partition])
         (.seek consumer partition offset)))

     consumer)))