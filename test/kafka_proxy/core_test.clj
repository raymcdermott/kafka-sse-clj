(ns kafka-proxy.core-test
  (:require [clojure.test :refer :all]
            [kafka-proxy.kafka-sse :refer :all])
  (:import (org.apache.kafka.clients.producer KafkaProducer ProducerRecord)))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))


;------------------------------------------------------------------------
; Short hand for experimentation in the REPL
;------------------------------------------------------------------------

; TODO write tests to check the basics

(comment

  ; TODO: env for the web server
  (def server (http/start-server handler {:port 10000}))

  (def producer (KafkaProducer. (merge kafka-brokers marshalling-options)))

  (defn produce
    [topic k v]
    (.send producer (ProducerRecord. topic k v)))

  (let [rando (rand-int 1000)]
    (produce "simple-proxy-topic" "rando-event" (str "{\"id\" " rando " \"message\" \"Hello SSE\"}")))

  )

