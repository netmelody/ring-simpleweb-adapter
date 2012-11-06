(defproject ring-simpleweb-adapter "0.1.0-SNAPSHOT"
  :description "Ring adapter for the simpleframwork lightweight server"
  :url "http://github.com/netmelody/ring-simpleweb-adapter"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[ring/ring-core "1.1.6"]
                 [org.simpleframework/simple "4.1.21"]]
  :profiles
  {:1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
   :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}})