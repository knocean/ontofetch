(ns ontofetch.parse.owl
  (:require
   [clojure.java.io :as io]
   [ontofetch.utils :as u])
  (:import
   (org.semanticweb.owlapi.apibinding OWLManager)
   (org.semanticweb.owlapi.model OWLOntologyManager OWLOntology IRI)))

(defn load-ontology
  "Given a filepath to an ontology,
   return the ontology as an OWLOntology."
  [filepath]
  (let [manager (OWLManager/createOWLOntologyManager)]
    (.loadOntologyFromOntologyDocument manager (io/file filepath))))

(defn get-ontology-iri
  "Given an OWLOntology, return the Ontology IRI."
  [owl-ont]
  (let [iri (.getOntologyIRI (.getOntologyID owl-ont))]
    (if (.isPresent iri)
      (.toString (.get iri))
      nil)))

(defn get-version-iri
  "Given an OWLOntology, return the Version IRI."
  [owl-ont]
  (let [iri (.getVersionIRI (.getOntologyID owl-ont))]
    (if (.isPresent iri)
      (.toString (.get iri))
      nil)))

(defn get-imports
  "Given an OWLOntology, return a vect of direct imports."
  [owl-ont]
  (reduce
    (fn [l o]
      (conj l (.toString o)))
    [] (.getDirectImportsDocuments owl-ont)))

(defn get-more-imports
  "Given a list of imports and a directory they are saved in,
   return a map of imports (keys) and their imports (vals)."
  [imports dir]
  (reduce
    (fn [m i]
      (let [ont (load-ontology (u/get-path-from-purl dir i))]
        (conj m {i (get-imports ont)})))
    {} imports))
