(ns ontofetch.core.fetch
  (:require
   [clojure.tools.logging :as log]
   [ontofetch.parse.jena :as jena]
   [ontofetch.parse.owl :as owl]
   [ontofetch.parse.xml :as xml]
   [ontofetch.tools.files :as f]
   [ontofetch.tools.http :as h]
   [ontofetch.tools.utils :as u]))

(defn try-get-imports
  "Given a filepath to an import, get a vector of indirect imports.
   Try with XML, Jena, then OWLAPI. Otherwise, log exception."
  [filepath]
  (try
    (xml/get-imports (xml/get-metadata-node (xml/parse-xml filepath)))
    (catch Exception e
      (do
        (log/debug (.getMessage e))
        (try
          (jena/get-imports (jena/read-triples filepath))
          (catch Exception e
            (do
              (log/debug (.getMessage e))
              (try
                (owl/get-imports (owl/load-ontology filepath))
                (catch Exception e
                  (log/error (.getMessage e)))))))))))

(defn get-more-imports
  "Given a list of imports and the directory they are saved in,
   download the import and get its indirect imports. Return a vector
   of maps with the import URL, HTTP response details, and indirect
   imports."
  [imports dir]
  ;; First, fetch the imports
  (let [i-maps (h/fetch-imports! dir imports)]
    (reduce
     (fn [v i]
       (if-not (nil? (:response i))
          ;; Get the indirect imports for the entry
         (let [more (try-get-imports
                      ;; Filepath for import
                     (u/path-from-url dir
                        ;; URL is the first of redirects
                                      (first (:redirs (:response i)))))]
           (if-not (empty? more)
              ;; If the vector isn't empty, call fn on list of indirs
             (conj v (assoc i :imports (get-more-imports more dir)))
              ;; Otherwise just conj it into the vector
             (conj v i)))))
     [] i-maps)))

(defn try-xml
  "Given a directory and a filepath to a fetched ontology, parse as
   XML, get the metadata, and fetch all imports to the same dir."
  [dir filepath]
  ;; Parse XML, then get the RDF node, the metadata node,
  ;; and a list of imports
  (let [xml (xml/parse-xml filepath)
        rdf (xml/get-rdf-node xml)
        md (xml/get-metadata-node xml)
        imports (xml/get-imports md)]
    [(xml/get-ontology-iri md)
     (xml/get-version-iri md)
     (get-more-imports imports dir)]))

(defn try-jena
  "Given a directory and a filepath to a fetched ontology, parse with
   Jena, get the metadata, and fetch all imports to the same dir."
  [dir filepath]
  ;; Get the triples and prefixes
  (let [ttl (jena/read-triples filepath)
        trps (second ttl)
        imports (jena/get-imports trps)]
    [(jena/get-ontology-iri trps)
     (jena/get-version-iri trps)
     (get-more-imports imports dir)]))

(defn try-owl
  "Given a directory and a filepath to a fetched ontology, parse with
   OWLAPI, get the metadata, and fetch all imports to the same dir."
  [dir filepath]
  ;; Get the ontology as an OWLOntology
  (let [owl-ont (owl/load-ontology filepath)
        imports (owl/get-imports owl-ont)]
    [(owl/get-ontology-iri owl-ont)
     (owl/get-version-iri owl-ont)
     (get-more-imports imports dir)]))

(defn parse-ontology
  "Given a directory and a filepath to a fetched ontology, try to get
   the metadata, and fetch all imports to the same dir."
  [dir filepath]
  (log/debug "parsing" filepath "as XML.")
  (try
    (try-xml dir filepath)
    (catch Exception e
      (do
        (log/debug (.getMessage e))
        (log/debug "parsing" filepath "with Jena.")
        (try
          (try-jena dir filepath)
          (catch Exception e
            (do
              (log/debug (.getMessage e))
              (log/debug "parsing" filepath "with OWLAPI.")
              (try
                (try-owl dir filepath)
                (catch Exception e
                  (log/error (.getMessage e)))))))))))

(defn fetch
  "Given a working directory (wd) and a project name, read the config
   for that project and fetch the ontology specified by :url to the 
   directory specified by :dir.
   Given a wd, directory, and a URL to an ontology, fetch the ontology
   at URL to wd/dir.
   Given a wd, project name, directory, and a URL, fetch the ontology
   at URL to project/wd/dir."
  ;; (fetch wd dir url)
  ([wd dir url zip]
   (let [wd-dir (str wd dir)
         start (clj-time.local/local-now)
         response (h/get-response url)
         filepath (u/path-from-url wd-dir url)]
     (f/make-dir! wd-dir filepath)
     (h/fetch-ontology! filepath (last (:redirs response)))
     (f/spit-content!
      wd-dir
      (u/map-request
       filepath
       response
       (parse-ontology wd-dir filepath)
       start))
     (if zip
       (f/zip! wd-dir)
       true)))
  ;; (fetch wd project dir url)
  ([wd project dir url zip]
   (fetch wd (str project "/" dir) url zip))
  ;; (fetch wd project)
  ([wd project zip]
   (let [config (f/get-project-config wd project)]
     (fetch
      wd
      (str project "/" (u/format-dir-date (:dir config)))
      (:url config)
      zip))))
