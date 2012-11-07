(ns ring.adapter.simpleweb
  "Adapter for the simpleframework web server."
  (:require [clojure.java.io :as io])
  (:import (org.simpleframework.http Status)
           (org.simpleframework.http.core Container)
           (org.simpleframework.transport.connect Connection SocketConnection)
           (org.simpleframework.http Response Request)
           (java.net InetSocketAddress SocketAddress)
           (java.io PrintStream File InputStream FileInputStream)))

(defn build-request-map [request]
  {
;   :server-port        (.getPort uri)
;   :server-name        (.getHost uri)
;   :remote-addr (-> request .getClientAddress .getHostAddress)
   :uri                (-> request .getPath .toString)
   :query-string       (-> request .getQuery .toString)
;   :scheme             ""
   :request-method     (-> request .getMethod .toLowerCase keyword)
;   :headers            {}
   :content-type       (-> request .getContentType str)
   :content-length     (-> request .getContentLength)
;   :character-encoding (-> request .getContentType .getCharset)
;   :ssl-client-cert    nil
   :body               (-> request .getContent)
   })

(defn set-headers
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

(defn write-response [^Response response {:keys [status headers body]}]
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
        (when response-map
          (write-response response response-map))
        (.close response)))))

(defn ^Connection run-simpleweb
  "Start a simpleframework web server to serve the given handler according to the supplied options:
    :port - the port to listen on (defaults to 8181)"
  [handler {:keys [port]}]
  (let [container (proxy-handler handler)
        ^Connection connection (SocketConnection. container)
        ^SockectAddress address (InetSocketAddress. (or port 8181))]
    (.connect connection address)
    connection))

