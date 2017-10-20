(ns ontofetch.core
  (:require
   [clojure.string :as s]
   [ontofetch.files :as of]
   [ontofetch.http :as oh]
   [ontofetch.xml :as ox])
  (:gen-class))

;; TODO: General error handling (error logs)

;; TODO: HTML output using hiccup (main focus)
;;       See https://github.com/knocean/knode/blob/tree-views/src/knode/server/template.clj
;;       for bootstrap implementation

(def ont-metadata (atom {}))

(defn map-request
  "Returns a map of the request details for a given ontology."
  [filepath redirs metadata]
  {:request-url (first redirs),
   :directory (first (s/split filepath #"/")),
   :request-date (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") (java.util.Date.)),
   :location filepath,
   :redirect-path redirs,
   :metadata metadata})

;; TODO: Generate catalog-v001.xml

(defn -main
  [dir url]
  ;; Get list of redirs
  (let [redirs (oh/get-redirects url)
        filepath (oh/get-path-from-purl (of/make-dir! dir) url)]
    ;; Make a new dir and download the ontology
    (oh/fetch-ontology! filepath (last redirs))
    ;; Get the ontology metadata from file
    (let [xml (ox/get-metadata-node filepath)]
      (swap! ont-metadata
             (fn [current-metadata]
               (merge-with conj current-metadata {:ontology-iri (ox/get-ontology-iri xml)
                                                  :version-iri (ox/get-version-iri xml)})))
      ;; Get list of imports and download them
      (let [imports (ox/get-imports xml)]
        (swap! ont-metadata
               (fn [current-metadata]
                 (merge-with conj current-metadata {:imports imports})))
        (oh/fetch-imports! imports dir)))
    (of/add-request! (map-request filepath redirs @ont-metadata))))
