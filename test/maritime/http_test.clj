(ns maritime.http-test
  (:require [clojure.test :refer [deftest is testing]]
            [jsonista.core :as json]
            [maritime.http :as http]
            [ring.mock.request :as mock]))

(defn response-json [response]
  (json/read-value (:body response) json/keyword-keys-object-mapper))

(deftest route-api
  (testing "lists curated routes"
    (let [response (http/app (mock/request :get "/api/routes"))
          body (response-json response)]
      (is (= 200 (:status response)))
      (is (= 4 (count (:routes body))))
      (is (= true (:demo body)))
      (is (= "nosniff" (get-in response [:headers "X-Content-Type-Options"])))))
  (testing "returns GeoJSON with a validated objective"
    (let [response (http/app (mock/request :get "/api/routes/piraeus-mykonos?objective=safety"))
          body (response-json response)]
      (is (= 200 (:status response)))
      (is (= "Feature" (:type body)))
      (is (= "LineString" (get-in body [:geometry :type])))
      (is (= "safety" (get-in body [:properties :objective])))))
  (testing "rejects arbitrary objective values"
    (let [response (http/app (mock/request :get "/api/routes/piraeus-mykonos?objective=anything"))]
      (is (= 400 (:status response)))
      (is (= "invalid-objective" (:error (response-json response))))))
  (testing "does not interpolate route IDs into paths or files"
    (is (= 404 (:status (http/app (mock/request :get "/api/routes/..%2F..%2Fetc%2Fpasswd")))))))

(deftest static-page
  (let [response (http/app (mock/request :get "/"))]
    (is (= 200 (:status response)))
    (is (= "DENY" (get-in response [:headers "X-Frame-Options"])))
    (is (re-find #"Pelagos" (slurp (:body response))))))

