(ns terrain.services.email
  (:require
   [terrain.util.email :as email]))

(defn send-email
  [body]
  (-> (select-keys body [:to :cc :bcc :subject :template :values])
      (assoc :from-addr (:from_addr body)
             :from-name (:from_name body))
      email/send-email)
  {:status "OK"})
