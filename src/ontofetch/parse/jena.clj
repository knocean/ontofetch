(ns ontofetch.parse.jena
  (:require
   [clojure.string :as s]
   [ontofetch.tools.utils :as u])
  (:import
   (com.hp.hpl.jena.graph Triple Node_URI Node_Blank Node_Literal)
   (com.hp.hpl.jena.sparql.core Quad)
   (com.hp.hpl.jena.rdf.model RDFNode)
   (org.apache.jena.riot.system StreamRDF)
   (org.apache.jena.riot RDFDataMgr RDFLanguages Lang)))

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
  [triples]
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
    (^void base   [_ ^String base])
    (^void prefix [_ ^String prefix ^String iri])
    (^void finish [_])))

(defn read-triples
  "Given a source path, return the triples."
  [source]
  (let [triples (atom [])]
    (RDFDataMgr/parse (stream-triples triples) source)
    @triples))

(defn get-ontology-iri
  "Given a vector of all triples from an ontology,
   return the ontology IRI."
  [all-triples]
  (->> all-triples
       (filter #(= "http://www.w3.org/2002/07/owl#Ontology" (nth % 2)))
       flatten
       first))

(defn get-version-iri
  "Given a vector of all triples from an ontology,
   return the version IRI (or nil)."
  [all-triples]
  (->> all-triples
       (filter #(= "http://www.w3.org/2002/07/owl#versionIRI" (second %)))
       flatten
       last))

(defn get-imports
  "Given a vector of all triples from an ontology,
   return a list of direct imports (or nil)."
  [all-triples]
  (->> all-triples
       (filter #(= "http://www.w3.org/2002/07/owl#imports" (second %)))
       (mapv #(nth % 2))))

(defn get-more-imports
  [imports dir]
  (reduce
   (fn [m i]
     (let [trps (read-triples (u/get-path-from-purl dir i))]
       (conj m {i (get-imports trps)})))
   {} imports))
