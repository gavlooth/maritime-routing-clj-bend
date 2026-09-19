(ns maritime.routing-test
  (:require [clojure.test :refer [deftest is testing]]
            [maritime.domain :as domain]
            [maritime.routing :as routing]))

(deftest reference-dijkstra
  (let [g (domain/graph 5
                        {0 [{:to 1 :weight 10} {:to 2 :weight 3}]
                         1 [{:to 3 :weight 2}]
                         2 [{:to 1 :weight 4} {:to 3 :weight 8}]
                         3 [{:to 4 :weight 1}]})]
    (is (= {:status :ok :engine :reference :cost 10 :path [0 2 1 3 4]
            :relaxations 6}
           (routing/shortest-path routing/reference-engine g 0 4)))
    (testing "unreachable target is explicit"
      (is (= :unreachable
             (:status (routing/shortest-path routing/reference-engine g 4 0)))))))

