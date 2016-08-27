#Purpose

A minimal HTTP lib that supports Server-Sent events from Kafka

#Implementation
The proxy is written in Clojure and can be deployed via Docker or Heroku

#Benefits of Clojure
- Transforms / filters can be provided by a transducer function
- New data items can be checked against a core.spec
- High degree of composability on top of some sane defaults

#Output Messages
Output of the event complies to the eventsource spec and has these semantics:

- id (item offset in kafka topic)
- event (the message key)
- data (the message value)

To use the defaults, messages placed on the Kafka topic must comply with these semantics.

#Composable
Hopefully the defaults will be useful for some people.

If not, it's fully composable to allow you to vary message semantics, filters and output with functions.


#Operations
Environment variables are used for basic configuration and to influence some aspects of its behaviour.

#Testing

working on it with embedded K / ZK

#Example (using aleph)

```clojure
(defn sse-handler-using-defaults
  "Stream SSE data from the Kafka topic"
  [request]
  (let [topic (get (:params request) "topic" "default-topic")
        ch (sse/kafka->sse-ch request topic)]
    {:status  200
     :headers {"Content-Type"  "text/event-stream;charset=UTF-8"
               "Cache-Control" "no-cache"}
     :body    (s/->source ch)}))


(def handler
  (params/wrap-params
    (compojure/routes
      (GET "/kafka-sse" [] sse-handler-using-defaults)
      (route/not-found "No such page."))))
```



