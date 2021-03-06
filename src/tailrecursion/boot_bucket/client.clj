(ns tailrecursion.boot-bucket.client
  (:require
    [clojure.java.io :as io]
    [clojure.string :refer [lower-case]])
  (:import
    [com.amazonaws.auth              AWSStaticCredentialsProvider BasicAWSCredentials]
    [com.amazonaws.services.s3       AmazonS3ClientBuilder]
    [com.amazonaws.services.s3.model ListObjectsV2Request]
    [com.amazonaws.services.s3.model PutObjectRequest]
    [com.amazonaws.services.s3.model CannedAccessControlList]
    [com.amazonaws.services.s3.model ObjectMetadata]))

(def canned-acls
  {:private                   CannedAccessControlList/Private
   :log-delivery-write        CannedAccessControlList/LogDeliveryWrite
   :bucket-owner-read         CannedAccessControlList/BucketOwnerRead
   :bucket-owner-full-control CannedAccessControlList/BucketOwnerFullControl
   :authenticated-read        CannedAccessControlList/AuthenticatedRead
   :public-read               CannedAccessControlList/PublicRead
   :public-read-write         CannedAccessControlList/PublicReadWrite})

(defn client [acc-key sec-key region]
  (let [creds (AWSStaticCredentialsProvider. (BasicAWSCredentials. acc-key sec-key))]
    (-> (AmazonS3ClientBuilder/standard)
        (.withCredentials creds)
        (.withRegion region)
        (.build)
        (delay))))

(defn list-digests [{:keys [access-key secret-key region bucket]}]
  (let [client @(client access-key secret-key region)]
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

(defn request [bucket base-dir path acl metadata]
  (doto (PutObjectRequest. bucket path (io/file base-dir path))
        (.withCannedAcl (canned-acls acl CannedAccessControlList/Private))
        (.withMetadata (reduce-kv meta-set! (ObjectMetadata.) (get metadata path)))))

(defn put-file! [{:keys [access-key secret-key region bucket canned-acl metadata]} base-dir path]
  (let [client @(client access-key secret-key region)
        req    (request bucket base-dir path canned-acl metadata)]
    (.putObject client req)
    path))
