(ns terrain.routes.apps.admin.resource-presets
  (:require [common-swagger-api.schema :refer [context describe DELETE GET PATCH POST PUT]]
            [common-swagger-api.schema.apps :refer [ResourcePreset ResourcePresetList
                                                    ResourcePresetRequest ResourcePresetUpdateRequest]]
            [ring.util.http-response :refer [ok]]
            [terrain.clients.apps.raw :as apps]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config])
  (:import [java.util UUID]))

;; Declarations to avoid lint warnings for query and path parameter bindings.
(declare body preset-id)

(defn admin-resource-preset-routes
  []
  (optional-routes
   [#(and (config/admin-routes-enabled)
          (config/app-routes-enabled))]

   (context "/resource-presets" []
     :tags ["admin-resource-presets"]

     (GET "/" []
       :return ResourcePresetList
       :summary "List Resource Presets"
       :description "Lists all resource presets, including disabled ones."
       (ok (apps/admin-list-resource-presets)))

     (POST "/" []
       :body [body ResourcePresetRequest]
       :return ResourcePreset
       :summary "Create a Resource Preset"
       :description "Creates a new resource preset."
       (ok (apps/admin-create-resource-preset body)))

     (context "/:preset-id" []
       :path-params [preset-id :- (describe UUID "The Resource Preset's UUID")]

       (GET "/" []
         :return ResourcePreset
         :summary "Get a Resource Preset"
         :description "Gets a resource preset by its identifier."
         (ok (apps/admin-get-resource-preset preset-id)))

       (PATCH "/" []
         :body [body ResourcePresetUpdateRequest]
         :return ResourcePreset
         :summary "Update a Resource Preset"
         :description "Updates an existing resource preset."
         (ok (apps/admin-update-resource-preset preset-id body)))

       (DELETE "/" []
         :summary "Delete a Resource Preset"
         :description "Permanently deletes a resource preset."
         (ok (apps/admin-delete-resource-preset preset-id)))

       (PUT "/default" []
         :return ResourcePreset
         :summary "Set Default Resource Preset"
         :description "Sets this preset as the global default."
         (ok (apps/admin-set-default-resource-preset preset-id)))))))

(defn resource-preset-routes
  "Non-admin route for listing enabled resource presets."
  []
  (optional-routes
   [config/app-routes-enabled]

   (context "/resource-presets" []
     :tags ["resource-presets"]

     (GET "/" []
       :return ResourcePresetList
       :summary "List Resource Presets"
       :description "Lists enabled resource presets available for analysis launch."
       (ok (apps/list-resource-presets))))))
