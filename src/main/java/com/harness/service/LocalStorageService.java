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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
            // Convert S3 key format (cdn/v1/{truckModel}/harnesses/{harnessId}/...) to
            // local file path
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
    public List<String> listKeys(String truckModel, String harnessId) {
        try {
            String harnessDirPrefix = "cdn/v1/" + truckModel + "/harnesses/" + harnessId;
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
            log.error("Failed to list keys for truckModel={}, harnessId={}", truckModel, harnessId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> listAllDownloadableKeys(String truckModel) {
        try {
            Path harnessesRoot = Paths.get(storagePath, "cdn/v1/" + truckModel + "/harnesses");

            if (!Files.exists(harnessesRoot)) {
                log.debug("Harnesses root directory does not exist for truckModel={}: {}", truckModel, harnessesRoot);
                return new ArrayList<>();
            }

            try (Stream<Path> paths = Files.walk(harnessesRoot)) {
                return paths
                        .filter(Files::isRegularFile)
                        .map(path -> {
                            Path relativePath = Paths.get(storagePath).relativize(path);
                            return relativePath.toString().replace("\\", "/");
                        })
                        .filter(key -> key.contains("/troubleshooting/") || key.contains("/powerflow/"))
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("Failed to list all downloadable keys for truckModel={}", truckModel, e);
            return new ArrayList<>();
        }
    }

    /**
     * Return troubleshooting + powerflow file keys across ALL truck model folders.
     */
    @Override
    public List<String> listAllDownloadableKeys() {
        try {
            Path cdnRoot = Paths.get(storagePath, "cdn/v1");
            if (!Files.exists(cdnRoot)) {
                log.debug("CDN root directory does not exist: {}", cdnRoot);
                return new ArrayList<>();
            }
            try (Stream<Path> paths = Files.walk(cdnRoot)) {
                return paths
                        .filter(Files::isRegularFile)
                        .map(path -> Paths.get(storagePath).relativize(path).toString().replace("\\", "/"))
                        .filter(key -> key.contains("/troubleshooting/") || key.contains("/powerflow/")
                                || key.contains("/workshopmanual/"))
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("Failed to list all downloadable keys", e);
            return new ArrayList<>();
        }
    }

    /**
     * Return ALL file keys stored under cdn/v1/ across every truck model folder.
     */
    @Override
    public List<String> listAllFiles() {
        try {
            Path cdnRoot = Paths.get(storagePath, "cdn/v1");
            if (!Files.exists(cdnRoot)) {
                log.debug("CDN root directory does not exist: {}", cdnRoot);
                return new ArrayList<>();
            }
            try (Stream<Path> paths = Files.walk(cdnRoot)) {
                return paths
                        .filter(Files::isRegularFile)
                        .map(path -> Paths.get(storagePath).relativize(path).toString().replace("\\", "/"))
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("Failed to list all files", e);
            return new ArrayList<>();
        }
    }

    /**
     * Search across ALL truck model folders under cdn/v1/ for the given harness ID.
     * Returns files from the first truck model folder that contains the harness ID.
     */
    @Override
    public List<String> findKeysByHarnessId(String harnessId) {
        try {
            Path cdnRoot = Paths.get(storagePath, "cdn/v1");

            if (!Files.exists(cdnRoot)) {
                log.debug("CDN root directory does not exist: {}", cdnRoot);
                return new ArrayList<>();
            }

            // Walk top-level truck model directories under cdn/v1/
            try (Stream<Path> truckModelDirs = Files.list(cdnRoot)) {
                return truckModelDirs
                        .filter(Files::isDirectory)
                        .sorted() // deterministic order
                        .flatMap(truckDir -> {
                            Path harnessDir = truckDir.resolve("harnesses").resolve(harnessId);
                            if (!Files.exists(harnessDir))
                                return Stream.empty();
                            try {
                                return Files.walk(harnessDir)
                                        .filter(Files::isRegularFile)
                                        .map(path -> Paths.get(storagePath).relativize(path)
                                                .toString().replace("\\", "/"));
                            } catch (IOException e) {
                                log.error("Error walking harnessDir={}", harnessDir, e);
                                return Stream.empty();
                            }
                        })
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("Failed to find keys for harnessId={}", harnessId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public String presign(String key, int ttlSeconds) {
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

    @Override
    public String getPublicUrl(String key) {
        return presign(key, 0); // ttl doesn't matter for local
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

    // ── Image (single-file slot) ─────────────────────────────────────────────

    private static final String IMAGE_DIR = "cdn/v1/images";

    @Override
    public void uploadImage(MultipartFile file) throws Exception {
        Path imageDir = Paths.get(storagePath, IMAGE_DIR);
        Files.createDirectories(imageDir);

        // Delete any existing image file(s)
        try (Stream<Path> existing = Files.list(imageDir)) {
            existing.filter(Files::isRegularFile).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("Could not delete old image: {}", p, e);
                }
            });
        }

        // Determine extension from original filename; default to 'jpg'
        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("image.jpg");
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.') + 1) : "jpg";
        String key = IMAGE_DIR + "/image." + ext;

        Path dest = Paths.get(storagePath, key);
        Files.write(dest, file.getBytes());
        log.info("Image uploaded to local storage: {}", dest);
    }

    @Override
    public byte[] downloadImage() throws IOException {
        Path imageDir = Paths.get(storagePath, IMAGE_DIR);
        if (!Files.exists(imageDir)) {
            throw new IOException("No image found at " + IMAGE_DIR);
        }
        try (Stream<Path> files = Files.list(imageDir)) {
            return files.filter(Files::isRegularFile)
                    .findFirst()
                    .map(p -> {
                        try {
                            return Files.readAllBytes(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .orElseThrow(() -> new IOException("No image found at " + IMAGE_DIR));
        }
    }

    @Override
    public String getImageKey() {
        Path imageDir = Paths.get(storagePath, IMAGE_DIR);
        if (!Files.exists(imageDir))
            return null;
        try (Stream<Path> files = Files.list(imageDir)) {
            return files.filter(Files::isRegularFile)
                    .findFirst()
                    .map(p -> Paths.get(storagePath).relativize(p).toString().replace("\\", "/"))
                    .orElse(null);
        } catch (IOException e) {
            log.error("Failed to check image key", e);
            return null;
        }
    }

    // ── GLTF / Truck Icon (per-truck slot at cdn/v1/{truckModel}/GLTF/) ──────

    private String gltfDirLocal(String truckModel) {
        return "cdn/v1/" + truckModel + "/GLTF";
    }

    private String gltfKeyLocal(String truckModel) {
        return gltfDirLocal(truckModel) + "/" + truckModel + "_model.gltf";
    }

    @Override
    public void uploadGltf(String truckModel, MultipartFile gltfFile) throws Exception {
        String key = gltfKeyLocal(truckModel);
        Path dest = Paths.get(storagePath, key);
        Files.createDirectories(dest.getParent());
        Files.write(dest, gltfFile.getBytes());
        log.info("GLTF uploaded to local storage: {}", dest);
    }

    @Override
    public void uploadTruckIcon(String truckModel, MultipartFile iconFile) throws Exception {
        Path dir = Paths.get(storagePath, gltfDirLocal(truckModel));
        Files.createDirectories(dir);
        String gltfFileName = truckModel + "_model.gltf";

        // Delete previous icon file(s), keep the .gltf
        if (Files.exists(dir)) {
            try (Stream<Path> existing = Files.list(dir)) {
                existing.filter(Files::isRegularFile)
                        .filter(p -> !p.getFileName().toString().equals(gltfFileName))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                log.warn("Could not delete old icon: {}", p, e);
                            }
                        });
            }
        }

        String originalName = Optional.ofNullable(iconFile.getOriginalFilename()).orElse("truck_icon");
        Path dest = dir.resolve(originalName);
        Files.write(dest, iconFile.getBytes());
        log.info("Truck icon uploaded to local storage: {}", dest);
    }

    @Override
    public java.util.Map<String, String> getGltfKeys(String truckModel) {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        result.put("gltfKey", null);
        result.put("iconKey", null);

        Path dir = Paths.get(storagePath, gltfDirLocal(truckModel));
        if (!Files.exists(dir))
            return result;

        String expectedGltfName = truckModel + "_model.gltf";
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile).forEach(p -> {
                String relative = Paths.get(storagePath).relativize(p).toString().replace("\\", "/");
                if (p.getFileName().toString().equals(expectedGltfName)) {
                    result.put("gltfKey", relative);
                } else {
                    result.put("iconKey", relative);
                }
            });
        } catch (IOException e) {
            log.error("Failed to list GLTF keys for truckModel={}", truckModel, e);
        }
        return result;
    }

    // ── EndDevice (per-device slot at cdn/v1/{truckModel}/enddevices/) ──

    private String endDeviceDirLocal(String truckModel) {
        return "cdn/v1/" + truckModel + "/enddevices";
    }

    @Override
    public void uploadEndDeviceFile(String truckModel, MultipartFile file) throws Exception {
        Path dir = Paths.get(storagePath, endDeviceDirLocal(truckModel));
        Files.createDirectories(dir);

        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
        Path dest = dir.resolve(originalName);

        // Replace if already exists
        Files.deleteIfExists(dest);

        Files.write(dest, file.getBytes());
        log.info("EndDevice file uploaded to local storage: {}", dest);
    }

    @Override
    public List<String> getEndDeviceFiles(String truckModel) {
        Path dir = Paths.get(storagePath, endDeviceDirLocal(truckModel));
        if (!Files.exists(dir)) {
            return new ArrayList<>();
        }

        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .map(p -> Paths.get(storagePath).relativize(p).toString().replace("\\", "/"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list EndDevice files for truckModel={}", truckModel, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void deleteEndDeviceFile(String truckModel, String fileName) throws Exception {
        Path filePath = Paths.get(storagePath, endDeviceDirLocal(truckModel), fileName);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("EndDevice file deleted from local storage: {}", filePath);
        } else {
            log.warn("EndDevice file NOT found for deletion: {}", filePath);
            throw new IOException("File not found: " + fileName);
        }
    }

    // ── FaultCodes (per-truck multi-file slot at cdn/v1/{truckModel}/faultcodes/)
    // ──

    private String getFaultCodeDirLocal(String truckModel) {
        return "cdn/v1/" + truckModel + "/faultcodes";
    }

    @Override
    public void uploadFaultCodeFile(String truckModel, MultipartFile file) throws Exception {
        Path dir = Paths.get(storagePath, getFaultCodeDirLocal(truckModel));
        Files.createDirectories(dir);

        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
        Path dest = dir.resolve(originalName);

        // Delete if already exists to ensure replacement if filenames match
        Files.deleteIfExists(dest);

        Files.write(dest, file.getBytes());
        log.info("FaultCode file uploaded to local storage: {}", dest);
    }

    @Override
    public List<String> getFaultCodeFiles(String truckModel) {
        Path dir = Paths.get(storagePath, getFaultCodeDirLocal(truckModel));
        if (!Files.exists(dir)) {
            return new ArrayList<>();
        }

        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .map(p -> Paths.get(storagePath).relativize(p).toString().replace("\\", "/"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list FaultCode files for truckModel={}", truckModel, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> listTruckFiles(String truckModel) {
        Path truckDir = Paths.get(storagePath, "cdn/v1/" + truckModel);
        if (!Files.exists(truckDir)) {
            return new ArrayList<>();
        }
        try (Stream<Path> paths = Files.walk(truckDir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(p -> Paths.get(storagePath).relativize(p).toString().replace("\\", "/"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list truck files for truckModel={}", truckModel, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void deleteFaultCodeFile(String truckModel, String fileName) throws Exception {
        Path filePath = Paths.get(storagePath, getFaultCodeDirLocal(truckModel), fileName);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("FaultCode file deleted from local storage: {}", filePath);
        } else {
            log.warn("FaultCode file NOT found for deletion: {}", filePath);
            throw new IOException("File not found: " + fileName);
        }
    }

    // ── Recursive delete helper ──────────────────────────────────────────────

    private void deleteDirectoryRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("Could not delete path: {}", p, e);
                }
            });
        }
    }

    // ── Truck model and harness listing ──────────────────────────────────────

    @Override
    public List<String> listTruckModels() {
        Path cdnRoot = Paths.get(storagePath, "cdn/v1");
        if (!Files.exists(cdnRoot)) {
            return new ArrayList<>();
        }
        try (Stream<Path> dirs = Files.list(cdnRoot)) {
            return dirs.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list truck models", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> listHarnessIds(String truckModel) {
        Path harnessesDir = Paths.get(storagePath, "cdn/v1", truckModel, "harnesses");
        if (!Files.exists(harnessesDir)) {
            return new ArrayList<>();
        }
        try (Stream<Path> dirs = Files.list(harnessesDir)) {
            return dirs.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list harness IDs for truckModel={}", truckModel, e);
            return new ArrayList<>();
        }
    }

    // ── Bulk deletions ────────────────────────────────────────────────────────

    @Override
    public void deleteTruck(String truckModel) throws Exception {
        Path truckDir = Paths.get(storagePath, "cdn/v1", truckModel);
        deleteDirectoryRecursively(truckDir);
        log.info("Truck deleted from local storage: {}", truckDir);
    }

    @Override
    public void deleteHarness(String truckModel, String harnessId) throws Exception {
        Path harnessDir = Paths.get(storagePath, "cdn/v1", truckModel, "harnesses", harnessId);
        deleteDirectoryRecursively(harnessDir);
        log.info("Harness deleted from local storage: {}", harnessDir);
    }

    @Override
    public void deleteHarnessFile(String truckModel, String harnessId,
            String category, String fileName) throws Exception {
        Path filePath = Paths.get(storagePath, "cdn/v1", truckModel,
                "harnesses", harnessId, category, fileName);
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + fileName);
        }
        Files.delete(filePath);
        log.info("Harness file deleted from local storage: {}", filePath);
    }

    @Override
    public void deleteGltfFolder(String truckModel) throws Exception {
        Path gltfDir = Paths.get(storagePath, gltfDirLocal(truckModel));
        deleteDirectoryRecursively(gltfDir);
        log.info("GLTF folder deleted from local storage: {}", gltfDir);
    }

    @Override
    public void deleteAllFaultCodeFiles(String truckModel) throws Exception {
        Path dir = Paths.get(storagePath, getFaultCodeDirLocal(truckModel));
        deleteDirectoryRecursively(dir);
        log.info("All faultcodes deleted for truckModel={}", truckModel);
    }

    @Override
    public void deleteAllEndDeviceFiles(String truckModel) throws Exception {
        Path dir = Paths.get(storagePath, endDeviceDirLocal(truckModel));
        deleteDirectoryRecursively(dir);
        log.info("All end devices deleted for truckModel={}", truckModel);
    }

    // ── WorkshopManual (per-truck multi-file slot at cdn/v1/{truckModel}/workshopmanual/) ──

    private String getWorkshopManualDirLocal(String truckModel) {
        return "cdn/v1/" + truckModel + "/workshopmanual";
    }

    @Override
    public void uploadWorkshopManualFile(String truckModel, MultipartFile file) throws Exception {
        Path dir = Paths.get(storagePath, getWorkshopManualDirLocal(truckModel));
        Files.createDirectories(dir);

        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
        Path dest = dir.resolve(originalName);

        Files.deleteIfExists(dest);
        Files.write(dest, file.getBytes());
        log.info("WorkshopManual file uploaded to local storage: {}", dest);
    }

    @Override
    public List<String> getWorkshopManualFiles(String truckModel) {
        Path dir = Paths.get(storagePath, getWorkshopManualDirLocal(truckModel));
        if (!Files.exists(dir)) {
            return new ArrayList<>();
        }

        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .map(p -> Paths.get(storagePath).relativize(p).toString().replace("\\", "/"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list WorkshopManual files for truckModel={}", truckModel, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void deleteWorkshopManualFile(String truckModel, String fileName) throws Exception {
        Path filePath = Paths.get(storagePath, getWorkshopManualDirLocal(truckModel), fileName);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("WorkshopManual file deleted from local storage: {}", filePath);
        } else {
            log.warn("WorkshopManual file NOT found for deletion: {}", filePath);
            throw new IOException("File not found: " + fileName);
        }
    }

    @Override
    public void deleteAllWorkshopManualFiles(String truckModel) throws Exception {
        Path dir = Paths.get(storagePath, getWorkshopManualDirLocal(truckModel));
        deleteDirectoryRecursively(dir);
        log.info("All workshop manuals deleted for truckModel={}", truckModel);
    }
}
