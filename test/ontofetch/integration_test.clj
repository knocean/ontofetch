(ns ontofetch.integration-test
  (:require
   [clojure.test :refer :all]
   [ontofetch.ontofetch :refer :all]
   [ontofetch.parse.jena :as jena]
   [ontofetch.parse.owl :as owl]
   [ontofetch.parse.xml :as xml]
   [ontofetch.tools.utils :as u]))

(def dir "test/resources")

(def owl-dir (str dir "/owl"))
(def jena-dir (str dir "/jena"))
(def xml-dir (str dir "/xml"))
(def file "/test-1.owl")
(def file-obo "/test-1.obo")
(def file-ttl "/test-1.ttl")
(def file-2 "/test-3.owl")
(def file-ttl-2 "/test-3.ttl")
(def file-obo-2 "/test-3.obo")

;; -------------------------------------------------------------------
;;                         INTEGRATION TEST
;; -------------------------------------------------------------------

;; Expected output

(def md-1 {:ontology-iri "http://test.com/resources/test-1.owl",
           :version-iri "http://test.com/resources/2017/test-1.owl",
           :imports
           {"http://test.com/resources/test-2.owl"
            ["http://test.com/resources/test-4.owl"],
            "http://test.com/resources/test-3.owl" []}})

;; TODO: Deal with OBO imports at some point
;;       Quick fix so it doesn't fail every time...
(def md-obo-1 {:ontology-iri
               "http://purl.obolibrary.org/obo/test-1.owl",
               :version-iri
               "http://purl.obolibrary.org/obo/test-1/2017/test-1.owl",
               :imports {}})

(def md-ttl-1 {:ontology-iri "http://test.com/resources/test-1.ttl",
               :version-iri
               "http://test.com/resources/2017/test-1.ttl",
               :imports
               {"http://test.com/resources/test-2.ttl"
                ["http://test.com/resources/test-4.ttl"],
                "http://test.com/resources/test-3.ttl" []}})

(def md-2 {:ontology-iri "http://test.com/resources/test-3.owl",
           :version-iri nil,
           :imports {}})

(def md-obo-2 {:ontology-iri "http://purl.obolibrary.org/obo/test-3.owl",
               :version-iri nil,
               :imports {}})

(def md-ttl-2 {:ontology-iri "http://test.com/resources/test-3.ttl",
               :version-iri nil,
               :imports {}})

;; Test with direct & indirect imports

(def test-jena (str jena-dir file-ttl))
(def md-1-from-jena
  (let [trps (second (jena/read-triples test-jena))]
    (u/map-metadata
     [(jena/get-ontology-iri trps)
      (jena/get-version-iri trps)
      (try-get-imports
       (jena/get-imports trps)
       jena-dir)])))

(def test-obo (str owl-dir file-obo))
(def md-1-from-obo
  (let [ont (owl/load-ontology test-obo)]
    (u/map-metadata
     [(owl/get-ontology-iri ont)
      (owl/get-version-iri ont)
      (try-get-imports
       (owl/get-imports ont)
       owl-dir)])))

(def test-owl (str owl-dir file))
(def md-1-from-owl
  (let [ont (owl/load-ontology test-owl)]
    (u/map-metadata
     [(owl/get-ontology-iri ont)
      (owl/get-version-iri ont)
      (try-get-imports
       (owl/get-imports ont)
       owl-dir)])))

(def test-xml (str xml-dir file))
(def md-1-from-xml
  (let [xml (xml/get-metadata-node (xml/parse-xml test-xml))]
    (u/map-metadata
     [(xml/get-ontology-iri xml)
      (xml/get-version-iri xml)
      (try-get-imports
       (xml/get-imports xml)
       xml-dir)])))

;; Test with no imports

(def test-jena-2 (str jena-dir file-ttl-2))
(def md-2-from-jena
  (let [trps (second (jena/read-triples test-jena-2))]
    (u/map-metadata
     [(jena/get-ontology-iri trps)
      (jena/get-version-iri trps)
      (try-get-imports
       (jena/get-imports trps)
       jena-dir)])))

(def test-obo-2 (str owl-dir file-obo-2))
(def md-2-from-obo
  (let [ont (owl/load-ontology test-obo-2)]
    (u/map-metadata
     [(owl/get-ontology-iri ont)
      (owl/get-version-iri ont)
      (try-get-imports
       (owl/get-imports ont)
       owl-dir)])))

(def test-owl-2 (str owl-dir file-2))
(def md-2-from-owl
  (let [ont (owl/load-ontology test-owl-2)]
    (u/map-metadata
     [(owl/get-ontology-iri ont)
      (owl/get-version-iri ont)
      (try-get-imports
       (owl/get-imports ont)
       owl-dir)])))

(def test-xml-2 (str xml-dir file-2))
(def md-2-from-xml
  (let [xml (xml/get-metadata-node (xml/parse-xml test-xml-2))]
    (u/map-metadata
     [(xml/get-ontology-iri xml)
      (xml/get-version-iri xml)
      (try-get-imports
       (xml/get-imports xml)
       xml-dir)])))

(deftest test-jena
  (is (= md-ttl-1 md-1-from-jena))
  (is (= md-ttl-2 md-2-from-jena)))

(deftest test-owl
  (is (= md-1 md-1-from-owl))
  (is (= md-2 md-2-from-owl))
  (is (= md-obo-1 md-1-from-obo))
  (is (= md-obo-2 md-2-from-obo)))

(deftest test-xml
  (is (= md-1 md-1-from-xml))
  (is (= md-2 md-2-from-xml)))
