(ns dht-cljc.runner
  (:require
    [cljs.test :as t]
    [dht-cljc.infohash-test]
    [dht-cljc.core-test]))

(enable-console-print!)

(t/run-tests 'dht-cljc.infohash-test
             'dht-cljc.core-test)
