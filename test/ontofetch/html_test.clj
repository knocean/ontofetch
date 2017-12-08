(ns ontofetch.html-test
  (:use [ontofetch.tools.html])
  (:require [clojure.test :refer :all]))

;; Expected HTML output
(def html
  (str
   "<html lang=\"en\"><head><meta charset=\"utf-8\" /><meta content="
   "\"width=device-width, initial-scale=1, shrink-to-fit=no\" name=\""
   "viewport\" /><title>Ontofetch Report</title><link href=\"resource"
   "s/static/css/bootstrap.min.css\" rel=\"stylesheet\" /></head><bod"
   "y><div class=\"container\"><div><h1>ontofetch Requests</h1><p cla"
   "ss=\"lead\">See the full metadata in <a href=\"catalog.edn\" targ"
   "et=\"_blank\">catalog.edn</a></p></div><hr /><div><h5><a href=\"h"
   "ttp://test.com/resources/test-1.owl\" target=\"_blank\">http://te"
   "st.com/resources/test-1.owl</a> on 2018-01-01T00:00:00</h5><b>Ope"
   "ration Time: </b>0 ms<br /><b>Version: </b>http://test.com/resour"
   "ces/2017/test-1.owl<br /><b>Location: </b><a href=\"test/resource"
   "s/test-1.owl\" target=\"_blank\">test/resources/test-1.owl</a><br"
   " /><div><b>Imports: </b><ul><li>http://test.com/resources/test-2."
   "owl<ul><li>http://test.com/resources/test-4.owl</li></ul></li><li"
   ">http://test.com/resources/test-3.owl</li></ul></div></div><div><"
   "h5><a href=\"http://test.com/resources/test-3.owl\" target=\"_bla"
   "nk\">http://test.com/resources/test-3.owl</a> on 2018-01-01T00:00"
   ":00</h5><b>Operation Time: </b>0 ms<br /><b>Version: </b>http://t"
   "est.com/resources/2017/test-3.owl<br /><b>Location: </b><a href="
   "\"test/resources/test-3.owl\" target=\"_blank\">test/resources/te"
   "st-3.owl</a><br /><div><b>Imports: </b>none<br /><br /></div></di"
   "v></div></body></html>"))

;; Read in test metadata (edn format)
(def catalog
  (clojure.edn/read-string
   (slurp "test/resources/test-catalog.edn")))

(deftest test-html
  (is (= html (gen-html catalog))))