(ns ontofetch.core
  (:require
   [clojure.string :as s]
   [ontofetch.files :as files]
   [ontofetch.http :as http]
   [ontofetch.xml :as xml])
  (:gen-class))

;; TODO: General error handling (error logs)

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

(defn update-ont-metadata
  [new-details]
  (swap! ont-metadata
         (fn [cur] (merge-with conj cur new-details))))

(defn -main
  [dir url]
  (let [redirs (http/get-redirects url)
        filepath (http/get-path-from-purl (files/make-dir! dir) url)]
    (http/fetch-ontology! filepath (last redirs))
    (let [xml-tree (xml/get-metadata-node filepath)]
      (update-ont-metadata {:ontology-iri (xml/get-ontology-iri xml-tree)
                            :version-iri (xml/get-version-iri xml-tree)})
      (let [imports (xml/get-imports xml-tree)]
        (update-ont-metadata {:imports imports})
        (http/fetch-imports! imports dir)))
    (files/spit-catalog-v001! dir (xml/catalog-v001))
    (files/update-catalog! (map-request filepath redirs @ont-metadata))
    (files/spit-report!)))
