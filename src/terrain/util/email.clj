(ns terrain.util.email
  (:require [clj-http.client :as client]
            [clojure.string :as string]
            [terrain.auth.user-attributes :refer [current-user]]
            [terrain.util.config :as config]))

(defn send-email
  "Sends an e-mail message via the iPlant e-mail service."
  [& {:keys [to from-addr from-name subject template values]}]
  (client/post
   (config/iplant-email-base-url)
   {:content-type :json
    :form-params  {:to        to
                   :from-addr from-addr
                   :from-name from-name
                   :subject   subject
                   :template  template
                   :values    values}}))

(defn send-tool-request-email
  "Sends the email message informing Core Services of a tool request."
  [tool-req {:keys [firstname lastname _email]}]
  (let [template-values {:username           (str firstname " " lastname)
                         :environment        (config/environment-name)
                         :toolrequestid      (:uuid tool-req)
                         :toolrequestdetails tool-req}]
    (send-email
      :to        (config/tool-request-dest-addr)
      :from-addr (config/tool-request-src-addr)
      :subject   "New Tool Request"
      :template  "tool_request"
      :values    template-values)))

(defn- send-permanent-id-request
  "Sends a Permanent ID Request email message to data curators."
  [subject template template-values]
  (send-email
    :to        (config/permanent-id-request-dest-addr)
    :from-addr (config/permanent-id-request-src-addr)
    :subject   subject
    :template  template
    :values    template-values))

(defn send-permanent-id-request-new
  "Sends an email message informing data curators of a new Permanent ID Request."
  [request-type path {:keys [commonName shortUsername]}]
  (let [template-values {:username     shortUsername
                         :user         commonName
                         :environment  (config/environment-name)
                         :request_type request-type
                         :path         path}]
    (send-permanent-id-request
      "New Permanent ID Request"
      "permanent_id_request"
      template-values)))

(defn send-permanent-id-request-data-move-error
  "Sends an email message informing data curators that a Permanent ID Request data folder could not be moved to the
   commons repo folder."
  [path dest-path {:keys [commonName shortUsername]} error-msg]
  (send-permanent-id-request
    "Could not move Permanent ID Request data folder"
    "permanent_id_request_move_error"
    {:username      shortUsername
     :user          commonName
     :environment   (config/environment-name)
     :path          path
     :dest          dest-path
     :error_message error-msg}))

(defn send-permanent-id-request-complete
  "Sends an email message informing data curators of a Permanent ID Request completion."
  [request-type doi path api-response]
  (let [template-values {:environment  (config/environment-name)
                         :request_type request-type
                         :doi          doi
                         :path         path
                         :api_response api-response}]
    (send-permanent-id-request
      "Permanent ID Request Complete"
      "permanent_id_request_complete"
      template-values)))

(defn send-permanent-id-request-complete-for-user
  "Sends an email message informing the requesting user of a Permanent ID Request completion."
  [request-type doi path {:keys [name email]}]
  (let [template-values {:user name
                         :path path
                         :doi  doi}
        subject         (str request-type " Permanent ID Request Complete")]
    (send-email
      :to        email
      :from-addr (config/permanent-id-request-src-addr)
      :subject   subject
      :template  "permanent_id_request_completion_user"
      :values    template-values)))

(defn send-permanent-id-request-submitted
  "Sends an email message to the user requesting a new Permanent ID Request."
  [request-type path {:keys [commonName email]}]
  (let [template-values {:user         commonName
                         :environment  (config/environment-name)
                         :request_type request-type
                         :path         path}
        subject         (str request-type " Permanent ID Request Submitted")]
    (send-email
      :to        email
      :from-addr (config/permanent-id-request-src-addr)
      :subject   subject
      :template  "permanent_id_request_submitted"
      :values    template-values)))

(defn format-field
  "Formats a single field in a support email."
  [[k v]]
  (str (->> (if (sequential? v) v [v])
            (mapv (partial str " - "))
            (concat [(name k)])
            (string/join "\n"))
       "\n\n"))

(defn send-support-email
  "Sends email messages containing information about a request for support."
  [{:keys [email fields subject]}]
  (send-email
   :to        (config/support-email-dest-addr)
   :from-addr (or email (:email current-user) (config/support-email-src-addr))
   :subject   (or subject "DE Support Request")
   :template  "blank"
   :values    {:contents (apply str (mapv format-field fields))}))
