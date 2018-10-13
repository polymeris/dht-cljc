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
    (testing "Randomly insert 100 nodes. It shouldn't be 1 or 100"
      (is (as-> (repeatedly 100 infohash/generate!) v
            (doall (reduce #(insert %1 0 %2 "127.0.0.1" 6881) table v))
            (:router v)
            (count v)
            (< 1 v 100))))))

(deftest query-operations
  (let [table
        {:router [{:infohash "0x7e240de74fb1ed08fa08d38063f6a6a91462a815"
                   :depth 0
                   :ip "1.2.3.4"
                   :port 6881
                   :last-seen 0}
                  {:infohash "0x70c881d4a26984ddce795f6f71817c9cf4480e79"
                   :depth 0
                   :ip "1.2.3.4"
                   :port 6881
                   :last-seen 0}
                  {:infohash "0xdf51e37c269aa94d38f93e537bf6e2020b21406c"
                   :depth 1
                   :ip "1.2.3.4"
                   :port 6881
                   :last-seen 1200000}
                  {:infohash "0xf7a9e24777ec23212c54d7a350bc5bea5477fdbb"
                   :depth 1
                   :ip "1.2.3.4"
                   :port 6881
                   :last-seen 1200000}
                  {:infohash "0xe93b4e3c464ffd51732fbd6ded717e9efda28aad"
                   :depth 1
                   :ip "1.2.3.4"
                   :port 6881
                   :last-seen 2400000}
                  {:infohash "0xb480c074d6b75947c02681f31c90c668c46bf6b8"
                   :depth 3
                   :ip "1.2.3.4"
                   :port 6881
                   :last-seen (fifteen-minutes-overdue!)}]
         :client-infohash "0xa9993e364706816aba3e25717850c26c9cd0d89d"
         :splits 0
         :max-bucket-count 8}]
    (testing "Query bucket containing client's infohash"
      (is (= 6 (count (get-by-depth table 0)))))
    (testing "Query bucket simulating outside client bucket"
      (is (= 2 (count (get-by-depth (update table :splits inc) 0))))
      (is (= 4 (count (get-by-depth (update table :splits inc) 1))))
      (is (= 4 (count (get-by-depth (update table :splits inc) 2)))))
    (testing "Query nodes by expiry"
      (is (= 0 (count (get-by-overdue table 0))))
      (is (= 2 (count (get-by-overdue table 1))))
      (is (= 2 (count (get-by-overdue table 1200000))))
      (is (= 4 (count (get-by-overdue table 2400000))))
      (is (= 5 (count (get-by-overdue table 2400001))))
      (is (= 6 (count (get-by-overdue table (fifteen-minutes-overdue!))))))))
