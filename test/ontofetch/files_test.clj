(ns ontofetch.files-test
  (:use [ontofetch.tools.files])
  (:require
   [clojure.test :refer :all]
   [clojure.java.io :as io]))

(def dir "test/resources")

;; Invalid directories
(def dir-1 "dir-1")
;; Valid directory
(def dir-2 (str dir "/dir_2"))
(def fp (str dir-2 "/test.txt"))
(def test-mkdir
  (do
    (make-dir! dir-2 fp)
    (spit-catalog-v001! dir-2 "test")
    (.exists (io/as-file (str dir-2 "/catalog-v001.xml")))))

(def test-zip (zip! dir-2))
(def test-exists
  (let [res (file-exists? dir-2 "http://test.com/catalog-v001.xml")]
    (io/delete-file (io/as-file (str dir-2 ".zip")))
    res))

(deftest test-files
  (is
   (thrown-with-msg?
    Exception
    #"Directory name can only include letters, numbers, or underscores."
    (make-dir! dir-1 (str dir-1 fp))))
  (is (= true test-mkdir))
  (is (= true test-zip))
  (is (= true test-exists)))