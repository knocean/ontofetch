(ns ontofetch.core
  (:require
   [clojure.tools.cli :refer [parse-opts]])
  (:use
   [ontofetch.command])
  (:gen-class))

(def cli-options
  [["-d" "--dir <arg>" "Directory"
    :parse-fn #(String. %)]
   ["-e" "--extracts <arg>" "Extract directory"
    :parse-fn #(str (String. %) "/")
    :default "./"]
   ["-u" "--url <arg>" "URL"
    :parse-fn #(String. %)]
   ["-p" "--project <arg>" "Project name"
    :parse-fn #(String. %)]
   ["-w" "--working-dir <arg>" "Working directory"
    :parse-fn #(str (String. %) "/")
    :default "./"]
   ["-z" "--zip" "Zip results"]
   ["-h" "--help"]])

(defn validate-args
  "Given CLI arguments, return corresponding action."
  [args]
  (let [{:keys [options arguments summary errors]}
        (parse-opts args cli-options)]
    (cond
      errors {:exit-msg errors :ok? false}
      (:help options)
      {:exit-msg (get-usage (first arguments)) :ok? true}
      (= 1 (count arguments))
      {:action (first arguments) :opts options}
      :else {:exit-msg (usage summary :ok? false)})))

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
      (do
        (run-ontofetch action opts)
        (exit 0)))))
