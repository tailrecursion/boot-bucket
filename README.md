# boot-bucket

Boot task `spew` for spewing output files into an s3 bucket.

[](dependency)
```clojure
[tailrecursion/boot-bucket "1.1.0"] ;; latest release
```
[](/dependency)

## Overview

This task uploads any files whose filenames or hashes differ from those in the
targeted s3 bucket, then decorates them with boot metadata so they may be
identified by subsequent tasks (see [boot-front](https://github.com/tailrecursion/boot-front)).

Also optionally allows S3 metadata to be set so that HTTP headers can be
configured on a per-file basis for files served by S3/Cloudfront.

## Usage

Excerpt of a build.boot file using boot-bucket with boot-front for deployment.

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
  spew  {:access-key (System/getenv "<AWS_ACCESS_KEY_ENV_VAR>")
         :secret-key (System/getenv "<AWS_SECRET_KEY_ENV_VAR>")}
  burst {:access-key (System/getenv "<AWS_ACCESS_KEY_ENV_VAR>")
         :secret-key (System/getenv "<AWS_SECRET_KEY_ENV_VAR>")})
```

## Public read ACL on all files

**All files are set to public read upon upload.**

This is so that the files can be served over HTTP either directly from S3 or
through Cloudfront.

This suits the primary use-case - compiling an SPA through CLJS.

If you have a different need that requires private uploads, you'll need to
submit a pull request with some conditional logic around
`tailrecursion.boot-bucket.client/with-public-read!`

## s3 metadata

S3 supports two kinds of metadata, system and user. These are served by S3 as
headers over HTTP.

`spew` supports providing a map of path/metadata where metadata is a map of k/v
pairs.

Internally the boot task normalizes user/system metadata so that the correct fns
in the AWS SDK are called, based on the metadata key name.

### Example: AOT gzipping

We can set the `Content-Encoding` header to `gzip` for a large file such as
`index.html.js` that we [already compressed in an earlier task](https://github.com/martinklepsch/boot-gzip).

```clojure
(spew
 :access-key ; ...
 :secret-key ; ...
 :metadata {"index.html.js" {:content-encoding "gzip"}})
```

Gzipping compiled CLJS output is pretty important as core/goog can weigh in at
multiple MB and compression can drop this file size by 80%+.

In fact, it's possible for compiled CLJS to weigh in at 10MB+ and compress to
under 2MB - but at this point Cloudfront will no longer apply compression due to
platform limitations.

Pre-gzipping files and setting the correct metadata headers in this way has
several benefits:

- Achieve compressed files without Cloudfront (e.g. serving site from S3)
- Avoid the 1MB min/10MB max limits for on-the-fly compression in Cloudfront
- Reduce S3 resources needed in upload and storage
- Minor performance benefits as Cloudfront does not need to apply compression

Note that Cloudfront will not apply on-the-fly compression to any file with the
`Content-Encoding` header set, so be careful to only set it on files that have
been gzipped during deployment.

## Parallel file uploading

By default `spew` will upload all files sequentially, but can be configured to
parallel uploads through the `-p` flag or `:parallel?` task config.
