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

(deftest depth-function
  (testing "Depth is defined by the highest bit, not the greatest distance"
    (let [original (utils/sha1 (.getBytes "aaa"))]
      (is (= 4 (depth (distance original (utils/sha1 (.getBytes "aaaa"))))))
      (is (= 0 (depth (distance original (utils/sha1 (.getBytes "aaaaa"))))))
      (is (= 0 (depth (distance original (utils/sha1 (.getBytes "aaaaaa"))))))
      (is (= 0 (depth (distance original (utils/sha1 (.getBytes "aaaaaaa"))))))
      (is (= 0 (depth (distance original (utils/sha1 (.getBytes "aaaaaaaa"))))))))
  (doseq [random-id (repeatedly 100 get-random-id!)]
    (testing "The depth of an id against itself is 160: " random-id
      (is (= 160 (depth (distance random-id random-id)))))
    (testing "The depth of an opposite is 0: " random-id
      (is (zero? (depth (distance random-id (byte-array (map bit-not random-id)))))))))

(deftest table-operations
  (let [table (generate-table (utils/sha1 (.getBytes "abc")))
        h-fn #(utils/sha1 (.getBytes %))]
    (testing "Insert a few options into the table"
      (is (-> table
              (insert (h-fn "abc") "1.2.3.4" 6881)
              (get-in [:router 0 :depth])
              (= 160)))
      (is (-> table
              (insert (h-fn "aaa") "1.2.3.4" 6881)
              (get-in [:router 0 :depth])
              (= 0)))
      (is (-> table
              (insert (h-fn "aaaaaaaa") "1.2.3.4" 6881)
              (get-in [:router 0 :depth])
              (= 3))))
    (testing "Querying"
      (let [_table (-> table
                       (insert (h-fn "abc") "1.2.3.4" 6881)
                       (insert (h-fn "aaa") "1.2.3.4" 6881)
                       (insert (h-fn "aaaaaaaa") "1.2.3.4" 6881))]
        (is (= 3 (count (:router _table))))
        (testing "Query bucket containing client's id"
          (is (= 3 (count (get-by-depth _table 0)))))
        (testing "Query bucket simulating outside client bucket"
          (is (= 1 (count (get-by-depth (update _table :splits inc) 0))))
          (is (= 2 (count (get-by-depth (update _table :splits inc) 1)))))))
    (testing "Randomly insert 100 nodes. It shouldn't be 1 or 100"
      (is (as-> (repeatedly 100 get-random-id!) v
            (doall (reduce #(insert %1 %2 "127.0.0.1" 6881) table v))
            (:router v)
            (count v)
            (< 1 v 100))))))
