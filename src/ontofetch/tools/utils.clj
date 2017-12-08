(ns ontofetch.tools.utils
  (:require
   [clojure.string :as s]
   [clj-time.core :as t]
   [clj-time.format :as tf]
   [clj-time.local :as tl]))



(defn map-metadata
  "Given a vector containing [ontology-iri version-iri imports],
   return a map."
  [metadata]
  {:ontology-iri (first metadata)
   :version-iri (second metadata)
   :imports (last metadata)})

(defn get-duration
  [times]
  (t/in-seconds (t/interval)))

(defn map-request
  "Returns a map of the request details for a given ontology."
  [filepath redirs metadata start]
  (let [end (tl/local-now)]
    {:request-url (first redirs),
     :directory (first (s/split filepath #"/")),
     :location filepath,
     :redirect-path redirs,
     :start-time (.toString start),
     :end-time (.toString end),
     :duration (t/in-millis (t/interval start end)),
     :metadata (map-metadata metadata)}))

(defn path-from-url
  "Creates a filepath from a directory and a url,
   giving the file the same name as the url."
  [dir url]
  (str dir "/" (last (s/split url #"/"))))

(defn replace-chars
  "Given a string, return a string with special chars replaced with
   the character reference."
  [string]
  (if (string? string)
    (-> string
        (s/replace "&" "&amp;")
        (s/replace "<" "&lt;")
        (s/replace ">" "&gt;")
        (s/replace "\n" " "))))

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
         (+ 1)
         (subs uri))))
