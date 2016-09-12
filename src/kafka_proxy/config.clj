(ns kafka-proxy.config
  (:require [environ.core :refer [env]]))

(def CONSUME_LATEST -1)

(defn env-or-fail
  [env-var-name]
  (if-let [env-var (env env-var-name)]
    env-var
    (throw (RuntimeException. (str "Must set environment variable: " env-var-name)))))

(defn env-or-default [env-var-name default]
  (if-let [env-var (env env-var-name)] env-var default))