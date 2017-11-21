(ns ontofetch.parse.xml
  (:require
   [clojure.data.xml :as data]
   [clojure.string :as s]
   [clojure.xml :as x]
   [clojure.zip :as zip]
   [ontofetch.tools.utils :as u]))

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

(defn parse-xml
  "Given a filepath to an ontology,
   return parsed XML."
  [filepath]
  (x/parse (java.io.FileInputStream. filepath)))

(defn get-metadata-node
  "Given parsed XML from an owl:Ontology,
   return the Ontology metadata."
  [xml]
  (->> xml
       zip/xml-zip
       zip/children
       (map (fn [xml-node] [(:tag xml-node) xml-node]))
       (filter #(= :owl:Ontology (first %)))
       first
       second))

(defn get-ontology-iri
  "Given an XML node containing ontology metadata,
   return the ontology IRI."
  [xml]
  (get-in xml [:attrs :rdf:about]))

(defn get-version-iri
  "Given an XML node containing ontology metadata,
   return the version IRI (or nil)."
  [xml]
  (first
   (reduce
    (fn [s c]
      (if (= (:tag c) :owl:versionIRI)
        (conj s (get-in c [:attrs :rdf:resource]))
        s))
    [] (:content xml))))

(defn get-imports
  "Given an XML node containing ontology metadata,
   return the list of imports (or an empty list)."
  [xml]
  (reduce
   (fn [s c]
     (if (= (:tag c) :owl:imports)
       (conj s (get-in c [:attrs :rdf:resource]))
       s))
   [] (:content xml)))

(defn get-more-imports
  "Given a list of direct imports and the directory they are saved in,
   return a map of direct imports (keys) and their imports (vals)."
  [imports dir]
  (reduce
   (fn [m i]
     (let [md (get-metadata-node (u/get-path-from-purl dir i))]
       (conj m {i (get-imports md)})))
   {} imports))

(defn get-declarations
  "Given parsed XML of an owl:Ontology,
   return the XML declarations (as XML)."
  [xml]
  (let [decs (:attrs xml)]
    (->> {:attrs decs}
         (conj {:tag :rdf:RDF}))))

(defn get-ont-element
  "Given RDF declarations as XML and a map of the metadata,
   return the Ontology element as XML string."
  [decs metadata]
  (->> {:content (vector metadata)}
       (into decs)
       x/emit-element
       with-out-str))
