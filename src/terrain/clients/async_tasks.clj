(ns terrain.clients.async-tasks
  (:require [async-tasks-client.core :as async-tasks-client]
            [clojure.string :as string]
            [terrain.util.config :as config]
            [otel.otel :as otel]))

(defn run-async-thread
  [async-task-id thread-function prefix]
  (otel/with-span [outer-span ["run-async-thread" {:kind :producer :attributes {"async-task-id" (str async-task-id)}}]]
    (let [^Runnable task-thread (fn []
                                  (with-open [_ (otel/span-scope outer-span)]
                                    (otel/with-span [s ["async thread" {:kind :consumer :attributes {"async-task-id" (str async-task-id)}}]]
                                      (thread-function))))]
      (.start (Thread. task-thread (str prefix "-" async-task-id))))
    async-task-id))

(defn get-by-id
  [id]
  (async-tasks-client/get-by-id (config/async-tasks-client) id))
