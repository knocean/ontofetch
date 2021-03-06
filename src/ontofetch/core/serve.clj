(ns ontofetch.core.serve
  (:require
   [clj-time.core :as t]
   [clj-time.format :as tf]
   [clj-time.local :as tl]
   [clojure.core.async :as async
    :refer [<! >! <!! timeout chan alt! go close!]]
   [clojure.string :as s]
   [clojure.tools.logging :as log]
   [ontofetch.core.extract :as e]
   [ontofetch.core.update :as upd]
   [ontofetch.tools.files :as f]
   [ontofetch.tools.utils :as u]
   [overtone.at-at :as at]))

(def ch (atom nil))
(def fmt (tf/formatter :date-time))
(def pool (at/mk-pool))

(defn reports-dir!
  "Given a working directory (wd),
   create directories for the HTML reports."
  [wd]
  ;; Make sure the main dir is there
  (f/make-dir! (str wd "reports"))
  ;; Create a dated subdir
  (let [dir (str
             wd "reports/" (u/format-dir-date "yyyyMMdd"))]
    (f/make-dir! dir)
    ;; Return the name of the dir
    dir))

(defn serve-helper
  "Given a working directory (wd), the slurped config file, and a map
   of serve options, update and maybe extract for all projects, then
   generate HTML reports."
  [wd config opts]
  ;; First run update
  (upd/dir-update wd (:zip opts))
  ;; If extracts, extract the ont elements
  (if (:extracts opts)
    (e/dir-extract wd))
  ;; Generate reports (all catalog entries after serve start date)
  (let [subset (filter
                #(t/after?
                  (tf/parse fmt (:start-time %)) (:date opts))
                (f/slurp-catalog wd))
        ids (reduce
             (fn [v e] (conj v (:id e))) [] (:projects config))]
    ;; Report for all fetches
    (f/spit-fetch-report! (:reports opts) subset)
    ;; Reports for each project
    (doseq [id ids]
      (let [proj-subset (filter
                         #(= (first (s/split (:directory %) #"/")) id)
                         subset)]
        (f/spit-project-report! (:reports opts) id proj-subset)))))

(defn run
  "Given an interval in milliseconds, a working directory (wd), the
   slurped config file, and a map of serve options, run serve."
  [ms wd config opts]
  (go (at/interspaced ms #(serve-helper wd config opts) pool)))

(defn kill!
  "Given a start time (DateTime), provide feedback on the operation
   and kill the running process."
  [start]
  (let [end (tl/local-now)]
    (log/info "serve killed at" (tf/unparse fmt end))
    (log/info
     "ran for" (t/in-minutes (t/interval start end)) "minutes")
    (close! @ch)))

(defn serve
  "Given a working directory (wd) and option to zip results,
   run update on a regular time interval as specified in config.edn."
  [wd zip extracts]
  (let [config (f/slurp-config wd)
        ;; If interval not specified, default to 4 hours  
        ms (or (:serve-interval (f/slurp-config wd)) 14400000)
        ;; Put options into map to pass into helper
        opts {:zip zip,
              :extracts extracts,
              :date (tl/local-now),
              :reports (reports-dir! wd)}
        start (tl/local-now)]
    ;; Run every given ms
    (log/info "serve started at" (tf/unparse fmt start))
    (reset! ch (run ms wd config opts))
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. #(kill! start)))
    (while true <!! @ch)))
