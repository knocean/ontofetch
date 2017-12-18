(ns ontofetch.fetch-test
  (:use
   [ontofetch.core.fetch])
  (:require
   [clojure.test :refer :all]))

(def ro "test/resources/ro/ro.")
(def out "test/output")

(def expected
  ["http://purl.obolibrary.org/obo/ro.owl"
   "http://purl.obolibrary.org/obo/ro/releases/2017-10-02/ro.owl"
   {"http://purl.obolibrary.org/obo/ro/annotations.owl" [],
    "http://purl.obolibrary.org/obo/ro/pato_import.owl" [],
    "http://purl.obolibrary.org/obo/ro/temporal-intervals.owl"
    ["http://purl.obolibrary.org/obo/ro/annotations.owl"
     "http://purl.obolibrary.org/obo/ro/core.owl"],
    "http://purl.obolibrary.org/obo/ro/go_cc_import.owl" [],
    "http://purl.obolibrary.org/obo/ro/go_mf_import.owl" [],
    "http://purl.obolibrary.org/obo/ro/core.owl"
    ["http://purl.obolibrary.org/obo/ro/annotations.owl"
     "http://purl.obolibrary.org/obo/ro/bfo-axioms.owl"
     "http://purl.obolibrary.org/obo/ro/bfo-classes-minimal.owl"],
    "http://purl.obolibrary.org/obo/ro/rohom.owl" [],
    "http://purl.obolibrary.org/obo/ro/go-biotic.owl" [],
    "http://purl.obolibrary.org/obo/ro/el-constraints.owl" []}])

(deftest test-fetch
  (is (= expected (parse-ontology out (str ro "owl"))))
  (is (= expected (parse-ontology out (str ro "ttl"))))
  (is (= expected (parse-ontology out (str ro "omn")))))
  ;;(is (= expected (parse-ontology ro-dir (str ro "obo")))))