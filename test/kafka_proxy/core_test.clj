(ns kafka-proxy.core-test
  (:require [clojure.test :refer :all]
            [environ.core :refer [env]]
            [kafka-proxy.kafka-sse :refer [kafka->sse-ch]])
  (:import (org.apache.kafka.clients.producer KafkaProducer ProducerRecord)
           (org.apache.kafka.common.serialization StringSerializer StringDeserializer))
  (:use [clojure.java.io :only (file)]))

;------------------------------------------------------------------------
; Short hand for experimentation in the REPL
;------------------------------------------------------------------------

; TODO write tests to check the basics

(def ^:private embedded-broker {"host.name"         "localhost"
                                "port"              "9091"
                                "brokerid"          "0"
                                "zookeeper.connect" "127.0.0.1:9090"
                                "enable.zookeeper"  "false"
                                "log.dir"           "/tmp/kafka-logs/"})


(comment

  (def ^:private marshalling-config {"key.serializer"     StringSerializer
                                     "value.serializer"   StringSerializer
                                     "key.deserializer"   StringDeserializer
                                     "value.deserializer" StringDeserializer})

  ; TODO: env for the web server
  (def server (http/start-server handler {:port 10000}))

  (def producer (KafkaProducer. (merge kafka-brokers marshalling-config)))

  (defn produce
    [topic k v]
    (.send producer (ProducerRecord. topic k v)))

  (map (fn [_] (let [rando (rand-int 1000)]
                 (produce "simple-proxy-topic" "rando-non-event" (str "{\"id\" " rando " \"message\" \"Hello SSE\"}")))) (range 10))

  )

