(ns gumshoe.track)

(defn assert-valid-fdecl
  "A good fdecl looks like (([a] ...) ([a b] ...)) near the end of defn."
  [fdecl]
  (when (empty? fdecl) (throw (IllegalArgumentException.
                               "Parameter declaration missing")))
  (let [argdecls (map
                  #(if (seq? %)
                     (first %)
                     (throw (IllegalArgumentException.
                             (if (seq? (first fdecl))
                               (str "Invalid signature \""
                                    %
                                    "\" should be a list")
                               (str "Parameter declaration \""
                                    %
                                    "\" should be a vector")))))
                  fdecl)
        bad-args (seq (remove #(vector? %) argdecls))]
    (when bad-args
      (throw (IllegalArgumentException. (str "Parameter declaration \"" (first bad-args)
                                             "\" should be a vector"))))))

(defn sigs [fdecl]
  (assert-valid-fdecl fdecl)
  (let [asig
        (fn [fdecl]
          (let [arglist (first fdecl)
                                        ;elide implicit macro args
                arglist (if (clojure.lang.Util/equals '&form (first arglist))
                          (clojure.lang.RT/subvec arglist 2 (clojure.lang.RT/count arglist))
                          arglist)
                body (next fdecl)]
            (if (map? (first body))
              (if (next body)
                (with-meta arglist (conj (if (meta arglist) (meta arglist) {}) (first body)))
                arglist)
              arglist)))]
    (if (seq? (first fdecl))
      (loop [ret [] fdecls fdecl]
        (if fdecls
          (recur (conj ret (asig (first fdecls))) (next fdecls))
          (seq ret)))
      (list (asig fdecl)))))

(defmacro def-locals [base-name]
  (let [env# &env]
    `(do
       ~@(for [k (keys env#)
               :when (not (contains? #{'&env '&form} k))
               :let [sym (symbol (str "-" base-name "-" k))]]
           `(intern *ns* '~sym ~k)))))

(defn deft [&form &env name & fdecl]
  ;; Note: Cannot delegate this check to def because of the call to (with-meta name ..)
  (if (instance? clojure.lang.Symbol name)
    nil
    (throw (IllegalArgumentException. "First argument to defn must be a symbol")))
  (let [m (if (string? (first fdecl))
            {:doc (first fdecl)}
            {})
        fdecl (if (string? (first fdecl))
                (next fdecl)
                fdecl)
        m (if (map? (first fdecl))
            (conj m (first fdecl))
            m)
        fdecl (if (map? (first fdecl))
                (next fdecl)
                fdecl)
        fdecl (if (vector? (first fdecl))
                (list fdecl)
                fdecl)
        m (if (map? (last fdecl))
            (conj m (last fdecl))
            m)
        fdecl (if (map? (last fdecl))
                (butlast fdecl)
                fdecl)
        m (conj {:arglists (list 'quote (sigs fdecl))} m)
        m (let [inline (:inline m)
                ifn (first inline)
                iname (second inline)]
            ;; same as: (if (and (= 'fn ifn) (not (symbol? iname))) ...)
            (if (if (clojure.lang.Util/equiv 'fn ifn)
                  (if (instance? clojure.lang.Symbol iname) false true))
              ;; inserts the same fn name to the inline fn if it does not have one
              (assoc m :inline (cons ifn (cons (clojure.lang.Symbol/intern (.concat (.getName ^clojure.lang.Symbol name) "__inliner"))
                                               (next inline))))
              m))
        m (conj (if (meta name) (meta name) {}) m)]
    (list 'def (with-meta name m)
          ;;todo - restore propagation of fn name
          ;;must figure out how to convey primitive hints to self calls first
          ;;(cons `fn fdecl)
          (concat (list `fn) (for [decl fdecl
                                   :let [args (first decl)
                                         body (rest decl)]]
                               (concat (list args)
                                       ;; tracker
                                       (list `(def-locals ~name))
                                       (rest decl)))))))

(. (var deft) (setMacro))

;; TODO: finish porting over tools.trace stuff for tracking an entire namespace

;; (defn track-var*
;;   "If the specified Var holds an IFn and is not marked as a macro, its
;;   contents is replaced with a version wrapped in a tracking call;
;;   otherwise nothing happens. Can be undone with untrack-var.

;;   In the unary case, v should be a Var object or a symbol to be
;;   resolved in the current namespace.

;;   In the binary case, ns should be a namespace object or a symbol
;;   naming a namespace and s a symbol to be resolved in that namespace."
;;   ([ns s]
;;      (track-var* (ns-resolve ns s)))
;;   ([v]
;;      (let [^clojure.lang.Var v (if (var? v) v (resolve v))
;;            ns (.ns v)
;;            s  (.sym v)]
;;        (if (and (ifn? @v) (-> v meta :macro not) (-> v meta ::tracked not))
;;          (let [f @v
;;                vname (symbol (str ns "/" s))]
;;            (doto v
;;              (alter-var-root #(fn tracking-wrapper [& args]
;;                                 ;(trace-fn-call vname % args)
;;                                 ))
;;              (alter-meta! assoc ::tracked f)))))))

;; (defn untrack-var*
;;   "Reverses the effect of track-var / track-vars / track-ns for the
;;   given Var, replacing the tracked function with the original, untracked
;;   version. No-op for non-tracked Vars.

;;   Argument types are the same as those for track-var."
;;   ([ns s]
;;      (untrack-var* (ns-resolve ns s)))
;;   ([v]
;;      (let [^clojure.lang.Var v (if (var? v) v (resolve v))
;;            ns (.ns v)
;;            s  (.sym v)
;;            f  ((meta v) ::tracked)]
;;        (when f
;;          (doto v
;;            (alter-var-root (constantly ((meta v) ::tracked)))
;;            (alter-meta! dissoc ::tracked))))))

;; (defmacro track-vars
;;   "Track each of the specified Vars.
;;   The arguments may be Var objects or symbols to be resolved in the current
;;   namespace."
;;   [& vs]
;;   `(do ~@(for [x vs]
;;            `(if (var? ~x)
;;               (track-var* ~x)
;;               (track-var* (quote ~x))))))

;; (defmacro untrack-vars
;;   "Untrack each of the specified Vars.
;;   Reverses the effect of track-var / track-vars / track-ns for each
;;   of the arguments, replacing the tracked functions with the original,
;;   untracked versions."
;;   [& vs]
;;   `(do ~@(for [x vs]
;;            `(if (var? ~x)
;;               (untrack-var* ~x)
;;               (untrack-var* (quote ~x))))))

;; (defn track-ns*
;;   "Replaces each function from the given namespace with a version wrapped
;;   in a tracking call. Can be undone with untrack-ns. ns should be a namespace
;;   object or a symbol.

;;   No-op for clojure.core and gumshoe.track"
;;   [ns]
;;   (let [ns (the-ns ns)]
;;     (when-not ('#{clojure.core gumshoe.track} (.name ns))
;;       (let [ns-fns (->> ns ns-interns vals (filter (comp fn? var-get)))]
;;         (doseq [f ns-fns]
;;           (track-var* f))))))

;; (defn resolves-as-var?
;;   "Try to resolve the symbol in several ways to find out if it's a var or not."
;;   [n]
;;   (cond
;;     (coll? n) nil
;;     (try (find-ns n) (catch Exception _)) nil
;;     :else
;;     (if-let [v (try (ns-resolve *ns* n) (catch Exception _))] (var? v))))

;; (defmacro track-ns
;;   "Track all fns in the given name space. The given name space can be quoted, unquoted or stored in a var.
;;    We must try to resolve the expression passed to us partially to find out if it needs to be quoted or not
;;    when passed to track-ns*"
;;   [n]
;;   (let [quote? (not (or (resolves-as-var? n) (and (coll? n) (= (first n) (quote quote)))))
;;         n (if quote? (list 'quote n) n)]
;;     `(track-ns* ~n)))

;; (defn untrack-ns*
;;   "Reverses the effect of track-var / track-vars / track-ns for the
;;   Vars in the given namespace, replacing each tracked function from the
;;   given namespace with the original, untracked version."
;;   [ns]
;;   (let [ns-fns (->> ns the-ns ns-interns vals)]
;;     (doseq [f ns-fns]
;;           (untrack-var* f))))

;; (defmacro untrack-ns
;;   "Untrack all fns in the given name space. The given name space can be quoted, unquoted or stored in a var.
;;    We must try to resolve the expression passed to us partially to find out if it needs to be quoted or not
;;    when passed to untrack-ns*"
;;   [n]
;;   (let [quote? (not (or (resolves-as-var? n) (and (coll? n) (= (first n) (quote quote)))))
;;         n (if quote? (list 'quote n) n)]
;;      `(untrack-ns* ~n)))

;; (defn tracked?
;;   "Returns true if the given var is currently tracked, false otherwise"
;;   [v]
;;   (let [^clojure.lang.Var v (if (var? v) v (resolve v))]
;;     (-> v meta ::tracked nil? not)))

;; (defn trackable?
;;   "Returns true if the given var can be tracked, false otherwise"
;;   [v]
;;   (let [^clojure.lang.Var v (if (var? v) v (resolve v))]
;;     (and (ifn? @v) (-> v meta :macro not))))

;; ;; TODO:
;; ;;  1. Helper to replay the function call
;; ;;    a. Handle multiple arities
;; ;;       - How do you know which arity was last called?
;; ;;  2.
