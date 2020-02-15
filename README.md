# boot-bucket

a boot task for spewing files into an S3 bucket.

[](dependency)
```clojure
[tailrecursion/boot-bucket "2.1.2"] ;; latest release
```
[](/dependency)

## overview

the `spew` task uploads any files whose filenames or hashes differ from those in
the targeted S3 bucket, then decorates them with boot metadata so they may be
identified by subsequent tasks (see [boot-front](https://github.com/tailrecursion/boot-front)).

the files themselves may also be individually adorned with AWS metadata to
configure HTTP headers in S3 and cloundfront.

## canned access control lists (ACLs)

in a departure from previous releases, as of `2.0.0`, boot-bucket defaults to
the `:private` canned ACL, which should be changed to `:public-read` when
serving files from an S3 bucket.  alternatively, any of the following canned ACL
keywords may be specified to the `canned-acl` parameter:

```
:private (default)
:log-delivery-write
:bucket-owner-read
:bucket-owner-full-control
:authenticated-read
:public-read
:public-read-write
```

custom acls are currently unsupported, but pull requests are most welcome.

## metadata
both user and system metadata may be passed to the `metadata` parameter in the
same map to modify the http headers served up by S3. these are supported on a
per-file basis of the form `{"<filename>" {:<meta-key> "<meta-value>"}}`. an
example is shown below:

```
{"index.html.js" {:content-encoding "gzip"}}
```

## usage

the following is an excerpt of a `build.boot' file using boot-bucket together
with [boot-front][1] for deployments:

```clojure
(require
  '[adzerk.boot-cljs          :refer [cljs]]
  '[adzerk.boot-reload        :refer [reload]]
  '[hoplon.boot-hoplon        :refer [hoplon]]
  '[tailrecursion.boot-bucket :refer [spew]]
  '[tailrecursion.boot-front  :refer [burst]]
  '[tailrecursion.boot-static :refer [serve]])

(def buckets
  {:production "<appname>-production-application"
   :staging    "<appname>-staging-application"})

(def distributions
  {:production "<production-cloudfront-id>"
   :staging    "<staging-cloudfront-id>"})

(deftask develop
  "Continuously rebuild the application during development."
  [o optimizations OPM kw "Optimizations to pass the cljs compiler."]
  (let [o (or optimizations :none)]
    (comp (watch) (speak) (hoplon) (target) (reload) (cljs :optimizations o) (serve))))

(deftask build
  "Build the application with advanced optimizations."
  [o optimizations OPM kw "Optimizations to pass the cljs compiler."]
  (let [o (or optimizations :advanced)]
    (comp (speak) (hoplon) (cljs :optimizations o :compiler-options {:elide-asserts true}) (sift))))

(deftask deploy
  "Build the application with advanced optimizations then deploy it to s3."
  [e environment   ENV kw "The application environment to be utilized by the service."
   o optimizations OPM kw "Optimizations to pass the cljs compiler."]
  (assert environment "Missing required environment argument.")
  (let [b (buckets       environment)
        d (distributions environment)]
    (comp (build :optimizations optimizations) (spew :bucket b) (burst :distribution d))))

(task-options!
  serve {:port 3001}
  sift  {:include #{#"index.html.out/" #"<app-ns>/"} :invert true}
  spew  {:canned-acl :public-read
         :region     "us-east-1"
         :access-key (System/getenv "<AWS_ACCESS_KEY_ENV_VAR>")
         :secret-key (System/getenv "<AWS_SECRET_KEY_ENV_VAR>")}
  burst {:access-key (System/getenv "<AWS_ACCESS_KEY_ENV_VAR>")
         :secret-key (System/getenv "<AWS_SECRET_KEY_ENV_VAR>")})
```

# aot gzipping

the `Content-Encoding` header may be set to `"gzip"` for a large file such as
an `index.html.js` artifact that was compressed upstream by another task like
[boot-gzip][2]:

```clojure
(spew
 :canned-acl :public-read
 :region     "us-east-1"
 :access-key "<aws-access-key>"
 :secret-key "<aws-secret-key>"
 :metadata   {"index.html.js" {:content-encoding "gzip"}})
```

note that it's often good practice to zip the output from the CLJS compiler.
boot-gzip may drop the file size as much as 80%, which results in a much better
performance when serving a website from an S3 bucket and conserves space in S3.
furtherwore, even when deploying the resulting javascript artifact behind
cloudfront with the built-in compression enabled, if the file size exceeds
10MB, it will be ignored.

also note that front will not apply on-the-fly compression to any file with the
`Content-Encoding` header set, so be careful to only set it on files that have
been gzipped during deployment.

[1]: http://github.com/tailrecursion/boot-front
[2]: https://github.com/martinklepsch/boot-gzip
