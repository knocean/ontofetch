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

;; TODO: General error handling (error logs)

;; TODO: Deal with if catalog doesn't exist
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

;; TODO: It's still parsing the whole file in (takes ~10 seconds for GO)
(defn get-ont-metadata
  "Returns the XML node containing the ontology metadata."
  [filepath]
  ((fn find-md [xml]
     (if-not (zip/end? xml)
       (if (= (:tag (zip/node xml)) :owl:Ontology)
         (zip/node xml)
         (find-md (zip/next xml)))))
   (zip/xml-zip (xml/parse (io/file filepath)))))

(defn get-ontology-iri
  "Returns the ontology IRI from an RDF/XML OWL file."
  [xml]
  (get-in xml [:attrs :rdf:about]))

(defn get-version-iri
  "Returns the version IRI from an RDF/XML OWL file."
  [xml]
  (loop [n 0]
    (if (< n (count xml))
      (let [content (nth (:content xml) n)]
        (if (= (:tag content) :owl:versionIRI)
          (get-in content [:attrs :rdf:resource])
          (recur (+ n 1)))))))

(defn get-imports
  "Gets a list of import URLs from an RDF/XML OWL file."
  [xml]
  (loop [n 0 imports []]
    (if (< n (count xml))
      (let [content (nth (:content xml) n)]
        (if (= (:tag content) :owl:imports)
          (recur (+ n 1) (conj imports (get-in content [:attrs :rdf:resource])))
          (recur (+ n 1) imports)))
      imports)))

(defn map-ontology
  "Returns a map of the ontology metadata."
  [filepath]
  (let [xml (get-ont-metadata filepath)]
    {:ontology-iri (get-ontology-iri xml),
     :version-iri (get-version-iri xml)
     :imports (get-imports xml)}))

(defn get-ontology-key
  "Returns a keyword based on the filename to be used as key in catalog.edn."
  [filepath]
  (-> filepath
      (s/split #"/")
      last
      (s/split #"\.")
      first
      keyword))

(defn map-request
  "Returns a map of the request details for a given ontology (which acts as the key)."
  [filepath redirs metadata]
  {(get-ontology-key filepath)
   {:request-url (first redirs),
    :directory (first (s/split filepath #"/")),
    :request-date date,
    :location filepath,
    :redirect-path redirs,
    :metadata metadata}})

(defn create-artifact-record
  "Creates a record of the last request made for the requested ontology."
  [filepath]
  (let [ont-key (get-ontology-key filepath)]
    (if-let [prev (ont-key @ont-metadata)]
      (swap! ont-metadata
             (fn [current]
               (merge-with conj current {ont-key
                                         {:last-request
                                          {:request-date (:request-date prev),
                                           :request-url (:request-url prev),
                                           :directory (:directory prev),
                                           :error-log "TO BE IMPLEMENTED"}}}))))))
(defn update-metadata
  "Updates ont-metadata with new details."
  [new-details]
  (swap! ont-metadata
         (fn [current]
           (merge-with conj current new-details))))

(defn write-to-edn!
  "Writes the ont-metadata to catalog.edn"
  []
  (pp/pprint @ont-metadata (io/writer +catalog+)))

;; Store requests in vector instead (log of operations in order done)
;; Don't use 'ontology key'
;; Can go through and see which are the requests for the same ontology IRI

(defn -main
  "Downloads the ontology into given dir and updates the catalog.edn the metadata."
  [& args]
  (let [redirs (get-redirects (second args))]
    (let [filepath (fetch! (first args) (last redirs))]
      (do (create-artifact-record filepath)
          (update-metadata (map-request filepath redirs (map-ontology filepath))))))
  (write-to-edn!))
