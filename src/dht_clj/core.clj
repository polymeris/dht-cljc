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
