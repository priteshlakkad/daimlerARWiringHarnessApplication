package com.harness.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "app.use-s3", havingValue = "true")
public class S3Service extends S3ServiceBase {

        private final S3Client s3;
        private final S3Presigner presigner;
        private final String bucket;

        public S3Service(S3Client s3, @Value("${aws.s3.bucket}") String bucket) {
                this.s3 = s3;
                this.bucket = bucket;

                this.presigner = S3Presigner.builder()
                                .region(s3.serviceClientConfiguration().region())
                                .credentialsProvider(s3.serviceClientConfiguration().credentialsProvider())
                                .build();

                ensureBucket();
        }

        private void ensureBucket() {
                try {
                        s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
                } catch (NoSuchBucketException e) {
                        // Region-safe bucket creation
                        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                                        .bucket(bucket)
                                        .createBucketConfiguration(
                                                        CreateBucketConfiguration.builder()
                                                                        .locationConstraint(
                                                                                        s3.serviceClientConfiguration()
                                                                                                        .region().id())
                                                                        .build())
                                        .build();
                        s3.createBucket(createBucketRequest);
                }
        }

        @Override
        public void upload(String key, MultipartFile file) throws IOException {
                PutObjectRequest req = PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(Optional.ofNullable(file.getContentType())
                                                .orElse("application/octet-stream"))
                                .build();

                s3.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        }

        @Override
        public List<String> listKeys(String truckModel, String harnessId) {
                String prefix = "cdn/v1/" + truckModel + "/harnesses/" + harnessId + "/";
                ListObjectsV2Response res = s3.listObjectsV2(ListObjectsV2Request.builder()
                                .bucket(bucket)
                                .prefix(prefix)
                                .build());
                return res.contents().stream().map(S3Object::key).collect(Collectors.toList());
        }

        @Override
        public List<String> listAllDownloadableKeys(String truckModel) {
                String prefix = "cdn/v1/" + truckModel + "/harnesses/";
                ListObjectsV2Response res = s3.listObjectsV2(ListObjectsV2Request.builder()
                                .bucket(bucket)
                                .prefix(prefix)
                                .build());
                return res.contents().stream()
                                .map(S3Object::key)
                                .filter(key -> key.contains("/troubleshooting/") || key.contains("/knowledge/"))
                                .collect(Collectors.toList());
        }

        /**
         * Return troubleshooting + powerflow file keys across ALL truck model folders.
         */
        @Override
        public List<String> listAllDownloadableKeys() {
                String prefix = "cdn/v1/";
                ListObjectsV2Response res = s3.listObjectsV2(ListObjectsV2Request.builder()
                                .bucket(bucket)
                                .prefix(prefix)
                                .build());
                return res.contents().stream()
                                .map(S3Object::key)
                                .filter(key -> key.contains("/troubleshooting/") || key.contains("/powerflow/"))
                                .collect(Collectors.toList());
        }

        /**
         * Return ALL file keys stored under cdn/v1/ across every truck model folder.
         */
        @Override
        public List<String> listAllFiles() {
                String prefix = "cdn/v1/";
                ListObjectsV2Response res = s3.listObjectsV2(ListObjectsV2Request.builder()
                                .bucket(bucket)
                                .prefix(prefix)
                                .build());
                return res.contents().stream().map(S3Object::key).collect(Collectors.toList());
        }

        /**
         * Search across ALL truck model folders for the given harness ID.
         * Lists all objects under cdn/v1/ and filters by /{harnessId}/
         */
        @Override
        public List<String> findKeysByHarnessId(String harnessId) {
                String prefix = "cdn/v1/";
                ListObjectsV2Response res = s3.listObjectsV2(ListObjectsV2Request.builder()
                                .bucket(bucket)
                                .prefix(prefix)
                                .build());
                return res.contents().stream()
                                .map(S3Object::key)
                                .filter(key -> key.contains("/harnesses/" + harnessId + "/"))
                                .collect(Collectors.toList());
        }

        @Override
        public String presign(String key, int ttlSeconds) {
                GetObjectRequest gor = GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build();

                GetObjectPresignRequest preq = GetObjectPresignRequest.builder()
                                .signatureDuration(Duration.ofSeconds(ttlSeconds))
                                .getObjectRequest(gor)
                                .build();

                return presigner.presignGetObject(preq).url().toString();
        }

