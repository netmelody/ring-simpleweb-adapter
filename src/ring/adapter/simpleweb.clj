(ns ring.adapter.simpleweb
  "Adapter for the simpleframework web server."
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import (org.simpleframework.http Status)
           (org.simpleframework.http.core ContainerServer Container)
           (org.simpleframework.transport.connect Connection SocketConnection)
           (org.simpleframework.http Response Request)
           (java.net InetSocketAddress SocketAddress)
           (java.io PrintStream File InputStream FileInputStream)))

(defn- get-headers
  "Creates a name/value map of all the request headers."
  [^Request request]
  (reduce
    (fn [headers, ^String name] (assoc headers (.toLowerCase name) (string/join ", " (.getValues request name))))
    {}
    (.getNames request)))

(defn- build-request-map [^Request request]
  (let [content-type (-> request .getContentType)
        [host port] (-> request (.getValue "Host") (.split ":"))]
    {
     :server-port        (if port (Integer/valueOf port) nil)
     :server-name        host
     :remote-addr        (-> request .getClientAddress .getAddress .getHostAddress)
     :uri                (-> request .getPath .toString)
     :query-string       (-> request .getQuery .toString)
     :scheme             :http
     :request-method     (-> request .getMethod .toLowerCase keyword)
     :headers            (get-headers request)
     :content-type       (if (nil? content-type) nil (.toString content-type))
     :content-length     (-> request .getContentLength)
     :character-encoding (if (nil? content-type) nil (.getCharset content-type))
     :ssl-client-cert    nil
     :body               (-> request .getInputStream)
    }))

(defn- set-headers
  "Update a simpleweb Response with a map of headers."
  [^Response response, headers]
  (doseq [[key val-or-vals] headers]
    (if (string? val-or-vals)
      (.set response ^String key ^String val-or-vals)
      (doseq [val val-or-vals]
        (.add response ^String key ^String val)))))

(defn- set-body
  "Update a simpleweb Response body with a String, ISeq, File or InputStream."
  [^Response response, body]
  (cond
    (string? body)
      (with-open [writer (.getPrintStream response)]
        (.print writer body))
    (seq? body)
      (with-open [writer (.getPrintStream response)]
        (doseq [chunk body]
          (.print writer (str chunk))
          (.flush writer)))
    (instance? InputStream body)
      (with-open [^InputStream b body]
        (io/copy b (.getOutputStream response)))
    (instance? File body)
      (let [^File f body]
        (with-open [stream (FileInputStream. f)]
          (set-body response stream)))
    (nil? body)
      nil
    :else
      (throw (Exception. ^String (format "Unrecognized body: %s" body)))))

(defn- write-response [^Response response {:keys [status headers body]}]
  (when status
    (.setCode response status)
    (.setText response (Status/getDescription status)))
  (set-headers response headers)
  (set-body response body))

(defn- proxy-handler
  "Returns a SimpleWeb Container implementation for the given Ring handler."
  [handler]
  (proxy [Container] []
    (handle [^Request request ^Response response]
      (let [request-map (build-request-map request)
            response-map (handler request-map)]
        (println (:headers request-map)) 
        (when response-map
          (write-response response response-map))
        (.close response)))))

(defn ^Connection run-simpleweb
  "Start a simpleframework web server to serve the given handler according to the supplied options:
    :port - the port to listen on (defaults to 8181)
    :max-threads  - the maximum number of threads to use (default 50)"
  [handler options]
  (let [container (proxy-handler handler)
        ^Connection connection (SocketConnection. (ContainerServer. container (options :max-threads 50)))
        ^SockectAddress address (InetSocketAddress. (or (:port options) 8181))]
    (.connect connection address)
    connection))

