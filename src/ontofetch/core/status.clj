(ns ontofetch.core.status
  (:require 
   [clojure.string :as s]
   [ontofetch.tools.files :as f]
   [ontofetch.tools.http :as h]))

;; TODO: get status of imports & indirect imports as well

(defn status
 "Given a working directory (wd) and a URL, return the status of that
  ontology. If a fetch operation has been logged in the catalog, and 
  the ETag or Last-Modified headers are the same, return last fetched
  location. If they are different, or the file does not exist, return
  nil (update needed)." 
 [wd url]
 (let [response (h/get-response url)
       metadata (f/get-current-metadata wd url response)
       last-loc (:location metadata)]
    ;; Is there metadata from the same version?
    (if-not (= nil metadata)
      (f/check-for-file last-loc))))

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
