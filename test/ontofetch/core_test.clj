(ns ontofetch.core-test
  (:use [ontofetch.core])
  (:require
   [clojure.test :refer :all]
   [ontofetch.ontofetch :refer :all]
   [ontofetch.parse.jena :as jena]
   [ontofetch.parse.owl :as owl]
   [ontofetch.parse.xml :as xml]
   [ontofetch.tools.files :as files]
   [ontofetch.tools.html :as html]
   [ontofetch.tools.http :as http]
   [ontofetch.tools.utils :as u]
   [clojure.java.io :as io]))

(def dir "test/resources")

(def owl-dir (str dir "/owl"))
(def jena-dir (str dir "/jena"))
(def xml-dir (str dir "/xml"))
(def file "/test-1.owl")
(def file-ttl "/test-1.ttl")
(def file-2 "/test-3.owl")
(def file-ttl-2 "/test-3.ttl")

;; ----------------------------------------------------------
;;                     INTEGRATION TEST
;; ----------------------------------------------------------

;; Expected output

(def md-1 {:ontology-iri "http://test.com/resources/test-1.owl",
           :version-iri "http://test.com/resources/2017/test-1.owl",
           :imports
           {"http://test.com/resources/test-2.owl"
            ["http://test.com/resources/test-4.owl"],
            "http://test.com/resources/test-3.owl" []}})

(def md-ttl-1 {:ontology-iri "http://test.com/resources/test-1.ttl",
               :version-iri "http://test.com/resources/2017/test-1.ttl",
               :imports
               {"http://test.com/resources/test-2.ttl"
                ["http://test.com/resources/test-4.ttl"],
                "http://test.com/resources/test-3.ttl" []}})

(def md-2 {:ontology-iri "http://test.com/resources/test-3.owl",
           :version-iri "http://test.com/resources/2017/test-3.owl",
           :imports {}})

(def md-ttl-2 {:ontology-iri "http://test.com/resources/test-3.ttl",
               :version-iri "http://test.com/resources/2017/test-3.ttl",
               :imports {}})

;; Test with direct & indirect imports

(def test-jena (str jena-dir file-ttl))
(def md-1-from-jena
  (let [trps (jena/read-triples test-jena)]
    (u/map-metadata
     [(jena/get-ontology-iri trps)
      (jena/get-version-iri trps)
      (try-get-imports
       (jena/get-imports trps)
       jena-dir)])))

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
  (let [xml (xml/get-metadata-node test-xml)]
    (u/map-metadata
     [(xml/get-ontology-iri xml)
      (xml/get-version-iri xml)
      (try-get-imports
       (xml/get-imports xml)
       xml-dir)])))

;; Test with no imports

(def test-jena (str jena-dir file-ttl-2))
(def md-2-from-jena
  (let [trps (jena/read-triples test-jena)]
    (u/map-metadata
     [(jena/get-ontology-iri trps)
      (jena/get-version-iri trps)
      (try-get-imports
       (jena/get-imports trps)
       jena-dir)])))

(def test-owl (str owl-dir file-2))
(def md-2-from-owl
  (let [ont (owl/load-ontology test-owl)]
    (u/map-metadata
     [(owl/get-ontology-iri ont)
      (owl/get-version-iri ont)
      (try-get-imports
       (owl/get-imports ont)
       owl-dir)])))

(def test-xml (str xml-dir file-2))
(def md-2-from-xml
  (let [xml (xml/get-metadata-node test-xml)]
    (u/map-metadata
     [(xml/get-ontology-iri xml)
      (xml/get-version-iri xml)
      (try-get-imports
       (xml/get-imports xml)
       xml-dir)])))

(deftest test-parsing
  (is (= md-ttl-1 md-1-from-jena))
  (is (= md-1 md-1-from-owl))
  (is (= md-1 md-1-from-xml))
  (is (= md-ttl-2 md-2-from-jena))
  (is (= md-2 md-2-from-owl))
  (is (= md-2 md-2-from-xml)))

;; ----------------------------------------------------------
;;                        UNIT TESTS
;; ----------------------------------------------------------

;; ----------------------- CLI TESTS ------------------------

(def summary
  (str
   "  -d, --dir DIR    Directory to save downloads.\n  -p, "
   "--purl PURL  PURL of the ontology to download.\n  -h, -"
   "-help"))

(def input ["--dir" "d" "--purl" "p"])

(def valid-return
  {:opts {:dir "d" :purl "p"}})

(def help ["--help"])

(def help-return
  {:exit-msg (usage summary)
   :ok? true})

