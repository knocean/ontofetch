(ns ontofetch.xml
  (:require
   [clojure.data.xml :as data]
   [clojure.string :as s]
   [clojure.xml :as xml]
   [clojure.zip :as zip]))

;; TODO: Import & utilize Jena to parse ttl files (low priority)
;;       OWLAPI can read OBO & Manchester but is heavy and slow.
;;       Look at ROBOT for examples

;; Used to track direct & indirect imports for use in catalog-v001
(def all-imports (atom #{}))

(defn catalog-v001
  "Generates catalog-v001 from set of imports."
  []
  (data/indent-str
   (data/sexp-as-element
    [:catalog {:xmlns "urn:oasis:names:tc:entity:xmlns:xml:catalog"
               :prefer "public"}
     (map
      (fn [uri]
        [:uri {:name uri :uri (last (s/split uri #"/"))}])
      @all-imports)])))

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
  "Returns the ontology IRI from an RDF/XML OWL file."
  [xml]
  (get-in xml [:attrs :rdf/about]))

(defn get-version-iri
  "Returns the version IRI from an RDF/XML OWL file."
  [xml]
  (loop [n 0]
    (if (< n (count xml))
      (let [content (nth (:content xml) n)]
        (if (= (:tag content) :versionIRI)
          (get-in content [:attrs :rdf/resource])
          (recur (+ n 1)))))))

(defn get-imports
  "Returns a list of import URLs from an RDF/XML OWL file."
  [xml]
  (loop [n 0 imports []]
    (if (< n (count (:content xml)))
      (let [content (nth (:content xml) n)]
        (if (= (:tag content) :imports)
          (recur (+ n 1) (conj imports (get-in content [:attrs :rdf/resource])))
          (recur (+ n 1) imports)))
      (do
        ;; Update imports set
        (swap! all-imports
               (fn [current-imports]
                 (into current-imports imports)))
        ;; Return list of imports
        imports))))