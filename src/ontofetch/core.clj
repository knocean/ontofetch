(ns ontofetch.core
  (:gen-class))
(require '[clojure.xml :as xml]
         '[clojure.java.io :as io]
         '[clojure.zip :as zip])

;; TODO: General error handling
;;       What if URL does not return any content, or incorrect content?
;;       nil IRIs, etc.

;; TODO: Issues with PR & CHEBI bc they redirect to OBO files

(defn validate-dir
  "Checks if a directory is in proper format and that it does not exist. 
   If so, returns dir name as str."
  [dir]
	;; TODO: not finding escaped backslash
  (if-not (or (re-find #"[^\w\\s]" dir) (.isDirectory (io/file dir)))
    (identity dir)
    (throw (Exception. "Directory name can only include letters, numbers, or 
			underscores, and must not already exist in the file system."))))

(defn return-content
  "Checks if the url content is XML. If not, finds a URL to redir to
   (assuming 'this page has moved' & provides new URL). If XML, returns content."
  [url]
  (if-not (re-find #"xml version" (slurp url))
    (recur (first (re-find
    	           #"(http|ftp|https)://([\w_-]+(?:(?:\.[\w_-]+)+))([\w.,@?^=%&:/~+#-]*[\w@?^=%&/~+#-])?"
                   (slurp url))))
    (slurp url)))

(defn fetch!
  "Makes a directory and downloads given ontology to it. Returns path to file."
  [dir url]
  (do (let [filepath (str dir "/" (last (clojure.string/split url #"/")))]
        (.mkdir (java.io.File. (validate-dir dir)))
        (let [content (return-content url)]
          (spit filepath content))
        filepath)))

(defn zip-xml
  "Parses a an XML file and creates a zipper to traverse."
  [filepath]
  (zip/xml-zip (xml/parse (io/file filepath))))

;; TODO: What if you don't have an ontology def in the file?
;;		 Implement validation checks on IRIs.
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

(defn map-ontology
  "Returns a map of the ontology metadata."
  [filepath]
  (let [xml-tree (zip-xml filepath)]
  	{:ontology-iri (get-ontology-iri xml-tree),
     :version-iri (get-version-iri xml-tree)}))

(defn -main
  "Just downloads the ontology into given dir and returns IRIs."
  [& args]
  (map-ontology (fetch! (first args) (second args))))
