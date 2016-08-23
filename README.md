#Purpose

A Kafka HTTP Proxy that supports putting and getting data from Kafka using
- POST for adding data to a topic
- Server Sent Events to obtain streamed results from a topic

#Implementation
The proxy is written in Clojure and can be deployed as is via Docker or Heroku

#Benefits of Clojure
- Transforms / filters can be provided by a transducer function
- New data items can be checked against a core.spec

#Output Messages
Output of the event complies to the eventsource spec and has these semantics:

- id (item offset in kafka topic)
- event (the message key)
- data (the message value)

It is essential that messages placed on the Kafka topic comply with these semantics.

By default the output will also include
- retry (milliseconds after a dropped connection, that the client should wait before retry)

This can be fine-tuned or turned off via configuration

#Operations
Environment variables are used for basic configuration and to influence some aspects of its behaviour.

#Testing
A simple EventSource client is provided for testing purposes