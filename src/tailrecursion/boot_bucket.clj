(ns tailrecursion.boot-bucket
  {:boot/export-tasks true}
  (:require
    [boot.core :as boot]
    [boot.pod  :as pod]
    [boot.util :as util]))

(def ^:private deps
  '[[com.amazonaws/aws-java-sdk-s3 "1.11.95"]])

(defn- warn-deps [deps]
  (let [conflict (delay (util/warn "Overriding project dependencies, using:\n"))]
    (doseq [dep deps]
      (when (pod/dependency-loaded? dep)
        @conflict
        (util/warn "• %s\n" (pr-str dep))))))

(defn- pod-env [deps]
  (let [dep-syms (->> deps (map first) set)]
    (warn-deps deps)
    (-> (dissoc pod/env :source-paths)
        (update-in [:dependencies] #(remove (comp dep-syms first) %))
        (update-in [:dependencies] into deps))))

(boot/deftask spew
  [b bucket     NAME       str "AWS Bucket Identifier"
   a access-key ACCESS_KEY str "AWS Access Key"
   s secret-key SECRET_KEY str "AWS Secret Key"
   m metadata META edn "Path k/v pairs for metadata, e.g. {\"index.html\" {:content-encoding \"gzip\"}}"]
 (let [pod (pod/make-pod (pod-env deps))
       out (boot/tmp-dir!)]
  (boot/with-pre-wrap fileset
   (util/info "Spewing files into the %s bucket...\n" bucket)
   (let [src-files*  (boot/output-files fileset)
         tgt-digests (pod/with-call-in pod
                       (tailrecursion.boot-bucket.client/list-digests ~*opts*))
         src-files   (remove #(some (fn [[p h]] (and (= (:path %) p) (= (:hash %) h))) tgt-digests) src-files*)]
    (doseq [{:keys [dir path]} src-files]
     (util/info "• %s\n" path)
     (pod/with-call-in pod
      (tailrecursion.boot-bucket.client/put-file! ~*opts* ~(.getPath dir) ~path))
     (boot/add-meta fileset (into {} (mapv #(vector (:path %) {::uploaded true}) src-files))))
    fileset))))
