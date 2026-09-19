(ns maritime.main
  (:gen-class)
  (:require [clojure.pprint :as pprint]
            [maritime.domain :as domain]
            [maritime.physics :as physics]
            [maritime.routing :as routing]))

(def demo-weather
  {:wind-speed-ms 10.0
   :wind-direction-deg 20.0
   :current-speed-ms 0.2
   :current-direction-deg 90.0})

(defn weighted-edge [to distance heading]
  (let [metrics (physics/edge-cost domain/default-ship demo-weather
                                   {:distance-m distance :heading-deg heading}
                                   {:mode :time})]
    {:to to
     :weight (physics/scale-cost (:cost metrics) 1000)
     :metrics metrics}))

(defn -main [& _]
  (let [graph (domain/graph
               5
               {0 [(weighted-edge 1 12000.0 40.0)
                   (weighted-edge 2 8000.0 70.0)]
                1 [(weighted-edge 4 18000.0 80.0)]
                2 [(weighted-edge 3 7000.0 75.0)]
                3 [(weighted-edge 4 9000.0 85.0)]})
        result (routing/shortest-path routing/reference-engine graph 0 4)]
    (println "Weather-aware maritime routing demo")
    (pprint/pprint result)))

