(ns gumshoe.quick-test
  (:require [gumshoe.track :refer (deft)])
  (:refer-clojure :exclude [find-ns all-ns find-var ns-aliases]    ;[find-ns find-var all-ns ns-aliases]
                  ))

(def NSES :cljs.analyzer/namespaces)

(deft easier [a]
  (println a))

(deft all-ns
  [env]
  (NSES env))

(deft test-fn [a b c]
  a)

(let [a (rand-int 1000)]
  (println "rand-int" a)
  (println "res" (test-fn a 2 3))
  (println "-test-fn-a" (ns-resolve 'gumshoe.quick-test '-test-fn-a))
  (println -test-fn-a))
