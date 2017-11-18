(ns ontofetch.core
  (:require
   [clojure.string :as s]
   [clojure.tools.cli :refer [parse-opts]]
   [ontofetch.files :as files]
   [ontofetch.http :as http]
   [ontofetch.parse.parse :as p]
   [ontofetch.utils :as u])
  (:gen-class))

;; TODO: General error handling (error logs)

;; Make association between what you asked for in import and the IRI that you ended up with
;; Look at obi-edit file (core.owl import not resolving)

(def cli-options
  [["-d" "--dir DIR" "Directory"
    :desc "Directory to save downloads."
    :parse-fn #(String. %)]
   ["-p" "--purl PURL" "PURL"
    :desc "PURL of the ontology to download."
    :parse-fn #(String. %)]
   ["-h" "--help"]])

(defn usage
  [options-summary]
  (->> [""
        "ontofetch gets an ontology and it's imports, summarizes" 
        "the metadata, and returns a .zip of all downloads."
        ""
        "Usage: ontofetch [options]"
        ""
        "Options:"
        options-summary
        ""
        "Please see documentation for more information."
        ""]
       (s/join \newline)))

(defn validate-args
  "Given CLI arguments, return corresponding action."
  [args]
  (let [{:keys [options arguments summary errors]}
        (parse-opts args cli-options)]
    (cond
      (:help options)             ;; Return usage
      {:exit-msg (usage summary)
       :ok? true}
      errors                      ;; Return errors
      {:exit-msg errors}
      (= 0 (count arguments))     ;; No args - run ontofetch
      {:opts options}
      :else                       ;; Unhandled, return usage
      {:exit-msg (usage summary)})))

(defn exit
  "Given a status (0 or 1) and an (optional) exit message,
   return the message and exit ontofetch."
  ([status]
   (exit status nil)) 
  ([status msg]
   (if msg
     (println msg))
   (System/exit status)))

(defn -main
  [& args]
  (let [{:keys [action opts exit-msg ok?]} (validate-args args)]
    (if exit-msg
      (exit
        (if ok? 0 1)
        exit-msg)
      (let [{:keys [dir purl]} opts]
        (let [redirs (http/get-redirects purl)
              fp (u/get-path-from-purl (files/make-dir! dir) purl)]
          (http/fetch-ontology! fp (last redirs))
          (if (true? (p/parse-ontology redirs dir fp))
            (exit 0)
            (exit 1 (str "Unable to fetch " purl))))))))
