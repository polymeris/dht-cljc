# dht-clj

> Lord Helmet: Found anything yet?
> Mook 1: Nothing yet, sir!
> Lord Helmet: How 'bout you?
> Mook 2: Not a thing, sir!
> Lord Helmet: How 'bout you guys?
> Mook 3: We ain't found shit!
> - Spaceballs

A Clojure BitTorrent DHT plumbing library.

## Installation

[com.jamesleonis/dht-clj "0.1.0-SNAPSHOT"]

## Quickstart

Coming soon!

## Usage

`dht-clj` gives you the tools needed to build, maintain, and query a BitTorrent DHT routing table. Operations are centered around manipulating a Routing Table with purely functional manipulators suitable for Atom or Agent storage by the consumer.

### No network support

As it stands, the [BEP][bep-5] defines both the protocol and the transport medium, UDP. This project aims to implement the protocol while pointedly ignoring the UDP requirement. This keeps the library lightweight and composable, but sacrifices automatic pinging and refreshing which must be handled by the consumer.

### Router Table

The Router Table contains both the nodes of the DHT, as well as some configuration of the table itself. The vector of nodes is found in the `:router` key. A table can be initialized with the `generate-table` function.

### Nodes

Nodes are represented as a map describing the node. The keys are `#{:infohash :depth :last-seen :ip :port}`. "Buckets" are represented by querying against the `:depth` key.

### Depth

In the [BEP][bep-5] the buckets are described as a range between powers of 2. A careful reading will reveal the organization is a descending bit from 160 to 0. This library refers to this as `depth`, or a measure of how far this bit is from the highest order bit, as the same mathematical equivalent as a smaller integer distance in the original BEP. As such, any node that shares a depth are considered in the same bucket. This is stored in the node's `:depth` key.

#### Client Bucket

There is one exception to the depth bucket rule: The client infohash bucket. As defined in the [BEP][bep-5], buckets are only split when a new node is inserted into the Client infohash's bucket (or the bucket is not full, but ignore that for now). As such we need to know what *is* the Client Bucket and how many nodes are in it.

#### `:splits`

The Router Table keeps track of the number of bucket splits. Every time the client bucket is split, this number increments. This *also* means that any `:depth` that is >= `:splits` is considered in the Client Bucket. Convenient, eh?

**But why?**

1. Math is cool.
2. All the nodes whose `:depth` that are >= `:splits` number less than the maximum for a bucket, thus the client bucket doesn't need to split.

### Insertion

`insert` automatically handles the splitting, rejecting-if-full, and adding nodes. It does *not* ping questionable nodes, as that is the responsibility of the consumer.

### Queries

#### Get nodes by depth

To get all the nodes of a certain bucket, defined by `depth` above, we simply look for all the items that match that depth. But as also noted above, depths below `:splits` will return all nodes in the Client infohash bucket. The `get-by-depth` function performs this operation.

#### Find the closest node from an arbitrary infohash

Coming Soon!

#### Find nodes in that need to be refreshed/pinged

Coming Soon!

## Why?

The BitTorrent DHT is an important component of the torrent ecosystem, and a growing number of additional technologies are beginning to use the network as well. The aim is to make this module small and embeddable in different Clojure applications. This allows the consuming app to define and control the network transport while keeping this library lightweight and composable.

## License

Copyright © 2018 James Leonis

Distributed under the EPLv2 license. See LICENSE file.

[bep-5]: http://www.bittorrent.org/beps/bep_0005.html
