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

That being said, I'm gonna put breaking changes in willy-nilly until it is
published to maven central.

## deps.edn

```clojure
io.github.bowbahdoe/microhttp-ring-adapter {:git/sha "0e62b63661f6f4a3d9fc844f45f2ff26a19c1e6e"}
```

## Usage

```clojure
(require '[dev.mccue.microhttp-ring-adapter :as microhttp])

(defn handler 
  [request]
  {:status 200 
   :headers {}
   :body    (str request)})

(def event-loop (microhttp/create-event-loop #'handler {:port 1242}))

(.start event-loop)
```