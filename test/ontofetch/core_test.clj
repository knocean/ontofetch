(ns ontofetch.core-test
  (:use [ontofetch.core])
  (:require
   [clojure.test :refer :all]))

(def summary
  (str
   "  -d, --dir <arg>        Directory to save downloads.\n"
   "  -u, --url <arg>        URL of the ontology to fetch.\n"
   "  -p, --project <arg>    Parent directory for the project.\n"
   "  -z, --zip              Compress the results.\n"
   "  -h, --help"))

(def input ["--dir" "d" "--url" "u" "--project" "p" "--zip"])

(def valid-return
  {:opts {:dir "d" :url "u" :project "p/" :zip true}})

(def help ["--help"])

(def help-return
  {:exit-msg (usage summary)
   :ok? true})

(deftest test-cli
  (is (= valid-return (validate-args input)))
  (is (= help-return (validate-args help))))
