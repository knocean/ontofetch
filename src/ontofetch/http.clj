(ns ontofetch.http
  (:require
   [ontofetch.utils :as u]
   [ontofetch.parse.xml :as xml]
   [org.httpkit.client :as http]))

;; TODO: Handle ftp
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

(defn invoke-timeout [f timeout-ms]
  (let [thr (Thread/currentThread)
        fut (future (Thread/sleep timeout-ms)
                    (.interrupt thr))]
    (try (f)
         (catch Exception e
           (println "Request timeout."))
         (finally
           (future-cancel fut)))))

(defmacro timeout [url ms & body] `(invoke-timeout (fn [] ~@body) ~ms))

(defn fetch-ontology!
  "Downloads an ontology from URL to a given filepath. Returns path to file."
  [filepath final-url]
  (->> final-url
       slurp
       (spit filepath)
       (timeout final-url 50000)))

;; TODO: Tests for error handling

(defn fetch-direct!
  "Fetches each direct import file and returns list of fetched imports."
  [dir imports]
  (if-not (empty? imports)
    (loop [purls imports
           fetched []]
      (let [purl (first purls)]
        (if-let [final-url (last (get-redirects purl))]
          (do
            (fetch-ontology! (u/get-path-from-purl dir purl) final-url)
            (if-not (empty? (rest purls))
              (recur (rest purls) (conj fetched purl))
              (conj fetched purl)))
          (do
            (println "Cannot fetch direct import " purl)
            (if-not (empty? (rest purls))
              (recur (rest purls) fetched)
              fetched)))))))

(defn fetch-indirect!
  "Fetches each indirect import file and returns
   map of direct imports (key) and their indirect imports (val)."
  [dir imports]
  (if-not (nil? imports)
    (loop [purls imports i-map {} fetched #{}]
      (let [parent (first purls)
            others (rest purls)
            more-imports (xml/get-imports (xml/get-metadata-node (u/get-path-from-purl dir parent)))]
        (doseq [purl more-imports]
          (if-let [final-url (last (get-redirects purl))]
            (if-not (contains? fetched purl)
              (fetch-ontology! (u/get-path-from-purl dir purl) final-url))
            (println "Cannot fetch indirect import " purl)))
        (if-not (empty? others)
          (recur others (into i-map {parent more-imports}) (into fetched more-imports))
          (into i-map {parent more-imports}))))))

(defn map-imports
  "Given a list of direct imports and a directory,
   save all direct & indirect imports, and return a map of them."
  [dir imports]
  (->> imports
       (fetch-direct! dir)
       (fetch-indirect! dir)))
