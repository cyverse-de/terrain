(ns terrain.services.bags
  (:use [terrain.util.service])
  (:require [terrain.clients.bags :as bags]))

(defn has-bags
  [username]
  (bags/has-bags username))

(defn get-bags
  [username]
  (bags/get-bags username))

(defn get-bag
  [username bag-id]
  (bags/get-bag username bag-id))

(defn add-bag
  [username contents]
  (bags/add-bag username contents))

(defn update-bag
  [username bag-id contents]
  (bags/update-bag username bag-id contents))

(defn delete-all-bags
  [username]
  (bags/delete-all-bags username))

(defn delete-bag
  [username bag-id]
  (bags/delete-bag username bag-id))

(defn get-default-bag
  [username]
  (bags/get-default-bag username))

(defn update-default-bag
  [username contents]
  (bags/update-default-bag username contents))

(defn delete-default-bag
  [username]
  (bags/delete-default-bag username))
