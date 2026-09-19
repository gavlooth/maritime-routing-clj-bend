(ns maritime.frontend.app
  (:require [clojure.string :as str]
            ["leaflet" :as L]
            [uix.core :as uix :refer [$ defui]]
            [uix.dom]))

(def accent "#ee8d5a")
(def route-color "#ffb36b")

(defn fetch-json [url]
  (-> (js/fetch url #js {:headers #js {"Accept" "application/json"}})
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error. (str "Request failed: " (.-status response)))))))
      (.then #(js->clj % :keywordize-keys true))))

(defn latlngs [feature]
  (clj->js
   (mapv (fn [[longitude latitude]] [latitude longitude])
         (get-in feature [:geometry :coordinates]))))

(defn marker-icon [kind]
  (L/divIcon
   #js {:className (str "port-marker port-marker--" kind)
        :html "<span></span>"
        :iconSize #js [22 22]
        :iconAnchor #js [11 11]}))

(defui sea-map [{:keys [feature]}]
  (let [element-ref (uix/use-ref nil)
        map-ref (uix/use-ref nil)
        route-layer-ref (uix/use-ref nil)]
    (uix/use-effect
     (fn []
       (let [map-instance (L/map @element-ref
                                 #js {:zoomControl false
                                      :attributionControl true
                                      :minZoom 5
                                      :maxZoom 11})]
         (.setView map-instance #js [37.3 25.1] 6)
         (-> (L/tileLayer
              "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              #js {:maxZoom 19
                   :attribution "&copy; OpenStreetMap contributors"})
             (.addTo map-instance))
         (-> (L/control.zoom #js {:position "bottomright"})
             (.addTo map-instance))
         (reset! map-ref map-instance)
         (fn []
           (.remove map-instance)
           (reset! map-ref nil))))
     [])
    (uix/use-effect
     (fn []
       (when (and feature @map-ref)
         (when-let [old-layer @route-layer-ref]
           (.remove old-layer))
         (let [points (latlngs feature)
               group (L/layerGroup)
               line (L/polyline points
                                #js {:color route-color
                                     :weight 4
                                     :opacity 0.95
                                     :lineCap "round"
                                     :dashArray "1 0"})
               first-point (aget points 0)
               last-point (aget points (dec (.-length points)))]
           (.addLayer group line)
           (.addLayer group (L/marker first-point #js {:icon (marker-icon "origin")}))
           (.addLayer group (L/marker last-point #js {:icon (marker-icon "destination")}))
           (.addTo group @map-ref)
           (.fitBounds @map-ref (.getBounds line)
                       #js {:padding #js [72 72] :maxZoom 8})
           (reset! route-layer-ref group)))
       js/undefined)
     [feature])
    ($ :div.map-shell
       ($ :div.map-grid)
       ($ :div.map-label.map-label--top "AEGEAN SEA · LIVE ROUTE VIEW")
       ($ :div {:class-name "sea-map" :ref element-ref})
       ($ :div.map-vignette))))

(defui brand []
  ($ :div.brand
     ($ :div.brand-mark
        ($ :span.brand-mark__wake)
        ($ :span.brand-mark__ship))
     ($ :div
        ($ :div.brand-name "PELAGOS")
        ($ :div.brand-subtitle "WEATHER ROUTING SYSTEM"))))

(defui stat [{:keys [label value suffix tone]}]
  ($ :div.stat
     ($ :span.stat__label label)
     ($ :span {:class-name (str "stat__value" (when tone (str " stat__value--" tone)))}
        value
        (when suffix ($ :small suffix)))))

(defui route-card [{:keys [route selected? on-select]}]
  ($ :button {:class-name (str "route-card" (when selected? " route-card--selected"))
              :on-click #(on-select (:id route))}
     ($ :span.route-card__signal)
     ($ :span.route-card__copy
        ($ :strong (:name route))
        ($ :small (str (:distance-nm route) " NM · " (:duration-hours route) " HRS")))
     ($ :span.route-card__arrow "↗")))

(defui objective-toggle [{:keys [value on-change]}]
  ($ :div.objectives
     (for [[id label] [["time" "FASTEST"]
                       ["fuel" "LOW FUEL"]
                       ["balanced" "BALANCED"]
                       ["safety" "SAFEST"]]]
       ($ :button {:key id
                   :class-name (when (= id value) "active")
                   :on-click #(on-change id)}
          label))))

(defui dashboard []
  (let [[routes set-routes!] (uix/use-state [])
        [selected-id set-selected-id!] (uix/use-state nil)
        [objective set-objective!] (uix/use-state "time")
        [feature set-feature!] (uix/use-state nil)
        [status set-status!] (uix/use-state :loading)
        [error set-error!] (uix/use-state nil)]
    (uix/use-effect
     (fn []
       (-> (fetch-json "/api/routes")
           (.then (fn [payload]
                    (let [items (:routes payload)]
                      (set-routes! items)
                      (when-let [first-route (first items)]
                        (set-selected-id! (:id first-route))))))
           (.catch (fn [cause]
                     (set-error! (.-message cause))
                     (set-status! :error))))
       js/undefined)
     [])
    (uix/use-effect
     (fn []
       (when selected-id
         (set-status! :loading)
         (-> (fetch-json (str "/api/routes/" (js/encodeURIComponent selected-id)
                              "?objective=" objective))
             (.then (fn [payload]
                      (set-feature! payload)
                      (set-status! :ready)))
             (.catch (fn [cause]
                       (set-error! (.-message cause))
                       (set-status! :error)))))
       js/undefined)
     [selected-id objective])
    (let [properties (:properties feature)
          weather (:weather properties)]
      ($ :main.app
         ($ :header.topbar
            ($ brand)
            ($ :div.system-status
               ($ :span.status-dot)
               ($ :span "BEND ENGINE")
               ($ :strong "ONLINE"))
            ($ :div.topbar__meta
               ($ :span "FORECAST 06:00 UTC")
               ($ :span "H3 GRID · AEGEAN")))
         ($ :section.workspace
            ($ :aside.sidebar
               ($ :div.sidebar__intro
                  ($ :p.eyebrow "ROUTE CONTROL")
                  ($ :h1 "Navigate the\nweather window.")
                  ($ :p.lede "Compare shipping corridors across wind, wave and fuel constraints."))
               ($ :div.field-label "OPTIMIZATION MODE")
               ($ objective-toggle {:value objective :on-change set-objective!})
               ($ :div.field-label "ACTIVE VOYAGES")
               ($ :div.route-list
                  (for [route routes]
                    ($ route-card {:key (:id route)
                                   :route route
                                   :selected? (= selected-id (:id route))
                                   :on-select set-selected-id!})))
               ($ :div.notice
                  ($ :span.notice__icon "i")
                  ($ :p "Demonstration corridors only. Not approved for navigation.")))
            ($ :section.viewport
               ($ sea-map {:feature feature})
               (when (= status :loading)
                 ($ :div.loading-card ($ :span.spinner) "CALCULATING ROUTE"))
               (when (= status :error)
                 ($ :div.loading-card.loading-card--error (or error "Unable to load route")))
               (when properties
                 ($ :div.route-summary
                    ($ :div.route-summary__heading
                       ($ :p.eyebrow "SELECTED PASSAGE")
                       ($ :h2 (:name properties))
                       ($ :div.route-badge (str/upper-case objective)))
                    ($ :div.stats
                       ($ stat {:label "DISTANCE" :value (:distance-nm properties) :suffix " NM"})
                       ($ stat {:label "VOYAGE TIME" :value (:duration-hours properties) :suffix " H"})
                       ($ stat {:label "FUEL" :value (:fuel-tonnes properties) :suffix " T"})
                       ($ stat {:label "RISK" :value (:risk properties)
                                :tone (if (= "Low" (:risk properties)) "safe" "warn")}))
                    ($ :div.weather-strip
                       ($ :span "WIND " ($ :strong (str (:wind-knots weather) " KT " (:direction weather))))
                       ($ :span "SIGNIFICANT WAVE " ($ :strong (str (:wave-metres weather) " M")))
                       ($ :span "ENGINE " ($ :strong "BEND / CPU")))))))))))

(defonce root (uix.dom/create-root (js/document.getElementById "root")))

(defn start []
  (uix.dom/render-root ($ dashboard) root))
