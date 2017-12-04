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
      (let [{:keys [status headers body error]
             :as res} @(http/request {:url new-url
                                      :method :head
                                      :follow-redirects false})]
        (case status
          200 (conj redirs new-url)
          (301 302 303 307 308)
          (recur (conj redirs new-url) (:location headers))
          nil)))))       ;; Anthing else will not be returned

;; TODO: Better error handling
;;       Will return timeout if the folder isn't created
(defn fetch-ontology!
  "Given a filepath and a URL, download contents of the URL."
  [filepath final-url]
  ;; Skip imports that resolve to berkeleybop.org
  ;; they do not exist at that PURL?
  (if-not (= "http://ontologies.berkeleybop.org" final-url)
    (->> final-url
         slurp
         (spit filepath))))

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
          (fetch-ontology! (u/get-path-from-purl dir (first is)) url)
          (if-not (empty? (rest is))
            (recur (rest is) (+ n 1))))
        ;; If not redirs, import cannot be fetched
        (do
          (println (str "Cannot fetch " (first is)))
          (if-not (empty? (rest is))
            (recur (rest is) (+ n 1))))))))
