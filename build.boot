(set-env!
  :resource-paths #{"src"}
  :dependencies   '[[org.clojure/clojure "1.9.0"  :scope "provided"]
                    [adzerk/bootlaces    "0.1.13" :scope "test"]
                    [org.clojure/tools.namespace "0.2.3" :scope "test"]
                    [com.amazonaws/aws-java-sdk-s3 "1.11.95" :scope "test"]])

(require
  '[adzerk.bootlaces :refer :all])

(def +version+ "0.3.1-SNAPSHOT")

(bootlaces! +version+)

(deftask develop []
  (comp (watch) (speak) (build-jar)))

(deftask deploy []
  (comp (speak) (build-jar) (push-snapshot)))

(task-options!
 pom  {:project     'tailrecursion/boot-bucket
       :version     +version+
       :description "Boot task for syncing fileset output to an AWS S3 bucket."
       :url         "https://github.com/tailrecursion/boot-bucket"
       :scm         {:url "https://github.com/tailrecursion/boot-bucket"}
       :license     {"EPL" "http://www.eclipse.org/legal/epl-v10.html"}})