(deftest test-cli
  (is (= valid-return (validate-args input)))
  (is (= help-return (validate-args help))))

;; -------------------- ONTOFETCH.FILES ---------------------
;; Invalid directories
(def dir-1 "dir-1")
(def dir-2 "test")
;; Valid directory
(def dir-3 (str dir "/dir_3"))
(def test-mkdir
  (do
    (files/make-dir! dir-3)
    (.exists (io/as-file dir-3))))

(def test-zip
  (let [res (files/zip-folder! dir-3)]
    (io/delete-file (str dir-3 ".zip"))
    res))

(deftest test-files
  (is
   (thrown-with-msg?
    Exception
    #"Directory name can only include letters, numbers, or underscores."
    (files/make-dir! dir-1)))
  (is
   (thrown-with-msg?
    Exception
    #"Directory must not already exist in the file system."
    (files/make-dir! dir-2)))
  (is (= true test-mkdir))
  (is (= true test-zip)))

;; --------------------- ONTOFETCH.HTML ---------------------
;; Expected HTML output
(def html
  (str
   "<html lang=\"en\"><head><meta charset=\"utf-8\" /><meta c"
   "ontent=\"width=device-width, initial-scale=1, shrink-to-f"
   "it=no\" name=\"viewport\" /><title>Ontofetch Report</titl"
   "e><link href=\"resources/static/css/bootstrap.min.css\" r"
   "el=\"stylesheet\" /></head><body><div class=\"container\""
   "><div><h1>ontofetch Requests</h1><p class=\"lead\">See th"
   "e full metadata in <a href=\"catalog.edn\" target=\"_blan"
   "k\">catalog.edn</a></p></div><hr /><div><h5><a href=\"htt"
   "p://test.com/resources/test-1.owl\" target=\"_blank\">htt"
   "p://test.com/resources/test-1.owl</a> on 2018-01-01 00:00"
   ":00</h5>Version: http://test.com/resources/2017/test-1.ow"
   "l<br />Location: <a href=\"test/resources/test-1.owl\" ta"
   "rget=\"_blank\">test/resources/test-1.owl</a><br /><div>I"
   "mports: <ul><li>http://test.com/resources/test-2.owl<ul><"
   "li>http://test.com/resources/test-4.owl</li></ul></li><li"
   ">http://test.com/resources/test-3.owl</li></ul></div></di"
   "v><div><h5><a href=\"http://test.com/resources/test-3.owl"
   "\" target=\"_blank\">http://test.com/resources/test-3.owl"
   "</a> on 2018-01-01 00:00:00</h5>Version: http://test.com/"
   "resources/2017/test-3.owl<br />Location: <a href=\"test/r"
   "esources/test-3.owl\" target=\"_blank\">test/resources/te"
   "st-3.owl</a><br /><div>Imports: none<br /><br /></div></d"
   "iv></div></body></html>"))

;; Read in test metadata (edn format)
(def catalog
  (clojure.edn/read-string
   (slurp "test/resources/test-catalog.edn")))

(deftest test-html
  (is (= html (html/gen-html catalog))))

;; --------------------- ONTOFETCH.HTTP ---------------------

(def bfo (str dir "/bfo.owl"))

;; Expected final URLs from redirects
(def bfo-final
  "https://raw.githubusercontent.com/BFO-ontology/BFO/v2.0/bfo.owl")
(def pr-final
  "ftp://ftp.proconsortium.org/databases/ontology/pro_obo/pro_reasoned.owl")
;; Get last from redirects
(def test-bfo
  (last (http/get-redirects "http://purl.obolibrary.org/obo/bfo.owl")))
(def test-pr
  (last (http/get-redirects "http://purl.obolibrary.org/obo/pr.owl")))

;; Run fetch, delete file, and return (hopefully) true
(def fetch-bfo
  (do
    (http/fetch-ontology! bfo bfo-final)
    (let [res (.exists (io/as-file bfo))]
      (io/delete-file bfo)
      res)))

;; Fetch an import, delete it, and return (hopefully) true
(def fetch-import
  (do
    (http/fetch-imports! dir [bfo-final])
    (let [res (.exists (io/as-file bfo))]
      (io/delete-file bfo)
      res)))

(deftest test-http
  (is (= bfo-final test-bfo))
  (is (= pr-final test-pr))
  (is (= true fetch-bfo))
  (is (= true fetch-import)))

;; -------------------- ONTOFETCH.XML ----------------------

(def catalog-v001
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

(deftest test-xml
  (is (= catalog-v001 (xml/catalog-v001 i-map))))
