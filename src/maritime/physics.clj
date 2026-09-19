(ns maritime.physics)

(def knots-per-metre-second 1.94384)
(def metres-per-nautical-mile 1852.0)

(defn wind->beaufort [wind-speed-ms]
  (if (pos? wind-speed-ms)
    (Math/pow (/ (double wind-speed-ms) 0.836) (/ 2.0 3.0))
    0.0))

(defn froude-number [speed-knots waterline-length-m]
  (/ (* (double speed-knots) 0.5144)
     (Math/sqrt (* 9.81 (double waterline-length-m)))))

(defn- nearest-coefficient [x]
  (apply min-key #(Math/abs (- (double x) %)) [0.55 0.60 0.65 0.70 0.75 0.80 0.85]))

(defn correction-factor [{:keys [block-coefficient condition design-speed-knots
                                  waterline-length-m]}]
  (let [cb (nearest-coefficient block-coefficient)
        f (froude-number design-speed-knots waterline-length-m)
        [a b c] (get {[:normal 0.55] [1.7 -1.4 -7.4]
                      [:normal 0.60] [2.2 -2.5 -9.7]
                      [:normal 0.65] [2.6 -3.7 -11.6]
                      [:normal 0.70] [3.1 -5.3 -12.4]
                      [:laden 0.75] [2.4 -10.6 -9.5]
                      [:laden 0.80] [2.6 -13.1 -15.1]
                      [:laden 0.85] [3.1 -18.7 28.0]
                      [:ballast 0.75] [2.6 -12.5 -13.5]
                      [:ballast 0.80] [3.0 -16.3 -21.6]
                      [:ballast 0.85] [3.4 -20.9 31.8]}
                     [condition cb])]
    (when-not a
      (throw (ex-info "unsupported block coefficient and condition"
                      {:condition condition :block-coefficient block-coefficient})))
    (+ a (* b f) (* c f f))))

(defn head-weather-loss-fraction [beaufort {:keys [condition displacement-m3]}]
  (let [linear (if (= condition :laden) 0.5 0.7)
        divisor (if (#{:laden :ballast} condition) 2.7 22.0)
        percent (+ (* linear beaufort)
                   (/ (Math/pow beaufort 6.5)
                      (* divisor (Math/pow displacement-m3 (/ 2.0 3.0)))))]
    (/ percent 100.0)))

(defn relative-angle [direction-a direction-b]
  (let [d (mod (Math/abs (- (double direction-a) (double direction-b))) 360.0)]
    (min d (- 360.0 d))))

(defn direction-factor [angle beaufort]
  (cond
    (<= angle 30.0) 1.0
    (<= angle 60.0) (/ (- 1.7 (* 0.03 (Math/pow (- beaufort 4.0) 2.0))) 2.0)
    (<= angle 150.0) (/ (- 0.9 (* 0.06 (Math/pow (- beaufort 6.0) 2.0))) 2.0)
    :else (/ (- 0.4 (* 0.03 (Math/pow (- beaufort 8.0) 2.0))) 2.0)))

(defn edge-cost
  "Returns unscaled time/fuel values and the selected objective cost."
  [ship weather edge {:keys [mode alpha safety-beaufort safety-penalty]
                      :or {mode :time alpha 0.5 safety-beaufort 7.0 safety-penalty 0.0}}]
  (let [bn (wind->beaufort (:wind-speed-ms weather 0.0))
        angle (relative-angle (:wind-direction-deg weather 0.0)
                              (:heading-deg edge))
        loss (* (correction-factor ship)
                (direction-factor angle bn)
                (head-weather-loss-fraction bn ship))
        still-water (* (:design-speed-knots ship) (- 1.0 loss))
        current (* (:current-speed-ms weather 0.0)
                   knots-per-metre-second
                   (Math/cos (Math/toRadians
                              (relative-angle (:current-direction-deg weather 0.0)
                                              (:heading-deg edge)))))
        speed (max 0.5 (+ still-water current))
        hours (/ (/ (:distance-m edge) metres-per-nautical-mile) speed)
        fuel (* hours (+ (:fuel-rate-base-kg-h ship)
                         (* (:fuel-rate-per-knot-kg-h ship) speed)))
        weighted (+ (* alpha hours) (* (- 1.0 alpha) fuel))
        cost (case mode
               :time hours
               :fuel fuel
               :weighted weighted
               :safety (+ weighted (if (> bn safety-beaufort) safety-penalty 0.0))
               (throw (ex-info "unsupported cost mode" {:mode mode})))]
    {:cost cost :hours hours :fuel-kg fuel :speed-knots speed :beaufort bn}))

(defn scale-cost [cost scale]
  (long (Math/round (* (double cost) (long scale)))))

