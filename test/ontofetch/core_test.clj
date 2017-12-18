(ns ontofetch.core-test
  (:use [ontofetch.core])
  (:require
   [clojure.test :refer :all]
   [ontofetch.command :as cmd]))

(def extract
  (:exit-msg (validate-args ["extract" "-h"])))

(def fetch
  (:exit-msg (validate-args ["fetch" "-h"])))

(def status
  (:exit-msg (validate-args ["status" "-h"])))

(def update
  (:exit-msg (validate-args ["update" "-h"])))

(def invalid
  (:exit-msg (validate-args ["invalid" "-h"])))

(deftest test-cli
  (is (= cmd/extract-usage extract))
  (is (= cmd/fetch-usage fetch))
  (is (= cmd/status-usage status))
  (is (= cmd/update-usage update))
  (is (= cmd/usage invalid)))
