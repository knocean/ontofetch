(ns ontofetch.html-test
  (:use [ontofetch.tools.html])
  (:require [clojure.test :refer :all]))

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
  (is (= html (gen-html catalog))))