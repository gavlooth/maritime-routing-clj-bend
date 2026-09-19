(ns maritime.domain)

(def default-ship
  {:design-speed-knots 15.0
   :waterline-length-m 200.0
   :displacement-m3 30000.0
   :block-coefficient 0.65
   :condition :normal
   :fuel-rate-base-kg-h 50.0
   :fuel-rate-per-knot-kg-h 5.0})

(defn graph
  "Creates and validates a directed adjacency map. Edges require :to and :weight."
  [node-count adjacency]
  (when-not (and (integer? node-count) (pos? node-count))
    (throw (ex-info "node-count must be positive" {:node-count node-count})))
  (doseq [[from edges] adjacency
          {:keys [to weight]} edges]
    (when-not (and (integer? from) (<= 0 from) (< from node-count)
                   (integer? to) (<= 0 to) (< to node-count))
      (throw (ex-info "edge references an invalid node"
                      {:from from :to to :node-count node-count})))
    (when-not (and (number? weight) (not (neg? weight)))
      (throw (ex-info "edge weight must be non-negative"
                      {:from from :to to :weight weight}))))
  {:node-count node-count
   :adjacency (into {} (map (fn [node] [node (vec (get adjacency node []))])
                            (range node-count)))})

