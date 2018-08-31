(ns tailrecursion.boot-bucket.client
  (:require
    [clojure.java.io :as io]
    [clojure.string :refer [lower-case]])
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

(defn meta-set! [o k v]
  (case (lower-case (name k))
    "cache-control"       (.setCacheControl       o v)
    "content-disposition" (.setContentDisposition o v)
    "content-encoding"    (.setContentEncoding    o v)
    "content-language"    (.setContentLanguage    o v)
    "content-length"      (.setContentLength      o v)
    "content-type"        (.setContentType        o v)
                          (.addUserMetadata       o k v)) o)

(defn request [bucket base-dir path metadata]
  (doto (PutObjectRequest. bucket path (io/file base-dir path))
        (.withCannedAcl CannedAccessControlList/PublicRead)
        (.withMetadata (reduce-kv meta-set! (ObjectMetadata.) (get metadata path)))))

(defn put-file! [{:keys [access-key secret-key bucket metadata]} base-dir path]
  (let [client @(client access-key secret-key)
        req    (request bucket base-dir path metadata)]
    (.putObject client req)))
