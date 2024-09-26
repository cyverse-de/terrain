(ns terrain.util
  "Utility functions for defining services in Terrain. This namespace is used by terrain.core and
   several other top-level service definition namespaces."
  (:require [common-swagger-api.schema :refer [routes]]
            [terrain.util.service :refer [success-response]]
            [terrain.util.transformers :refer [add-current-user-to-map]]
            [terrain.util.validators :refer [parse-body]]))

(defn optional-routes
  "Creates a set of optionally defined routes."
  [[option-fn] & handlers]
  (when (option-fn)
    (apply routes handlers)))

(defn flagged-routes
  "Creates a set of routes, removing any nil route definitions."
  [& handlers]
  (apply routes (remove nil? handlers)))

(defn- pre-process-request
  [req & {:keys [slurp?] :or {slurp? false}}]
  (let [req (assoc req :params (add-current-user-to-map (:params req)))]
    (if slurp?
      (assoc req :body (parse-body (slurp (:body req))))
      req)))

(defn- ctlr
  [req slurp? func & args]
  (let [req     (pre-process-request req :slurp? slurp?)
        get-arg (fn [arg] (if (keyword? arg) (get req arg) arg))
        argv    (mapv get-arg args)]
    (success-response (apply func argv))))

(defn controller
  [req func & args]
  (let [p (if (contains? (set args) :body)
            (partial ctlr req true func)
            (partial ctlr req false func))]
    (apply p args)))

(defn disable-redirects
  ([]
   (disable-redirects {}))
  ([opts]
   (assoc opts
          :redirect-strategy    :none
          :unexceptional-status (fn [status] (<= 200 status 299)))))
