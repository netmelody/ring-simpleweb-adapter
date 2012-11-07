(ns ring.adapter.simpleweb
  "Adapter for the simpleframework web server."
  (:import (org.simpleframework.http.core Container)
           (org.simpleframework.transport.connect Connection SocketConnection)
           (org.simpleframework.http Response Request)
           (java.net InetSocketAddress SocketAddress)
           (java.io PrintStream)))

(defn build-request-map [request]
  {
;   :remote-addr (-> request .getClientAddress .getHostAddress)
;   :server-port        (.getPort uri)
;   :server-name        (.getHost uri)
   :uri                (-> request .getPath .toString)
   :query-string       (-> request .getQuery .toString)
   :request-method     (-> request .getMethod .toLowerCase keyword)
;   :headers            {}
   :content-type       (-> request .getContentType str)
   :content-length     (-> request .getContentLength)
;   :character-encoding (-> request .getContentType .getCharset)
   :body               (-> request .getContent)
   })

(defn write-response [^Response response response-map]
  (let [body (.getPrintStream response)]
    (.set response "Content-Type", "text/plain")
    (.set response "Server", "HelloWorld/1.0 (Simple 4.0)")
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

