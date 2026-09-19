(ns maritime.physics-test
  (:require [clojure.test :refer [deftest is testing]]
            [maritime.domain :as domain]
            [maritime.physics :as physics]))

(deftest weather-costs
  (is (zero? (physics/wind->beaufort 0.0)))
  (is (< 4.0 (physics/wind->beaufort 10.0) 6.0))
  (is (= 20.0 (physics/relative-angle 350.0 10.0)))
  (testing "distance increases time under otherwise identical conditions"
    (let [weather {:wind-speed-ms 5.0 :wind-direction-deg 0.0}
          short (physics/edge-cost domain/default-ship weather
                                   {:distance-m 1000.0 :heading-deg 0.0}
                                   {:mode :time})
          long (physics/edge-cost domain/default-ship weather
                                  {:distance-m 2000.0 :heading-deg 0.0}
                                  {:mode :time})]
      (is (pos? (:cost short)))
      (is (< (:cost short) (:cost long))))))

