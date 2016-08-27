(ns kafka-proxy.heroku-kafka
  (:require
    [environ.core :refer [env]]))

; TODO Heroku deploy: connect using template from https://github.com/heroku/heroku-kafka-demo-java

(def broker-url (env :kafka-url))
(def trusted-cert (env :kafka-trusted-cert))
(def client-cert (env :kafka-client-cert))
(def client-cert-key (env :kafka-client-cert-key))

; This is only an option for Heroku Private Spaces (HPS)
(def hps-broker-url (env :kafka-plaintext-url))

; Assume we are on Heroku if all are available, favour HPS config if it is set
(def brokers (and client-cert-key client-cert trusted-cert broker-url
                  {"bootstrap.servers" (or hps-broker-url broker-url)}))



