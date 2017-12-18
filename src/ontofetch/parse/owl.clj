(ns ontofetch.parse.owl
  (:require
   [clojure.string :as s]
   [clojure.java.io :as io]
   [ontofetch.tools.utils :as u])
  (:import
   (org.semanticweb.owlapi.apibinding OWLManager)
   (org.semanticweb.owlapi.model OWLOntologyManager OWLOntology IRI)
   (org.semanticweb.owlapi.search EntitySearcher)))

(def base-prefixes
  {:xmlns:owl "http://www.w3.org/2002/07/owl#",
   :xmlns:rdf "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
   :xmlns:rdfs "http://www.w3.org/2000/01/rdf-schema#",
   :xmlns:xml "http://www.w3.org/XML/1998/namespace",
   :xmlns:xsd "http://www.w3.org/2001/XMLSchema#"})

(defn load-ontology
  "Given a filepath to an ontology,
   return the ontology as an OWLOntology."
  [filepath]
  (let [manager (OWLManager/createOWLOntologyManager)]
    (.loadOntologyFromOntologyDocument manager (io/file filepath))))

;;---------------------------- METADATA ------------------------------
;; Methods to get specific metadata elements from an OWLOntology

(defn get-ontology-iri
  "Given an OWLOntology, return the Ontology IRI."
  [owl-ont]
  (let [iri (.getOntologyIRI (.getOntologyID owl-ont))]
    (if (.isPresent iri)
      (.toString (.get iri)))))

;; Will always return purl.obolibrary.org
(defn get-version-iri
  "Given an OWLOntology, return the Version IRI."
  [owl-ont]
  (let [iri (.getVersionIRI (.getOntologyID owl-ont))]
    (if (.isPresent iri)
      (.toString (.get iri)))))

;; TODO: OBO format does not load imports :(
(defn get-imports
  "Given an OWLOntology, return a list of direct imports."
  [owl-ont]
  ;; Reverse to get in same order as XML
  (reverse
   (mapv
    #(.toString (.getIRI %))
    (iterator-seq (.iterator (.importsDeclarations owl-ont))))))

(defn get-more-imports
  "Given a list of imports and a directory they are saved in,
   return a map of imports (keys) and their imports (vals)."
  [imports dir]
  (reduce
   (fn [m i]
     (let [ont (load-ontology (u/path-from-url dir i))]
       (conj m {i (get-imports ont)})))
   {} imports))

;;--------------------------- FORMATTING -----------------------------
;; Methods to format ontology annotations for conversion to XML

(defn parse-property
  "Given a property from an Annotation,
   return [uri prefix property]."
  [p]
  (let [p-str (.toStringID p)] 
    ;; Check if it's a full URI or a CURIE
    (if (s/includes? p-str "http://")
      ;; Create a prefix from the full URI
      (let [pref (u/get-ns-prefix (u/get-namespace p-str))]
        [p-str
         ;; rdf-schema is rdfs
         (if (= pref "rdf-schema")
           "rdfs"
           pref)
         (u/get-entity-id p-str)])
      ;; Otherwise split the CURIE
      (into [p-str] (s/split (.toString p) #":")))))

(defn parse-value
  "Given a value from an Annotation,
   return [annotation-value datatype]."
  [v]
  (if (instance? IRI v)
    ;; Parse IRIs to string (TODO: FIX ME! Needs to parse as resource)
    [(u/get-entity-id (.getIRIString v))
     "http://www.w3.org/2001/XMLSchema#string"]
    [(.getLiteral v) (.toString (.getDatatype v))]))

(defn parse-statement
  "Helper function to create vector of annotations as
   [[uri prefix property] [annotation-value datatype]]"
  [l a]
  (let [p (.getProperty a)
        v (.getValue a)]
    (conj
     l
     [(parse-property p)
      (parse-value v)])))

(defn get-annotations
  "Given an OWLOntology, return the ontology annotations"
  [owl-ont]
  (reduce parse-statement [] (.getAnnotations owl-ont)))

(defn get-base-prefixes
  "Given an ontology IRI, return a map of base prefixes."
  [iri]
  (conj
   {:xmlns (str iri "#")
    :xml:base iri}
   base-prefixes))

;; Need OWL, RDF, RDFS, XML, XSD... plus any that the ontology uses
(defn map-prefixes
  "Given an ontology IRI and a vector of annotations,
   return a map of prefixes for parsing into XML."
  [iri annotations]
  ;; Add prefixes used in annotations
  ;; For the most part, these should be defined by base prefixes
  ;; but some ontologies may use their own or imported props...
  (reduce
   (fn [m a]
     (let [[[uri pref _] _] a
           k (keyword (str "xmlns:" pref))]
       ;; Don't need to update if it's already in there
       (if-not (contains? m k)
         (conj m {k (u/get-namespace uri)})
         m)))
   ;; Put into the map of base prefixes
   (get-base-prefixes iri) annotations))

;; TODO: Test grabbing prefixes ^^ (i.e. using dc)

(defn add-import
  "Helper function to map imports for XML parsing."
  [v i]
  (conj v
        {:tag :owl:imports,
         :attrs {:rdf:resource i},
         :content nil}))

(defn add-annotation
  "Helper function to map annotations for XML parsing."
  [v annotation]
  (let [[[_ pref ap] [value dt]] annotation]
    (if-not (nil? dt)
      (conj v
            {:tag (keyword (str pref ":" ap))
             :attrs {:rdf:datatype dt}
             :content [value]})
      (conj v
            {:tag (keyword (str pref ":" ap))
             :attrs nil
             :content [value]}))))

(defn map-rdf-node
  "Given an ontology IRI and the annotations,
   return a mapped RDF node for conversion to XML."
  [iri annotations]
  {:tag :rdf:RDF,
   :attrs (map-prefixes iri annotations)})

(defn map-metadata
  "Given the IRI and version IRI, a list of direct imports, and the
   annotations, return the mapped metadata for conversion to XML."
  [iri version imports annotations]
  {:tag :owl:Ontology,
   :attrs {:rdf:about iri},
   :content
   (into
    (reduce add-import
            [{:tag :owl:versionIRI,
              :attrs {:rdf:resource version}}]
            imports)
    (reduce add-annotation [] annotations))})
