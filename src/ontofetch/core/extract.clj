(ns ontofetch.core.extract
  (:require
   [clojure.tools.logging :as log]
   [ontofetch.parse.jena :as jena]
   [ontofetch.parse.owl :as owl]
   [ontofetch.parse.xml :as xml]
   [ontofetch.tools.files :as f]))

(defn try-xml
  "Given a directory for extracts and a location of the ontology to
   extract from, try extracting as XML."
  [extracts loc]
  ;; TODO: better way to determine xml?
  (xml/parse-xml loc)
  (f/spit-ont-element!
   extracts
   loc
   (f/extract-element loc)))

(defn try-jena
  "Given a directory for extracts and a location of the ontology to
   extract from, try extracting with Jena."
  [extracts loc]
  (let [ttl (jena/read-triples loc)]
    (f/spit-ont-element!
     extracts
     loc
     (xml/node->xml-str
      (jena/map-rdf-node ttl)
      (jena/map-metadata ttl)))))

(defn try-owl
  "Given a directory for extracts and a location of the ontology to
   extract from, try extracting with OWLAPI."
  [extracts loc]
  (let [ont (owl/load-ontology loc)
        annotations (owl/get-annotations ont)
        iri (owl/get-ontology-iri ont)
        version (owl/get-version-iri ont)
        imports (owl/get-imports ont)]
    (f/spit-ont-element!
     extracts
     loc
     (xml/node->xml-str
      (owl/map-rdf-node iri annotations)
      (owl/map-metadata iri version imports annotations)))))

(defn extract
  "Given a directory for extracts and a location of the ontology to
   extract from, extract the owl:Ontology element."
  [extracts loc]
  (f/make-dir! extracts)
  (log/debug "parsing" loc "as XML.")
  (try
    (try-xml extracts loc)
    (catch Exception e
      (do
        (log/debug e)
        (log/debug "parsing" loc "with Jena.")
        (try
          (try-jena extracts loc)
          (catch Exception e
            (do
              (log/debug e)
              (log/debug "parsing" loc "with OWLAPI.")
              (try
                (try-owl extracts loc)
                (catch Exception e
                  (log/fatal e))))))))))

(defn project-extract
  "Given a working directory (wd) and a project name, extract the
   owl:Ontology element from the most recent fetch."
  [wd project]
  (let [extracts (:extracts (f/slurp-config wd))
        loc (->> (f/slurp-catalog wd)
                 (filter
                  #(=
                    (:request-url %)
                    (:url (f/get-project-config wd project))))
                 first
                 :location)]
    (if-not (nil? loc)
      (extract (str wd extracts) loc)
      (do
        (log/fatal project "has not been fetched.")
        false))))

(defn dir-extract
  "Given a working directory (wd), get all projects from config and
   extract the owl:Ontology elements from their most recent fetches."
  [wd]
  (let [ids (reduce
             (fn [v p]
               (conj v (:id p)))
             [] (:projects (f/slurp-config wd)))]
    (dorun (map #(project-extract wd %) ids))))
