(ns ontofetch.parse-test
  (:use [ontofetch.parse.xml])
  (:require
   [clojure.test :refer :all]
   [ontofetch.parse.jena :as jena]
   [ontofetch.parse.owl :as owl]))

(def dir "test/resources")
(def owl-dir (str dir "/owl"))
(def jena-dir (str dir "/jena"))
(def xml-dir (str dir "/xml"))
(def file "/test-1.owl")
(def file-ttl "/test-1.ttl")
(def file-obo "/test-1.obo")

;; ------------------------- ONTOFETCH.JENA --------------------------
;; Prepare for use in XML test
(def test-rdf-node-ttl
  (let [ttl (jena/read-triples (str jena-dir file-ttl))]
    (clojure.string/replace
     (node->xml-str
      (jena/map-rdf-node ttl)
      (jena/map-metadata
       "http://test.com/resources/test-1.ttl"
       ttl))
     "\n" "")))

;; -------------------------- ONTOFETCH.OWL --------------------------
;; Prepare for use in XML test
(def test-rdf-node-obo
  (let [iri "http://test.com/resources/test-1.owl"
        version "http://test.com/resources/2017/test-1.owl"
        owl-ont (owl/load-ontology (str owl-dir file-obo))
        imports (owl/get-imports owl-ont)
        annotations (owl/get-annotations owl-ont)]
    (clojure.string/replace
     (node->xml-str
      (owl/map-rdf-node iri annotations)
      (owl/map-metadata iri version imports annotations))
     "\n" "")))

(def test-rdf-node-owl
  (let [iri "http://test.com/resources/test-1.owl"
        version "http://test.com/resources/2017/test-1.owl"
        owl-ont (owl/load-ontology (str owl-dir file))
        imports (owl/get-imports owl-ont)
        annotations (owl/get-annotations owl-ont)]
    (clojure.string/replace
     (node->xml-str
      (owl/map-rdf-node iri annotations)
      (owl/map-metadata iri version imports annotations))
     "\n" "")))

;; -------------------------- ONTOFETCH.XML --------------------------

(def test-catalog-v001
  (str
   "<?xml version=\"1.0\" encoding=\"UTF-8\"?><catalog xml"
   "ns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\" pre"
   "fer=\"public\">\n  <uri name=\"http://test.com/resourc"
   "es/test-4.owl\" uri=\"test-4.owl\"/>\n  <uri name=\"ht"
   "tp://test.com/resources/test-2.owl\" uri=\"test-2.owl\""
   "/>\n  <uri name=\"http://test.com/resources/test-3.owl"
   "\" uri=\"test-3.owl\"/>\n</catalog>\n"))

(def i-map {"http://test.com/resources/test-2.owl"
            ["http://test.com/resources/test-4.owl"],
            "http://test.com/resources/test-3.owl" []})

(def rdf-node-owl
  (str
   "<rdf:RDF xmlns=\"http://test.com/resources/test-1.owl#\" xml:base"
   "=\"http://test.com/resources/test-1.owl\" xmlns:owl=\"http://www."
   "w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rd"
   "f-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#"
   "\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" xmlns:xsd="
   "\"http://www.w3.org/2001/XMLSchema#\"><owl:Ontology rdf:about=\"h"
   "ttp://test.com/resources/test-1.owl\"><owl:versionIRI rdf:resourc"
   "e=\"http://test.com/resources/2017/test-1.owl\"/><owl:imports rdf"
   ":resource=\"http://test.com/resources/test-3.owl\"/><owl:imports "
   "rdf:resource=\"http://test.com/resources/test-2.owl\"/><rdfs:comm"
   "ent rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">Test"
   " ontology for ontofetch.</rdfs:comment></owl:Ontology></rdf:RDF>"))

(def rdf-node-ttl
  (str
   "<rdf:RDF xmlns=\"http://test.com/resources/test-1.ttl#\" xml:base"
   "=\"http://test.com/resources/test-1.ttl\" xmlns:owl=\"http://www."
   "w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rd"
   "f-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#"
   "\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" xmlns:xsd="
   "\"http://www.w3.org/2001/XMLSchema#\"><owl:Ontology rdf:about=\"h"
   "ttp://test.com/resources/test-1.ttl\"><owl:versionIRI rdf:resourc"
   "e=\"http://test.com/resources/2017/test-1.ttl\"/><owl:imports rdf"
   ":resource=\"http://test.com/resources/test-3.ttl\"/><owl:imports "
   "rdf:resource=\"http://test.com/resources/test-2.ttl\"/><rdfs:comm"
   "ent rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">Test"
   " ontology for ontofetch.</rdfs:comment></owl:Ontology></rdf:RDF>"))

(def rdf-node-xml
  (str
   "<?xml version=\"1.0\"?><rdf:RDF xmlns=\"http://test.com/resource"
   "s/test-1.owl#\" xml:base=\"http://test.com/resources/test-1.owl"
   "\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xml"
   "ns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:xml=\"http://www"
   ".w3.org/XML/1998/namespace\" xmlns:xsd=\"http://www.w3.org/2001/"
   "XMLSchema#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#"
   "\"><owl:Ontology rdf:about=\"http://test.com/resources/test-1.ow"
   "l\"><owl:versionIRI rdf:resource=\"http://test.com/resources/201"
   "7/test-1.owl\"/><owl:imports rdf:resource=\"http://test.com/reso"
   "urces/test-3.owl\"/><owl:imports rdf:resource=\"http://test.com/"
   "resources/test-2.owl\"/><rdfs:comment rdf:datatype=\"http://www."
   "w3.org/2001/XMLSchema#string\">Test ontology for ontofetch.</rdf"
   "s:comment></owl:Ontology></rdf:RDF>"))

(def test-rdf-node-xml
  (clojure.string/replace
   (clojure.string/replace
    (ontofetch.tools.files/extract-element (str xml-dir file))
    "  " "")
   "\n" ""))

(deftest test-xml
  (is (= test-catalog-v001 (catalog-v001 i-map)))
  (is (= rdf-node-owl test-rdf-node-obo))
  (is (= rdf-node-owl test-rdf-node-owl))
  (is (= rdf-node-ttl test-rdf-node-ttl))
  (is (= rdf-node-xml test-rdf-node-xml)))
