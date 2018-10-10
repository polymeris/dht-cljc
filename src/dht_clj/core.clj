(ns dht-clj.core
  (:require [dht-clj.utils :as utils]))

(defn get-random-id!
  "Generates a random SHA1 ID useful for clients.
  Returns a byte-stream"
  []
  (utils/sha1 (.getBytes (str (rand (System/currentTimeMillis))))))

(defn distance
  "Get the XOR difference between two infohashes"
  [a b]
  (bigint (.xor (BigInteger. 1 a) (BigInteger. 1 b))))
