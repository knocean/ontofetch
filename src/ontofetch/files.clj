(ns ontofetch.files
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.string :as s]
   [ontofetch.html]))

(def +catalog+ "catalog.edn")
(def +report+ "report.html")
(def catalog (if (.exists (io/as-file +catalog+))
               (edn/read-string (slurp +catalog+))
               []))

(defn valid-dir?
  "Checks if a directory is in proper format and that it does not exist.
   Returns true if so."
  [dir]
  (when-not (re-matches #"[A-Za-z0-9_]+" dir)
    (throw (Exception. "Directory name can only include letters, numbers, or
      underscores.")))
  (when (.isDirectory (io/file dir))
    (throw (Exception. "Directory must not already exist in the file system.")))
  true)

(defn make-dir!
  "Checks if arg is a valid dir name, then makes a new directory & returns the name."
  [dir]
  (if (valid-dir? dir)
    (.mkdir (java.io.File. dir)))
  dir)

(defn spit-report!
  []
  (spit +report+ (ontofetch.html/gen-html catalog)))

(defn spit-request!
  "Adds the request details to the catalog."
  [request-details]
  (pp/pprint (conj catalog request-details) (io/writer +catalog+)))

(defn spit-catalog-v001!
  [dir catalog-v001]
  (let [filepath (str dir "/catalog-v001.xml")]
    (spit filepath catalog-v001)))
