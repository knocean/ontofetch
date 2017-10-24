(ns ontofetch.html
  (:require
   [hiccup.core :as hic]))

(def +bootstrap+ "resources/static/css/bootstrap.min.css")

(defn gen-entry
  "Generates HTML for each entry in the catalog"
  [catalog-entry]
  [:div
   [:h5
    [:a {:href (:request-url catalog-entry)
         :target "_blank"}
     (:request-url catalog-entry)]
    (str " on " (:request-date catalog-entry))]
   (str "Version: " (get-in catalog-entry [:metadata :version-iri]))
   [:br]
   "Location: " [:a {:href (:location catalog-entry)
                     :target "_blank"}
                 (:location catalog-entry)]
   [:br]
   "Imports: "
   (if-not (empty? (get-in catalog-entry [:metadata :imports]))
     [:ul
      (for [url (get-in catalog-entry [:metadata :imports])]
        [:li url])]
     (str "none"))
   [:br] [:br]])

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
       [:h1 "Auto-Generated Ontofetch Report"]
       [:p {:class "lead"}
        "A history of past ontofetch requests. See the full metadata in catalog.edn"]]
      [:hr]
      (map gen-entry catalog)]]]))