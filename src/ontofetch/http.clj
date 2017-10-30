(ns ontofetch.http
  (:require
   [clojure.string :as s]
   [ontofetch.xml :as xml]
   [org.httpkit.client :as http]))

(def fetched-urls (atom #{}))

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

;; TODO: Set timeouts
;; TODO: Tests for error handling
;; TODO: Return IRIs, not filepaths

(defn fetch-direct!
  "Fetches each direct import file and returns list of fetched imports."
  [imports dir]
  (loop [purls imports
         fetched []]
    (let [purl (first purls)]
      (if-let [final-url (last (get-redirects purl))]
        (do
          (fetch-ontology! (get-path-from-purl dir purl) final-url)
          (if-not (empty? (rest purls))
            (recur (rest purls) (conj fetched purl))
            (conj fetched purl)))
        (do
          (println "Cannot fetch direct import " purl)
          ;; Will not add to list of fetched imports
          (if-not (empty? (rest purls))
            (recur (rest purls) fetched)
            fetched))))))

(defn fetch-indirect!
  "Fetches each indirect import file and returns
   map of direct imports (key) and their indirect imports (val)."
  [imports dir]
  (loop [purls imports i-map {} fetched #{}]
    (let [parent (first purls)
          others (rest purls)
          more-imports (xml/get-imports (xml/get-metadata-node (get-path-from-purl dir parent)))]
      (doseq [purl more-imports]
        (if-let [final-url (last (get-redirects purl))]
          (if-not (contains? fetched purl)
            (fetch-ontology! (get-path-from-purl dir purl) final-url))
            ;; Do not need to re-fetch
          (println "Cannot fetch indirect import " purl)))
      (if-not (empty? others)
        (recur others (into i-map {parent more-imports}) (into fetched more-imports))
        (into i-map {parent more-imports})))))
