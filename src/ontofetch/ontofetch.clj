(ns ontofetch.ontofetch
  (:require
   [clojure.java.io :as io]
   [ontofetch.parse.jena :as jena]
   [ontofetch.parse.owl :as owl]
   [ontofetch.parse.xml :as xml]
   [ontofetch.tools.files :as f]
   [ontofetch.tools.http :as h]
   [ontofetch.tools.utils :as u]))

;; TODO: Divide up methods to create different CLI args?

(def catalog (atom []))

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
  "Given redirects to an ontology, a directory that it was downloaded
   in, and the ontology filepath, get metadata and generate by parsing
   XML. And more..."
  [response dir filepath start]
  ;; Parse XML, then get the RDF node, the metadata node,
  ;; and a list of imports
  (let [xml (xml/parse-xml filepath)
        rdf (xml/get-rdf-node xml)
        md (xml/get-metadata-node xml)
        imports (xml/get-imports md)]
    ;; Create an XML file with just the Ontology element
    ;; Placed in the current dir, or project dir if specified
    ;; TODO: Split into separate subcommand
    (f/spit-ont-element! dir (f/extract-element filepath))
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
       (conj
        @catalog
         ;; Formatted metadata map
        (u/map-request
         filepath
         response
         [(xml/get-ontology-iri md)
          (xml/get-version-iri md)
          i-map]
         start))
       ;; Catalog for protege
       (if-not (empty? imports)
         (xml/catalog-v001 i-map))))))

(defn try-jena
  "Given redirects to an ontology, a directory that it was downloaded
   in, and the ontology filepath, read the triples to get the
   metadata. And more..."
  [response dir filepath start]
  ;; Get the triples and prefixes
  (let [ttl (jena/read-triples filepath)
        trps (second ttl)]
    ;; Generate the ontology element as XML
    ;; Placed in the current dir, or project dir if specified
    (f/spit-ont-element!
     dir
     (xml/node->xml-str
      (jena/map-rdf-node ttl)
      (jena/map-metadata (first (:redirs response)) ttl)))
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
         (conj
          @catalog
           ;; Formatted metadata map
          (u/map-request
           filepath
           response
           [(jena/get-ontology-iri trps)
            (jena/get-version-iri trps)
            i-map]
           start))
         ;; Catalog for protege
         (if-not (empty? imports)
           (xml/catalog-v001 i-map)))))))

(defn try-owl
  "Given redirects to an ontology, a directory that it was downloaded
   in, and the ontology filepath, use OWLAPI to parse the ontology.
   And more..."
  [response dir filepath start]
  ;; Get the ontology as an OWLOntology
  (let [owl-ont (owl/load-ontology filepath)]
    ;; Get a list of the imports
    (let [iri (owl/get-ontology-iri owl-ont)
          version (owl/get-version-iri owl-ont)
          imports (owl/get-imports owl-ont)
          annotations (owl/get-annotations owl-ont)]
      ;; Generate the ontology element as XML
      ;; Placed in the current dir, or project dir if specified
      (f/spit-ont-element!
       dir
       (xml/node->xml-str
        (owl/map-rdf-node iri annotations)
        (owl/map-metadata iri version imports annotations)))
      ;; Download the direct imports
      ;; TODO: Maybe don't download if-not (:update? response)
      (h/fetch-imports! dir imports)
      ;; Get a map of direct imports (key) & indirect imports (vals)
      (let [i-map (try-get-imports imports dir)]
        ;; Download the indirect imports
        ;; TODO: Maybe don't download if-not (:update? response)
        (for [indirs (vals i-map)]
          ((partial h/fetch-imports! dir) indirs))
        (f/gen-content!
         dir
         (conj
          @catalog
           ;; Formatted metadata map
          (u/map-request
           filepath
           response
           [iri version i-map]
           start))
         ;; Catalog for protege
         (if-not (empty? imports)
           (xml/catalog-v001 i-map)))))))

(defn parse-ontology
  [response dir filepath start]
  (try
    (try-xml response dir filepath start)
    (catch Exception e
      (do
        (println e)
        (try
          (try-jena response dir filepath start)
          (catch Exception e
            (try
              (try-owl response dir filepath start)
              (catch Exception e
                (.getMessage e)))))))))

(defn skip-fetch
  [dir last-request response start]
  ;; Only delete if the dir is emtpy
  (if (nil? (second (file-seq (io/file dir))))
    (io/delete-file (io/file dir)))
  (f/gen-content!
   dir
   (conj
    @catalog
     ;; Use the same metadata, since nothing changed
    (u/map-request
     (:location last-request)
     (assoc response :update? false)
     [(:ontology-iri last-request)
      (:version-iri last-request)
      (:imports last-request)]
     start))
   nil))

(defn swap-catalog
  "Given a directory path, read the catalog of the working directory
   (one level up) into the catalog atom."
  [dir]
  (swap! catalog
   (fn [cur]
     (if-let [cat (f/read-catalog
                   (subs dir 0
                         (+ 1 (clojure.string/index-of dir "/"))))]
       (into cur cat)))))

(defn ontofetch
  [dir url start]
  (let [response (h/get-response url)
        filepath (u/path-from-url dir url)]
    ;; Make sure the parent directories exist
    (f/make-dir! dir filepath)
    ;; Read the catalog in working dir
    (swap-catalog dir)
    ;; If the version is the same as last request, get details
    (let [last-request (f/get-last-metadata catalog url response)
          last-loc (:location last-request)]
      ;; Check if there's a last request, and also that file exists
      (if (or (= nil last-request) (not (f/file-exists? last-loc)))
        ;; Download it if it's a new version or the file is missing
        (do
          (h/fetch-ontology! filepath (last (:redirs response)))
          ;; Do all the stuff & hopefully return true
          (parse-ontology
           (assoc response :update? true) dir filepath start))
        ;; Otherwise we can skip fetching
        (skip-fetch dir last-request response start)))))
        ;; TODO: Check imports for new version
