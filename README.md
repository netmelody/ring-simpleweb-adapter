# ring-simpleweb-adapter

A [ring](https://github.com/ring-clojure/ring) adapter for the [Simple Http Engine](http://www.simpleframework.org/), commonly referred to as SimpleWeb.

## Motivation

Simple provides a pure-Java HTTP server without using Servlets, and vastly outperforms most popular Java based servers (see [benchmarks](http://www.simpleframework.org/performance/comparison.php)).

This adapter aims to allow ring-based clojure applications to quicky transition to SimpleWeb and experience the resulting performance improvements, as well as a reduction in artifact size.

## Availability

The adapter is distributed via [Clojars](http://clojars.org/ring-simpleweb-adapter) and can be included in your [leiningen](http://leiningen.org/) `project.clj` with:

```clojure
[ring-simpleweb-adapter "0.2.0"]
```

## Usage

```clj
(use 'ring.adapter.simpleweb)

;; Define a basic ring handler
(defn handler [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Hello world from SimpleWeb"})

;; Start the server on the specified port, returning a connection
(def connection (run-simpleweb handler {:port 8080}))

;; To stop the server, invoke .close method on the returned connection.
(.close connection)
```

## Configuration

The `run-simpleweb` function accepts a ring `handler` and an `options` map. The supported options are as follows (with the specified default values):

```clj
{
 :port          8181 ;; the port to listen on
 :max-threads   50   ;; the maximum number of threads to use"
}
```

## License

Copyright © 2012 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
