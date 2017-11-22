(ns ontofetch.parse.jena
  (:require
   [clojure.set]
   [clojure.string :as s]
   [ontofetch.tools.utils :as u])
  (:import
   (com.hp.hpl.jena.graph Triple Node_URI Node_Blank Node_Literal)
   (com.hp.hpl.jena.sparql.core Quad)
   (com.hp.hpl.jena.rdf.model RDFNode)
   (org.apache.jena.riot.system StreamRDF)
   (org.apache.jena.riot RDFDataMgr RDFLanguages Lang)))

;;----------------------------- PARSING ------------------------------
;; Methods to parse triples & prefixes from a (usually) Turtle file

(defmulti read-node
  class)

(defmethod read-node :default
  [node]
  nil)

(defmethod read-node Node_URI
  [node]
  (.getURI node))

(defmethod read-node Node_Blank
  [node]
  (str "_:" (.getLabelString (.getBlankNodeId node))))

(defmethod read-node Node_Literal
  [node]
  (let [value (.getLiteralLexicalForm node)
        type  (.getLiteralDatatype node)
        type  (when type (.getURI type))
        lang  (.getLiteralLanguage node)]
    (merge
     {:value value}
     (when type
       (when (not= type "http://www.w3.org/2001/XMLSchema#string")
         {:type type}))
     (when-not (s/blank? lang)
       {:lang lang}))))

(defmethod read-node RDFNode
  [node]
  (cond
    (.isURIResource node)
    (.getURI node)
    (.isAnon node)
    (str "_:" (.getLabelString (.getId (.asResource node))))
    (.isLiteral node)
    (let [value (.getLiteralLexicalForm node)
          type  (.getLiteralDatatype node)
          type  (when type (.getURI type))
          lang  (.getLiteralLanguage node)]
      (merge
       {:value value}
       (when type
         (when (not= type "http://www.w3.org/2001/XMLSchema#string")
           {:type type}))
       (when-not (s/blank? lang)
         {:lang lang})))))

;; TODO: Possible to JUST get metadata?
(defn stream-triples
  "Given atom for a sequence for ExpandedTriples,
   return an instance of StreamRDF for collecting triples.
   Quads are ignored."
  [prefs triples]
  (reify StreamRDF
    (^void start  [_])
    (^void triple [_ ^Triple triple]
      (swap!
       triples
       conj
       [(read-node (.getSubject triple))
        (read-node (.getPredicate triple))
        (read-node (.getObject triple))]))
    (^void quad   [_ ^Quad quad])
    (^void base   [_ ^String base]
      (swap!
       prefs
       assoc
       :base
       base))
    (^void prefix [_ ^String prefix ^String iri]
      (swap!
       prefs
       assoc
       (if (s/blank? prefix) nil (keyword prefix))
       iri))
    (^void finish [_])))

(defn read-triples
  "Given a source path, return the triples."
  [source]
  (let [prefs (atom {})
        triples (atom [])]
    (RDFDataMgr/parse (stream-triples prefs triples) source)
    [@prefs @triples]))

;;---------------------------- METADATA ------------------------------
;; Methods to get specific metadata elements from parsed triples

(defn get-ontology-iri
  "Given a vector of all triples from an ontology,
   return the ontology IRI."
  [triples]
  (->> triples
       (filter
        #(= "http://www.w3.org/2002/07/owl#Ontology"
          (nth % 2)))
       flatten
       first))

(defn get-version-iri
  "Given a vector of all triples from an ontology,
   return the version IRI (or nil)."
  [triples]
  (->> triples
       (filter
        #(= "http://www.w3.org/2002/07/owl#versionIRI"
          (second %)))
       flatten
       last))

(defn get-imports
  "Given a vector of all triples from an ontology,
   return a list of direct imports (or nil)."
  [triples]
  (->> triples
       (filter
        #(= "http://www.w3.org/2002/07/owl#imports"
          (second %)))
       (mapv #(nth % 2))))

(defn get-more-imports
  "Given a list of direct imports and the directory they are saved to,
   return a map of direct import (key) and indirect imports (val)."
  [imports dir]
  (reduce
   (fn [m i]
     (let [trps (read-triples (u/get-path-from-purl dir i))]
       (conj m {i (get-imports trps)})))
   {} imports))

;;----------------------------- FORMAT -------------------------------
;; Methods to format parsed triples as maps to convert to XML

(defn map-prefixes
  "Given a map to build into, a key (prefix), and value (full URI),
   associate the XML/XMLNS prefix with the URI."
  [m k v]
  (if (= k :base)
    (assoc m :xml:base v)
    (if (nil? k)
      (assoc m :xmlns v)
      (assoc m (keyword (str "xmlns:" (name k))) v))))

(defn map-rdf-node
  "Given a map of prefixes,
   return a map of the RDF node that can be parsed into XML."
  [prefs]
  (->> prefs
       (reduce-kv map-prefixes {})
       (assoc {} :attrs)
       (conj {:tag :rdf:RDF})))

(defn ns->prefix
  "Given a full URI, a key to split (# or /), and a map of namespaces
   (keys) and prefixes (vals), return the prefix."
  [uri k prefs]
  (->> (.lastIndexOf uri k)
       (+ 1)
       (subs uri 0)
       (get prefs)
       name))

(defn get-prefixed-annotations
  "Given [prefix-map triples], return a vector of prefixed ontology
   annotations."
  [ttl]
  (let [prefs (clojure.set/map-invert (first ttl))
        md (filter
            #(= "http://test.com/resources/test-1.ttl"
                (first %))
            (second all-triples))]
    (reduce 
      (fn [v md]
          (let [[_ p o] md]
            (if-let [prop (subs p (+ 1 (.indexOf p "#")))]
              (if-let [pref (ns->prefix p "#" prefs)] 
                (let [tag (keyword (str pref ":" prop))]
                  (conj v [tag o]))
                ;; TODO: Handle prefix not found?
                (let [tag (keyword prop)]
                  (conj v [tag o])))
              (let [prop (subs p (+ 1 (.lastIndexOf p "/")))]
                (if-let [pref (ns->prefix p "/" prefs)]
                  (let [tag (keyword (str pref ":" prop))]
                    (conj v [tag o]))
                  ;; TODO: Handle prefix not found?
                  (let [tag (keyword prop)]
                    (conj v [tag o])))))))
      [] md)))

(defn map-annotation
  "Given a vector to conj into and an annotation as [prop object],
   return a map of the annotation to be parsed to XML."
  [v annotation]
  (let [[p o] annotation]
    ;; Skip the actual Ontology element
    (if-not (= o "http://www.w3.org/2002/07/owl#Ontology")
      ;; Will return nil if not map
      (let [content (:value o)]
        (if (nil? content)
          ;; No content parsed as resource
          (u/conj*
            v
            {:tag p,
             :attrs {:rdf:resource o},
             :content content})
          ;; Content has unknown attrs (datatypes not parsed)
          (u/conj*
            v
            {:tag p,
             :attrs
             {:rdf:datatype
              "http://www.w3.org/2001/XMLSchema#string"},
             :content [content]})))))) 

(defn map-metadata
  "Given an ontology IRI and the parsed ttl file (prefixes & triples),
   return a map of the full metadata to be parsed to XML."
  [iri ttl]
  (let [annotations (get-prefixed-annotations ttl)]
    {:tag :owl:Ontology,
     :attrs {:rdf:about iri},
     :content (reduce map-annotation [] annotations)}))
