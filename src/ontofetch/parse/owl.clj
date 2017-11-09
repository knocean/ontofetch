(ns ontofetch.parse.owl
  (:require
   [clojure.java.io :as io]
   [ontofetch.utils :as u])
  (:import
   (org.semanticweb.owlapi.apibinding OWLManager)
   (org.semanticweb.owlapi.model OWLOntologyManager OWLOntology IRI)))

(defn load-ont
  "Given a filepath to an ontology,
   return the ontology as an OWLOntology." 
  [filepath]
  (let [manager (OWLManager/createOWLOntologyManager)]
    (.loadOntologyFromOntologyDocument manager (io/file filepath))))

(defn format-iri
  "Format IRI string returned from OWLAPI." 
  [iri]
  (subs iri (inc (.indexOf iri "[")) (.indexOf iri "]")))

(defn get-ontology-iri
  "Given an OWLOntology, return the Ontology IRI." 
  [owl-ont]
  (-> owl-ont
      .getOntologyID
      .getOntologyIRI
      .toString
      format-iri))

(defn get-version-iri
  "Given an OWLOntology, return the Version IRI." 
  [owl-ont]
  (-> owl-ont
      .getOntologyID
      .getVersionIRI
      .toString
      format-iri))

(defn full-get-imports
  "Given an OWLOntology, return a map of direct (key)
   and indirect imports (vals)."  
  [owl-ont]
  (if-not (empty? (.getDirectImportsDocuments owl-ont))
    (loop [is (.getDirectImports owl-ont)
           import-map {}] 
      (let [dir (get-ontology-iri (first is))
            indirs (mapv get-ontology-iri (.getImports (first is)))]
        (if-not (empty? (rest is))
          (recur (rest is) (conj import-map {dir indirs})) 
          (conj import-map {dir indirs}))))))

(def get-imports (memoize full-get-imports))

(defn save-owl-ont!
  "Given an OWLOntology and a filepath, save the ontology." 
  [owl-ont filepath]
  (println (str "Saving: " filepath))
  (->> filepath
       io/file
       io/as-url
       IRI/create
       (.saveOntology (.getOWLOntologyManager owl-ont) owl-ont)))

(defn fetch-imports!
  "Given a directory and an OWLOntology,
   save all imports to the directory." 
  [dir owl-ont]
  (map
    #(save-owl-ont! % (u/get-path-from-purl dir (get-ontology-iri %)))
    (.getImports owl-ont)))  
