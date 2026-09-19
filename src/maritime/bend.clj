(ns maritime.bend
  "Process supervision boundary for the Bend-owned physics and routing engine."
  (:require [clojure.java.io :as io]))

(def default-command ["build/maritime-bend" "--threads" "2"])

(defn engine-available?
  ([] (engine-available? default-command))
  ([[program & _]]
   (.canExecute (io/file program))))

(defn engine-descriptor
  "Describes the configured engine without reproducing any Bend computation."
  ([] (engine-descriptor default-command))
  ([command]
   {:owner :bend
    :command command
    :available? (engine-available? command)
    :responsibilities #{:ship-physics
                        :weather-costs
                        :objective-functions
                        :edge-weighting
                        :shortest-path
                        :parallel-routing}}))

