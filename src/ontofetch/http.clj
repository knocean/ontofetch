(ns ontofetch.http
  (:require
   [clojure.string :as s]
   [ontofetch.xml :as xml]
   [org.httpkit.client :as http]))

(def fetched-urls (atom #{}))
(def error-log (atom []))

(defn get-redirects
  "Manually follows redirects and returns a vector containing all redirect URLs.
   The final URL with content is the last entry."
  [url]
  (loop [redirs []
         new-url url]
    (let [{:keys [status headers body error]
           :as res} @(http/request {:url new-url
                                    :method :head
                                    :follow-redirects false})]
      (case status
        200 (conj redirs new-url)
        (301 302 303 307 308) (recur (conj redirs new-url) (:location headers))
        nil))))       ;; Anthing else will not be returned

(defn get-path-from-purl
  "Creates a filepath from a directory and a purl,
   giving the file the same name as the purl."
  [dir url]
  (str dir "/" (last (s/split url #"/"))))

(defn fetch-ontology!
  "Downloads an ontology from URL to a given filepath. Returns path to file."
  [filepath final-url]
  (spit filepath (slurp final-url)))

;; TODO: Do we want to set a timeout for each import?

(defn fetch-imports!
  "Fetches each import file, and subsequently its imports.
   Checks for duplicates before fetching."
  [imports dir]
  (doseq [url imports]
    (if-not (contains? @fetched-urls url)             ;; Check if import is already fetched
      (if-let [redirs (get-redirects url)]            ;; Make sure redirs is not nil
        ((swap! fetched-urls                          ;; Update fetched URLs
                (fn [current-urls] (into current-urls redirs)))
         (let [filepath (get-path-from-purl dir url)]
           (fetch-ontology! filepath (last redirs))   ;; Fetch the import
           (let [more-imports (xml/get-imports (xml/get-metadata-node filepath))]
             (if-not (empty? more-imports)            ;; Get imports of import
               (fetch-imports! more-imports dir)))))
        (swap! error-log                              ;; Log if redirs is nil
               (fn [cur] (into cur (str "Unable to fetch import: " url))))))))
