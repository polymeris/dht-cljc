(ns dht-cljc.infohash-test
  (:require
    [dht-cljc.infohash :refer :all]
    [dht-cljc.utils :as utils]
    [clojure.test :refer :all]))

(deftest sha1-hash
  (doseq [[input check]
          [["test123" "0x7288edd0fc3ffcbe93a0cf06e3568e28521687bc"]
           ["abc" "0xa9993e364706816aba3e25717850c26c9cd0d89d"]
           ["0x80" "0x70070b762760cd34e9b8180a5569dcef7e1942b3"]
           ["hello" "0xaaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d"]
           ["aaa" "0x7e240de74fb1ed08fa08d38063f6a6a91462a815"]]]
    (testing (str "sha1(" input "): " check)
      (let [sha (sha1 (utils/string->bytes input))]
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
      (is (= huge (distance huge zero))))
    (doseq [random-infohash (repeatedly 100 generate!)]
      (testing "Maximum distance is inverse, maximum 20 bytes of -1: " random-infohash
        (is (= huge (distance random-infohash (map bit-not random-infohash)))))
      (testing "Distance of itself is 0: " random-infohash
        (is (every? zero? (distance random-infohash random-infohash)))))))

(deftest depth-function
  (testing "Depth is defined by the highest bit, not the greatest distance"
    (let [original (sha1 (utils/string->bytes "aaa"))]
      (is (= 4 (depth (distance original (sha1 (utils/string->bytes "aaaa"))))))
      (is (= 0 (depth (distance original (sha1 (utils/string->bytes "aaaaa"))))))
      (is (= 0 (depth (distance original (sha1 (utils/string->bytes "aaaaaa"))))))
      (is (= 0 (depth (distance original (sha1 (utils/string->bytes "aaaaaaa"))))))
      (is (= 0 (depth (distance original (sha1 (utils/string->bytes "aaaaaaaa"))))))))
  (doseq [random-infohash (repeatedly 100 generate!)]
    (testing "The depth of an infohash against itself is 160: " random-infohash
      (is (= 160 (depth (distance random-infohash random-infohash)))))
    (testing "The depth of an opposite is 0: " random-infohash
      (is (zero? (depth (distance random-infohash (map bit-not random-infohash))))))))
