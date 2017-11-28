(ns ontofetch.tools.files
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.tools.file-utils :as ctfu]
   [ontofetch.parse.xml :as xml]
   [ontofetch.tools.html :as h])
  (:import
   [java.util.zip ZipEntry ZipOutputStream]))

(def +catalog+ "catalog.edn")
(def +report+ "report.html")
(def catalog (atom
              (if (.exists (io/as-file +catalog+))
                (edn/read-string (slurp +catalog+))
                [])))
(def ont-elements "resources/elements/")

;;----------------------------- FOLDERS ------------------------------

(defn valid-dir?
  "Given a directory name, checks if it already exists and that the
   name is formatted correctly."
  [dir]
  (when-not (re-matches #"[A-Za-z0-9_/]+" dir)
    (throw
     (Exception.
      (str "Directory name can only include letters, numbers,"
           " or underscores."))))
  (when (.isDirectory (io/file dir))
    (throw
     (Exception.
      "Directory must not already exist in the file system.")))
  true)

(defn make-dir!
  "Given a directory name, checks if it is a valid dir name,
   then makes a new directory & returns the name."
  [dir]
  (if (valid-dir? dir)
    (.mkdir (java.io.File. dir)))
  dir)

(defn zip-folder!
  "Given a directory name, compress that directory
   and delete the original."
  [dir]
  (with-open [zip (ZipOutputStream.
                   (io/output-stream (str dir ".zip")))]
    (doseq [f (file-seq (io/file dir)) :when (.isFile f)]
      (.putNextEntry zip (ZipEntry. (.getPath f)))
      (io/copy f zip)
      (.closeEntry zip)))
  (ctfu/recursive-delete (io/file dir)))

;;----------------------------- OUTPUT -------------------------------

(defn spit-report!
  "Assuming a catalog atom exists, generate the HTML report."
  []
  (spit +report+ (h/gen-html @catalog))
  true)

(defn spit-catalog-v001!
  "Given a directory and the catalog-v001 as an XML string,
   spit the string to catalog-v001.xml."
  [dir catalog-v001]
  (let [filepath (str dir "/catalog-v001.xml")]
    (spit filepath catalog-v001)))

(defn spit-ont-element!
  "Given a directory and the XML string of the ontology element,
   spit the file as [dir]-element.owl."
  [dir ont-element]
  (spit (str dir "/" dir "-element.owl") ont-element))

(defn update-catalog!
  "Given a map of request details, add the details to the catalog atom
   and write the catalog to current directory."
  [request-details]
  (swap! catalog (fn [cur] (conj cur request-details)))
  (pp/pprint @catalog (io/writer +catalog+)))

(defn gen-content!
  "Do a bunch of stuff."
  [dir request-details catalog-v001]
  (update-catalog! request-details)
  (spit-catalog-v001! dir catalog-v001)
  (spit-report!))
