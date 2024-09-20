(ns terrain.clients.user-sessions
  (:require [clj-http.client :as http]
            [cemerick.url :refer [url]]
            [cheshire.core :as json]
            [clojure-commons.error-codes :as ce]
            [slingshot.slingshot :refer [throw+]]
            [terrain.util.config :as config]))

(defn session-url
  [user]
  (str (url (config/sessions-base-url) user)))

(defn get-session
  [user]
  (let [resp (http/get (session-url user))]
    (cond
     (= (:status resp) 404)
     (throw+ {:error_code ce/ERR_NOT_A_USER :user user})

     (= (:status resp) 400)
     (throw+ {:error_code ce/ERR_BAD_REQUEST :user user})

     (= (:status resp) 500)
     (throw+ {:error_code ce/ERR_UNCHECKED_EXCEPTION :msg "Error thrown by user-sessions service"})

     (not (<= 200 (:status resp) 299))
     (throw+ {:error_code ce/ERR_UNCHECKED_EXCEPTION :msg "Unknown error thrown by the user-sessions service"})

     :else
     (json/parse-string (:body resp) true))))

(defn set-session
  [user session]
  (let [req-options  {:body session
                      :content-type "application/json"
                      :throw-exceptions false}
        resp         (http/post (session-url user) req-options)]
    (cond
     (= (:status resp) 404)
     (throw+ {:error_code ce/ERR_NOT_A_USER :user user})

     (= (:status resp) 400)
     (throw+ {:error_code ce/ERR_BAD_REQUEST :session session})

     (= (:status resp) 415)
     (throw+ {:error_code ce/ERR_BAD_REQUEST :content-type "application/json"})

     (= (:status resp) 500)
     (throw+ {:error_code ce/ERR_UNCHECKED_EXCEPTION :msg "Error thrown by user-sessions service"})

     (not (<= 200 (:status resp) 299))
     (throw+ {:error_code ce/ERR_UNCHECKED_EXCEPTION :msg "Unknown error thrown by the user-sessions service"})

     :else
     (json/parse-string (:body resp) true))))

(defn delete-session
  [user]
  (let [resp (http/delete (session-url user))]
    (cond
     (= (:status resp) 404)
     (throw+ {:error_code ce/ERR_NOT_A_USER :user user})

     (= (:status resp) 400)
     (throw+ {:error_code ce/ERR_BAD_REQUEST :user user})

     (= (:status resp) 500)
     (throw+ {:error_code ce/ERR_UNCHECKED_EXCEPTION :msg "Error thrown by user-sessions service"})

     (not (<= 200 (:status resp) 299))
     (throw+ {:error_code ce/ERR_UNCHECKED_EXCEPTION :msg "Unknown error thrown by the user-sessions service"}))))
