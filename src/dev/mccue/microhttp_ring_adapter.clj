(ns dev.mccue.microhttp-ring-adapter
  (:require [clojure.string :as string]
            [ring.core.protocols :as ring-protocols])
  (:import (org.microhttp Request Handler Response Header EventLoop Options DebugLogger)
           (java.io ByteArrayInputStream ByteArrayOutputStream)
           (java.util.concurrent Executors ForkJoinPool)))

(set! *warn-on-reflection* true)

(defn- ->req
  [{:keys [host port]} ^Request microhttp-request]
  (let [[uri query-string] (string/split (.uri microhttp-request)
                                         #"\?")]
    {:server-port     port
     :server-name     host
     :remote-addr     "" ; TODO: microhttp doesn't have a way to recover this
     :uri             uri
     :query-string    query-string
     :scheme          :http
     :request-method  (keyword (string/lower-case (.method microhttp-request)))
     :headers         (reduce (fn [acc ^Header header]
                                (assoc acc
                                  (string/lower-case (.name header))
                                  (.value header)))
                              {}
                              (.headers microhttp-request))
     :body            (ByteArrayInputStream. (or (.body microhttp-request)
                                                 (byte-array 0)))}))

(def ^{:private true} status->reason
  {100 "Continue"
   101 "Switching Protocols"
   200 "OK"
   201 "Created"
   202 "Accepted"
   203 "Non-Authoritative Information"
   204 "No Content"
   205 "Reset Content"
   206 "Partial Content"
   300 "Multiple Choices"
   301 "Moved Permanently"
   302 "Found"
   303 "See Other"
   304 "Not Modified"
   305 "Use Proxy"
   307 "Temporary Redirect"
   400 "Bad Request"
   401 "Unauthorized"
   402 "Payment Required"
   403 "Forbidden"
   404 "Not Found"
   405 "Method Not Allowed"
   406 "Not Acceptable"
   407 "Proxy Authentication Required"
   408 "Request Time-out"
   409 "Conflict"
   410 "Gone"
   411 "Length Required"
   412 "Precondition Failed"
   413 "Request Entity Too Large"
   414 "Request-URI Too Large"
   415 "Unsupported Media Type"
   416 "Requested range not satisfiable"
   417 "Expectation Failed"
   500 "Internal Server Error"
   501 "Not Implemented"
   502 "Bad Gateway"
   503 "Service Unavailable"
   504 "Gateway Time-out"
   505 "HTTP Version not supported"})

(defn- <-res
  [ring-response]
  (let [out (ByteArrayOutputStream.)]
    (ring-protocols/write-body-to-stream (:body ring-response)
                                         ring-response
                                         out)
    (Response. (:status ring-response)
               (or (status->reason (:status ring-response)) "IDK")
               (mapv #(Header. (key %) (val %))
                     (:headers ring-response))
               (.toByteArray out))))

(defn- ring-handler->Handler
  [{:keys [host port debug executor]} ring-handler]
  (reify Handler
    (handle [_ microhttp-request callback]
      (let [ring-request (->req {:host host
                                 :port port} microhttp-request)]
        (.submit executor
                 ^Runnable
                 (fn []
                   (.accept callback
                            (try
                              (<-res (ring-handler ring-request))
                              (catch Exception e
                                (<-res {:status 500
                                        :headers {}
                                        :body    (if debug
                                                   (.getMessage e)
                                                   "Internal Server Error")}))))))))))

(defn create-event-loop
  ([handler]
   (create-event-loop handler {}))
  ([handler options]
   (let [microhttp-options (cond-> (Options.)
                             (contains? options :host)
                             (.withHost (:host options))

                             (some? (options :port))
                             (.withPort (:port options))

                             (some? (options :reuse-addr))
                             (.withReuseAddr (:reuse-addr options))

                             (some? (options :reuse-port))
                             (.withReusePort (:reuse-port options))

                             (some? (options :resolution))
                             (.withResolution (:resolution options))

                             (some? (options :request-timeout))
                             (.withRequestTimeout (:request-timeout options))

                             (some? (options :read-buffer-size))
                             (.withReadBufferSize (:read-buffer-size options))

                             (some? (options :accept-length))
                             (.withAcceptLength (:accept-length options))

                             (some? (options :max-request-size))
                             (.withMaxRequestSize (:max-request-size options)))
         microhttp-logger (or (:logger options) (DebugLogger.))
         executor         (or (:executor options) (ForkJoinPool/commonPool))]
     (EventLoop. microhttp-options
                 microhttp-logger
                 (ring-handler->Handler
                   {:host     (.host microhttp-options)
                    :port     (.port microhttp-options)
                    :debug    (or (:debug options) false)
                    :executor executor}
                   handler)))))

(comment
  (defn handler [req]
    {:status  200
     :headers {"Content-Type" "text/html"}
     :body    (str "<pre>"
                   (with-out-str
                     (clojure.pprint/pprint req))
                   "</pre>")})
  (def event-loop (create-event-loop #'handler {:port 1242})))