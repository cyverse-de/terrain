(ns terrain.clients.util
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:require [clojure-commons.error-codes :as ce]))

(defmacro with-trap
  [[handle-error] & body]
  `(try+
    (do ~@body)
    (catch [:status 400] bad-request#
      (~handle-error ce/ERR_BAD_REQUEST bad-request#))
    (catch [:status 404] not-found#
      (~handle-error ce/ERR_NOT_FOUND not-found#))
    (catch (comp number? :status) server-error#
      (~handle-error ce/ERR_REQUEST_FAILED server-error#))))
