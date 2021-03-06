(ns clj-photomanager.core
  (:use compojure.core)
  (:use ring.middleware.json)
  (:use ring.util.codec)
  (:require [compojure.core :refer :all]
            [cheshire.core :refer :all]
            [compojure.route :as route]
            [clojure.string :as strlib]
            [compojure.handler :as handler]
            [org.httpkit.client :as http]
            [clojure.core.memoize :as memo]))


;(def upstream "http://localhost:8000")
(def upstream "http://eco11-srv.ops.local.netconomy.net:8080")




(defn prep_json_resp [resp]
  (parse-string (:body resp) true))

(defn doreq [url] (prep_json_resp @(http/get (str upstream url) {})))





(defn get-galleries []
  (doreq "/rest/gallery/"))


(defn prefixmatch [text, prefix]
  (re-matches 
    (re-pattern (str (strlib/lower-case prefix) ".*")) 
    (strlib/lower-case text)))


(defn find-gallery-by-name-prefix [prefix]
  (filter
    (fn [gallery] (prefixmatch (:name gallery) prefix))
    (get-galleries)))


(defn calc-gallery-size [gid]
  (reduce 
    (fn [sizesum photo] (+ sizesum (:filesize photo)))
    0 
    (first 
      (map
        (fn [gallery] (:photos gallery))
        (filter 
          (fn [gallery] (= (str gid) (str (:id gallery)))) 
          (get-galleries))))))


(defn get-photo-gallery-map []
  (reduce
    (fn [init entry] (merge-with concat init entry)) 
    {}
    (map
      (fn [gallery]
        (reduce
          (fn [photos photo] (assoc photos (:name photo) (dissoc (dissoc gallery :photos) :mainPhoto))) 
          {} 
          (:photos gallery)))
      (get-galleries))))






(defroutes app-routes
  (GET "/" [] (slurp "index.html"))
  (GET "/gallery/" [] {:body (get-galleries)})
  (GET "/gallery/:prefix" [prefix] {:body (find-gallery-by-name-prefix prefix)})
  (GET "/photomap/" [] {:body (get-photo-gallery-map)})
  (GET "/gallerysize/:gid" [gid] {:body {:gallery-size (calc-gallery-size gid)}})
  (route/not-found {:body {:error 404}}))

(defn wrap-error-handling [f]
  (fn [req]
    (try (f req)
         (catch Exception e
           {:status 500
            :body {:error (str e)}}))))

(def app
  (-> app-routes
      (wrap-error-handling)
      (ring.middleware.json/wrap-json-response)
      (handler/api)))

