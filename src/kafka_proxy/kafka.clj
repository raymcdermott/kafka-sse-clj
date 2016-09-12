(ns kafka-proxy.kafka
  (:require [environ.core :refer [env]]
            [kafka-proxy.config :as config])
  (:import (org.apache.kafka.common.serialization StringSerializer StringDeserializer)
           (org.apache.kafka.clients.consumer KafkaConsumer)
           (org.apache.kafka.common TopicPartition)
           (java.util UUID)
           (org.apache.kafka.clients CommonClientConfigs)))

(def ^:private local-brokers {CommonClientConfigs/BOOTSTRAP_SERVERS_CONFIG "localhost:9092"})

(def ^:private brokers-from-env (if-let [u (env :sse-proxy-kafka-broker-url)]
                                  {CommonClientConfigs/BOOTSTRAP_SERVERS_CONFIG u}))

(def ^:private kafka-brokers (or brokers-from-env local-brokers))

(def ^:private marshalling-config {"key.serializer"     StringSerializer
                                   "value.serializer"   StringSerializer
                                   "key.deserializer"   StringDeserializer
                                   "value.deserializer" StringDeserializer})

(def ^:private autocommit-config {"enable.auto.commit" "false"})

(def ^:private proxy-group (str "kafka-proxy-" (UUID/randomUUID)))

(defn sse-consumer
  "Obtain an appropriately positioned kafka consumer that is ready to be polled"
  ([topic offset]
   (sse-consumer topic offset kafka-brokers))

  ([topic offset brokers]
   (sse-consumer topic offset brokers autocommit-config))

  ([topic offset brokers options]
   (sse-consumer topic offset brokers options marshalling-config))

  ([topic offset brokers options marshallers]
   {:pre [(or (= offset config/CONSUME_LATEST) (>= offset 0))]}
   (let [consumer-group {"group.id" (str proxy-group "-" (rand))}
         merged-options (merge options autocommit-config consumer-group)
         consumer (KafkaConsumer. (merge brokers marshallers merged-options))]

     (if (= offset CONSUME_LATEST)
       (.subscribe consumer [topic])
       (let [partition (TopicPartition. topic 0)]
         (.assign consumer [partition])
         (.seek consumer partition offset)))

     consumer)))