(ns dht-clj.core
  (:require [dht-clj.infohash :as infohash])
  (:import java.math.BigInteger))

(def
  ^{:doc "A list of BitTorrent DHT bootstrap nodes."}
  bootstrap-nodes
  [["router.utorrent.com" 6881]
   ["dht.transmissionbt.com" 6881]
   ["dht.aelitis.com" 6881]])

(defn generate-table
  "Generates a blank routing table ready to be filled with addresses

    :router - The vector of clients. See 'insert for client format
    :client-infohash - The infohash for the currently running node
    :splits - The depth of the client-infohash bucket
    :max-bucket-count - The maximum good nodes in a bucket before a split.
                        Default 8 per the spec"
  [client-infohash & [max-bucket-count]]
  {:router []
   :client-infohash client-infohash
   :splits 0
   :max-bucket-count (or max-bucket-count 8)})

(defn get-by-depth
  "Gets the list of clients at a specific depth. If depth is absent or
  >= 'splits, the client's bucket is returned."
  ([{:keys [router splits] :as table} d]
   (let [f (if (< d splits)
             (partial = d)
             (partial <= splits))]
     (filter #(-> % :depth f) router)))
  ([table]
   (get-by-depth table 160)))

(defn insert
  "Inserts the given node into the router, respecting full and dividing buckets.
  Refer to BEP_0005 for more information.

    table - The table whose router we want to update
    last-seen - (optional) timestamp since this node was last seen
    remote-infohash - The infohash of the node we want to insert, in bytes
    ip, port - The IP and port of the node

  Returns the updated table"
  ([{:keys [client-infohash max-bucket-count] :as table} last-seen ^bytes remote-infohash ip port]
   (loop [{:keys [splits] :as _table} table
          node-depth (infohash/depth (infohash/distance remote-infohash client-infohash))
          node {:infohash remote-infohash :depth node-depth :ip ip :port port :last-seen last-seen}]
     (let [num-nodes-in-bucket (count (get-by-depth _table node-depth))
           is-client-bucket? (>= node-depth splits)]
       (if (< num-nodes-in-bucket max-bucket-count)
         (update _table :router conj node)
         (if-not is-client-bucket?
           _table
           (recur (update _table :splits inc) node-depth node)))))))

(defn insert!
  "Same as 'insert, but fills out the 'last-seen param with (System/currentTimeMillis)"
  [table ^bytes remote-infohash ip port]
  (insert table (System/currentTimeMillis) remote-infohash ip port))
