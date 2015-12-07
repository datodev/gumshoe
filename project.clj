(defproject dato/gumshoe "0.1.1"
  :description "Debugging tool for tracking arguments passed to Clojure functions"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :profiles {:dev {:source-paths ["src" "dev"]
                   :plugins [[cider/cider-nrepl "0.9.1"]]
                   :dependencies [[org.clojure/tools.nrepl "0.2.10"]
                                  [cider/cider-nrepl "0.9.1" :exclusions [org.clojure/tools.nrepl]]
                                  [org.clojure/tools.logging "0.3.1"]
                                  [compojure "1.3.4"]
                                  [hiccup "1.0.5"]
                                  [org.immutant/web "2.0.2"]]}})
