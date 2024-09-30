(ns terrain.routes.schemas.filesystem.tickets
  (:require [common-swagger-api.schema :refer [describe]]
            [common-swagger-api.schema.data.tickets :as schema]
            [schema.core :as s]
            [schema-tools.core :as st]))

;; Convert the keywords in the `mode` param to strings,
;; so that the correct param format is passed through to the apps service.
;; Remove `for-job` param.
;; FIXME: Make `public` param optional (for backwards compatibility).
(s/defschema AddTicketQueryParams
  (-> schema/AddTicketQueryParams
      (st/dissoc :for-job)
      (st/optional-keys [:public])
      (merge {schema/ModeParamOptionalKey
              (describe (apply s/enum (map name schema/ModeParamValues))
                        schema/ModeParamDocs)})))
