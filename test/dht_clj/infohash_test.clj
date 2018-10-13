(ns dht-clj.infohash-test
  (:require
    [dht-clj.infohash :refer :all]
    [clojure.test :refer :all]))

(deftest sha1-hash
  (doseq [[input check]
          [["test123" "0x7288edd0fc3ffcbe93a0cf06e3568e28521687bc"]
           ["abc" "0xa9993e364706816aba3e25717850c26c9cd0d89d"]
           ["0x80" "0x70070b762760cd34e9b8180a5569dcef7e1942b3"]
           ["hello" "0xaaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d"]
           ["aaa" "0x7e240de74fb1ed08fa08d38063f6a6a91462a815"]]]
    (testing (str "sha1(" input "): " check)
      (let [sha (sha1 (.getBytes input))]
        (is (= check (bytes->hex sha)))
        (is (= (bytes->hex (hex->bytes check)) (bytes->hex sha)))
        (is (= check (-> sha
                         bytes->hex
                         hex->bytes
                         bytes->hex
                         hex->bytes
                         bytes->hex)))))))

(deftest distance-function
  (let [huge (byte-array (repeat 20 255))
        zero (byte-array (repeat 20 0))]
    (testing "Maximum distance is inverse, maximum 20 bytes of 255"
      (is (= (BigInteger. 1 huge) (distance huge zero))))
    (doseq [random-infohash (repeatedly 100 generate!)]
      (testing "Maximum distance is inverse, maximum 20 bytes of 255: " random-infohash
        (is (= (BigInteger. 1 huge) (distance random-infohash (byte-array (map bit-not random-infohash))))))
      (testing "Distance of itself is 0: " random-infohash
        (is (zero? (distance random-infohash random-infohash)))))))

(deftest depth-function
  (testing "Depth is defined by the highest bit, not the greatest distance"
    (let [original (sha1 (.getBytes "aaa"))]
      (is (= 4 (depth (distance original (sha1 (.getBytes "aaaa"))))))
      (is (= 0 (depth (distance original (sha1 (.getBytes "aaaaa"))))))
      (is (= 0 (depth (distance original (sha1 (.getBytes "aaaaaa"))))))
      (is (= 0 (depth (distance original (sha1 (.getBytes "aaaaaaa"))))))
      (is (= 0 (depth (distance original (sha1 (.getBytes "aaaaaaaa"))))))))
  (doseq [random-infohash (repeatedly 100 generate!)]
    (testing "The depth of an infohash against itself is 160: " random-infohash
      (is (= 160 (depth (distance random-infohash random-infohash)))))
    (testing "The depth of an opposite is 0: " random-infohash
      (is (zero? (depth (distance random-infohash (byte-array (map bit-not random-infohash)))))))))
