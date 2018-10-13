(ns dht-clj.core
  (:require [dht-clj.utils :as utils])
  (:import java.math.BigInteger))

(defn get-random-id!
  "Generates a random SHA1 ID useful for clients.
  Returns a byte-stream"
  []
  (utils/sha1 (.getBytes (str (rand (System/currentTimeMillis))))))

(defn distance
  "Get the XOR difference (abs (xor a b)) between two infohashes.
  Returns a BigInteger of the distance."
  [^bytes a ^bytes b]
  (.xor (BigInteger. 1 a)
        (BigInteger. 1 b)))

(defn depth
  "Given a BigInteger of the XOR distance, count the left-most zero bits. This
  represents the point of divergence away from the measured infohash.
  Returns an integer."
  [^BigInteger dist]
  (- 160 (.bitLength dist)))

(defn generate-table
  "Generates a blank routing table ready to be filled with addresses

    :router - The vector of clients. See 'insert for client format
    :client-id - The ID for the currently running node
    :splits - The depth of the client-id bucket
    :max-bucket-count - The maximum good nodes in a bucket before a split.
                        Default 8 per the spec"
  [client-id & [max-bucket-count]]
  {:router []
   :client-id client-id
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
    remote-id - The ID of the node we want to insert
    ip, port - The IP and port of the node

  Returns the updated table"
  ([{:keys [router client-id max-bucket-count] :as table} last-seen remote-id ip port]
   (loop [{:keys [splits] :as _table} table
          node-depth (depth (distance remote-id client-id))
          node {:id remote-id :depth node-depth :ip ip :port port :last-seen last-seen}]
     (let [num-nodes-in-bucket (count (get-by-depth _table node-depth))
           is-client-bucket? (>= node-depth splits)]
       (if (< num-nodes-in-bucket max-bucket-count)
         (update _table :router conj node)
         (if-not is-client-bucket?
           _table
           (recur (update _table :splits inc) node-depth node))))))
  ([table remote-id ip port]
   (insert table (System/currentTimeMillis) remote-id ip port)))

(comment
  (time
    (-> (generate-table (utils/sha1 (.getBytes "abc")))
;       (insert (utils/sha1 (.getBytes "abc")) "1.2.3.4" 6881)
;       (insert (utils/sha1 (.getBytes "aaa")) "1.2.3.4" 6881)
;       (insert (utils/sha1 (.getBytes "aaaa")) "1.2.3.4" 6881)
;       (insert (utils/sha1 (.getBytes "aaaaa")) "1.2.3.4" 6881)
;       (insert (utils/sha1 (.getBytes "aaaaaa")) "1.2.3.4" 6881)
;       (insert (utils/sha1 (.getBytes "aaaaaaa")) "1.2.3.4" 6881)
;       (insert (utils/sha1 (.getBytes "ab")) "5.6.7.8" 6881)
;       (insert (utils/sha1 (.getBytes "ccc")) "127.0.0.1" 6881)
;       (insert (get-random-id!) "127.0.0.1" 6881)
;       (insert (get-random-id!) "127.0.0.1" 6881)

;       :router
;       (->> (reduce (fn [coll {:keys [depth]}] (update coll depth (fnil inc 0))) {}))
;       (->> (map :depth))

;       (get-by-depth)

;       (get-client-bucket)

;       clojure.pprint/pprint
;       count
        ))
  )
