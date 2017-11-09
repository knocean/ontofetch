(ns ontofetch.core
  (:require
   [ontofetch.files :as files]
   [ontofetch.http :as http]
   [ontofetch.utils :as u]
   [ontofetch.parse.jena :as jena]
   [ontofetch.parse.owl :as owl]
   [ontofetch.parse.xml :as xml])
  (:gen-class))

;; TODO: General error handling (error logs)

;; Make association between what you asked for in import and the IRI that you ended up with
;; Look at obi-edit file (core.owl import not resolving)

(defn try-xml
  "Given redirects to an ontology, a directory that it was downloaded in,
   and the ontology filepath, get metadata and generate by parsing XML.
   And more..."
  [redirs dir filepath]
  (println "Trying to parse RDF/XML...")
  (let [xml (xml/get-metadata-node filepath)]
    (let [import-map (http/map-imports dir (xml/get-imports xml))]
      (files/gen-content!
        dir
        (u/map-request
          filepath
          redirs
          [(xml/get-ontology-iri xml)
           (xml/get-version-iri xml)
           import-map])
        (xml/catalog-v001 import-map)))))

(defn try-jena
  "Given redirects to an ontology, a directory that it was downloaded in,
   and the ontology filepath, read the triples to get the metadata. And more..."
  [redirs dir filepath]
  (println "Trying to parse with Jena...")
  (let [trps (jena/read-triples filepath)]
    (let [import-map (http/map-imports dir (jena/get-imports trps))]
      (files/gen-content!
        dir
        (u/map-request
          filepath
          redirs
          [(jena/get-ontology-iri trps)
           "undefined"
           import-map])
        (xml/catalog-v001 import-map)))))

(defn try-owl
  "Given redirects to an ontology, a directory that it was downloaded in,
   and the ontology filepath, use OWLAPI to parse the ontology. And more..."
  [redirs dir filepath]
  (println "Trying to parse with OWLAPI...")
  (let [owl-ont (owl/load-ont filepath)]
    (let [import-map (owl/get-imports owl-ont)])
      (owl/fetch-imports! dir owl-ont)
      (files/gen-content!
        dir
        (u/map-request
          filepath
          redirs
          [(owl/get-ontology-iri owl-ont)
           (owl/get-version-iri owl-ont)
           (owl/get-imports owl-ont)])
        (xml/catalog-v001 (owl/get-imports owl-ont)))))

(defn -main
  [dir url]
  (let [redirs (http/get-redirects url)
        filepath (u/get-path-from-purl (files/make-dir! dir) url)]
    (http/fetch-ontology! filepath (last redirs))
    (try
      (try-xml redirs dir filepath)
      (catch Exception e
        (try
          (try-jena redirs dir filepath)
          (catch Exception e
            (try
              (try-owl redirs dir filepath)
              (catch Exception e
                (.getMessage e)))))))))
