# boot bucket
a boot task for spewing output files into an s3 bucket.

[](dependency)
```clojure
[tailrecursion/boot-bucket "0.1.0-SNAPSHOT"] ;; latest release
```
[](/dependency)

## usage
excerpt of a build.boot file using boot-bucket for deployment.
```clojure
(require
  '[adzerk.boot-cljs          :refer [cljs]]
  '[adzerk.boot-reload        :refer [reload]]
  '[hoplon.boot-hoplon        :refer [hoplon]]
  '[tailrecursion.boot-bucket :refer [spew]]
  '[tailrecursion.boot-static :refer [serve]])

(def buckets
  {:production "<appname>-production-application"
   :staging    "<appname>-staging-application"})

(deftask develop
  "Continuously rebuild the application during development."
  [o optimizations OPM kw "Optimizations to pass the cljs compiler."]
  (let [o (or optimizations :none)]
    (comp (watch) (speak) (hoplon) (target) (reload) (cljs :optimizations o) (serve))))

(deftask build
  "Build the application with advanced optimizations."
  [o optimizations OPM kw "Optimizations to pass the cljs compiler."]
  (let [o (or optimizations :simple)] ; default to advanced when fixed
    (comp (speak) (hoplon) (cljs :optimizations o :compiler-options {:elide-asserts true}) (sift))))

(deftask deploy
  "Build the application with advanced optimizations then deploy it to s3."
  [e environment   ENV kw "The application environment to be utilized by the service."
   o optimizations OPM kw "Optimizations to pass the cljs copmiler."]
  (assert environment "Missing required environment argument.")
  (let [b (buckets environment)]
    (comp (build :optimizations optimizations) (spew :bucket b))))

(task-options!
  serve   {:port 3001}
  sift    {:include #{#"index.html.out/" #"<app-ns>/"} :invert true}
  spew    {:access-key (System/getenv "<AWS_ACCESS_KEY_ENV_VAR>")
           :secret-key (System/getenv "<AWS_SECRET_KEY_ENV_VAR>")})
```
