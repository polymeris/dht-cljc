(ns dht-cljc.infohash-test
  (:require
    #?(:clj  [clojure.test :refer [is testing deftest]]
       :cljs [cljs.test :refer [is testing deftest]])
    [dht-cljc.infohash :as infohash]
    [dht-cljc.utils :as utils]))

(deftest sha1-hash
  (doseq [[input check]
          [["test123" "0x7288edd0fc3ffcbe93a0cf06e3568e28521687bc"]
           ["abc" "0xa9993e364706816aba3e25717850c26c9cd0d89d"]
           ["0x80" "0x70070b762760cd34e9b8180a5569dcef7e1942b3"]
           ["hello" "0xaaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d"]
           ["aaa" "0x7e240de74fb1ed08fa08d38063f6a6a91462a815"]]]
    (testing (str "sha1(" input "): " check)
      (let [sha (infohash/sha1 (utils/string->bytes input))]
        (is (= check (utils/bytes->hex sha)))
        (is (= (utils/bytes->hex (utils/hex->bytes check)) (utils/bytes->hex sha)))
        (is (= check (-> sha
                         utils/bytes->hex
                         utils/hex->bytes
                         utils/bytes->hex
                         utils/hex->bytes
                         utils/bytes->hex)))))))

(deftest distance-function
  (let [huge (repeat 20 -1)
        zero (repeat 20 0)]
    (testing "Maximum distance is inverse, maximum 20 bytes of 255"
      (is (= huge (infohash/distance huge zero))))
    (doseq [random-infohash (repeatedly 100 infohash/generate!)]
      (testing "Maximum distance is inverse, maximum 20 bytes of -1: " random-infohash
        (is (= huge (infohash/distance random-infohash (map bit-not random-infohash)))))
      (testing "Distance of itself is 0: " random-infohash
        (is (every? zero? (infohash/distance random-infohash random-infohash)))))))

(deftest depth-function
  (testing "Depth is defined by the highest bit, not the greatest distance"
    (let [original (infohash/sha1 (utils/string->bytes "aaa"))]
      (doseq [[input check] {"aaaa" 4 "aaaaa" 0 "aaaaaa" 0 "aaaaaaa" 0 "aaaaaaaa" 0}]
        (is (= check (infohash/depth (infohash/distance original (infohash/sha1 (utils/string->bytes input)))))))))
  (doseq [random-infohash (repeatedly 100 infohash/generate!)]
    (testing "The depth of an infohash against itself is 160: " random-infohash
      (is (= 160 (infohash/depth (infohash/distance random-infohash random-infohash)))))
    (testing "The depth of an opposite is 0: " random-infohash
      (is (zero? (infohash/depth (infohash/distance random-infohash (map bit-not random-infohash))))))))
