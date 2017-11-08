(ns ontofetch.core
  (:require
   [clojure.string :as s]
   [ontofetch.files :as files]
   [ontofetch.http :as http]
   [ontofetch.parse.jena :as jena]
   [ontofetch.parse.xml :as xml])
  (:gen-class))

;; TODO: General error handling (error logs)

;; Make association between what you asked for in import and the IRI that you ended up with
;; Look at obi-edit file (core.owl import not resolving)

(defn map-request
  "Returns a map of the request details for a given ontology."
  [filepath redirs metadata]
  {:request-url (first redirs),
   :directory (first (s/split filepath #"/")),
   :request-date (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") (java.util.Date.)),
   :location filepath,
   :redirect-path redirs,
   :metadata {:ontology-iri (first metadata)
              :version-iri (second metadata)
              :imports (last metadata)}})

(defn try-jena
  "Given redirects to an ontology, a directory that it was downloaded in,
   and the ontology filepath, read the triples to get the metadata. And more..."
  [redirs dir filepath]
  (let [trps (jena/read-triples filepath)]
    (let [import-map (->> (jena/get-imports trps)
                          (http/fetch-direct! dir)
                          (http/fetch-indirect! dir))]
      (files/gen-content!
       dir
       (map-request
        filepath
        redirs
        [(jena/get-ontology-iri trps) "undefined" import-map])
       (xml/catalog-v001 import-map)))))

(defn try-xml
  "Given redirects to an ontology, a directory that it was downloaded in,
   and the ontology filepath, get metadata and generate by parsing XML. And more..."
  [redirs dir filepath]
  (let [xml (xml/get-metadata-node filepath)]
    (let [import-map (->> (xml/get-imports xml)
                          (http/fetch-direct! dir)
                          (http/fetch-indirect! dir))]
      (files/gen-content!
       dir
       (map-request
        filepath
        redirs
        [(xml/get-ontology-iri xml) (xml/get-version-iri xml) import-map])
       (xml/catalog-v001 import-map)))))

(defn -main
  [dir url]
  (let [redirs (http/get-redirects url)
        filepath (http/get-path-from-purl (files/make-dir! dir) url)]
    (http/fetch-ontology! filepath (last redirs))
    (try
      (try-xml redirs dir filepath)
      (try-jena redirs dir filepath)
      (catch Exception e (.getMessage e)))))
