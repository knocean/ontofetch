(ns ontofetch.core.update
  (:require
   [clojure.tools.logging :as log]
   [ontofetch.core.fetch :as f]
   [ontofetch.core.status :as s]))

(defn project-update
  "Given a working directory (wd) and a project name, check the status
   and fetch if out of sync."
  [wd project zip]
  (if-let [loc (s/project-status wd project)]
    (log/info project "is up-to-date at" loc)
    (do
      (log/info "updating" project)
      (f/fetch wd project zip))))

(defn dir-update
  "Given a working directory (wd), get the status of all configured
   projects as {:id id :location last-loc}. If last location is nil
   (needs updating), fetch the project."
  [wd zip]
  (dorun
   (map
    #(project-update wd (:id %) zip)
    (s/dir-status wd))))
