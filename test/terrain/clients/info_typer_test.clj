(ns terrain.clients.info-typer-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as http]
            [terrain.clients.info-typer :as info-typer]
            [terrain.util.config :as cfg]))

(def ^:private base-url "http://info-typer")

(defn- capture-request
  "Invokes f with the given clj-http verb redefined to record its arguments, and returns the
   captured [url opts] pair."
  [verb f]
  (let [captured (atom nil)]
    (with-redefs-fn {#'cfg/info-typer-base-url (constantly base-url)
                     verb                      (fn [url opts] (reset! captured [url opts]) {:body {}})}
      f)
    @captured))

(deftest info-typer-requests
  (are [verb f expected-url expected-opts]
       (let [[url opts] (capture-request verb f)]
         (and (= expected-url url)
              (= expected-opts (select-keys opts (keys expected-opts)))))

    #'http/get #(info-typer/get-type-list)
    "http://info-typer/file-types"
    {:as :json}

    ;; the user has to travel as a query parameter; info-typer reads it from the query string
    #'http/put #(info-typer/set-file-type "someuser" "some-uuid" "csv")
    "http://info-typer/data/some-uuid/type"
    {:form-params {:type "csv"} :query-params {:user "someuser"} :content-type :json :as :json}

    ;; an empty type unsets the file's type rather than setting it to ""
    #'http/put #(info-typer/set-file-type "someuser" "some-uuid" "")
    "http://info-typer/data/some-uuid/type"
    {:form-params {:type ""} :query-params {:user "someuser"}}))

(deftest info-typer-requests-have-timeouts
  (are [verb f] (let [[_ opts] (capture-request verb f)]
                  (and (pos? (:socket-timeout opts)) (pos? (:conn-timeout opts))))
    #'http/get #(info-typer/get-type-list)
    #'http/put #(info-typer/set-file-type "someuser" "some-uuid" "csv")))
