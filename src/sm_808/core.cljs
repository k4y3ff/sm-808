(ns sm-808.core
  (:require
    [chronoid.core :as c]
    [clojure.string :as str]
    [dommy.core :as dommy :refer-macros [sel sel1]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Constants
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def min-ms 60000)
(def green "#39FF14")

(def clock (c/clock))
(c/start! clock)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; State
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def scheduled (atom []))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Read-Focused Functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-bpm
  "Retrieves the selected bpm value and parses it into an integer."
  []
  (int (dommy/value (sel1 :#bpm))))

(defn num-from-point-div
  "Given an HTMLElement representing a point div, parses the column 1-16 the
  div represents."
  [div-element]
  (-> div-element
     (.-id)
     (str/split #"-")
     (last)
     (int)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Write-Focused Functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn flash-point!
  "Given an HTMLElement representing a point on the sequencer, makes the div
  background rapidly change green and then black."
  [point-element]
  (do
    (dommy/set-style! point-element :background green)
    (js/setTimeout #(dommy/set-style! point-element :background "black")
                   100)))

(defn schedule-point!
  "Given an HTMLElement representing a point on the sequencer, determines when
  the point should flash, then schedules the event, accordingly."
  [point-element]
  (let [bpm (get-bpm)
        ms-between-beats (/ min-ms bpm)
        num (num-from-point-div point-element)
        rate (* ms-between-beats 16)
        delay (* ms-between-beats (- num 1))]
    (swap! scheduled
           conj
           (-> (c/set-timeout! clock
                               #(-> point-element
                                    (flash-point!))
                               delay)
               (c/repeat! rate)))))

(defn schedule-points!
  "Selects the points that have been selected by the user, then schedules the
  events corresponding to their being flashed."
  []
  (doseq [point (sel :.selected)]
    (schedule-point! point)))

(defn unschedule-points!
  "Clears the cache containing scheduled events."
  []
  (do
    (doseq [event @scheduled]
      (c/clear! event))
    (reset! scheduled [])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event Handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn play-click-handler
  "Changes the background of the play button to green, then changes the
  background of the stop button to grey. Finally, schedules the points
  selected by the user to be flashed according to the BPM selected."
  [e]
  (let [stop-button (sel1 :#stop)
        play-button (sel1 :#play)]
    (do
      (dommy/set-style! play-button :background green)
      (dommy/set-style! stop-button :background "grey")
      (schedule-points!))))

(defn stop-click-handler
  "Changes the background of the play button to grey, then changes the background
  of the stop button to red. Finally, clears the cache of scheduled events,
  thus ending the notes being played by the sequencer."
  [e]
  (let [play-button (sel1 :#play)
        stop-button (sel1 :#stop)]
    (do
      (dommy/set-style! play-button :background "grey")
      (dommy/set-style! stop-button :background "red")
      (unschedule-points!))))

(defn point-click-handler
  "If the selected point element has already been selected, updates the
  background to be white and marks the div as having been unselected. If the
  selected point element has not already been selected, marks it as selected."
  [e]
  (let [point-element (.-selectedTarget e)
        is-selected (dommy/has-class? point-element :selected)]
    (if is-selected
      (-> point-element
          (dommy/set-style! :background "white")
          (dommy/remove-class! :selected))
      (-> point-element
          (dommy/set-style! :background "black")
          (dommy/add-class! :selected)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Listeners
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(dommy/listen! (sel1 :#play) :click play-click-handler)

(dommy/listen! (sel1 :#stop) :click stop-click-handler)

(dommy/listen! [(sel1 :#sequencer) :.point] :click point-click-handler)
