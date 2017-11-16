(ns ontofetch.html
  (:require
   [hiccup.core :as hic]))

(def +bootstrap+ "resources/static/css/bootstrap.min.css")

(defn list-indirects
  "Helper fn to list indirect imports under the direct import."
  [indirects]
  (when (seq indirects)
    [(into [:ul] (mapv (fn [i] [:li i]) indirects))]))

(defn list-imports
  "Generates a list element for each import,
   as long as imports exists."
  [imports]
  (if-not (empty? imports)
    [:div "Imports: "
     (into [:ul]
           (for [[url indirects] imports]
             (into [:li url] (list-indirects indirects))))]
    [:div "Imports: none" [:br] [:br]]))

(defn gen-entry
  "Generates HTML for each entry in the catalog"
  [catalog-entry]
  [:div
   [:h5
    [:a {:href (:request-url catalog-entry)
         :target "_blank"}
     (:request-url catalog-entry)]
    (str " on " (:request-date catalog-entry))]
   (if-let [v (get-in catalog-entry [:metadata :version-iri])]
     (str "Version: " (get-in catalog-entry [:metadata :version-iri]))
     "Version: undefined")
   [:br]
   "Location: " [:a {:href (:location catalog-entry)
                     :target "_blank"}
                 (:location catalog-entry)]
   [:br]
   (list-imports (get-in catalog-entry [:metadata :imports]))])

(defn gen-html
  "Generates a full HTML report of all requests."
  [catalog]
  (hic/html
   [:html {:lang "en"}
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
     [:title "Ontofetch Report"]
     [:link {:rel "stylesheet" :href +bootstrap+}]]
    [:body
     [:div {:class "container"}
      [:div
       [:h1 "ontofetch Requests"]
       [:p {:class "lead"}
        "See the full metadata in "
        [:a {:href "catalog.edn" :target "_blank"} "catalog.edn"]]]
      [:hr]
      (map gen-entry catalog)]]]))
