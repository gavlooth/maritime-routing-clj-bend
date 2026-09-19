(ns maritime.domain)

(defn graph
  "Validates graph identifiers at the Clojure boundary. It does not compute
  weights or perform graph algorithms; those responsibilities belong to Bend."
  [node-count adjacency]
  (when-not (and (integer? node-count) (pos? node-count))
    (throw (ex-info "node-count must be positive" {:node-count node-count})))
  (doseq [[from edges] adjacency
          {:keys [to]} edges]
    (when-not (and (integer? from) (<= 0 from) (< from node-count)
                   (integer? to) (<= 0 to) (< to node-count))
      (throw (ex-info "edge references an invalid node"
                      {:from from :to to :node-count node-count})))
    (when-not (map? (first (filter #(= to (:to %)) edges)))
      (throw (ex-info "edge must be a map" {:from from :to to}))))
  {:node-count node-count
   :adjacency (into {} (map (fn [node] [node (vec (get adjacency node []))])
                            (range node-count)))})
