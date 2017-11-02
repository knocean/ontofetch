(ns ontofetch.core
  (:require
   [clojure.string :as s]
   [ontofetch.files :as files]
   [ontofetch.http :as http]
   [ontofetch.xml :as xml])
  (:gen-class))

;; TODO: General error handling (error logs)

;; TODO: Add time to date (timestamp)

;; Make association between what you asked for in import and the IRI that you ended up with
;; Look at obi-edit file (core.owl import not resolving)

(defn map-request
  "Returns a map of the request details for a given ontology."
  [filepath redirs metadata]
  {:request-url (first redirs),
   :directory (first (s/split filepath #"/")),
   :request-date (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") (java.util.Date.)),
   :location filepath,
   :redirect-path redirs,
   :metadata metadata})

(defn -main
  [dir url]
  (let [redirs (http/get-redirects url)
        filepath (http/get-path-from-purl (files/make-dir! dir) url)]
    (http/fetch-ontology! filepath (last redirs))
    (let [xml-tree (xml/get-metadata-node filepath)]
      (if-let [imports (xml/get-imports xml-tree)]
        (let [i-map (http/fetch-indirect! (http/fetch-direct! imports dir) dir)]
          (files/update-catalog!
           (map-request filepath redirs {:ontology-iri (xml/get-ontology-iri xml-tree)
                                         :version-iri (xml/get-version-iri xml-tree)
                                         :imports i-map}))
          (files/spit-catalog-v001! dir (xml/catalog-v001 i-map))
          (files/spit-report!)
          (files/zip-folder! dir))))))
