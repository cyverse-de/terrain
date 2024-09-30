(ns terrain.clients.async-tasks
  (:require [async-tasks-client.core :as async-tasks-client]
            [terrain.util.config :as config]))

(defn run-async-thread
  [async-task-id thread-function prefix]
  (.start (Thread. thread-function (str prefix "-" async-task-id)))
  async-task-id)

(defn get-by-id
  [id]
  (async-tasks-client/get-by-id (config/async-tasks-client) id))
