(ns terrain.routes.schemas.alerts
  (:require [common-swagger-api.schema :refer [describe]]
            [schema.core :refer [defschema Any optional-key]]))

(defschema Alert
  {(optional-key :start-date)
   (describe String "The timestamp when the alert should begin showing, YYYY-MM-DDTHH:MM:SS-ZZ:ZZ format. Ignored for deletion requests.")

   :end-date
   (describe String "The timestamp when the alert should stop showing, YYYY-MM-DDTHH:MM:SS-ZZ:ZZ format. Part of what identifies an alert, so must be provided to delete an alert.")

   :alert-text
   (describe String "The text of the alert to be shown. Markdown formatting may be interpreted by the UI. Part of what identifes an alert, so must be provided to delete an alert.")})
