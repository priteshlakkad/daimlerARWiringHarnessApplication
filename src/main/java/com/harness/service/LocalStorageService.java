package com.harness.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Log4j2
@ConditionalOnProperty(name = "app.use-s3", havingValue = "false", matchIfMissing = true)
public class LocalStorageService extends S3ServiceBase {

    private final String storagePath;

    public LocalStorageService(@Value("${app.local-storage-path:./uploads}") String storagePath) {
        this.storagePath = storagePath;
        initializeStorage();
    }

    /**
     * Initialize the storage directory if it doesn't exist
     */
    private void initializeStorage() {
        try {
            Path path = Paths.get(storagePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created local storage directory at: {}", storagePath);
            } else {
                log.info("Local storage directory already exists at: {}", storagePath);
            }
        } catch (IOException e) {
            log.error("Failed to initialize local storage directory: {}", storagePath, e);
            throw new RuntimeException("Failed to initialize local storage", e);
        }
    }

    @Override
    public void upload(String key, MultipartFile file) throws IOException {
        try {
            // Convert S3 key format (cdn/v1/harnesses/ID/...) to local file path
            Path filePath = Paths.get(storagePath, key);

            // Create parent directories if they don't exist
            Files.createDirectories(filePath.getParent());

            // Save the file
            Files.write(filePath, file.getBytes());
            log.info("File uploaded successfully to local storage: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to upload file with key={}", key, e);
            throw new IOException("Failed to upload file to local storage", e);
        }
    }

    @Override
    public List<String> listKeys(String harnessId) {
        try {
            String harnessDirPrefix = "cdn/v1/harnesses/" + harnessId;
            Path harnessDir = Paths.get(storagePath, harnessDirPrefix);

            if (!Files.exists(harnessDir)) {
                log.debug("Harness directory does not exist: {}", harnessDir);
                return new ArrayList<>();
            }

            // Recursively find all files under the harness directory
            try (Stream<Path> paths = Files.walk(harnessDir)) {
                return paths
                        .filter(Files::isRegularFile)
                        .map(path -> {
                            Path relativePath = Paths.get(storagePath).relativize(path);
                            return relativePath.toString().replace("\\", "/");
                        })
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("Failed to list keys for harnessId={}", harnessId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public String presign(String key, int ttlSeconds) {
        // For local storage, we return a local file path URL
        // You can modify this to return a direct download URL from your application
        try {
            Path filePath = Paths.get(storagePath, key);

            if (!Files.exists(filePath)) {
                log.warn("File not found for presign request: {}", key);
                return null;
            }

            // Return a local file URL or your application's download endpoint
            // Format: http://localhost:8080/api/v1/files/download?key={key}
            String presignedUrl = "/api/v1/files/download?key=" + key;
            log.info("Presigned URL generated for local file: {}", presignedUrl);
            return presignedUrl;
        } catch (Exception e) {
            log.error("Failed to presign file with key={}", key, e);
            return null;
        }
    }

    /**
     * Download a file from local storage (helper method)
     */
    public byte[] downloadFile(String key) throws IOException {
        Path filePath = Paths.get(storagePath, key);

        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + key);
        }

        return Files.readAllBytes(filePath);
    }

    /**
     * Get the file extension
     */
    public String getFileExtension(String key) {
        if (key != null && key.contains(".")) {
            return key.substring(key.lastIndexOf(".") + 1);
        }
        return "";
    }
}

