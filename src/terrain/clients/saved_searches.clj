(ns terrain.clients.saved-searches
  (:require [clj-http.client :as http]
            [clojure-commons.error-codes :as ce]
            [cemerick.url :refer [url]]
            [slingshot.slingshot :refer [throw+]]
            [terrain.util.config :as config]))

(defn saved-searches-url
  [user]
  (str (url (config/saved-searches-base-url) user)))

(defn get-saved-searches
  [user]
  (let [resp (http/get (saved-searches-url user) {:as :json})]
    (cond
      (= (:status resp) 404)
      (throw+ {:error_code ce/ERR_NOT_A_USER :user user})

      (= (:status resp) 400)
      (throw+ {:error_code ce/ERR_BAD_REQUEST :user user})

      (= (:status resp) 500)
      (throw+ {:error_code ce/ERR_UNCHECKED_EXCEPTION :msg "Error thrown by the saved-searches service"})

      (not (<= 200 (:status resp) 299))
      (throw+ {:error_code ce/ERR_UNCHECKED_EXCEPTION :msg "Unknown error thrown by the saved-searches service"})

      :else
      (:body resp))))

(defn set-saved-searches
  [user saved-searches]
  (let [req-options {:form-params      saved-searches
                     :content-type     :json
                     :throw-exceptions false
                     :as               :json}
        resp        (http/post (saved-searches-url user) req-options)]
    (cond
      (= (:status resp) 404)
      (throw+ {:error_code ce/ERR_NOT_A_USER :user user})

      (= (:status resp) 400)
      (throw+ {:error_code ce/ERR_BAD_REQUEST :saved-searches saved-searches})

      (= (:status resp) 415)
      (throw+ {:error_code ce/ERR_BAD_REQUEST :content-type "application/json"})

      (= (:status resp) 500)
      (throw+ {:error_code ce/ERR_UNCHECKED_EXCEPTION :msg "Error thrown by the saved-searches service"})

      (not (<= 200 (:status resp) 299))
      (throw+ {:error_code ce/ERR_UNCHECKED_EXCEPTION :msg "Unknown error thrown by the saved-searches service"})

      :else
      (:body resp))))

(defn delete-saved-searches
  [user]
  (let [resp (http/delete (saved-searches-url user))]
    (cond
     (= (:status resp) 404)
     (throw+ {:error_code ce/ERR_NOT_A_USER :user user})

     (= (:status resp) 400)
     (throw+ {:error_code ce/ERR_BAD_REQUEST :user user})

     (= (:status resp) 500)
     (throw+ {:error_code ce/ERR_UNCHECKED_EXCEPTION :msg "Error thrown by the saved-searches service"})

     (not (<= 200 (:status resp) 299))
     (throw+ {:error_code ce/ERR_UNCHECKED_EXCEPTION :msg "Unknown error thrown by the saved-searches service"})

     :else
     nil)))
