(ns ontofetch.core
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.string :as s]
   [clojure.xml :as xml]
   [clojure.zip :as zip]
   [org.httpkit.client :as http])
  (:gen-class))

;; TODO: General error handling

(def +catalog+ "catalog.edn")
(def date (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") (java.util.Date.)))
(def ont-metadata (atom (edn/read-string (slurp +catalog+))))

(defn validate-dir
  "Checks if a directory is in proper format and that it does not exist. 
   If so, returns dir name as str."
  [dir]
  (when-not (re-matches #"[A-Za-z0-9_]+" dir)
    (throw (Exception. "Directory name can only include letters, numbers, or 
      underscores.")))
  (when (.isDirectory (io/file dir))
    (throw (Exception. "Directory must not already exist in the file system.")))
  dir)

(defn fetch!
  "Makes a directory and downloads given ontology to it. Returns path to file."
  [dir final-url]
  (do (let [filepath (str dir "/" (last (s/split final-url #"/")))]
        (.mkdir (java.io.File. (validate-dir dir)))
        (let [content (slurp final-url)]
          (spit filepath content))
        filepath)))

(defn zip-xml
  "Parses a an XML file and creates a zipper to traverse."
  [filepath]
  (zip/xml-zip (xml/parse (io/file filepath))))

;; TODO: change get-iri methods to find IRIs by name of element
;;       instead of assuming their location.

(defn get-ontology-iri
  "Returns the ontology IRI from a RDF/XML OWL file."
  [xml-tree]
  (get-in (-> xml-tree
              zip/down
              zip/node) [:attrs :rdf:about]))

(defn get-version-iri
  "Returns the version IRI from a RDF/XML OWL file."
  [xml-tree]
  (get-in (-> xml-tree
              zip/down
              zip/down
              zip/node) [:attrs :rdf:resource]))

;; TODO: Look for imports (will not be in the same place every time)
;;       use the name of the element instead of just the position

(defn get-redirects
  "Manually follows redirects and returns a vector containing all redirect URLs.
   The final URL with content is the last entry."
  [url]
  (loop [redirs []
         new-url url]
    (let [{:keys [status headers body error] :as res} @(http/request {:url new-url :method :head :follow-redirects false})]
      (case status
        200 (conj redirs new-url)
        (301 302 303 307 308) (recur (conj redirs new-url) (:location headers))
        304 nil
        (throw (Exception. "Unhandled status."))))))

(defn map-ontology
  "Returns a map of the ontology metadata."
  [filepath]
  (let [xml-tree (zip-xml filepath)]
    {:ontology-iri (get-ontology-iri xml-tree),
     :version-iri (get-version-iri xml-tree)}))

(defn map-request
  "Returns a map of the request details for a given ontology (which acts as the key)."
  [filepath redirs metadata]
  {(-> filepath
       (s/split #"/")
       last
       (s/split #"\.")
       first) {:request-url (first redirs),
               :directory (first (s/split filepath #"/")),
               :request-date date,
               :location filepath,
               :redirect-path redirs,
               :metadata metadata}})

;; TODO: Log artifact records (#7)
(defn update-metadata
  "Updates ont-metadata with new details."
  [new-details]
  (swap! ont-metadata
         (fn [current-details]
           (merge-with conj current-details new-details))))

(defn write-to-edn!
  "Writes the ont-metadata to catalog.edn"
  []
  (pp/pprint @ont-metadata (io/writer +catalog+)))

(defn -main
  "Just downloads the ontology into given dir and returns metadata."
  [& args]
  (let [redirs (get-redirects (second args))]
    (let [filepath (fetch! (first args) (last redirs))]
      (update-metadata (map-request filepath redirs (map-ontology filepath)))))
  (write-to-edn!))
