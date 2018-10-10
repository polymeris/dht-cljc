# dht-clj

> Lord Helmet: Found anything yet?
> Mook 1: Nothing yet, sir!
> Lord Helmet: How 'bout you?
> Mook 2: Not a thing, sir!
> Lord Helmet: How 'bout you guys?
> Mook 3: We ain't found shit!
> - Spaceballs

A Clojure BitTorrent DHT library.

## Installation

[com.jamesleonis/dht-clj "0.1.0-SNAPSHOT"]

## Usage

### Quickstart

Coming soon!

## Why?

The BitTorrent DHT is an important component of the torrent ecosystem, and a growing number of additional technologies are beginning to use the network as well. The aim is to make this module small and embeddable in different Clojure applications.

### Differences from the BEP-5 spec

As it stands, the [spec][bep-5] defines both the protocol and the transport medium, UDP. This project aims to implement the protocol while pointedly ignoring the UDP requirement. This allows the consuming app to define and control the network transport.

## License

Copyright © 2018 James Leonis

Distributed under the EPLv2 license. See LICENSE file.

[bep-5]: http://www.bittorrent.org/beps/bep_0005.html
