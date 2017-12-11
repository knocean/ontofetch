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

(defn get-last-metadata
  "Given a request URL and response headers (ETag, Last-Modified),
   check if the catalog has data on the same version already.
   If so, return that request's metadata, otherwise return nil."
  [url response]
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
        (if
         (=
          (get-in e [:response :last-modified])
          (:last-modified response))
          ;; If the same, get details (metadata & location)
          (assoc (:metadata e) :location (:location e)))))))

;; TODO: Unused. Might need this later.
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
  "Given a directory path (project/directory) and the XML string of
   the ontology element, spit the file as [project]-element.owl. If
   no project is specified, the file will be [dir]-element.owl."
  [dir ont-element]
  (if (s/includes? dir "/")
    ;; If there is a project name specified, use that as the filename
    (->> ont-element
         (spit (str (first (s/split dir #"/")) "-element.owl")))
    ;; Otherwise, use the dir name (./dir)
    (->> ont-element
         (spit (str dir "-element.owl")))))

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
  (if-not (nil? catalog-v001)
    (spit-catalog-v001! dir catalog-v001))
  (spit-report!))
