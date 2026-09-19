(ns maritime.test-runner
  (:gen-class)
  (:require [clojure.test :as test]
            maritime.domain-test
            maritime.http-test))

(defn -main [& _]
  (let [{:keys [fail error]}
        (test/run-tests 'maritime.domain-test
                        'maritime.http-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
