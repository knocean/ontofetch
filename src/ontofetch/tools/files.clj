(ns ontofetch.tools.files
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.string :as s]
   [clojure.tools.file-utils :as ctfu]
   [ontofetch.parse.xml :as xml]
   [ontofetch.tools.html :as h])
  (:import
   [java.util.zip ZipEntry ZipOutputStream ZipInputStream]))

(def +catalog+ "catalog.edn")
(def +report+ "report.html")
(def catalog (atom
              (if (.exists (io/as-file +catalog+))
                (clojure.edn/read-string (slurp +catalog+))
                [])))
(def ont-elements "resources/elements/")

;;----------------------------- READING ------------------------------

(defn extract-element
  "Given the filepath to an RDF-XML format ontology,
   extract just the owl:Ontology element."
  [filepath]
  (with-open [r (clojure.java.io/reader filepath)]
    (s/join
     "\n"
     (into
      (vec
       (take-while
        #(not (s/includes? % "</owl:Ontology>"))
        (line-seq r)))
      ["    </owl:Ontology>"
       "</rdf:RDF>"]))))

(defn same-version?
  "Given a request URL and response headers (ETag, Last-Modified),
   check if the catalog has data on the same version already.
   Return true if so."
  [url response]
  ;; Filter the most recent request for this URL
  (if-let [e (->> @catalog
                  (filter #(= (:request-url %) url))
                  last
                  :response)] 
    (if-not (nil? (:etag response))
      ;; Compare the existing ETag to new ETag
      (= (:etag e) (:etag response))
      (if-not (nil? (:last-modified response))
        ;; Compare modification dates
        (= (:last-modified e) (:last-modified response))))))

(defn unzip!
  "Given the path to a zip file, unzip all contents."
  [zip]
  ;; Open a stream
  (with-open [s (-> zip
                    (io/as-file)
                    (io/input-stream)
                    (java.util.zip.ZipInputStream.))]
    ;; Loop over stream
    (loop [p (.getName (.getNextEntry s))]
      ;; Folder should exist, but just in case...
      (io/make-parents p)
      (io/copy s (io/as-file p))
      (if-let [n (.getNextEntry s)]
        ;; Keep going if there's a next entry
        (recur (.getName n))
        ;; Otherwise delete the zip and return true
        (do
          (io/delete-file zip) 
          true)))))

(defn file-exists?
  "Given a directory name and a request URL,
   check if the download already exists in that directory.
   If it doesn't, check if the zip exists, and unzip it if so.
   Otherwise, returns false."
  [dir url]
  ;; Check if the file exists
  (if 
    (.exists 
      (io/as-file (ontofetch.tools.utils/path-from-url dir url)))
    true
    ;; Check if the folder has been zipped
    (if (.exists (io/as-file (str dir ".zip")))
      (do
        ;; Unzip it, and then make sure the file is in there
        (unzip! (str dir ".zip"))
        (recur dir url))
      false)))

;;----------------------------- FOLDERS ------------------------------

(defn valid-dir?
  "Given a directory name, checks if it already exists and that the
   name is formatted correctly."
  [dir]
  (when-not (re-matches #"[A-Za-z0-9_/.]+" dir)
    (throw
     (Exception.
      (str "Directory name can only include letters, numbers,"
           " or underscores."))))
  true)

(defn make-dir!
  "Given a directory name, checks if it is a valid dir name,
   then makes a new directory & returns the name."
  [dir]
  ;; If it already exists, we don't need to make it again
  (if (and (valid-dir? dir) (not (.isDirectory (io/file dir)))) 
    (.mkdir (java.io.File. dir)))
  dir)

(defn zip!
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
  (spit (str dir "-element.owl") ont-element))

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
