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

(defn get-by-overdue
  "Find all nodes in a table that are past the given 'expired-before param,
  in milliseconds"
  [{:keys [router] :as table} expired-before]
  (filter #(-> % :last-seen (< expired-before)) router))

(defn fifteen-minutes-overdue!
  "Gets the time, in milliseconds, 15 minutes ago. This is the default used
  in the BEP_0005 spec for overdue nodes. Useful for 'get-by-overdue"
  []
  (- (System/currentTimeMillis) 900000))

(defn get-nearest-peers
  "Finds the nearest peers to a given infohash in the routing table. The peer
  list is then sorted by distance, ascending, from the infohash."
  [{:keys [client-infohash] :as table} infohash]
  (->> (infohash/distance infohash client-infohash)
       infohash/depth
       (get-by-depth table)
       (sort-by #(-> % :infohash (infohash/distance infohash)))))

(defn refresh
  "Refreshes the table's nodes with new :last-seen timestamps.
  Accepts a variadic number of responses. Each response is a tuple of the
  infohash and the new timestamp, or [infohash timestamp]"
  [{:keys [router] :as table} & responses]
  (let [response-map (reduce
                       (fn [m [i t]] (assoc m (vec i) t))
                       {}
                       responses)
        response-set (set (keys response-map))]
    (->> router
         (mapv
           (fn [{:keys [infohash] :as m}]
             (if-let [vec-hash (response-set (vec infohash))]
               (assoc m :last-seen (response-map vec-hash))
               m)))
         (assoc table :router))))

(defn prune
  "Removes listed infohashes from the router, combining buckets as needed"
  [{:keys [router max-bucket-count] :as table} & infohashes]
  (let [infohash-vec-set (set (map vec infohashes))
        cleared-router (remove (comp infohash-vec-set vec :infohash) router)]
    (loop [_table (assoc table :router cleared-router)]
      (if (zero? (:splits _table))
        _table
        (if (< max-bucket-count
               (->> _table
                    :splits
                    dec
                    (get-by-depth _table)
                    count))
          _table
          (recur (update _table :splits dec)))))))

(defn insert
  "Inserts the given node into the router, respecting full and dividing buckets.
  Refer to BEP_0005 for more information.

    table - The table whose router we want to update
    last-seen - (optional) timestamp since this node was last seen
    remote-infohash - The infohash of the node we want to insert, in bytes
    ip, port - The IP and port of the node

  Returns the updated table"
  [table last-seen remote-infohash ip port]
  (let [{:keys [client-infohash max-bucket-count]} table
        node-depth (-> remote-infohash
                       (infohash/distance client-infohash)
                       infohash/depth)
        node {:infohash remote-infohash
              :depth node-depth
              :ip ip
              :port port
              :last-seen last-seen}]
    (loop [{:keys [splits] :as _table} table]
      (let [num-nodes-in-bucket (count (get-by-depth _table node-depth))
            is-client-bucket? (>= node-depth splits)]
        (if (< num-nodes-in-bucket max-bucket-count)
          (update _table :router conj node)
          (if-not is-client-bucket?
            _table
            (recur (update _table :splits inc))))))))
