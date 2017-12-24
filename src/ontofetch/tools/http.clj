(ns ontofetch.tools.http
  (:require
   [clojure.tools.logging :as log]
   [ontofetch.tools.utils :as u]
   [ontofetch.parse.xml :as xml]
   [org.httpkit.client :as http]))

(defn get-response
  "Manually follows redirects and returns a vector containing all
   redirect URLs. The final URL with content is the last entry."
  [url]
  (loop [redirs []
         new-url url]
    ;; No headers from FTP, so that's final content
    (if (.contains new-url "ftp://")
      {:redirs (conj redirs new-url)}
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

;; Returns a vector of import details
;; [{:url import-url, :response {...}} 
;;  {:url import-url, :response {...}}]
(defn fetch-imports!
  "Given a directory and a vector of imports, get the HTTP response,
   download the import ontology, and return a vector of maps with
   request details."
  [dir imports]
  (reduce
    (fn [v i]
      ;; Get the response -> final URL
      (if-let [response (get-response i)]
        (let [url (last (:redirs response))]
          ;; Fetch it if not nil
          (fetch-ontology! (u/path-from-url dir i) url)
          ;; Add the map to the vector of imports
          (conj v (assoc {:url i} :response response)))
        ;; If response is nil, still add it...
        (do
          (log/warn "Cannot fetch" i)
          (conj v {:url i, :response nil}))))
   [] imports))
