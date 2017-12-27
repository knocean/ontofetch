(ns ontofetch.tools.html
  (:require
   [hiccup.core :as hic]))

(def +bootstrap+
  "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css")

(defn list-indirects
  "Helper fn to list indirect imports under the direct import."
  [indirects]
  (when (seq indirects)
    [(into [:ul] (mapv (fn [i] [:li i]) indirects))]))

(defn list-imports
  "Generates a list element for each import,
   as long as imports exists."
  [dir-imports]
  (if (not-empty dir-imports)
    (into [:ul]
          (for [d dir-imports]
            (if-let [indirs (:imports d)]
              [:li (:url d) (list-imports (:imports d))]
              [:li (:url d)])))
    "none"))

(defn gen-entry
  "Generates HTML for each entry in the catalog"
  [catalog-entry]
  [:div
   [:h5
    [:a {:href (:request-url catalog-entry)
         :target "_blank"}
     (:request-url catalog-entry)]
    (str
     " on "
     (first
      (clojure.string/split (:start-time catalog-entry) #"\.")))]
   [:b "Operation Time: "]
   (str (:duration catalog-entry) " ms")
   [:br]
   [:b "Version: "]
   (if-let [v (get-in catalog-entry [:metadata :version-iri])]
     (get-in catalog-entry [:metadata :version-iri])
     "undefined")
   [:br]
   [:b "Location: "] [:a {:href (:location catalog-entry)
                          :target "_blank"}
                      (:location catalog-entry)]
   [:br]
   [:b "Imports: "]
   (list-imports (get-in catalog-entry [:metadata :imports]))
   [:br] [:br]])

(defn gen-html
  "Given an ID and a set of catalog entries,
   generate an HTML report with all entries."
  [id catalog]
  (hic/html
   [:html {:lang "en"}
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content
             "width=device-width, initial-scale=1, shrink-to-fit=no"}]
     [:title (str id " Report")]
     [:link {:rel "stylesheet" :href +bootstrap+}]]
    [:body
     [:div {:class "container"}
      [:div
       [:h1 (str id " Requests")]
       [:p {:class "lead"}
        "See the full metadata in "
        [:a {:href "catalog.edn" :target "_blank"} "catalog.edn"]]]
      [:hr]
      (map gen-entry catalog)]]]))
