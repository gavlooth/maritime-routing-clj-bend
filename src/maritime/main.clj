(ns maritime.main
  (:gen-class)
  (:require [clojure.pprint :as pprint]
            [maritime.bend :as bend]
            [maritime.domain :as domain]))

(defn -main [& _]
  (let [topology (domain/graph
                  5
                  {0 [{:to 1 :distance-m 12000.0 :heading-deg 40.0}
                      {:to 2 :distance-m 8000.0 :heading-deg 70.0}]
                   1 [{:to 4 :distance-m 18000.0 :heading-deg 80.0}]
                   2 [{:to 3 :distance-m 7000.0 :heading-deg 75.0}]
                   3 [{:to 4 :distance-m 9000.0 :heading-deg 85.0}]})]
    (println "Clojure orchestration boundary; physics and routing are Bend-owned.")
    (pprint/pprint {:topology topology
                    :engine (bend/engine-descriptor)})))