        @Override
        public String getPublicUrl(String key) {
                return String.format("https://%s.s3.%s.amazonaws.com/%s",
                                bucket,
                                s3.serviceClientConfiguration().region().id(),
                                key);
        }

        // ── Image (single-file slot at cdn/v1/images/) ───────────────────────────

        private static final String IMAGE_DIR = "cdn/v1/images/";

        @Override
        public void uploadImage(MultipartFile file) throws IOException {
                // Delete any existing image(s) under the slot
                ListObjectsV2Response existing = s3.listObjectsV2(
                                ListObjectsV2Request.builder().bucket(bucket).prefix(IMAGE_DIR).build());
                for (S3Object obj : existing.contents()) {
                        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(obj.key()).build());
                }

                String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("image.jpg");
                String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.') + 1)
                                : "jpg";
                String key = IMAGE_DIR + "image." + ext;

                PutObjectRequest req = PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(Optional.ofNullable(file.getContentType())
                                                .orElse("application/octet-stream"))
                                .build();
                s3.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        }

        @Override
        public byte[] downloadImage() throws Exception {
                String key = getImageKey();
                if (key == null)
                        throw new Exception("No image found at " + IMAGE_DIR);
                try (var response = s3.getObject(
                                GetObjectRequest.builder().bucket(bucket).key(key).build())) {
                        return response.readAllBytes();
                }
        }

        @Override
        public String getImageKey() {
                ListObjectsV2Response res = s3.listObjectsV2(
                                ListObjectsV2Request.builder().bucket(bucket).prefix(IMAGE_DIR).build());
                return res.contents().stream().map(S3Object::key).findFirst().orElse(null);
        }

        // ── GLTF / Truck Icon (per-truck slot at cdn/v1/{truckModel}/GLTF/) ─────

        private String gltfDir(String truckModel) {
                return "cdn/v1/" + truckModel + "/GLTF/";
        }

        private String gltfKey(String truckModel) {
                return gltfDir(truckModel) + truckModel + "_model.gltf";
        }

        @Override
        public void uploadGltf(String truckModel, MultipartFile gltfFile) throws IOException {
                String key = gltfKey(truckModel);
                PutObjectRequest req = PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(Optional.ofNullable(gltfFile.getContentType())
                                                .orElse("model/gltf-binary"))
                                .build();
                s3.putObject(req, RequestBody.fromInputStream(gltfFile.getInputStream(), gltfFile.getSize()));
        }

        @Override
        public void uploadTruckIcon(String truckModel, MultipartFile iconFile) throws IOException {
                String dir = gltfDir(truckModel);
                String gltfFileName = truckModel + "_model.gltf";

                // Delete any previous icon file (leave the .gltf alone)
                ListObjectsV2Response existing = s3.listObjectsV2(
                                ListObjectsV2Request.builder().bucket(bucket).prefix(dir).build());
                for (S3Object obj : existing.contents()) {
                        if (!obj.key().endsWith(gltfFileName)) {
                                s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(obj.key()).build());
                        }
                }

                String originalName = Optional.ofNullable(iconFile.getOriginalFilename()).orElse("truck_icon");
                String key = dir + originalName;
                PutObjectRequest req = PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(Optional.ofNullable(iconFile.getContentType())
                                                .orElse("application/octet-stream"))
                                .build();
                s3.putObject(req, RequestBody.fromInputStream(iconFile.getInputStream(), iconFile.getSize()));
        }

        @Override
        public java.util.Map<String, String> getGltfKeys(String truckModel) {
                String dir = gltfDir(truckModel);
                String expectedGltfKey = gltfKey(truckModel);

                ListObjectsV2Response res = s3.listObjectsV2(
                                ListObjectsV2Request.builder().bucket(bucket).prefix(dir).build());

                java.util.Map<String, String> result = new java.util.HashMap<>();
                result.put("gltfKey", null);
                result.put("iconKey", null);

                for (S3Object obj : res.contents()) {
                        if (obj.key().equals(expectedGltfKey)) {
                                result.put("gltfKey", obj.key());
                        } else {
                                result.put("iconKey", obj.key());
                        }
                }
                return result;
        }
}
