(ns terrain.services.filesystem.validators
  (:require [slingshot.slingshot :refer [throw+]]
            [terrain.services.filesystem.common-paths :as cp]))

(defn not-superuser
  [user]
  (when (cp/super-user? user)
    (throw+ {:type :clojure-commons.exception/not-authorized
             :user user})))
