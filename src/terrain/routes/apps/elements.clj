(ns terrain.routes.apps.elements
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.common :only [IncludeHiddenParams]]
        [common-swagger-api.schema.apps.elements]
        [ring.util.http-response :only [ok]]
        [terrain.auth.user-attributes :only [require-authentication]]
        [terrain.util])
  (:require [terrain.clients.apps.raw :as apps]
            [terrain.util.config :as config]))

(defn app-elements-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (context "/apps/elements" []
      :tags ["app-element-types"]

      (GET "/" []
           :middleware [require-authentication]
           :query [params IncludeHiddenParams]
           :summary AppElementsListingSummary
           :description AppElementsListingDocs
           (ok (apps/get-all-workflow-elements params)))

      (GET "/data-sources" []
           :middleware [require-authentication]
           :return DataSourceListing
           :summary AppElementsDataSourceListingSummary
           :description AppElementsDataSourceListingDocs
           (ok (apps/get-workflow-elements "data-sources" nil)))

      (GET "/file-formats" []
           :middleware [require-authentication]
           :return FileFormatListing
           :summary AppElementsFileFormatListingSummary
           :description AppElementsFileFormatListingDocs
           (ok (apps/get-workflow-elements "file-formats" nil)))

      (GET "/info-types" []
           :middleware [require-authentication]
           :return InfoTypeListing
           :summary AppElementsInfoTypeListingSummary
           :description AppElementsInfoTypeListingDocs
           (ok (apps/get-workflow-elements "info-types" nil)))

      (GET "/parameter-types" []
           :middleware [require-authentication]
           :query [params AppParameterTypeParams]
           :return ParameterTypeListing
           :summary AppElementsParameterTypeListingSummary
           :description AppElementsParameterTypeListingDocs
           (ok (apps/get-workflow-elements "parameter-types" params)))

      (GET "/rule-types" []
           :middleware [require-authentication]
           :return RuleTypeListing
           :summary AppElementsRuleTypeListingSummary
           :description AppElementsRuleTypeListingDocs
           (ok (apps/get-workflow-elements "rule-types" nil)))

      (GET "/tool-types" []
           :middleware [require-authentication]
           :return ToolTypeListing
           :summary AppElementsToolTypeListingSummary
           :description AppElementsToolTypeListingDocs
           (ok (apps/get-workflow-elements "tool-types" nil)))

      (GET "/value-types" []
           :middleware [require-authentication]
           :return ValueTypeListing
           :summary AppElementsValueTypeListingSummary
           :description AppElementsValueTypeListingDocs
           (ok (apps/get-workflow-elements "value-types" nil))))))
