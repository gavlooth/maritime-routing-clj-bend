(ns maritime.routes
  "Read-only demo route catalogue. Coordinates are curated display fixtures,
  not computed routes. Production routes will be returned by the Bend worker.")

(def objectives #{"time" "fuel" "balanced" "safety"})

(def route-catalogue
  [{:id "piraeus-mykonos"
    :name "Piraeus → Mykonos"
    :origin {:name "Piraeus" :coordinates [23.6378 37.9475]}
    :destination {:name "Mykonos" :coordinates [25.3289 37.4467]}
    :distance-nm 94.2
    :duration-hours 7.1
    :fuel-tonnes 2.84
    :risk "Moderate"
    :weather {:wind-knots 21 :wave-metres 1.8 :direction "NNE"}
    :coordinates [[23.6378 37.9475] [23.8200 37.8500] [24.0500 37.7300]
                  [24.3100 37.6100] [24.6200 37.5300] [24.9300 37.4800]
                  [25.1600 37.4600] [25.3289 37.4467]]}
   {:id "piraeus-santorini"
    :name "Piraeus → Santorini"
    :origin {:name "Piraeus" :coordinates [23.6378 37.9475]}
    :destination {:name "Santorini" :coordinates [25.4615 36.3932]}
    :distance-nm 128.6
    :duration-hours 9.4
    :fuel-tonnes 3.77
    :risk "Elevated"
    :weather {:wind-knots 26 :wave-metres 2.4 :direction "N"}
    :coordinates [[23.6378 37.9475] [23.8500 37.7800] [24.0300 37.5600]
                  [24.1800 37.3200] [24.3500 37.0600] [24.6100 36.8300]
                  [24.9200 36.6200] [25.2300 36.4700] [25.4615 36.3932]]}
   {:id "piraeus-heraklion"
    :name "Piraeus → Heraklion"
    :origin {:name "Piraeus" :coordinates [23.6378 37.9475]}
    :destination {:name "Heraklion" :coordinates [25.1444 35.3387]}
    :distance-nm 178.3
    :duration-hours 12.8
    :fuel-tonnes 5.12
    :risk "Moderate"
    :weather {:wind-knots 18 :wave-metres 1.5 :direction "NW"}
    :coordinates [[23.6378 37.9475] [23.8300 37.7100] [24.0100 37.4200]
                  [24.1800 37.0800] [24.3400 36.7100] [24.5200 36.3000]
                  [24.7100 35.9000] [24.9300 35.5700] [25.1444 35.3387]]}
   {:id "rhodes-piraeus"
    :name "Rhodes → Piraeus"
    :origin {:name "Rhodes" :coordinates [28.2278 36.4510]}
    :destination {:name "Piraeus" :coordinates [23.6378 37.9475]}
    :distance-nm 238.7
    :duration-hours 16.6
    :fuel-tonnes 6.71
    :risk "Low"
    :weather {:wind-knots 14 :wave-metres 1.1 :direction "WNW"}
    :coordinates [[28.2278 36.4510] [27.8500 36.5300] [27.4200 36.6200]
                  [26.9800 36.7600] [26.5500 36.9400] [26.1200 37.1200]
                  [25.6900 37.3100] [25.2300 37.5100] [24.7600 37.6700]
                  [24.2600 37.8100] [23.6378 37.9475]]}])

(def routes-by-id (into {} (map (juxt :id identity) route-catalogue)))

(defn summaries []
  (mapv #(dissoc % :coordinates) route-catalogue))

(defn geojson [route objective]
  {:type "Feature"
   :id (:id route)
   :geometry {:type "LineString" :coordinates (:coordinates route)}
   :properties (-> route
                   (dissoc :coordinates)
                   (assoc :objective objective
                          :engine "bend-demo-fixture"
                          :navigational false))})

