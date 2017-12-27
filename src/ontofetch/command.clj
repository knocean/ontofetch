(ns ontofetch.command
  (:require
   [clojure.tools.logging :as log]
   [ontofetch.core.extract :as e]
   [ontofetch.core.fetch :as f]
   [ontofetch.core.serve :as sr]
   [ontofetch.core.status :as st]
   [ontofetch.core.update :as u]))

;; TODO: service

(def usage
  (->> [""
        "usage: ontofetch [command] [options] <arguments>"
        ""
        "options:"
        "  -h, --help   Print usage (ontofetch [command] --help)"
        ""
        "commands:"
        "  extract   Extract the owl:Ontology element"
        "  fetch     Fetch ontologies and their imports"
        "  status    Check if fetched ontologies are up-to-date"
        "  update    Update fetched ontologies"
        ""]
       (clojure.string/join \newline)))

(def extract-usage
  (->> [""
        (str
         "extract pulls the owl:Ontology element from a dir or proje"
         "ct and saves it in RDF/XML format as [ont]-element.owl.")
        ""
        "usage:"
        "  ontofetch extract [options] <arguments>"
        "  * extract"
        "      Extract from all configured projects"
        "  * extract --dir <arg>"
        "      Extract from ontology in dir to working directory"
        "  * extract --dir <arg> --extracts <arg>"
        "      Extract from ontology in dir to --extracts dir"
        "  * extract --project <arg>"
        "      Extract last fetch in project to configured dir"
        ""
        "options:"
        "  -d, --dir <arg>           Directory to extract from"
        "  -e, --extracts <arg>      Directory to extract to (./)"
        "  -p, --project <arg>       Project to extract from"
        "  -w, --working-dir <arg>   Working directory (default './')"
        ""]
       (clojure.string/join \newline)))

(def fetch-usage
  (->> [""
        (str
         "fetch retrieves an ontology from a URL and all imports. Ea"
         "ch operation's details are stored in catalog.edn. Supporte"
         "d formats include: RDF/XML, Turtle, OWL/XML, Manchester.")
        ""
        "usage:"
        "  ontofetch fetch [options] <arguments>"
        "  * fetch --dir <arg> --url <arg>"
        "      Fetch the URL to the directory"
        "  * fetch --project <arg> --dir <arg> --url <arg>"
        "      Fetch the URL to project/directory"
        "  * fetch --project <arg>"
        "      Fetch configured project to dated directory"
        ""
        "options:"
        "  -d, --dir <arg>           Directory to fetch to"
        "  -p, --project <arg>       Project name"
        "  -u, --url <arg>           URL to fetch from"
        "  -w, --working-dir <arg>   Working directory (default './')"
        "  -z, --zip                 Compress fetch contents"
        ""]
       (clojure.string/join \newline)))

(def serve-usage
  (->> [""
        (str
         "serve continuously updates all projects in a directory on a"
         " schedule.")
        ""
        "usage:"
        "  ontofetch serve [options] <arguments>"
        "  * serve"
        "      Run serve in current directory until killed"
        "  * serve --kill"
        "      Kill a running serve process"
        ""
        "options:"
        "  -e, --extracts            Include extract command"
        "  -w, --working-dir <arg>   Working directory (default './')"
        "  -z, --zip                 Compress fetch contents"
        ""]
       (clojure.string/join \newline)))

(def status-usage
  (->> [""
        (str
         "status checks if a project (or projects) is up-to-date bas"
         "ed on the HTTP headers of the resource.")
        ""
        "usage:"
        "  ontofetch status [options] <arguments>"
        "  * status"
        "      Get status of all configured projects"
        "  * status --project <arg>"
        "      Get status of configured project"
        ""
        "options:"
        "  -p, --project <arg>       Project name"
        "  -w, --working-dir <arg>   Working directory (default './')"
        ""]
       (clojure.string/join \newline)))

(def update-usage
  (->> [""
        (str
         "update runs status on a project (or projects), then fetche"
         "s if necessary.")
        ""
        "usage:"
        "  ontofetch update [options] <arguments>"
        "  * update"
        "      Update all configured projects"
        "  * update --project <arg>"
        "      Update configured project"
        ""
        "options:"
        "  -p, --project <arg>       Project name"
        "  -w, --working-dir <arg>   Working directory (default './')"
        ""]
       (clojure.string/join \newline)))

(defn get-usage
  [action]
  (cond
    (= action "extract") extract-usage
    (= action "fetch") fetch-usage
    (= action "serve") serve-usage
    (= action "status") status-usage
    (= action "update") update-usage
    :else usage))

(defn run-extract
  "Given CLI options, run the extract command."
  [opts]
  (let [{:keys [working-dir dir project extract-dir]} opts]
    (cond
      ;; --dir 
      (and dir (not project))
      (e/extract extract-dir
                 (ontofetch.tools.utils/find-ontology
                  working-dir
                  (ontofetch.tools.files/slurp-catalog working-dir)
                  dir))
      ;; --project
      (and project (not dir))
      (e/project-extract working-dir project)
      ;; otherwise...
      :else
      (e/dir-extract working-dir))))

(defn run-fetch
  "Given CLI options, run the fetch command."
  [opts]
  (let [{:keys [working-dir dir project url zip]} opts]
    (cond
      ;; --project --dir --url 
      (and project dir url) (f/fetch working-dir project dir url zip)
      ;; --dir --url
      (and dir url (not project)) (f/fetch working-dir dir url zip)
      ;; --project
      (and project (not dir) (not url))
      (f/fetch working-dir project zip)
      ;; otherwise... print usage
      :else
      (do
        (log/fatal "Invalid options for fetch.")
        (println fetch-usage)))))

;; TODO: Not fetching for some reason
(defn run-serve
  "Given CLI options, run the serve command."
  [opts]
  (let [{:keys [working-dir zip extracts kill]} opts]
    (if kill
      (sr/kill)
      (sr/serve working-dir zip extracts))))

(defn run-status
  "Given CLI options, run the status command."
  [opts]
  (let [{:keys [working-dir project]} opts]
    (if-not (nil? project)
      ;; --project 
      (if-let [s (st/project-status working-dir project)]
        (log/info project "is up-to-date at" s)
        (log/info project "is out of sync"))
      (let [ss (st/dir-status working-dir)]
        (log/info "OUT OF SYNC\n\t"
                  (clojure.string/join "\n\t "
                                       (reduce
                                        (fn [v s]
                                          (if (nil? (:location s))
                                            (conj v (:id s))))
                                        [] ss)))))))

(defn run-update
  "Given CLI options, run the update command."
  [opts]
  (let [{:keys [working-dir project zip]} opts]
    (if-not (nil? project)
      ;; --project 
      (u/project-update working-dir project zip)
      (u/dir-update working-dir zip))))

(defn run-ontofetch
  "Given an action and opts, perform the action with opts."
  [action opts]
  (cond
    (= action "extract") (run-extract opts)
    (= action "fetch") (run-fetch opts)
    (= action "serve") (run-serve opts)
    (= action "status") (run-status opts)
    (= action "update") (run-update opts)
    :else
    (do
      (log/fatal action " is not a valid command.")
      (println usage))))
