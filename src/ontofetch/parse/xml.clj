(ns ontofetch.parse.xml
  (:require
   [clojure.data.xml :as data]
   [clojure.java.io :as io]
   [clojure.string :as s]
   [clojure.xml :as x]
   [clojure.zip :as zip]
   [ontofetch.tools.utils :as u]))

(defn parse-xml
  "Given a filepath to an ontology,
   return parsed XML."
  [filepath]
  (data/parse (java.io.FileInputStream. filepath)))

;;------------------------------ NODES -------------------------------
;; Methods to get specific nodes from parsed XML

(defn get-rdf-node
  "Given parsed XML of an owl:Ontology,
   return the mapped RDF node."
  [xml]
  {:tag :rdf:RDF,
   :attrs (:attrs xml)})

(defn update-map-vals
  "Given a parsed metadata node from an owl:Ontology,
   update the :content tags to remove special characters."
  [md]
  (assoc
   md
   :content
   (reduce
    (fn [l m]
      (conj
       l
       ;; Update the content in each annotation
       (update
        m
        :content
        (fn [c]
          ;; Grab out of vector, then put update back in
          (if-not (nil? c)
            (vector (u/replace-chars (first c))))))))
    [] (:content md))))

(defn get-metadata-node
  "Given parsed XML from an owl:Ontology,
   return the Ontology metadata."
  [xml]
  (->> xml
       zip/xml-zip
       zip/children
       (map (fn [xml-node] [(:tag xml-node) xml-node]))
       (filter #(= :Ontology (first %)))
       first
       second))

;;---------------------------- METADATA ------------------------------
;; Methods to get specific metadata elements from the metadata node

(defn get-ontology-iri
  "Given an XML node containing ontology metadata,
   return the ontology IRI."
  [xml]
  (get-in xml [:attrs :rdf/about]))

(defn get-version-iri
  "Given an XML node containing ontology metadata,
   return the version IRI (or nil)."
  [xml]
  (first
   (reduce
    (fn [s c]
      (if (= (:tag c) :versionIRI)
        (conj s (get-in c [:attrs :rdf/resource]))
        s))
    [] (:content xml))))

(defn get-imports
  "Given an XML node containing ontology metadata,
   return the list of imports (or an empty list)."
  [xml]
  (reduce
   (fn [s c]
     (if (= (:tag c) :imports)
       (conj s (get-in c [:attrs :rdf/resource]))
       s))
   [] (:content xml)))

(defn get-more-imports
  "Given a list of direct imports and the directory they are saved in,
   return a map of direct imports (keys) and their imports (vals)."
  [imports dir]
  (reduce
   (fn [m i]
     (let [f (u/path-from-url dir i)]
       (if (.exists (io/as-file f))
         (conj m {i (get-imports (get-metadata-node (parse-xml f)))})
         m)))
   {} imports))

;;--------------------------- XML STRING -----------------------------
;; Methods to convert maps of nodes to XML strings

(defn node->xml-str
  "Given a mapped RDF node and a map of the metadata,
   return the Ontology element as XML string."
  [rdf-node metadata]
  (str
   "<?xml version=\"1.0\"?>\n"
   (s/replace
    (->> {:content
          (vector (update-map-vals metadata))}
         (into rdf-node)
         x/emit-element
         with-out-str)
    "'" "\"")))

(defn catalog-v001
  "Generates catalog-v001 from set of imports."
  [i-map]
  (let [imports (->> (vals i-map)
                     (into (keys i-map))
                     flatten
                     (into #{})
                     (filter identity))]
    (data/indent-str
     (data/sexp-as-element
      [:catalog {:xmlns "urn:oasis:names:tc:entity:xmlns:xml:catalog"
                 :prefer "public"}
       (map
        (fn [uri]
          [:uri {:name uri :uri (last (s/split uri #"/"))}])
        imports)]))))
