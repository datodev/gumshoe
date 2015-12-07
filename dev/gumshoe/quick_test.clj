(ns gumshoe.quick-test
  (:require [gumshoe.track :refer (deft)]))

(deft test-fn [a b c]
  a)

(let [a (rand-int 1000)]
  (println "rand-int" a)
  (println "res" (test-fn a 2 3))
  (println "-test-fn-a" (ns-resolve 'gumshoe.quick-test '-test-fn-a)))

(defnt interned-test [a b c]
  a)

(let [a (rand-int 1000)]
  (println "rand-int" a)
  (println "res" (interned-test a 2 3))
  (println "-interned-test-a" (ns-resolve 'gumshoe.quick-test '-interned-test-a)))
