(ns ontofetch.core
  (:use
   [ontofetch.ontofetch])
  (:require
   [clojure.string :as s]
   [clojure.tools.cli :refer [parse-opts]]
   [ontofetch.tools.files :as f]
   [ontofetch.tools.http :as h]
   [ontofetch.tools.utils :as u])
  (:gen-class))

;; TODO: General error handling (error logs)

;; Make association between what you asked for in import and the IRI that you ended up with
;; Look at obi-edit file (core.owl import not resolving)

(def cli-options
  [["-d" "--dir  DIR" "Directory"
    :desc "Directory to save downloads."
    :parse-fn #(String. %)]
   ["-p" "--url  URL" "URL"
    :desc "URL of the ontology to fetch."
    :parse-fn #(String. %)]
   ["-z" "--zip" "Zip Results"
    :desc "Compress the results."
    :default false]
   ["-h" "--help"]])

(defn usage
  [options-summary]
  (->> [""
        "ontofetch gets an ontology and it's imports, summarizes the "
        "metadata, and returns a directory (or compressed folder) of "
        "all downloads."
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
    ;; If validate returned msg, return msg to user
    (if exit-msg
      (exit
       ;; Should be OK if --help, otherwise error
       (if ok? 0 1)
       exit-msg)
      ;; No exit msg, get parsed options
      (let [{:keys [dir url zip]} opts]
        (let [redirs (h/get-redirects url)
              filepath (u/path-from-url (f/make-dir! dir) url)]
          ;; Download the ontology to created dir
          (h/fetch-ontology! filepath (last redirs))
          ;; Do all the stuff - successful if it returns true
          (if (true? (parse-ontology redirs dir filepath))
            (do
              ;; Zip directory if user provided flag
              (if zip
                (f/zip-folder! dir))
              (exit 0))
            (exit 1 (str "Unable to fetch " url))))))))
