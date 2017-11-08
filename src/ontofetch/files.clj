(ns ontofetch.files
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.string :as s]
   [clojure.tools.file-utils :as ctfu]
   [ontofetch.html :as html])
  (:import
   [java.util.zip ZipEntry ZipOutputStream]))

(def +catalog+ "catalog.edn")
(def +report+ "report.html")
(def catalog (atom
              (if (.exists (io/as-file +catalog+))
                (edn/read-string (slurp +catalog+))
                [])))

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
  "Checks if arg is a valid dir name,
   then makes a new directory & returns the name."
  [dir]
  (if (valid-dir? dir)
    (.mkdir (java.io.File. dir)))
  dir)

(defn spit-report!
  "Generates the HTML report in cuurent directory."
  []
  (spit +report+ (html/gen-html @catalog)))

(defn update-catalog!
  "Adds the request details to the catalog,
   then writes the catalog to current directory."
  [request-details]
  (do
    (swap! catalog (fn [cur] (conj cur request-details)))
    (pp/pprint @catalog (io/writer +catalog+))))

(defn spit-catalog-v001!
  [dir catalog-v001]
  (let [filepath (str dir "/catalog-v001.xml")]
    (spit filepath catalog-v001)))

(defn zip-folder!
  "Creates a compressed file containing directory contents.
   Deletes the original directory when complete."
  [dir]
  (with-open [zip (ZipOutputStream. (io/output-stream (str dir ".zip")))]
    (doseq [f (file-seq (io/file dir)) :when (.isFile f)]
      (.putNextEntry zip (ZipEntry. (.getPath f)))
      (io/copy f zip)
      (.closeEntry zip)))
  (ctfu/recursive-delete (io/file dir)))

(defn gen-content!
  [dir request-details catalog-v001]
  (update-catalog! request-details)
  (spit-catalog-v001! dir catalog-v001)
  (spit-report!)
  (zip-folder! dir))
