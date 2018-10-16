(ns dht-cljc.infohash
  (:require
    [dht-cljc.utils :as utils]
    #?@(:cljs
         [[goog.crypt.Sha1 :as Sha1]
          [goog.crypt :as crypt]])))

(defn sha1
  "Returns a hex string encoded SHA1 of the data"
  [data]
  (let [jsha #?(:clj (java.security.MessageDigest/getInstance "SHA-1")
                :cljs (goog.crypt.Sha1.))]
    (.update jsha data)
    (vec (.digest jsha))))

(defn generate!
  "Generates a random SHA1 infohash useful for clients.
  Returns a byte-stream"
  []
  (->> (utils/now!)
       rand
       str
       utils/string->bytes
       sha1))

(defn distance
  "Get the XOR difference (abs (xor a b)) between two infohashes."
  [a b]
  (mapv bit-xor a b))

(def bit-position-lookup
  ^:private
  (reduce #(assoc %1 (bit-shift-left 1 %2) (- 7 %2)) {} (range 8)))

(defn- byte-depth [byte]
  (if (zero? byte)
    8
    (as-> byte b
      (bit-and 0xFF b)
      (bit-or b (unsigned-bit-shift-right b 1))
      (bit-or b (unsigned-bit-shift-right b 2))
      (bit-or b (unsigned-bit-shift-right b 4))
      (- b (unsigned-bit-shift-right b 1))
      (bit-position-lookup b))))

(defn depth
  "Given the XOR distance, count the left-most zero bits. This
  represents the point of divergence away from the measured infohash.
  Returns an integer."
  [distance-bytes]
  (if-let [actual-bytes (seq (drop-while zero? distance-bytes))]
    (+ (* 8 (- 20 (count actual-bytes)))
       (byte-depth (first actual-bytes)))
    160))

(comment
  (as-> -1 b
    (bit-and 0xFF b)
    (bit-or b (unsigned-bit-shift-right b 1))
    (bit-or b (unsigned-bit-shift-right b 2))
    (bit-or b (unsigned-bit-shift-right b 4))
    (bit-xor b (unsigned-bit-shift-right b 1))
    )
  (= 0 (depth (distance (sha1 (utils/string->bytes "aaa")) (map bit-not (sha1 (utils/string->bytes "aaa"))))))
  )
