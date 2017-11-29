(ns ontofetch.tools.utils
  (:require
   [clojure.string :as s]))

;; Helper functions!

(defn map-metadata
  "Given a vector containing [ontology-iri version-iri imports],
   return a map."
  [metadata]
  {:ontology-iri (first metadata)
   :version-iri (second metadata)
   :imports (last metadata)})

(defn map-request
  "Returns a map of the request details for a given ontology."
  [filepath redirs metadata]
  {:request-url (first redirs),
   :directory (first (s/split filepath #"/")),
   :request-date
   (.format
    (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss")
    (java.util.Date.)),
   :location filepath,
   :redirect-path redirs,
   :metadata (map-metadata metadata)})

(defn get-path-from-purl
  "Creates a filepath from a directory and a purl,
   giving the file the same name as the purl."
  [dir url]
  (str dir "/" (last (s/split url #"/"))))

(defn conj*
  "Given a seq and an element to append, return a conj'd vector with
   the new element at the end."
  [s x]
  (conj (vec s) x))

(defn sort-prefixes
  "Given a map of prefixes, return a map that has the
   xmlns first, followed by the base, then the rest."
  [m]
  (let [xmlns (:xmlns m)
        base (:xml:base m)
        others (dissoc m :xmlns :xml:base)]
    (conj {:xmlns xmlns, :xml:base base} (into (sorted-map) others))))

(defn get-namespace
  "Given a URI and a key to split at (# or /),
   return just the namespace."
  [uri]
  (if (s/includes? uri "#")
    (->> (s/last-index-of uri "#")
         (+ 1)
         (subs uri 0))
    (->> (s/last-index-of uri "/")
         (+ 1)
         (subs uri 0))))

(defn get-entity-id
  "Given a URI and a key to split at (# or /),
   return just the entity identifier."
  [uri]
  (if (s/includes? uri "#")
    (->> (s/last-index-of uri "#")
         (+ 1)
         (subs uri))
    (->> (s/last-index-of uri "/")
         (+ 1 )
         (subs uri))))
