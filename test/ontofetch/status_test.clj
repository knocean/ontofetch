(ns ontofetch.status-test
  (:use
   [ontofetch.core.status])
  (:require
   [clojure.test :refer :all]))

(def wd "test/")

(def expected
  [{:id "output"
    :location "output/2017_12_18/bfo.owl"}])
(def output (dir-status wd))

(deftest test-status
  (is (= expected output)))
