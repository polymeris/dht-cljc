(ns dht-cljc.utils
  (:require
    [clojure.string :as s]
    #?(:cljs
       [goog.crypt :as crypt])))

(defn now!
  "Get the Unix epoch in milliseconds"
  []
  #?(:clj (System/currentTimeMillis)
     :cljs (.getTime (js/Date.))))

(defn string->bytes
  "Convert a given string to a byte vector"
  [string]
  #?(:clj (.getBytes string)
     :cljs (crypt/stringToUtf8ByteArray string)))

(defn bytes->hex
  "Converts bytes into a hex string"
  [bytes]
  (->> bytes
       (mapcat #(vector (bit-shift-right (bit-and % 0xF0) 4) (bit-and % 0x0F)))
       (map {0 \0 7 \7 1 \1 4 \4 15 \f 13 \d 6 \6 3 \3 12 \c 2 \2 11 \b 9 \9 5 \5 14 \e 10 \a 8 \8})
       (apply str "0x")))

(defn hex->bytes
  "Converts hex string to byte-array"
  [hex]
  (->> (if (s/starts-with? hex "0x") (subs hex 2) hex)
       (map {\a 10 \b 11 \c 12 \d 13 \e 14 \f 15 \0 0 \1 1 \2 2 \3 3 \4 4 \5 5 \6 6 \7 7 \8 8 \9 9})
       (partition 2)
       (map (fn [[h l]] (+ (bit-shift-left h 4) l)))))
