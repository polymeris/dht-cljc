(ns dht-clj.utils-test
  (:require
    [dht-clj.utils :refer :all]
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
