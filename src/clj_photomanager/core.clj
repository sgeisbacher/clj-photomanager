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


(defn prep_json_resp [resp]
  (parse-string (:body resp) true))

(defn doreq [url] (prep_json_resp @(http/get (str "http://localhost:8080" url) {})))



(defn prefixmatch [text, prefix]
  (re-matches (re-pattern (str (strlib/lower-case prefix) ".*")) text))


(defn getgalleries []
  (doreq "/rest/gallery/"))

(defn getgallerynames [prefix]
  (filter
    (fn [gname] (prefixmatch gname prefix))
    (map 
      (fn [g] (strlib/lower-case (:name g)))
      (getgalleries))))



(defroutes app-routes
  (GET "/" [] (str "hello" " " "sgeisbacher" "!"))
  (GET "/galleries" [] {:body (getgalleries)})
  (GET "/gallerynames/:prefix" [prefix] {:body {:names (getgallerynames prefix)}})
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

