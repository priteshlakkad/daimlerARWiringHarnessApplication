package com.harness.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Log4j2
@ConditionalOnProperty(name = "app.use-s3", havingValue = "nonexistent")
public class DummyS3Service extends S3ServiceBase {

    @Override
    public void upload(String key, MultipartFile file) {
        log.info("S3 is disabled. Skipping upload for key={}", key);
    }

    @Override
    public List<String> listKeys(String harnessId) {
        log.debug("S3 is disabled. listKeys called for harnessId={}", harnessId);
        return List.of();
    }

    @Override
    public String presign(String key, int ttlSeconds) {
        log.warn("S3 is disabled. Presign requested for key={}, ttlSeconds={}", key, ttlSeconds);
        return "S3_DISABLED";
    }
}

