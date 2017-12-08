(ns ontofetch.core-test
  (:use [ontofetch.core])
  (:require
   [clojure.test :refer :all]))

(def summary
  (str
   "  -d, --dir  DIR   Directory to save downloads."
   "\n  -p, --purl PURL  PURL of the ontology to download."
   "\n  -z, --zip        Compress the results."
   "\n  -h, --help"))

(def input ["--dir" "d" "--purl" "p" "--zip"])

(def valid-return
  {:opts {:dir "d" :purl "p" :zip true}})

(def help ["--help"])

(def help-return
  {:exit-msg (usage summary)
   :ok? true})

(deftest test-cli
  (is (= valid-return (validate-args input)))
  (is (= help-return (validate-args help))))
