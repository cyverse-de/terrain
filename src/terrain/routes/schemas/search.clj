(ns terrain.routes.schemas.search
  (:require [common-swagger-api.schema :refer [describe]]
            [schema.core :as s]))

(def AnyDocs "At least one of the following must be true")
(def AllDocs "All of the following must be true")
(def NoneDocs "All of the following must not be true")
(def ClauseTypeEnum (s/enum "created" "label" "metadata" "modified" "owner" "path" "size" "tag" "permissions"))
(def SearchSortFields (s/enum "id" "label" "path" "creator" "dateCreated" "dateModified" "fileSize"))
(def SearchDocLink "[/filesystem/search-documentation](#/filesystem/get_terrain_secured_filesystem_search_documentation)")

(s/defschema SearchSortParams
  {:field (describe SearchSortFields "The field to sort by")
   :order (s/enum "ascending" "descending")})

(s/defschema Clause
  {(s/optional-key :any)  (describe [(s/recursive #'Clause)] AnyDocs)
   (s/optional-key :all)  (describe [(s/recursive #'Clause)] AllDocs)
   (s/optional-key :none) (describe [(s/recursive #'Clause)] NoneDocs)
   (s/optional-key :type) (describe ClauseTypeEnum (str
                                    "A string containing the clause type. If the `type` key is used,
                                    the `args` key should also be used. Documentation on the clause types
                                    can be found via the " SearchDocLink " endpoint"))
   (s/optional-key :args) (describe s/Any (str
                                    "A set of arguments that further refine the clause terms. The `args`
                                    key should only be used alongside a corresponding `type` key.
                                    Documentation on the corresponding args for each clause can be found
                                    via the " SearchDocLink " endpoint"))})

(s/defschema Query
  {(s/optional-key :any)  (describe [Clause] AnyDocs)
   (s/optional-key :all)  (describe [Clause] AllDocs)
   (s/optional-key :none) (describe [Clause] NoneDocs)})

(s/defschema SearchRequest
  {(s/optional-key :query) (describe Query "A querydsl-compatible JSON query inside a JSON object. Mutually exclusive with 'scroll_id'.")
   (s/optional-key :size) (describe (s/constrained Long pos? 'positive-integer?)
                                    "Limits the response to X number of results. Default is 10")
   (s/optional-key :from) (describe (s/constrained Long (partial <= 0) 'non-negative-integer?)
                                    "Skips the first X number of results. Default is 0")
   (s/optional-key :sort) (describe [SearchSortParams]
                                    "Sorts the results based on a list of criteria")
   (s/optional-key :scroll) (describe String
                                    "Sets a scroll timeout for this search. Required if scroll_id is present.")
   (s/optional-key :scroll_id) (describe String
                                    "Gets the next set of results for a given scroll ID. Mutually exclusive with 'query'.")})
