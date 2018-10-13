(ns dht-clj.core-test
  (:refer-clojure :exclude [hash])
  (:require
    [dht-clj.core :refer :all]
    [dht-clj.infohash :as infohash]
    [clojure.test :refer :all])
  (:import java.math.BigInteger))

(def hash #(infohash/sha1 (.getBytes %)))

(deftest insert-operations
  (let [table (generate-table (hash "abc"))]
    (testing "Insert a few options into the table"
      (is (-> table
              (insert! (hash "abc") "1.2.3.4" 6881)
              (get-in [:router 0 :depth])
              (= 160)))
      (is (-> table
              (insert! (hash "aaa") "1.2.3.4" 6881)
              (get-in [:router 0 :depth])
              (= 0)))
      (is (-> table
              (insert! (hash "aaaaaaaa") "1.2.3.4" 6881)
              (get-in [:router 0 :depth])
              (= 3))))
    (testing "Querying"
      (let [_table (-> table
                       (insert! (h-fn "abc") "1.2.3.4" 6881)
                       (insert! (h-fn "aaa") "1.2.3.4" 6881)
                       (insert! (h-fn "aaaaaaaa") "1.2.3.4" 6881))]
        (is (= 3 (count (:router _table))))
        (testing "Query bucket containing client's infohash"
          (is (= 3 (count (get-by-depth _table 0)))))
        (testing "Query bucket simulating outside client bucket"
          (is (= 1 (count (get-by-depth (update _table :splits inc) 0))))
          (is (= 2 (count (get-by-depth (update _table :splits inc) 1)))))))
    (testing "Randomly insert 100 nodes. It shouldn't be 1 or 100"
      (is (as-> (repeatedly 100 infohash/generate!) v
            (doall (reduce #(insert! %1 %2 "127.0.0.1" 6881) table v))
            (:router v)
            (count v)
            (< 1 v 100))))))
