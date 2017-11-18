(ns ontofetch.parse.parse
  (:require
   [ontofetch.files :as files]
   [ontofetch.http :as http]
   [ontofetch.utils :as u]
   [ontofetch.parse.jena :as jena]
   [ontofetch.parse.owl :as owl]
   [ontofetch.parse.xml :as xml]))

(defn try-get-imports
  "Given a list of imports and the directory they are saved in,
   try to get their indirect imports (XML, jena, or OWLAPI)."
  [imports dir]
  (try
    (xml/get-more-imports imports dir)
    (catch Exception e
      (try
        (jena/get-more-imports imports dir)
        (catch Exception e
          (try
            (owl/get-more-imports imports dir)))))))

(defn try-xml
  "Given redirects to an ontology, a directory that it was downloaded in,
   and the ontology filepath, get metadata and generate by parsing XML.
   And more..."
  [redirs dir filepath]
  (let [xml (xml/get-metadata-node filepath)]
    (let [imports (xml/get-imports xml)]
      (http/fetch-imports! dir imports)
      (let [i-map (try-get-imports imports dir)]
        (for [indirs (vals i-map)]
          ((partial http/fetch-imports! dir) indirs))
        (files/gen-content!
         dir
         (u/map-request
          filepath
          redirs
          [(xml/get-ontology-iri xml)
           (xml/get-version-iri xml)
           i-map])
         (xml/catalog-v001 i-map))))))

(defn try-jena
  "Given redirects to an ontology, a directory that it was downloaded in,
   and the ontology filepath, read the triples to get the metadata.
   And more..."
  [redirs dir filepath]
  (let [trps (jena/read-triples filepath)]
    (let [imports (jena/get-imports trps)]
      (http/fetch-imports! dir imports)
      (let [i-map (try-get-imports imports dir)]
        (for [indirs (vals i-map)]
          ((partial http/fetch-imports! dir) indirs))
        (files/gen-content!
         dir
         (u/map-request
          filepath
          redirs
          [(jena/get-ontology-iri trps)
           (jena/get-version-iri trps)
           i-map])
         (xml/catalog-v001 i-map))))))

(defn try-owl
  "Given redirects to an ontology, a directory that it was downloaded in,
   and the ontology filepath, use OWLAPI to parse the ontology. And more..."
  [redirs dir filepath]
  (let [owl-ont (owl/load-ontology filepath)]
    (let [imports (owl/get-imports owl-ont)]
      (http/fetch-imports! dir imports)
      (let [i-map (try-get-imports imports dir)]
        (for [indirs (vals i-map)]
          ((partial http/fetch-imports! dir) indirs))
        (files/gen-content!
         dir
         (u/map-request
          filepath
          redirs
          [(owl/get-ontology-iri owl-ont)
           (owl/get-version-iri owl-ont)
           i-map])
         (xml/catalog-v001 i-map))))))

(defn parse-ontology
  [redirs dir filepath]
  (try
    (try-xml redirs dir filepath)
    (catch Exception e
      (try
        (try-jena redirs dir filepath)
        (catch Exception e
          (try
            (try-owl redirs dir filepath)
            (catch Exception e
              (.getMessage e))))))))
