# Microhttp Ring Adapter

## What 
Adapter for using [microhttp](https://github.com/ebarlas/microhttp)
as a ring server.

Doesn't support the required `:remote-addr` key properly because microhttp
does not support recovering that information. This isn't the biggest loss as
that information isn't reliable in modern cloud environments anyways, but
worth noting.

This was done as a proof of concept, but the code is simple enough that
I am confident it is as production ready as microhttp.

## deps.edn

```clojure
io.github.bowbahdoe/microhttp-ring-adapter {:git/tag "v0.0.1"}
```