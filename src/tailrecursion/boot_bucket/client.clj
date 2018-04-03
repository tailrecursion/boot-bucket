(ns tailrecursion.boot-bucket.client
  (:require
    [clojure.java.io :as io])
  (:import
    [com.amazonaws.auth              BasicAWSCredentials]
    [com.amazonaws.services.s3       AmazonS3]
    [com.amazonaws.services.s3       AmazonS3Client]
    [com.amazonaws.services.s3.model ListObjectsV2Request]
    [com.amazonaws.services.s3.model PutObjectRequest]
    [com.amazonaws.services.s3.model CannedAccessControlList]))

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

(defn put-file! [{:keys [access-key secret-key bucket]} base-dir path]
  (let [client @(client access-key secret-key)]
    (.putObject client (doto (PutObjectRequest. bucket path (io/file base-dir path))
                         (.withCannedAcl CannedAccessControlList/PublicRead)))))
