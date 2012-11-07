(ns ring.adapter.simpleweb
  "Adapter for the simpleframework web server."
  (:import (org.simpleframework.http Status)
           (org.simpleframework.http.core Container)
           (org.simpleframework.transport.connect Connection SocketConnection)
           (org.simpleframework.http Response Request)
           (java.net InetSocketAddress SocketAddress)
           (java.io PrintStream)))

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

(defn write-response [^Response response {:keys [status headers body]}]
  (let [body (.getPrintStream response)]
    (when status
      (.setCode response status)
      (.setText response (Status/getDescription status)))
    
    (set-headers response headers)
    (.print body "Hello World")
    (.close body)))

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

