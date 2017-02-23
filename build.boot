(set-env!
  :resource-paths #{"src"}
  :dependencies   '[[org.clojure/clojure "1.8.0"  :scope "provided"]
                    [adzerk/bootlaces    "0.1.13" :scope "test"]])

(require
  '[adzerk.bootlaces :refer :all])

(def +version+ "0.2.0-SNAPSHOT")

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
       :license     {"EPL" "http://www.eclipse.org/legal/epl-v10.html"} })
