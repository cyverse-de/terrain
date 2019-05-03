(ns terrain.routes.schemas.search
  (:use [common-swagger-api.schema :only [describe]])
  (:require [schema.core :as s]))

(def ConditionalArgs (s/enum :any :all :none))
(def ClauseTypeEnum (s/enum "created" "label" "metadata" "modified" "owner" "path" "size" "tag" "permissions"))
(def SearchSortFields (s/enum "id" "label" "path" "creator" "dateCreated" "dateModified" "fileSize"))
(def SearchDocLink "[/filesystem/search-documentation](#/filesystem/get_terrain_secured_filesystem_search_documentation)")

(s/defschema SearchSortParams
  {:field (describe SearchSortFields "The field to sort by")
   :order (s/enum "ascending" "descending")})

(s/defschema Clause
  {(s/optional-key :any)  (describe [(s/recursive #'Clause)] "At least one of the following must be true")
   (s/optional-key :all)  (describe [(s/recursive #'Clause)] "All of the following must be true")
   (s/optional-key :none) (describe [(s/recursive #'Clause)] "All of the following must not be true")
   (s/optional-key :type) (describe ClauseTypeEnum (str
                                    "A string containing the clause type. If the `type` key is used,
                                    the `args` key should also be used. Documentation on the clause types
                                    can be found via the " SearchDocLink " endpoint"))
   (s/optional-key :args) (describe s/Any (str
                                    "A set of arguments that further refine the clause terms. The `args`
                                    key should only be used alongside a corresponding `type` key.
                                    Documentation on the corresponding args for each clause can be found
                                    via the " SearchDocLink " endpoint"))})

(s/defschema ConditionalClause
  {ConditionalArgs [(s/recursive #'Clause)]})

(s/defschema ConditionalClauseDocs
  {:any-all-or-none
   (describe [Clause] "A conditional clause, specifically 'any', 'all', or 'none'")})

(s/defschema SearchQuery
  {:query ConditionalClause
   (s/optional-key :size) (s/both Long (s/pred pos? 'positive-integer?))
   (s/optional-key :from) (s/both Long (s/pred (partial <= 0) 'non-negative-integer?))
   (s/optional-key :sort) [SearchSortParams]})

(s/defschema SearchQueryDocs
  {:query                 (describe ConditionalClauseDocs
                                    "A querydsl-compatible JSON query inside a JSON object")
   (s/optional-key :size) (describe (s/both Long (s/pred pos? 'positive-integer?))
                                    "Limits the response to X number of results. Default is 10")
   (s/optional-key :from) (describe (s/both Long (s/pred (partial <= 0) 'non-negative-integer?))
                                    "Skips the first X number of results. Default is 0")
   (s/optional-key :sort) (describe [SearchSortParams]
                                    "Sorts the results based on a list of criteria")})