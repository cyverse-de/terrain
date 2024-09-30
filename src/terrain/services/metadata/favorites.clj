(ns terrain.services.metadata.favorites
  (:require [cheshire.core :as json]
            [terrain.auth.user-attributes :as user]
            [terrain.clients.data-info :as data]
            [terrain.clients.metadata.raw :as metadata])
  (:import [java.util UUID]))


(defn- format-favorites
  [favs]
  (letfn [(mk-fav [entry] (assoc entry :isFavorite true))]
    (assoc favs
      :files   (map mk-fav (:files favs))
      :folders (map mk-fav (:folders favs)))))


(defn- user-col->api-col
  [col]
  (case col
    :name         :base-name
    :id           :full-path
    :lastmodified :modify-ts
    :datecreated  :create-ts
    :size         :data-size
    :base-name))

(defn- user-order->api-order
  [order]
  (if order
    (case order
      :asc  :asc
      :desc :desc
      :asc)
    :asc))

(defn add-favorite
  "This function marks a given data item as a favorite of the authenticated user.

   Parameters:
     data-id - This is the `data-id` from the request.  It should be the UUID of the data item
               being marked."
  [data-id]
  (let [user (:shortUsername user/current-user)]
    (data/validate-uuid-accessible user data-id)
    (metadata/add-favorite data-id (data/resolve-data-type data-id))))


(defn remove-favorite
  "This function unmarks a given data item as a favorite of the authenticated user.

   Parameters:
     data-id - This is the `data-id` from the request.  It should be the UUID of the data item
               being unmarked."
  [data-id]
  (metadata/remove-favorite data-id))


(defn remove-selected-favorites
  "Unmarks all of the users favorites with the given entity type.

   Parameters:
     entity-type: a case-insensitive-string containing {file|folder|any}"
  [{:keys [entity-type]}]
  (metadata/remove-selected-favorites (name (or entity-type :any))))


(defn- ids-txt->uuids-set
  [ids-txt]
  (->> ids-txt (map #(UUID/fromString %)) set))

(defn- parse-filesystem-ids
  [json-txt]
  (-> json-txt (json/parse-string true) :filesystem))

(defn- extract-favorite-uuids-set
  [response]
  (-> response :body slurp parse-filesystem-ids ids-txt->uuids-set))


(defn list-favorite-data-with-stat
  "Returns a listing of a user's favorite data, including stat information about it. This endpoint
   is intended to help with paginating.

   Parameters:
     sort-col    - This is the value of the `sort-col` query parameter. It should be a case-
                   insensitive string containing one of the following:
                   DATECREATED|ID|LASTMODIFIED|NAME|SIZE
     sort-dir    - This is the value of the `sort-dir` query parameter. It should be a case-
                   insensitive string containing one of the following: ASC|DESC
     limit       - This is the value of the `limit` query parameter. It should contain a positive
                   number.
     offset      - This is the value of the `offset` query parameter. It should contain a non-
                   negative number.
     entity-type - This is the value of the `entity-type` query parameter. It should be a case-
                   insensitive string containing one of the following: ANY|FILE|FOLDER. If it is
                   nil, ANY will be used.
     info-type   - This is the value(s) of the `info-type` query parameter(s). It may be nil,
                   meaning return all info types, a string containing a single info type, or a
                   sequence containing a set of info types."
  [{:keys [sort-col sort-dir limit offset entity-type info-type]}]
  (let [user        (:shortUsername user/current-user)
        col         (user-col->api-col sort-col)
        ord         (user-order->api-order sort-dir)
        entity-type (or entity-type :any)
        uuids       (extract-favorite-uuids-set (metadata/list-favorites (name entity-type)))]
    (->> (data/stats-by-uuids-paged user col ord limit offset uuids info-type)
         format-favorites
         (hash-map :filesystem))))


(defn filter-favorites
  "Forwards a list of UUIDs for data items to the metadata service favorites filter
   endpoint, parsing its response and returning a set of the returned UUIDs.

   Parameters:
     data-ids - A list of UUIDs to filter."
  [data-ids]
  (extract-favorite-uuids-set (metadata/filter-favorites data-ids)))

(defn filter-accessible-favorites
  "Given a list of UUIDs for data items, it filters the list, returning only the UUIDS that
   are accessible and marked as favorite by the authenticated user.

   Parameters:
     body - This is the request body. It should contain a JSON document containing a field
            `filesystem` containing an array of UUIDs."
  [{data-ids :filesystem}]
  (let [user (:shortUsername user/current-user)]
    (->> (filter-favorites (set data-ids))
         (filter (partial data/uuid-accessible? user))
         (hash-map :filesystem))))
