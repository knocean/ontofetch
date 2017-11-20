(ns ontofetch.ontofetch
  (:require
   [ontofetch.parse.jena :as jena]
   [ontofetch.parse.owl :as owl]
   [ontofetch.parse.xml :as xml]
   [ontofetch.tools.files :as f]
   [ontofetch.tools.http :as h]
   [ontofetch.tools.utils :as u]))

;; TODO: Divide up methods to create different CLI args

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
  ;; Get the metadata node as XML element
  (let [xml (xml/get-metadata-node filepath)]
    ;; Get a list of the imports
    (let [imports (xml/get-imports xml)]
      ;; Download the direct imports
      (h/fetch-imports! dir imports)
      ;; Get a map of direct imports (key) & indirect imports (vals)
      (let [i-map (try-get-imports imports dir)]
        ;; Download the indirect imports
        (for [indirs (vals i-map)]
          ((partial h/fetch-imports! dir) indirs))
        ;; Generate content
        (f/gen-content!
         dir
         ;; Formatted metadata map
         (u/map-request
          filepath
          redirs
          [(xml/get-ontology-iri xml)
           (xml/get-version-iri xml)
           i-map])
         ;; Catalog for protege
         (xml/catalog-v001 i-map))))))

(defn try-jena
  "Given redirects to an ontology, a directory that it was downloaded in,
   and the ontology filepath, read the triples to get the metadata.
   And more..."
  [redirs dir filepath]
  ;; Get the triples
  (let [trps (jena/read-triples filepath)]
    ;; Get a list of the imports
    (let [imports (jena/get-imports trps)]
      ;; Download the direct imports
      (h/fetch-imports! dir imports)
      ;; Get a map of direct imports (key) & indirect imports (vals)
      (let [i-map (try-get-imports imports dir)]
        ;; Download the indirect imports
        (for [indirs (vals i-map)]
          ((partial h/fetch-imports! dir) indirs))
        (f/gen-content!
         dir
         ;; Formatted metadata map
         (u/map-request
          filepath
          redirs
          [(jena/get-ontology-iri trps)
           (jena/get-version-iri trps)
           i-map])
         ;; Catalog for protege
         (xml/catalog-v001 i-map))))))

(defn try-owl
  "Given redirects to an ontology, a directory that it was downloaded in,
   and the ontology filepath, use OWLAPI to parse the ontology. And more..."
  [redirs dir filepath]
  ;; Get the ontology as an OWLOntology
  (let [owl-ont (owl/load-ontology filepath)]
    ;; Get a list of the imports
    (let [imports (owl/get-imports owl-ont)]
      ;; Download the direct imports
      (h/fetch-imports! dir imports)
      ;; Get a map of direct imports (key) & indirect imports (vals)
      (let [i-map (try-get-imports imports dir)]
        ;; Download the indirect imports
        (for [indirs (vals i-map)]
          ((partial h/fetch-imports! dir) indirs))
        (f/gen-content!
         dir
         ;; Formatted metadata map
         (u/map-request
          filepath
          redirs
          [(owl/get-ontology-iri owl-ont)
           (owl/get-version-iri owl-ont)
           i-map])
         ;; Catalog for protege
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
