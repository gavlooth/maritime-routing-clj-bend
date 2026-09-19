(ns maritime.domain-test
  (:require [clojure.test :refer [deftest is testing]]
            [maritime.domain :as domain]))

(deftest graph-validation
  (testing "normalizes missing adjacency rows"
    (is (= [] (get-in (domain/graph 2 {0 [{:to 1 :weight 3}]})
                       [:adjacency 1]))))
  (testing "rejects invalid endpoints and negative weights"
    (is (thrown? clojure.lang.ExceptionInfo
                 (domain/graph 2 {0 [{:to 2 :weight 1}]})))
    (is (thrown? clojure.lang.ExceptionInfo
                 (domain/graph 2 {0 [{:to 1 :weight -1}]})))))

