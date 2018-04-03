(ns tailrecursion.boot-bucket.client
  (:require
    [clojure.java.io :as io])
  (:import
    [com.amazonaws.auth              BasicAWSCredentials]
    [com.amazonaws.services.s3       AmazonS3]
    [com.amazonaws.services.s3       AmazonS3Client]
    [com.amazonaws.services.s3.model ListObjectsV2Request]
    [com.amazonaws.services.s3.model PutObjectRequest]
    [com.amazonaws.services.s3.model CannedAccessControlList]
    [com.amazonaws.services.s3.model ObjectMetadata]))

(defn client [acc-key sec-key]
  (-> (BasicAWSCredentials. acc-key sec-key)
      (AmazonS3Client.)
      (delay)))

(defn list-digests [{:keys [access-key secret-key bucket]}]
  (let [client @(client access-key secret-key)]
    (->> (.withBucketName (ListObjectsV2Request.) bucket)
         (.listObjectsV2 client)
         (.getObjectSummaries)
         (mapv #(vector (.getKey %) (.getETag %))))))

(defn put-object-request?
 [por]
 (instance? com.amazonaws.services.s3.model.PutObjectRequest por))

(defn put-object-request
 [bucket base-dir path]
 (PutObjectRequest. bucket path (io/file base-dir path)))

(defn with-public-read!
 [por]
 {:pre [(put-object-request? por)]
  :post [(put-object-request? %)]}
 (.withCannedAcl por CannedAccessControlList/PublicRead)
 por)

(defn with-metadata!
 [por metadata]
 {:pre [(put-object-request? por)
        (map? metadata)]
  :post [(put-object-request? %)]}
 (doseq [[k v] metadata]
  (let [om (ObjectMetadata.)]
   (case (clojure.string/lower-case (name k))
    "cache-control" (.setCacheControl om v)
    "content-disposition" (.setContentDisposition om v)
    "content-encoding" (.setContentEncoding om v)
    "content-language" (.setContentLanguage om v)
    "content-length" (.setContentLength om v)
    "content-type" (.setContentType om v)
    (.addUserMetadata om k v))
   (.withNewObjectMetadata por om)))
 por)

(defn put-object!
 [por client]
 {:pre [(put-object-request? por)]}
 (.putObject client por)
 por)

(defn put-file! [{:keys [access-key secret-key bucket]} base-dir path]
 (let [client @(client access-key secret-key)]
  (-> (put-object-request bucket base-dir path)
   with-public-read!
   (with-metadata! {"Content-Encoding" "gzip"})
   (put-object! client))))
