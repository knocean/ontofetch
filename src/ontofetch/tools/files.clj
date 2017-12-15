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
(def +config+ "config.edn")
(def +report+ "report.html")

;; FOLDERS & FILES

(defn zip!
  "Given a directory name, compress that directory
   and delete the original."
  [dir]
  (with-open [zip (ZipOutputStream.
                   (io/output-stream
                    (str dir ".zip")))]
    (doseq [f (file-seq (io/file dir)) :when (.isFile f)]
      (.putNextEntry zip (ZipEntry. (.getPath f)))
      (io/copy f zip)
      (.closeEntry zip)))
  (ctfu/recursive-delete (io/file dir))
  true)

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
          (io/delete-file zip))))))

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
   then makes a new directory & returns the name.
   Given a directory name and a filepath, checks if it is a valid
   dir name, then makes the parent directories for the file."
  ([dir]
   (if (and (valid-dir? dir) (not (.isDirectory (io/file dir))))
     (.mkdir (java.io.File. dir))))
  ([dir filepath]
  ;; If it already exists, we don't need to make it again
   (if (and (valid-dir? dir) (not (.isDirectory (io/file dir))))
     (io/make-parents filepath))))

(defn check-for-file
  "Given a filepath, check if the download already exists. If not, 
   check if it has been zipped, unzip it, and check for the download.
   Return the location if it exists, otherwise nil."
  [filepath]
  (if (.exists (io/as-file filepath))
    filepath
    ;; Check if the folder has been zipped
    (let [dir (subs filepath 0 (s/last-index-of filepath "/"))
          zip (str dir ".zip")]
      (if (.exists (io/as-file zip))
        (do
          ;; Unzip it, make sure file is there
          (unzip! zip)
          (if-not (nil? (check-for-file filepath))
            ;; And finally re-zip it
            (zip! dir))
          filepath)))))

;; SLURPING

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

(defn slurp-config
  "Given a working directory path, read the config.edn contents. If it
   does not exist, throws exception."
  [wd]
  (let [config (str wd +config+)]
    (if (.exists (io/as-file config))
      (->> config
           slurp
           clojure.edn/read-string)
      (throw 
        (java.io.IOException. 
          (str wd "config.edn does not exist."))))))

(defn get-project-config
  "Given a working directory path and a project name, get the
   configuration for that project (if it exists)."
  [wd project]
  (let [projs (:projects (slurp-config wd))]
    (first (filter #(= (:id %) project) projs))))

(defn slurp-catalog
  "Given a working directory path, read the catalog.edn contents."
  [wd]
  (let [cat (str wd +catalog+)]
    (if (.exists (io/as-file cat))
      (->> cat
           slurp
           clojure.edn/read-string)
      [])))

(defn get-current-metadata
  "Given a request URL and response headers (ETag, Last-Modified),
   check if the catalog has data on the same version already.
   If so, return that request's metadata, otherwise return nil."
  [wd url response]
  ;; Filter the most recent request for this URL
  (if-let [e (->> (slurp-catalog wd)
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

;; SPITTING

(defn spit-ont-element!
  "Given a a directory for extracts, the location of the ontology, and
   the extracted element (as XML), spit the file as [ont]-element."
  [extracts loc element]
  (spit
    (str
      extracts
      "/"
      ;; Create the element filename from the extract location
      (-> loc
          (subs 
           (+ (s/last-index-of loc "/") 1) 
           (s/last-index-of loc "."))
          (str "-element.owl")))
    element)
  true)

(defn spit-report!
  "Given a working-dir, generate the HTML report
   based on the catalog of fetch requests."
  [wd]
  (spit 
    (str wd +report+)
    (h/gen-html (slurp-catalog wd))))

(defn spit-catalog-v001!
  "Given a directory and a map of imports,
   generate the catalog-v001.xml."
  [dir imports]
  (let [filepath (str dir "/catalog-v001.xml")]
    (spit filepath (xml/catalog-v001 imports))))

(defn spit-catalog!
  "Given a working-dir (wd) and a map of a fetch request,
   update catalog.edn with this request."
  [wd request]
  (pp/pprint
   (u/conj* (slurp-catalog wd) request)
   (io/writer (str wd +catalog+))))

(defn spit-content!
  "Given a directory (working-dir/(opt project)/dir) and a map of the
   completed fetch request, update the catalog, generate the dir's
   catalog-v001 (if there are imports), and update the HTML report."
  [dir request]
  (let [wd (str (first (s/split dir #"/")) "/")]
    (spit-catalog! wd request)
    (if-not (empty? (:imports request))
      (spit-catalog-v001! dir (:imports request)))
    (spit-report! wd)))
