(ns gumshoe.plugin
  (:require [clojure.java.io :as io]
            [leiningen.core.main :as lein]))

;; TODO: better way to handle this
(def VERSION "0.1.5")

(defn middleware
  [{:keys [dependencies] :as project}]
  (let [lein-version-ok? (lein/version-satisfies? (lein/leiningen-version) "2.5.2")
        clojure-version (->> dependencies
                             (some (fn [[id version & _]]
                                     (when (= id 'org.clojure/clojure)
                                       version))))
        clojure-version-ok? (if (nil? clojure-version)
                              ;; Lein 2.5.2+ uses Clojure 1.7 by default
                              lein-version-ok?
                              (lein/version-satisfies? clojure-version "1.7.0"))]

    (when-not lein-version-ok?
      (lein/warn "Warning: gumshoe requires Leiningen 2.5.2 or greater."))
    (when-not clojure-version-ok?
      (lein/warn "Warning: gumshoe requires Clojure 1.7 or greater."))
    (when-not (and lein-version-ok? clojure-version-ok?)
      (lein/warn "Warning: gumshoe will not be included in your project."))

    (cond-> project
      (and clojure-version-ok? lein-version-ok?)
      (-> (update-in [:dependencies]
                     (fnil into [])
                     [['dato/gumshoe VERSION]])
          (update-in [:injections]
                     (fnil into [])
                     ['(do (require 'gumshoe.track)
                           (gumshoe.track/deft-everything))])))))
