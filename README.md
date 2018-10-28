# dht-cljc

> Lord Helmet: Found anything yet?  
> Mook 1: Nothing yet, sir!  
> Lord Helmet: How 'bout you?  
> Mook 2: Not a thing, sir!  
> Lord Helmet: How 'bout you guys?  
> Mook 3: We ain't found shit!  
> - Spaceballs

A Clojure(script) BitTorrent DHT plumbing library.

`[com.jamesleonis/dht-cljc "0.1.0"]`

[![Clojars Project](https://img.shields.io/clojars/v/com.jamesleonis/dht-cljc.svg)](https://clojars.org/com.jamesleonis/dht-cljc)

[![cljdoc badge](https://cljdoc.org/badge/com.jamesleonis/dht-cljc)](https://cljdoc.xyz/d/com.jamesleonis/dht-cljc/CURRENT)

## Why?

The BitTorrent DHT is an important component of the torrent ecosystem, and a growing number of additional technologies are beginning to use the network as well. The aim is to make this module small and embeddable in different Clojure applications. This allows the consuming app to define and control the network transport and the library to expand to new Clojure ecosystems.

## Quickstart

```
(ns my-cool-project.core
  (:require
    [dht-cljc.core :as dht]
    [dht-cljc.infohash :as infohash]
    [dht-cljc.utils :as utils]))

; First you need a table. Here's an atom as an example.
(def table (atom (dht/generate-table (infohash/generate!))))

; Now we need to insert some nodes
(doseq [[remote-infohash ip port] (seq list-of-dht-nodes-from-network)]
  (swap! table dht/insert (utils/now!) remote-infohash ip port))

; Find nodes by a given depth
(dht/get-by-depth @table 2)
; => [ ...list of nodes... ]

(dht/get-nearest-peers @table (utils/generate!))
; => [ ...sorted list of nodes, by distance... ]

; Refresh the timestamps of recently contacted peers
(swap! table dht/refresh [infohash1 (utils/now!)] [infohash2 (utils/now!)]

; Find bad nodes with get-by-overdue...
(let [bad-nodes (dht/get-by-overdue @table (dht/fifteen-minutes-overdue!))]
; ...so we can prune them!
  (swap! table dht/prune (map :infohash bad-nodes)))
```

## Usage

`dht-cljc` is a DHT swiss army knife to build, maintain, and query a BitTorrent DHT routing table. Operations are centered around updating a Routing Table with pure functions suitable for Atom or Agent storage by the consumer.

### Transforms

* `insert` automatically handles the splitting, rejecting-if-full, and adding nodes. It does *not* ping questionable nodes, as that is the responsibility of the consumer. See `get-by-overdue` below.
* `prune` is the opposite (obviously...) of `insert`. It removes arbitrary nodes, by infohash, from the routing table, recombining buckets as necessary.
* `refresh` takes a list of `[infohash timestamp]` tuples and applies them to their respective nodes. Unlike the [BEP][bep-5] we do not track questionable nodes explicitly, preferring to keep that in control of the consumer (see `get-by-overdue`).

### Queries

While the list of nodes is available for all to see, sometimes it's helpful for common slices to be formalized. The primary functions combine `distance` and `depth` in ways that produce the [BEP][bep-5] recommendation operations.

* `get-by-depth` gets all the nodes of a certain bucket, defined by `depth` (see below), we simply look for all the items that match that depth. But as also noted above, depths below `:splits` will return all nodes in the Client infohash bucket.
* `get-nearest-peers` takes an infohash and finds the nearest bucket of nodes that are closest, then sorts them based on distance (ascending) from the infohash.
* `get-by-overdue` returns a list of nodes that are from before the provided timestamp. The [BEP][bep-5] describes a 15 minute window for refreshing clients, so the helper function `fifteen-minutes-overdue!` returns a timestamp representing 15 minutes from invocation in milliseconds.

### Other

I've included several other helpers for managing DHT clients.

* `dht-cljc.core/bootstrap-nodes` is a sequence of tuples `[URL port]` that describe public DHT bootstrap nodes that can be queried initially.
* The `dht-cljc.infohash` namespace has several functions for managing infohashes.
  * `depth` and `distance` fns
  * `sha1` that operates on bytes
  * `generate!` creates a random infohash for your client.
* The `dht-cljc.utils` namespace has several general helpers.
  * Transforms to and from hex-encoded strings to byte vectors
  * A portable `now!` fn that gets the Unix epoch time.
  * A portable `string->bytes` fn

## Details

### No network support

As it stands, the [BEP][bep-5] defines both the protocol and the transport medium, UDP. This project aims to implement the protocol while pointedly ignoring the UDP requirement. This keeps the library lightweight and composable, but sacrifices automatic pinging and refreshing which must be handled by the consumer. But never fear! All the tools you need to keep your nodes fresh are open and simple.

### Router Table

The Router Table contains both the nodes of the DHT, as well as some configuration of the table itself. A table can be initialized with the `generate-table` function. The Table itself is made of these keys.

* `:router` - A vector of nodes in the entire table. Queries slice and dice this in various ways, but it can be extended by *YOU!*
* `:client-infohash` - The infohash of the consuming app. This is used to determine distance and depth described below.
* `:splits` - How many times the router split buckets. This is modified by the `insert` and `prune` functions to dynamically adjust the number of buckets.
* `:max-bucket-count` - The maximum number of nodes in a given bucket. The default is set to 8, as recommended by the [BEP][bep-5], but this can be configured to any number. An example of such large buckets are DHT Bootstrapping nodes.

### Nodes

Nodes are represented as a map describing the node. The keys are `#{:infohash :depth :last-seen :ip :port}`. "Buckets" are represented by querying against the `:depth` key.

In a nod towards IPv6, several keys are left to the consumer. This accommodates different implementations or use cases. This describes how `dht-cljc` consumes the node.

* `:infohash` - A byte representation of the Infohash. This should not be modified, but can be transformed using the `dht-cljc.infohash` namespace.
* `:depth` - Integer representing the depth of the node. This should not be modified. See Depth below.
* `:last-seen` - The representation of when a node was last seen. Should be acceptable to the `<` compare function.
* `:ip` - Not Used.
* `:port` - Not Used.

### Depth

In the [BEP][bep-5] the buckets are described as a range between powers of 2. A careful reading will reveal the organization is a descending bit from 160 to 0. This library refers to this as `depth`, or a measure of how far this bit is from the highest order bit, as the same mathematical equivalent as a smaller integer distance in the original BEP. As such, any node that shares a depth are considered in the same bucket. This is stored in the node's `:depth` key.

#### Client Bucket

There is one exception to the depth bucket rule: The client infohash bucket. As defined in the [BEP][bep-5], buckets are only split when a new node is inserted into the Client infohash's bucket (or the bucket is not full, but ignore that for now). As such we need to know what *is* the Client Bucket and how many nodes are in it.

#### `:splits`

The Router Table keeps track of the number of bucket splits. Every time the client bucket is split, this number increments. This *also* means that any `:depth` that is >= `:splits` is considered in the Client Bucket. Convenient, eh?

**But why?**

1. Math is cool.
2. All the nodes whose `:depth` that are >= `:splits` number less than the maximum for a bucket, thus the client bucket doesn't need to split.

## Development

The test suite is built in CLJC, and is run under both Clojure and Clojurescript.

The Clojurescript unit tests require NodeJS to run. Otherwise only Leiningen is required.

* `lein test` runs the tests under Clojure.
* `lein cljs-test` runs the tests under Clojurescript and NodeJS.
* `lein test-all` runs both Clojure and Clojurescript test suite.
* `lein cljs-auto-test` automatically compiles and runs the Clojurescript tests on every change.

## License

Copyright © 2018 James Leonis

Distributed under the EPLv2 license. See LICENSE file.

[bep-5]: http://www.bittorrent.org/beps/bep_0005.html
