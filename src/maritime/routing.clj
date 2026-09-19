(ns maritime.routing)

(defprotocol RoutingEngine
  (shortest-path [engine graph source target]
    "Returns {:status :ok, :cost n, :path [...]} or {:status :unreachable}."))

(defn- reconstruct-path [previous source target]
  (loop [node target path ()]
    (cond
      (= node source) (vec (cons source path))
      (nil? node) nil
      :else (recur (get previous node) (cons node path)))))

(defrecord ReferenceEngine []
  RoutingEngine
  (shortest-path [_ {:keys [node-count adjacency]} source target]
    (when-not (and (<= 0 source) (< source node-count)
                   (<= 0 target) (< target node-count))
      (throw (ex-info "source or target is outside graph"
                      {:source source :target target :node-count node-count})))
    (loop [queue (sorted-set [0 source])
           distances {source 0}
           previous {}
           relaxations 0]
      (if-let [[distance node] (first queue)]
        (let [queue (disj queue [distance node])]
          (cond
            (not= distance (get distances node))
            (recur queue distances previous relaxations)

            (= node target)
            {:status :ok
             :engine :reference
             :cost distance
             :path (reconstruct-path previous source target)
             :relaxations relaxations}

            :else
            (let [[q ds ps rs]
                  (reduce
                   (fn [[q ds ps rs] {:keys [to weight]}]
                     (let [candidate (+ distance weight)]
                       (if (< candidate (get ds to Long/MAX_VALUE))
                         [(conj q [candidate to])
                          (assoc ds to candidate)
                          (assoc ps to node)
                          (inc rs)]
                         [q ds ps rs])))
                   [queue distances previous relaxations]
                   (get adjacency node))]
              (recur q ds ps rs))))
        {:status :unreachable
         :engine :reference
         :relaxations relaxations}))))

(def reference-engine (->ReferenceEngine))

