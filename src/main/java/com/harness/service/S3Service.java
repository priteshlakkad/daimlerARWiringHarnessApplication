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
                                .filter(key -> key.contains("/troubleshooting/") || key.contains("/powerflow/")
                                                || key.contains("/workshopmanual/"))
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

        // ── EndDevice (per-device slot at cdn/v1/{truckModel}/enddevices/) ──

        private String endDeviceDir(String truckModel) {
                return "cdn/v1/" + truckModel + "/enddevices/";
        }

        @Override
        public void uploadEndDeviceFile(String truckModel, MultipartFile file) throws IOException {
                String dir = endDeviceDir(truckModel);
                String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
                String key = dir + originalName;

                PutObjectRequest req = PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(Optional.ofNullable(file.getContentType())
                                                .orElse("application/octet-stream"))
                                .build();
                s3.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        }

        @Override
        public List<String> getEndDeviceFiles(String truckModel) {
                String dir = endDeviceDir(truckModel);
                ListObjectsV2Response res = s3.listObjectsV2(
                                ListObjectsV2Request.builder().bucket(bucket).prefix(dir).build());
                return res.contents().stream().map(S3Object::key).collect(Collectors.toList());
        }

        @Override
        public void deleteEndDeviceFile(String truckModel, String fileName) throws Exception {
                String key = endDeviceDir(truckModel) + fileName;

                // Check if object exists first
                try {
                        s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
                } catch (NoSuchKeyException e) {
                        throw new IOException("File not found in S3: " + fileName);
                }

                s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        }

        // ── FaultCodes (per-truck multi-file slot at cdn/v1/{truckModel}/faultcodes/)
        // ──

        private String getFaultCodeDir(String truckModel) {
                return "cdn/v1/" + truckModel + "/faultcodes/";
        }

        @Override
        public void uploadFaultCodeFile(String truckModel, MultipartFile file) throws IOException {
                String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
                String key = getFaultCodeDir(truckModel) + originalName;

                PutObjectRequest req = PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(Optional.ofNullable(file.getContentType())
                                                .orElse("application/octet-stream"))
                                .build();
                s3.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        }

        @Override
        public List<String> getFaultCodeFiles(String truckModel) {
                String prefix = getFaultCodeDir(truckModel);
                ListObjectsV2Response res = s3.listObjectsV2(ListObjectsV2Request.builder()
                                .bucket(bucket)
                                .prefix(prefix)
                                .build());
                return res.contents().stream().map(S3Object::key).collect(Collectors.toList());
        }

        @Override
        public List<String> listTruckFiles(String truckModel) {
                String prefix = "cdn/v1/" + truckModel + "/";
                ListObjectsV2Response res = s3.listObjectsV2(ListObjectsV2Request.builder()
                                .bucket(bucket)
                                .prefix(prefix)
                                .build());
                return res.contents().stream().map(S3Object::key).collect(Collectors.toList());
        }

        @Override
        public void deleteFaultCodeFile(String truckModel, String fileName) throws Exception {
                String key = getFaultCodeDir(truckModel) + fileName;

                // Check if object exists first to throw exception if not found, matching
                // LocalStorage behavior
                try {
                        s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
                } catch (NoSuchKeyException e) {
                        throw new IOException("File not found in S3: " + fileName);
                }

                s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        }

        // ── Bulk delete helper ───────────────────────────────────────────────────

        private void deleteByPrefix(String prefix) {
                String continuationToken = null;
                do {
                        ListObjectsV2Request.Builder reqBuilder = ListObjectsV2Request.builder()
                                        .bucket(bucket)
                                        .prefix(prefix);
                        if (continuationToken != null) {
                                reqBuilder.continuationToken(continuationToken);
                        }
                        ListObjectsV2Response res = s3.listObjectsV2(reqBuilder.build());

                        List<ObjectIdentifier> toDelete = res.contents().stream()
                                        .map(obj -> ObjectIdentifier.builder().key(obj.key()).build())
                                        .collect(Collectors.toList());

                        if (!toDelete.isEmpty()) {
                                s3.deleteObjects(DeleteObjectsRequest.builder()
                                                .bucket(bucket)
                                                .delete(Delete.builder().objects(toDelete).build())
                                                .build());
                        }

                        continuationToken = res.isTruncated() ? res.nextContinuationToken() : null;
                } while (continuationToken != null);
        }

        // ── Truck model and harness listing ──────────────────────────────────────

        @Override
        public List<String> listTruckModels() {
                ListObjectsV2Response res = s3.listObjectsV2(ListObjectsV2Request.builder()
                                .bucket(bucket)
                                .prefix("cdn/v1/")
                                .delimiter("/")
                                .build());
                return res.commonPrefixes().stream()
                                .map(CommonPrefix::prefix)
                                .map(p -> p.replace("cdn/v1/", "").replace("/", ""))
                                .filter(s -> !s.isBlank())
                                .collect(Collectors.toList());
        }

        @Override
        public List<String> listHarnessIds(String truckModel) {
                String prefix = "cdn/v1/" + truckModel + "/harnesses/";
                ListObjectsV2Response res = s3.listObjectsV2(ListObjectsV2Request.builder()
                                .bucket(bucket)
                                .prefix(prefix)
                                .delimiter("/")
                                .build());
                return res.commonPrefixes().stream()
                                .map(CommonPrefix::prefix)
                                .map(p -> p.replace(prefix, "").replace("/", ""))
                                .filter(s -> !s.isBlank())
                                .collect(Collectors.toList());
        }

        // ── Bulk deletions ────────────────────────────────────────────────────────

        @Override
        public void deleteTruck(String truckModel) throws Exception {
                deleteByPrefix("cdn/v1/" + truckModel + "/");
        }

        @Override
        public void deleteHarness(String truckModel, String harnessId) throws Exception {
                deleteByPrefix("cdn/v1/" + truckModel + "/harnesses/" + harnessId + "/");
        }

        @Override
        public void deleteHarnessFile(String truckModel, String harnessId,
                        String category, String fileName) throws Exception {
                String key = "cdn/v1/" + truckModel + "/harnesses/" + harnessId
                                + "/" + category + "/" + fileName;
                try {
                        s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
                } catch (NoSuchKeyException e) {
                        throw new IOException("File not found in S3: " + fileName);
                }
                s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        }

        @Override
        public void deleteGltfFolder(String truckModel) throws Exception {
                deleteByPrefix(gltfDir(truckModel));
        }

        @Override
        public void deleteAllFaultCodeFiles(String truckModel) throws Exception {
                deleteByPrefix(getFaultCodeDir(truckModel));
        }

        @Override
        public void deleteAllEndDeviceFiles(String truckModel) throws Exception {
                deleteByPrefix(endDeviceDir(truckModel));
        }

        // ── WorkshopManual (per-truck multi-file slot at cdn/v1/{truckModel}/workshopmanual/) ──

        private String getWorkshopManualDir(String truckModel) {
                return "cdn/v1/" + truckModel + "/workshopmanual/";
        }

        @Override
        public void uploadWorkshopManualFile(String truckModel, MultipartFile file) throws IOException {
                String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
                String key = getWorkshopManualDir(truckModel) + originalName;

                PutObjectRequest req = PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(Optional.ofNullable(file.getContentType())
                                                .orElse("application/octet-stream"))
                                .build();
                s3.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        }

        @Override
        public List<String> getWorkshopManualFiles(String truckModel) {
                String prefix = getWorkshopManualDir(truckModel);
                ListObjectsV2Response res = s3.listObjectsV2(ListObjectsV2Request.builder()
                                .bucket(bucket)
                                .prefix(prefix)
                                .build());
                return res.contents().stream().map(S3Object::key).collect(Collectors.toList());
        }

        @Override
        public void deleteWorkshopManualFile(String truckModel, String fileName) throws Exception {
                String key = getWorkshopManualDir(truckModel) + fileName;

                try {
                        s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
                } catch (NoSuchKeyException e) {
                        throw new IOException("File not found in S3: " + fileName);
                }

                s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        }

        @Override
        public void deleteAllWorkshopManualFiles(String truckModel) throws Exception {
                deleteByPrefix(getWorkshopManualDir(truckModel));
        }

        // ── 3D Model (per-harness slot at cdn/v1/{truckModel}/harnesses/{harnessId}/) ──

        @Override
        public com.harness.dtos.UploadedFileResponse upload3dModel(
                        String truckModel, String harnessId, MultipartFile file) throws IOException {
                String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("model.bin");
                String ext = originalName.contains(".")
                                ? originalName.substring(originalName.lastIndexOf('.') + 1)
                                : "bin";
                String key = String.format("cdn/v1/%s/harnesses/%s/%s.%s", truckModel, harnessId, harnessId, ext);
                PutObjectRequest req = PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(Optional.ofNullable(file.getContentType())
                                                .orElse("application/octet-stream"))
                                .build();
                s3.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
                return new com.harness.dtos.UploadedFileResponse(key, getPublicUrl(key));
        }
}
