(ns maritime.server
  (:gen-class)
  (:require [maritime.http :as http]
            [org.httpkit.server :as server]))

(defonce server-instance (atom nil))

(defn stop! []
  (when-let [stop-fn @server-instance]
    (stop-fn :timeout 100)
    (reset! server-instance nil)))

(defn start!
  ([] (start! 8080))
  ([port]
   (stop!)
   (reset! server-instance
           (server/run-server #'http/app
                              {:ip "127.0.0.1"
                               :port port}))
   (println (str "Maritime routing listening on http://127.0.0.1:" port))
   @server-instance))

(defn -main [& [port]]
  (start! (if port (Integer/parseInt port) 8080)))
