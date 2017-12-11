(ns ontofetch.tools.files
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.string :as s]
   [clojure.tools.file-utils :as ctfu]
   [ontofetch.parse.xml :as xml]
   [ontofetch.tools.html :as h]
   [ontofetch.tools.utils :as u])
  (:import
   [java.util.zip ZipEntry ZipOutputStream ZipInputStream]))

(def +catalog+ "catalog.edn")
(def +report+ "report.html")

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
  [dir filepath]
  ;; If it already exists, we don't need to make it again
  (if (and (valid-dir? dir) (not (.isDirectory (io/file dir))))
    (io/make-parents filepath)
    dir))

(defn zip!
  "Given a directory name, compress that directory
   and delete the original."
  [dir]
  ;; TODO: Put zips in project dir if specified
  (with-open [zip (ZipOutputStream.
                   (io/output-stream
                    (str dir ".zip")))]
    (doseq [f (file-seq (io/file dir)) :when (.isFile f)]
      (.putNextEntry zip (ZipEntry. (.getPath f)))
      (io/copy f zip)
      (.closeEntry zip)))
  (ctfu/recursive-delete (io/file dir)))

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

;;----------------------------- READING ------------------------------

(defn read-catalog
  "Given a working directory path, read the catalog.edn contents.
   If it does not exist, return empty vector."
  [working-dir]
  (let [cat (str working-dir +catalog+)]
    (if (.exists (io/as-file cat))
      (->> cat
           slurp
           clojure.edn/read-string))))

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

(defn get-last-metadata
  "Given a request URL and response headers (ETag, Last-Modified),
   check if the catalog has data on the same version already.
   If so, return that request's metadata, otherwise return nil."
  [catalog url response]
  ;; Filter the most recent request for this URL
  (if-let [e (->> @catalog
                  (filter #(= (:request-url %) url))
                  last)]
    (if-not (nil? (:etag response))
      ;; Compare the existing ETag to new ETag
      (if (= (get-in e [:response :etag]) (:etag response))
        ;; If the same, get details (metadata & location)
        (assoc (:metadata e) :location (:location e)))
      (if-not (nil? (:last-modified response))
        ;; Compare modification dates
        (if (= (get-in e [:response :last-modified]))
          (:last-modified response)
          ;; If the same, get details (metadata & location)
          (assoc (:metadata e) :location (:location e)))))))

(defn file-exists?
  "Given a filepath, check if the download already exists. If not, 
   check if it has been zipped, unzip it, and check for the download.
   If not there, return false."
  [filepath]
  (if
   (.exists
    (io/as-file filepath))
    true
    ;; Check if the folder has been zipped
    (let [dir (subs filepath 0 (s/last-index-of filepath "/"))
          zip (str dir ".zip")]
      (if (.exists (io/as-file zip))
        (do
          ;; Unzip it, make sure file is there
          (unzip! zip)
          (if (file-exists? filepath)
            ;; And finally re-zip it
            (zip! dir)
        ;; Otherwise, the file doesn't exist
            false))
        false))))

;;----------------------------- OUTPUT -------------------------------

(defn spit-report!
  "Given a working-dir and a vector of requests (catalog),
   generate the HTML report."
  [working-dir catalog-edn]
  (spit (str working-dir +report+) (h/gen-html catalog-edn))
  true)

(defn spit-catalog-v001!
  "Given a directory and the catalog-v001 as an XML string,
   spit the string to catalog-v001.xml."
  [dir catalog-v001]
  (let [filepath (str dir "/catalog-v001.xml")]
    (spit filepath catalog-v001)))

(defn spit-ont-element!
  "Given a directory path (project/directory) and the XML string of
   the ontology element, spit the file as [project]-element.owl. If
   no project is specified, the file will be [dir]-element.owl."
  [dir ont-element]
  (let [working-dir (subs dir 0 (+ 1 (s/index-of dir "/")))
        f-path (subs dir (+ 1 (s/index-of dir "/")))]
    (if (s/includes? f-path "/")
      ;; If there is a project name specified, use that as filename
      ;; Make sure it gets put in the working-dir
      (->> ont-element
           (spit
            (str
             working-dir
             (first (s/split f-path #"/"))
             "-element.owl")))
      ;; Otherwise, use just the dir name
      (->> ont-element
           (spit (str working-dir f-path "-element.owl"))))))

(defn update-catalog!
  "Given a working-dir and a vector of requests (catalog), update
   catalog.edn with vector."
  [working-dir catalog-edn]
  (pp/pprint
   catalog-edn
   (io/writer (str working-dir +catalog+))))

(defn gen-content!
  "Do a bunch of stuff."
  [dir catalog-edn catalog-v001]
  (let [working-dir (str (first (s/split dir #"/")) "/")]
    (update-catalog! working-dir catalog-edn)
    (if-not (nil? catalog-v001)
      (spit-catalog-v001! dir catalog-v001))
    (spit-report! working-dir catalog-edn)))
