(ns terrain.routes.apps.elements
  (:require [common-swagger-api.schema :refer [context GET]]
            [common-swagger-api.schema.common :refer [IncludeHiddenParams]]
            [common-swagger-api.schema.apps.elements :as app-elements-schema]
            [ring.util.http-response :refer [ok]]
            [terrain.auth.user-attributes :refer [require-authentication]]
            [terrain.clients.apps.raw :as apps]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations to get rid of lint warnings for path and query parameter bindings.
(declare params)

(defn app-elements-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (context "/apps/elements" []
      :tags ["app-element-types"]

      (GET "/" []
           :middleware [require-authentication]
           :query [params IncludeHiddenParams]
           :summary app-elements-schema/AppElementsListingSummary
           :description app-elements-schema/AppElementsListingDocs
           (ok (apps/get-all-workflow-elements params)))

      (GET "/data-sources" []
           :middleware [require-authentication]
           :return app-elements-schema/DataSourceListing
           :summary app-elements-schema/AppElementsDataSourceListingSummary
           :description app-elements-schema/AppElementsDataSourceListingDocs
           (ok (apps/get-workflow-elements "data-sources" nil)))

      (GET "/file-formats" []
           :middleware [require-authentication]
           :return app-elements-schema/FileFormatListing
           :summary app-elements-schema/AppElementsFileFormatListingSummary
           :description app-elements-schema/AppElementsFileFormatListingDocs
           (ok (apps/get-workflow-elements "file-formats" nil)))

      (GET "/info-types" []
           :middleware [require-authentication]
           :return app-elements-schema/InfoTypeListing
           :summary app-elements-schema/AppElementsInfoTypeListingSummary
           :description app-elements-schema/AppElementsInfoTypeListingDocs
           (ok (apps/get-workflow-elements "info-types" nil)))

      (GET "/parameter-types" []
           :middleware [require-authentication]
           :query [params app-elements-schema/AppParameterTypeParams]
           :return app-elements-schema/ParameterTypeListing
           :summary app-elements-schema/AppElementsParameterTypeListingSummary
           :description app-elements-schema/AppElementsParameterTypeListingDocs
           (ok (apps/get-workflow-elements "parameter-types" params)))

      (GET "/rule-types" []
           :middleware [require-authentication]
           :return app-elements-schema/RuleTypeListing
           :summary app-elements-schema/AppElementsRuleTypeListingSummary
           :description app-elements-schema/AppElementsRuleTypeListingDocs
           (ok (apps/get-workflow-elements "rule-types" nil)))

      (GET "/tool-types" []
           :middleware [require-authentication]
           :return app-elements-schema/ToolTypeListing
           :summary app-elements-schema/AppElementsToolTypeListingSummary
           :description app-elements-schema/AppElementsToolTypeListingDocs
           (ok (apps/get-workflow-elements "tool-types" nil)))

      (GET "/value-types" []
           :middleware [require-authentication]
           :return app-elements-schema/ValueTypeListing
           :summary app-elements-schema/AppElementsValueTypeListingSummary
           :description app-elements-schema/AppElementsValueTypeListingDocs
           (ok (apps/get-workflow-elements "value-types" nil))))))
