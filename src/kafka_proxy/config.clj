(ns kafka-proxy.config
  (:require [environ.core :refer [env]]))

(defn env-or-default [env-var-name default]
  (if-let [env-var (env env-var-name)] env-var default))