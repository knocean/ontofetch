(ns ontofetch.files-test
  (:use [ontofetch.tools.files])
  (:require
   [clojure.test :refer :all]
   [clojure.java.io :as io]))

(def dir "test/resources")

;; Invalid directories
(def dir-1 "dir-1")
(def dir-2 "test")
;; Valid directory
(def dir-3 (str dir "/dir_3"))
(def test-mkdir
  (do
    (make-dir! dir-3)
    (.exists (io/as-file dir-3))))

(def test-zip
  (let [res (zip-folder! dir-3)]
    (io/delete-file (str dir-3 ".zip"))
    res))

(deftest test-files
  (is
   (thrown-with-msg?
    Exception
    #"Directory name can only include letters, numbers, or underscores."
    (make-dir! dir-1)))
  (is
   (thrown-with-msg?
    Exception
    #"Directory must not already exist in the file system."
    (make-dir! dir-2)))
  (is (= true test-mkdir))
  (is (= true test-zip)))