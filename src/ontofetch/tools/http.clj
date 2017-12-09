(ns ontofetch.tools.http
  (:require
   [ontofetch.tools.utils :as u]
   [ontofetch.parse.xml :as xml]
   [org.httpkit.client :as http]))

(defn invoke-timeout [url f timeout-ms]
  (let [thr (Thread/currentThread)
        fut (future (Thread/sleep timeout-ms)
                    (.interrupt thr))]
    (try (f)
         (catch Exception e
           (println "Request timeout: " url))
         (finally
           (future-cancel fut)))))

(defmacro timeout
  [url ms & body]
  `(invoke-timeout ~url (fn [] ~@body) ~ms))

(defn get-redirects
  "Manually follows redirects and returns a vector containing all
   redirect URLs. The final URL with content is the last entry."
  [url]
  (loop [redirs []
         new-url url]
    ;; No redirs from FTP, so that's final content
    (if (.contains new-url "ftp://")
      (conj redirs new-url)
      ;; Otherwise get HTTP status and determine what to do
      (let [{:keys [status headers]
             :as res} @(http/request {:url new-url
                                      :method :head
                                      :follow-redirects false})]
        (case status
          200
          {:redirs (conj redirs new-url)
           ;; Also get ETag and Last-Modified
           :etag (:etag headers)
           :last-modified (:last-modified headers)}
          (301 302 303 307 308)
          (recur (conj redirs new-url) (:location headers))
          nil)))))       ;; Anthing else will not be returned

(defn fetch-ontology!
  "Given a filepath and a URL to an ontology,
   download the contents to the file."
  [filepath url]
  (if-not (= "http://ontologies.berkeleybop.org" url)
    (with-open [r (clojure.java.io/reader url)]
      (with-open [w (clojure.java.io/writer filepath)]
        (doseq [line (line-seq r)]
          (.write w (str line "\n")))))))

;; map doesn't work across this vector?
(defn fetch-imports!
  "Given a directory and a list of imports,
   download each import file to the directory."
  [dir imports]
  (loop [is imports
         n 0]
    (if (< n (count imports))
      ;; Try to get the redirects
      (if-let [url (last (get-redirects (first is)))]
        (do
          (fetch-ontology! (u/path-from-url dir (first is)) url)
          (if-not (empty? (rest is))
            (recur (rest is) (+ n 1))))
        ;; If not redirs, import cannot be fetched
        (do
          (println (str "Cannot fetch " (first is)))
          (if-not (empty? (rest is))
            (recur (rest is) (+ n 1))))))))
