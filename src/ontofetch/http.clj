(ns ontofetch.http
  (:require
   [ontofetch.utils :as u]
   [ontofetch.parse.xml :as xml]
   [org.httpkit.client :as http]))

(defn get-redirects
  "Manually follows redirects and returns a vector containing all redirect URLs.
   The final URL with content is the last entry."
  [url]
  (loop [redirs []
         new-url url]
    (if (.contains new-url "ftp://")
      (conj redirs new-url)
      (let [{:keys [status headers body error]
             :as res} @(http/request {:url new-url
                                      :method :head
                                      :follow-redirects false})]
        (case status
          200 (conj redirs new-url)
          (301 302 303 307 308) (recur (conj redirs new-url) (:location headers))
          nil)))))       ;; Anthing else will not be returned

(defn invoke-timeout [url f timeout-ms]
  (let [thr (Thread/currentThread)
        fut (future (Thread/sleep timeout-ms)
                    (.interrupt thr))]
    (try (f)
         (catch Exception e
           (println "Request timeout: " url))
         (finally
           (future-cancel fut)))))

(defmacro timeout [url ms & body] `(invoke-timeout ~url (fn [] ~@body) ~ms))

(defn fetch-ontology!
  "Downloads an ontology from URL to a given filepath. Returns path to file."
  [filepath final-url]
  (->> final-url
       slurp
       (spit filepath)
       (timeout final-url 50000)))

;; map doesn't work across this vector?
(defn fetch-imports!
  "Given a directory and a list of imports,
   download each import file to the directory."
  [dir imports]
  (loop [is imports
         n 0]
    (if (< n (count imports))
      (if-let [url (last (get-redirects (first is)))]
        (do
          (fetch-ontology! (u/get-path-from-purl dir url) url)
          (if-not (empty? (rest is))
            (recur (rest is) (+ n 1))))
        (do
          (println (str "Cannot fetch import " (first is)))
          (if-not (empty? (rest is))
            (recur (rest is) (+ n 1))))))))
