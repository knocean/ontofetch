(ns ontofetch.core.status
  (:require
   [clojure.string :as s]
   [ontofetch.tools.files :as f]
   [ontofetch.tools.http :as h]
   [ontofetch.tools.utils :as u]))

(defn import-status
  "Given a map of imports, return the status of the imports. True if
   all imports are up to date, nil if any imports are out of sync or 
   unknown status."
  [imports dir]
  (reduce
   (fn [v i]
      ;; Get stored response and new response to compare
     (let [prev (:response i)
           cur (h/get-response (:url i))
           loc (u/path-from-url dir (:url i))]
        ;; First check that the file exists
       (if-not (nil? (f/check-for-file loc))
         (if-not (nil? (:etag cur))
            ;; If etag is not nil, compare to previous etag
           (if-not (= (:etag cur) (:etag prev))
              ;; If they aren't equal, break with nil
             (reduced nil)
              ;; Otherwise return true
             true)
            ;; If it is nil, check for last-modified
           (if-not (nil? (:last-modified cur))
              ;; If not nil, compare to previous last-modified
             (if-not (= (:last-modified cur) (:last-modified prev))
                ;; If they aren't equal, break with nil
               (reduced nil)
                ;; Otherwise return true
               true)
              ;; Otherwise status is unknown, return nil
             (reduced nil)))
         (reduced nil))))
   [] imports))

(defn status
  "Given a working directory (wd) and a URL, return the status of that
  ontology. If a fetch operation has been logged in the catalog, and 
  the ETag or Last-Modified headers are the same, return last fetched
  location. If they are different, or the file does not exist, return
  nil (update needed)."
  [wd url]
  (let [response (h/get-response url)
        metadata (f/get-current-metadata wd url response)]
    (if-let [last-loc (:location metadata)]
      ;; If last location is not nil, check that the file exists
      ;; If it is nil, return nil
      (if-let [prev-file (f/check-for-file (str wd last-loc))]
        ;; If previous file is not nil, check that imports are current
        ;; If it is nil, return nil
        (let [imports (:imports metadata)
              ;; Get dir name based on last location
              dir (if (clojure.string/includes? last-loc "/")
                    (subs last-loc 0 (s/last-index-of last-loc "/"))
                    ".")]
          (if-not (empty? imports)
            (if (import-status imports dir)
              ;; If the import status is fine (true), return last-loc
              last-loc)
            ;; If the imports are empty (and you got here),
            ;; everything is up to date
            last-loc))))))

(defn project-status
  "Given a working directory (wd) and a project name, return the
   status of just that project. If it is up to date, status? will
   return it's last local location. If there is no config for a given
   project, return nil. Throws exception if a config file does not
   exist."
  [wd project]
  ;; If the project folder doesn't exist, no need to check status
  (if (.exists (clojure.java.io/as-file (str wd project)))
    ;; Get the project config from config.edn
    (if-let [config (f/get-project-config wd project)]
      (status wd (:url config)))))

(defn dir-status
  "Given a working directory (wd), read the config.edn file and return
   a vector of maps for all projects as {:id id :location location}.
   If a project is out of sync, :location will be nil. Throws
   exception if a config file does not exist."
  [wd]
  ;; Get all project IDs from config.edn
  (let [ids (reduce
             (fn [v p]
               (conj v (:id p)))
             [] (:projects (f/slurp-config wd)))]
    ;; Get the status of each project and return vector of maps
    (reduce
     (fn [v i] (conj v {:id i :location (project-status wd i)}))
     [] ids)))
