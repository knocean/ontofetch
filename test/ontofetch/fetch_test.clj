(ns ontofetch.fetch-test
  (:use
   [ontofetch.core.fetch])
  (:require
   [clojure.test :refer :all]))

(def ro "test/resources/ro/ro.")
(def out "test/output")

(def expected
  ["http://purl.obolibrary.org/obo/ro.owl"
   "http://purl.obolibrary.org/obo/ro/releases/2017-10-02/ro.owl"])

;; Not checking imports for now
;; etag & last-modified will probably be consistently changing
(deftest test-fetch
  (is (= expected (drop-last (parse-ontology out (str ro "owl")))))
  (is (= expected (drop-last (parse-ontology out (str ro "ttl")))))
  (is (= expected (drop-last (parse-ontology out (str ro "omn")))))
  (is (= expected (drop-last (parse-ontology out (str ro "obo"))))))
