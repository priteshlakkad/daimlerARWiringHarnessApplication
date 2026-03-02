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
public class S3Service extends S3ServiceBase{

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
                                    .locationConstraint(s3.serviceClientConfiguration().region().id())
                                    .build()
                    )
                    .build();
            s3.createBucket(createBucketRequest);
        }
    }

    @Override
    public void upload(String key, MultipartFile file) throws IOException {
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(Optional.ofNullable(file.getContentType()).orElse("application/octet-stream"))
                .build();

        s3.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    }

    @Override
    public List<String> listKeys(String harnessId) {
        String prefix = "cdn/v1/harnesses/" + harnessId + "/";
        ListObjectsV2Response res = s3.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build());
        return res.contents().stream().map(S3Object::key).collect(Collectors.toList());
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
}
