(defproject ontofetch "0.1.0-SNAPSHOT"
  :description "A tool for fetching OWL ontologies"
  :url "https://github.com/knocean/ontofetch"
  :license {:name "BSD 3-Clause License"
            :url "https://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[clj-time "0.14.2"]
           [clojure-tools "1.1.3"]
           [hiccup "1.0.5"]
           [http-kit "2.1.18"]
           [net.sourceforge.owlapi/owlapi-distribution "5.1.0"]
           [owlapi-tools "6.0.0"]
           [org.apache.jena/jena-arq "2.13.0"]
           [org.clojure/core.async "0.3.465"]
           [org.clojure/clojure "1.8.0"]
           [org.clojure/data.xml "0.0.8"]
           [org.clojure/tools.cli "0.3.5"]
           [org.clojure/tools.logging "0.4.0"]
           [overtone/at-at "1.2.0"]]
  :main ontofetch.core
  :target-path "target/%s"
  :bin {:name "ontofetch"
        :bin-path "target"
        :bootclasspath true}
  :profiles {:uberjar {:aot :all}}
  :plugins [[lein-bin "0.3.5"]
            [lein-cljfmt "0.5.7"]
            [lein-cloverage "1.0.10"]])
