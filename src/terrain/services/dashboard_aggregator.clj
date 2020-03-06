(ns terrain.services.dashboard-aggregator
  (:use [terrain.auth.user-attributes :only [current-user]]
        [terrain.util.service :only [success-response]])
  (:require [terrain.clients.dashboard-aggregator :as dcl]))

(defn dashboard-data
  []
  (success-response (dcl/get-dashboard-data (:username current-user))))