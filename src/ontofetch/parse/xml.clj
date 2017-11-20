(ns ontofetch.parse.xml
  (:require
   [clojure.data.xml :as data]
   [clojure.string :as s]
   [clojure.xml :refer [emit-element]]
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

(defn get-metadata-node
  "Returns the XML node containing the ontology metadata."
  [filepath]
  (->> filepath
       java.io.FileInputStream.
       data/parse
       zip/xml-zip
       zip/children
       (map (fn [xml-node] [(:tag xml-node) xml-node]))
       (filter #(= :Ontology (first %)))
       first
       second))

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
     (let [md (get-metadata-node (u/get-path-from-purl dir i))]
       (conj m {i (get-imports md)})))
   {} imports))

;; TODO: Store as edn instead
(defn get-ont-element
  [xml]
  (with-out-str (emit-element xml)))
