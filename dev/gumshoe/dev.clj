(ns gumshoe.dev
  (:require [clojure.pprint]
            [clojure.tools.nrepl.server :refer (start-server stop-server)]
            [clojure.tools.logging :as log]
            [compojure.core :refer (routes GET POST)]
            [compojure.route]
            [cider.nrepl]
            [hiccup.page]
            [hiccup.core]
            [immutant.web :as web]
            [gumshoe.track]))

(gumshoe.track/deft test-fn [a b & {:keys [c d e] :as extra-args}]
  (println "test-fn was called with" a b extra-args)
  :test-fn)

(defn something-something [a b d]
  :something-something)

(defmulti test-multi (fn [x] x))

(defmethod test-multi :default
  [x]
  :default)

(defmethod test-multi :a
  [x]
  :first-test-multi)

(defmethod test-multi :b
  [x]
  :second-test-multi)

(defn body []
  (hiccup.page/html5
   [:head
    [:title "Gumshoe Dev"]
    [:meta {:charset "utf-8"}]]
   [:body
    [:div "Test fn: " (test-fn 3 4 :c 4 :d 7)]
    [:div "Test multi default: " (test-multi nil)]
    [:div "Test multi a: " (test-multi :a)]
    [:div "Test multi b: " (test-multi :b)]
    [:div "Something something: " (something-something 1 2 3)]
    [:div "Vars: "
     [:pre (hiccup.core/h
            (pr-str
             (into {}
                   (filter #(re-find #"^-test-fn-[a-z]$" (name (first %)))
                           (ns-map 'gumshoe.dev)))))]]]))

(defn route-handler []
  (routes
   (GET "/" []
        {:status 200 :body (body)})
   (compojure.route/resources "/" {:root "public"
                                   :mime-types {:svg "image/svg"}})
   (fn [req]
     {:status 404
      :body "<body>Sorry, we couldn't find that page. <a href='/'>Back to home</a>.</body>"})))

(defn handler []
  (-> (route-handler)))

(defn nrepl-port []
  (if (System/getenv "NREPL_PORT")
    (Integer/parseInt (System/getenv "NREPL_PORT"))
    3005))

(defn server-port []
  (if (System/getenv "HTTP_PORT")
    (Integer/parseInt (System/getenv "HTTP_PORT"))
    4579))

(defn start-web []
  (let [port (server-port)]
    (println "Starting web server on port" port)
    (def web-server (web/server (web/run
                                  (handler)
                                  {:port port
                                   :host "0.0.0.0"})))))

(defn start-nrepl []
  (let [port (nrepl-port)]
    (println "Starting nrepl on port" port)
    (def nrepl-server (start-server :port port :handler cider.nrepl/cider-nrepl-handler))))

(defn init []
  (start-nrepl)
  (start-web))

(defn restart-web []
  (.stop web-server)
  (start-web))


(defn -main []
  (init))
