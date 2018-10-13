(ns dht-clj.infohash
  (:require [clojure.string :as s])
  (:import java.security.MessageDigest
           java.math.BigInteger))

(defn bytes->hex
  "Converts bytes into a hex string"
  [^bytes bytes]
  (->> bytes
       (mapcat #(vector (bit-shift-right (bit-and % 0xF0) 4) (bit-and % 0x0F)))
       (map {0 \0 7 \7 1 \1 4 \4 15 \f 13 \d 6 \6 3 \3 12 \c 2 \2 11 \b 9 \9 5 \5 14 \e 10 \a 8 \8})
       (apply str "0x")))

(defn hex->bytes
  "Converts hex string to byte-array"
  [^String hex]
  (->> (if (s/starts-with? hex "0x") (subs hex 2) hex)
       (map {\a 10 \b 11 \c 12 \d 13 \e 14 \f 15 \0 0 \1 1 \2 2 \3 3 \4 4 \5 5 \6 6 \7 7 \8 8 \9 9})
       (partition 2)
       (map (fn [[h l]] (+ (bit-shift-left h 4) l)))
       byte-array))

(defn sha1
  "Returns a hex string encoded SHA1 of the data"
  [^bytes data]
  (let [jsha (MessageDigest/getInstance "SHA-1")]
    (.update jsha data)
    (.digest jsha)))

(defn generate!
  "Generates a random SHA1 infohash useful for clients.
  Returns a byte-stream"
  []
  (sha1 (.getBytes (str (rand (System/currentTimeMillis))))))

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

(comment
  (bytes->hex (hex->bytes (bytes->hex (sha1 (.getBytes "aaa")))))
  )