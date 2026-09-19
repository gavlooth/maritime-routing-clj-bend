(ns maritime.http
  (:require [jsonista.core :as json]
            [maritime.bend :as bend]
            [maritime.routes :as routes]
            [reitit.ring :as ring]
            [ring.middleware.params :refer [wrap-params]]))

(def mapper
  (json/object-mapper {:encode-key-fn name}))

(defn json-response
  ([status body]
   {:status status
    :headers {"Content-Type" "application/json; charset=utf-8"}
    :body (json/write-value-as-string body mapper)}))

(defn security-headers [handler]
  (fn [request]
    (let [response (handler request)]
      (update response :headers merge
              {"Content-Security-Policy"
               (str "default-src 'self'; script-src 'self'; style-src 'self'; "
                    "img-src 'self' data: https://*.tile.openstreetmap.org; "
                    "connect-src 'self'; font-src 'self'; object-src 'none'; "
                    "base-uri 'none'; frame-ancestors 'none'")
               "Cross-Origin-Opener-Policy" "same-origin"
               "Referrer-Policy" "no-referrer"
               "X-Content-Type-Options" "nosniff"
               "X-Frame-Options" "DENY"}))))

(defn route-objective [request]
  (let [objective (get-in request [:query-params "objective"] "time")]
    (when (routes/objectives objective) objective)))

(defn route-detail [request]
  (let [id (get-in request [:path-params :id])
        objective (route-objective request)]
    (cond
      (nil? objective)
      (json-response 400 {:error "invalid-objective"
                          :allowed (sort routes/objectives)})

      :else
      (if-let [route (get routes/routes-by-id id)]
        (json-response 200 (routes/geojson route objective))
        (json-response 404 {:error "route-not-found"})))))

(def api-router
  (ring/router
   [["/api/health"
     {:get (fn [_]
             (json-response 200 {:status "ok"
                                 :service "maritime-routing"
                                 :bend (bend/engine-descriptor)}))}]
    ["/api/routes"
     {:get (fn [_]
             (json-response 200 {:routes (routes/summaries)
                                 :objectives (sort routes/objectives)
                                 :demo true}))}]
    ["/api/routes/:id"
     {:get route-detail}]]))

(def static-handler
  (ring/routes
   (ring/create-resource-handler {:path "/" :root "public"})
   (ring/create-default-handler
    {:not-found (constantly (json-response 404 {:error "not-found"}))
     :method-not-allowed (constantly (json-response 405 {:error "method-not-allowed"}))
     :not-acceptable (constantly (json-response 406 {:error "not-acceptable"}))})))

(def app
  (-> (ring/ring-handler api-router static-handler)
      wrap-params
      security-headers))

