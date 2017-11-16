(ns ontofetch.core-test
  (:require 
    [clojure.test :refer :all]
    [ontofetch.parse.jena :as jena]
    [ontofetch.parse.owl :as owl]
    [ontofetch.parse.xml :as xml]
    [ontofetch.utils :as u]
    [clojure.java.io :as io]))

(def owl-dir "test/resources/owl")
(def jena-dir "test/resources/jena")
(def xml-dir "test/resources/xml")
(def file "/test-1.owl")
(def file-ttl "/test-1.ttl")

(def md {:ontology-iri "http://test.com/resources/test-1.owl",
         :version-iri "http://test.com/resources/2017/test-1.owl",
         :imports
         {"http://test.com/resources/test-2.owl"
          ["http://test.com/resources/test-4.owl"],
          "http://test.com/resources/test-3.owl" []}})

(def md-ttl {:ontology-iri "http://test.com/resources/test-1.ttl",
             :version-iri "http://test.com/resources/2017/test-1.ttl",
             :imports
             {"http://test.com/resources/test-2.ttl"
              ["http://test.com/resources/test-4.ttl"],
              "http://test.com/resources/test-3.ttl" []}})

;; ----------------------------------------------------------
;;                     INTEGRATION TEST
;; ----------------------------------------------------------

(def test-jena (str jena-dir file-ttl))
(def md-from-jena
  (let [trps (jena/read-triples test-jena)]
    (u/map-metadata
     [(jena/get-ontology-iri trps)
      (jena/get-version-iri trps)
      (jena/get-more-imports
       (jena/get-imports trps)
       jena-dir)])))

(def test-owl (str owl-dir file))
(def md-from-owl
  (let [ont (owl/load-ontology test-owl)]
    (u/map-metadata
     [(owl/get-ontology-iri ont)
      (owl/get-version-iri ont)
      (owl/get-more-imports
       (owl/get-imports ont)
       owl-dir)])))

(def test-xml (str xml-dir file))
(def md-from-xml
  (let [xml (xml/get-metadata-node test-xml)]
    (u/map-metadata
     [(xml/get-ontology-iri xml)
      (xml/get-version-iri xml)
      (xml/get-more-imports
       (xml/get-imports xml)
       xml-dir)])))

(deftest get-metadata
  (is (= md-ttl md-from-jena))
  (is (= md md-from-owl))
  (is (= md md-from-xml)))
