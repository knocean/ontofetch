(ns ontofetch.core-test
  (:require 
    [clojure.test :refer :all]
    [ontofetch.http :as http]
    [ontofetch.parse.jena :as jena]
    [ontofetch.parse.owl :as owl]
    [ontofetch.parse.xml :as xml]
    [ontofetch.utils :as u]
    [clojure.java.io :as io]
    [clojure.tools.file-utils :as ctfu]))

(def dir "test/resources/temp")

;; TODO: Build cleaner test ontology,
;;       add local import files we don't have to fetch each time

(def md {:ontology-iri "http://purl.obolibrary.org/obo/ro.owl",
         :version-iri
         "http://purl.obolibrary.org/obo/ro/releases/2017-10-02/ro.owl",
         :imports
         {"http://purl.obolibrary.org/obo/ro/rohom.owl" [],
          "http://purl.obolibrary.org/obo/ro/core.owl"
          ["http://purl.obolibrary.org/obo/ro/annotations.owl"
           "http://purl.obolibrary.org/obo/ro/bfo-axioms.owl"
           "http://purl.obolibrary.org/obo/ro/bfo-classes-minimal.owl"],
          "http://purl.obolibrary.org/obo/ro/temporal-intervals.owl"
          ["http://purl.obolibrary.org/obo/ro/annotations.owl"
           "http://purl.obolibrary.org/obo/ro/core.owl"],
          "http://purl.obolibrary.org/obo/ro/go-biotic.owl" [],
          "http://purl.obolibrary.org/obo/ro/pato_import.owl" [],
          "http://purl.obolibrary.org/obo/ro/go_cc_import.owl" [],
          "http://purl.obolibrary.org/obo/ro/go_mf_import.owl" [],
          "http://purl.obolibrary.org/obo/ro/annotations.owl" [],
          "http://purl.obolibrary.org/obo/ro/el-constraints.owl" []}})

(def test-jena "test/resources/ro.ttl")
(def md-from-jena
  (let [trps (jena/read-triples test-jena)]
    (.mkdir (java.io.File. dir))
    (let [imports (jena/get-imports trps)]
      ((partial http/fetch-imports! dir) imports)
      (let [md (u/map-metadata
                [(jena/get-ontology-iri trps)
                 (jena/get-version-iri trps)
                 (jena/get-more-imports imports dir)])]
        (ctfu/recursive-delete (io/file dir))
        md))))

(def test-owlapi "test/resources/ro.omn")
(def md-from-owlapi
  (let [ont (owl/load-ontology test-owlapi)]
    (.mkdir (java.io.File. dir))
    (let [imports (owl/get-imports ont)]
      ((partial http/fetch-imports! dir) imports)
      (let [md (u/map-metadata
                [(owl/get-ontology-iri ont)
                 (owl/get-version-iri ont)
                 (owl/get-more-imports imports dir)])]
        (ctfu/recursive-delete (io/file dir))
        md))))

(def test-xml "test/resources/ro.owl")
(def md-from-xml
  (let [xml (xml/get-metadata-node test-xml)]
    (.mkdir (java.io.File. dir))
    (let [imports (xml/get-imports xml)]
      ((partial http/fetch-imports! dir) imports)
      (let [md (u/map-metadata
                [(xml/get-ontology-iri xml)
                 (xml/get-version-iri xml)
                 (xml/get-more-imports imports dir)])]
        (ctfu/recursive-delete (io/file dir))
        md))))

(deftest get-metadata
  (is (= md md-from-jena))
  (is (= md md-from-owlapi))
  (is (= md md-from-xml)))
