(ns kafka-proxy.core-test
  (:require [clojure.test :refer :all]
            [environ.core :refer [env]]
            [kafka-proxy.kafka-sse :refer :all])
  (:import (org.apache.kafka.clients.producer KafkaProducer ProducerRecord)
           (org.apache.kafka.common.serialization StringSerializer StringDeserializer)))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))


;------------------------------------------------------------------------
; Short hand for experimentation in the REPL
;------------------------------------------------------------------------

; TODO write tests to check the basics

(def ^:private local-brokers {"bootstrap.servers" "localhost:9092"})

(def ^:private brokers-from-env (and (env :kafka-proxy-broker-url)
                                     {"bootstrap.servers" (env :kafka-proxy-broker-url)}))

(def ^:private kafka-brokers (or brokers-from-env local-brokers))

(def ^:private marshalling-config {"key.serializer"     StringSerializer
                                   "value.serializer"   StringSerializer
                                   "key.deserializer"   StringDeserializer
                                   "value.deserializer" StringDeserializer})


(comment

  ; TODO: env for the web server
  (def server (http/start-server handler {:port 10000}))

  (def producer (KafkaProducer. (merge kafka-brokers marshalling-config)))

  (defn produce
    [topic k v]
    (.send producer (ProducerRecord. topic k v)))

  (map (fn [_] (let [rando (rand-int 1000)]
                 (produce "simple-proxy-topic" "rando-non-event" (str "{\"id\" " rando " \"message\" \"Hello SSE\"}")))) (range 10))

  )

