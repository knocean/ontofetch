(ns ontofetch.http-test
  (:use [ontofetch.tools.http])
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer :all]))

(def dir "test/resources")
(def bfo (str dir "/bfo.owl"))

;; Expected final URLs from redirects
(def bfo-final
  "https://raw.githubusercontent.com/BFO-ontology/BFO/v2.0/bfo.owl")
(def pr-final
  "ftp://ftp.proconsortium.org/databases/ontology/pro_obo/pro_reasoned.owl")
;; Get last from redirects
(def test-bfo
  (last (get-redirects "http://purl.obolibrary.org/obo/bfo.owl")))
(def test-pr
  (last (get-redirects "http://purl.obolibrary.org/obo/pr.owl")))

;; Run fetch, delete file, and return (hopefully) true
(def fetch-bfo
  (do
    (fetch-ontology! bfo bfo-final)
    (let [res (.exists (io/as-file bfo))]
      (io/delete-file bfo)
      res)))

;; Fetch an import, delete it, and return (hopefully) true
(def fetch-import
  (do
    (fetch-imports! dir [bfo-final])
    (let [res (.exists (io/as-file bfo))]
      (io/delete-file bfo)
      res)))

(deftest test-http
  (is (= bfo-final test-bfo))
  (is (= pr-final test-pr))
  (is (= true fetch-bfo))
  (is (= true fetch-import)))