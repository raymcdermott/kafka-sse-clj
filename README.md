#Purpose

A minimal approach (a small, single function) to support Server-Sent events from Kafka using a `ring` compliant web server.

The defaults can be tweaked by code or configuration.

The function `kafka->sse-ch` will return a channel that has mapping from the data on a Kafka channel to SSE.

You can use that channel in a variety of web servers and I have provided a simple example using `Aleph` and `Compojure`.

```clojure
kafka->sse-ch [request topic]
```

In this form you provide the `ring request` and the name of the topic from which to consume.

The function will use the `filter[event]` request parameter to perform filtering on event name:

```
http://server-name/sse?filter[event]=customer
```

Regular expressions are also supported in a comma separated list:

```
http://server-name/sse?filter[event]=customer,product.*
```

#Output Messages
The default output of the channel complies to the HTML5 `EventSource` spec and has these semantics:

- id (item offset in kafka topic)
- event (the message key as a string)
- data (the message value as a string)

```
id: 556
event: customer
data: {"id" 745 "message" "Hello SSE"}

id: 557
event: product-campaign
data: {"id" 964 "message" "Hello SSE"}
```

Of course, to use these defaults, messages placed on the Kafka topic must comply with these semantics.

#Further composition

A transducer can be provided to modify how the data on the channel is processed.

```clojure
kafka->sse-ch [request topic transducer]
```

This means that you can use the same core function but provide your own filtering and mapping.


#Operations
The table shows the supported environment variables and defaults.

| Environment Variable | Meaning | Default |
| ---------------------| ------- | --------|
| Content Cell         | Content | default |
| Content Cell         | Content | default |

#Keep Alive
By default an SSE comment will be sent every few seconds to maintain the connection.


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



