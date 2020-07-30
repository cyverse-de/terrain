(ns terrain.clients.async-tasks
  (:require [async-tasks-client.core :as async-tasks-client]
            [clojure.string :as string]
            [terrain.util.config :as config]))

(defn run-async-thread
  [async-task-id thread-function prefix]
  (let [^Runnable task-thread (fn [] (thread-function))]
    (.start (Thread. task-thread (str prefix "-" async-task-id))))
  async-task-id)

(defn get-by-id
  [id]
  (async-tasks-client/get-by-id (config/async-tasks-client) id))
