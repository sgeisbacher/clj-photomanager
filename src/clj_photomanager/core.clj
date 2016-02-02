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
 (doreq "/rest/gallery/")
  )

(defn prefixmatch [text, prefix]
  (not (nil? (re-matches (re-pattern (str prefix ".*")) text)))
  )

(defn find-gallery-by-name-prefix [prefix]
  (filter #(prefixmatch (:name %) prefix) (get-galleries))
 )

(defn reverse-single-gallery [gallery]
  (loop [result {}  photos (:photos gallery)] (if (empty? photos) result (recur (assoc result (:name (first photos)) {(:name gallery) (:id gallery)}) (rest photos))))
  )

(defn calc-gallery-size [gid]
  (reduce #(+ %1 %2) (map #(:filesize %) (:photos (first (filter #(= (:id %) gid) (get-galleries))))))
  )

(defn get-photo-gallery-map []
  (reduce #(merge %1 %2) {} (map reverse-single-gallery (get-galleries)))
  )



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
