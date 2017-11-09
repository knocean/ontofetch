(defproject ontofetch "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[clojure-tools "1.1.3"]
           [hiccup "1.0.5"]
           [http-kit "2.1.18"]
           [net.sourceforge.owlapi/owlapi-distribution "5.1.0"]
           [org.apache.jena/jena-arq "2.13.0"]
           [org.clojure/clojure "1.8.0"]
           [org.clojure/data.xml "0.0.8"]]
  :main ^:skip-aot ontofetch.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :plugins [[lein-cljfmt "0.5.7"]])
