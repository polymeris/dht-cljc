(ns dht-clj.core-test
  (:require
    [dht-clj.core :refer :all]
    [dht-clj.utils :as utils]
    [clojure.test :refer :all])
  (:import java.math.BigInteger))

(deftest distance-function
  (let [huge (byte-array (repeat 20 255))
        zero (byte-array (repeat 20 0))]
    (testing "Maximum distance is inverse, maximum 20 bytes of 255"
      (is (= (BigInteger. 1 huge) (distance huge zero))))
    (doseq [random-id (repeatedly 100 get-random-id!)]
      (testing "Maximum distance is inverse, maximum 20 bytes of 255: " random-id
        (is (= (BigInteger. 1 huge) (distance random-id (byte-array (map bit-not random-id))))))
      (testing "Distance of itself is 0: " random-id
        (is (zero? (distance random-id random-id)))))))
