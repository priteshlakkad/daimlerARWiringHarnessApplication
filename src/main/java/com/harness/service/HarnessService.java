package com.harness.service;

import com.harness.dtos.HarnessValidationResult;
import com.harness.dtos.TruckFilesResponse;
import com.harness.dtos.UploadedFileResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HarnessService {

    private final S3ServiceBase s3;

    /**
     * The storage backend is injected based on application configuration:
     * - If app.use-s3=true: S3Service bean is injected (AWS S3)
     * - If app.use-s3=false: LocalStorageService bean is injected (local
     * filesystem)
     *
     * This allows the same API and service logic to work with different storage
     * backends without creating duplicate controllers or services.
     */
    public HarnessService(S3ServiceBase s3) {
        this.s3 = s3;
    }

    public List<UploadedFileResponse> uploadFiles(String truckModel, String harnessId,
            com.harness.dtos.UploadHarnessRequest request)
            throws Exception {
        List<String> uploadedKeys = new ArrayList<>();

        // 1. Info PDF
        if (request.getInfo() != null && !request.getInfo().isEmpty()) {
            String key = String.format("cdn/v1/%s/harnesses/%s/info/%s_info.%s", truckModel, harnessId, harnessId,
                    getExtension(request.getInfo()));
            s3.upload(key, request.getInfo());
            uploadedKeys.add(key);
        }

        // 2. Troubleshooting PDF
        if (request.getTroubleshooting() != null) {
            String key = String.format("cdn/v1/%s/harnesses/%s/troubleshooting/%s_troubleshooting.%s", truckModel,
                    harnessId, harnessId, getExtension(request.getTroubleshooting()));
            s3.upload(key, request.getTroubleshooting());
            uploadedKeys.add(key);
        }

        // 3. Wiring PDF
        if (request.getWiring() != null) {
            String key = String.format("cdn/v1/%s/harnesses/%s/wiring/%s_wiring.%s", truckModel, harnessId, harnessId,
                    getExtension(request.getWiring()));
            s3.upload(key, request.getWiring());
            uploadedKeys.add(key);
        }

        // 4. Powerflow video
        if (request.getPowerFlow() != null) {
            String key = String.format("cdn/v1/%s/harnesses/%s/powerflow/%s_powerflow.%s", truckModel, harnessId,
                    harnessId, getExtension(request.getPowerFlow()));
            s3.upload(key, request.getPowerFlow());
            uploadedKeys.add(key);
        }

        // 5. Repair video
        if (request.getRepair() != null) {
            String key = String.format("cdn/v1/%s/harnesses/%s/repair/%s_repair.%s", truckModel, harnessId, harnessId,
                    getExtension(request.getRepair()));
            s3.upload(key, request.getRepair());
            uploadedKeys.add(key);
        }

        // 6. Enddevices JSON
        if (request.getEndDevices() != null) {
            String key = String.format("cdn/v1/%s/harnesses/%s/enddevices.%s", truckModel, harnessId,
                    getExtension(request.getEndDevices()));
            s3.upload(key, request.getEndDevices());
            uploadedKeys.add(key);
        }

        // 7. iOS Unity bundle
        if (request.getIosBundle() != null) {
            String key = String.format("cdn/v1/%s/harnesses/%s/bundles/ios/%s_ios.%s", truckModel, harnessId,
                    harnessId, getExtension(request.getIosBundle()));
            s3.upload(key, request.getIosBundle());
            uploadedKeys.add(key);
        }

        // 8. Knowledge PDFs (optional)
        if (request.getKnowledge() != null) {
            for (MultipartFile file : request.getKnowledge()) {
                String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("knowledge");
                String purpose = originalName.contains(".") ? originalName.substring(0, originalName.lastIndexOf('.'))
                        : originalName;
                String key = String.format("cdn/v1/%s/harnesses/%s/knowledge/%s_%s.%s", truckModel, harnessId,
                        harnessId, purpose, getExtension(file));
                s3.upload(key, file);
                uploadedKeys.add(key);
            }
        }

        return uploadedKeys.stream()
                .map(key -> new UploadedFileResponse(key, s3.getPublicUrl(key)))
                .collect(Collectors.toList());
    }

    /**
     * Get all files for a specific harness scoped under a specific truck model.
     */
    public List<UploadedFileResponse> getHarnessFiles(String truckModel, String harnessId, int ttlSeconds) {
        return s3.listKeys(truckModel, harnessId).stream()
                .map(key -> new UploadedFileResponse(key, s3.getPublicUrl(key)))
                .collect(Collectors.toList());
    }

    /**
     * Get all downloadable (troubleshooting + powerflow) files for a specific truck
     * model.
     */
    public List<UploadedFileResponse> getAllDownloadableFiles(String truckModel, int ttlSeconds) {
        return s3.listAllDownloadableKeys(truckModel).stream()
                .map(key -> new UploadedFileResponse(key, s3.getPublicUrl(key)))
                .collect(Collectors.toList());
    }

    /**
     * Get all downloadable (troubleshooting + powerflow) files across ALL truck
     * model folders — no truck ID required.
     */
    public List<UploadedFileResponse> getAllDownloadableFiles(int ttlSeconds) {
        return s3.listAllDownloadableKeys().stream()
                .map(key -> new UploadedFileResponse(key, s3.getPublicUrl(key)))
                .collect(Collectors.toList());
    }

    /**
     * Search for a harness ID across ALL truck model folders.
     * Returns files from the first truck model folder that contains the given
     * harness ID.
     */
    public List<UploadedFileResponse> findHarnessById(String harnessId, int ttlSeconds) {
        return s3.findKeysByHarnessId(harnessId).stream()
                .map(key -> new UploadedFileResponse(key, s3.getPublicUrl(key)))
                .collect(Collectors.toList());
    }

    /**
     * Return ALL files stored across every truck model folder (no filter).
     */
    public List<UploadedFileResponse> getAllFiles(int ttlSeconds) {
        return s3.listAllFiles().stream()
                .map(key -> new UploadedFileResponse(key, s3.getPublicUrl(key)))
                .collect(Collectors.toList());
    }

    /**
     * Return a nested folder/file tree for the given truck model.
     * Each leaf node is the presigned/public URL of the file.
     * Returns null if no files exist (truck not found).
     */
    public TruckFilesResponse getTruckFiles(String truckModel, int ttlSeconds) {
        List<String> keys = s3.listTruckFiles(truckModel);
        if (keys.isEmpty()) {
            return null;
        }

        String prefix = "cdn/v1/" + truckModel + "/";
        Map<String, Object> tree = new LinkedHashMap<>();

        for (String key : keys) {
            // Ignore directory markers (0-byte objects in S3 ending with '/')
            if (key.endsWith("/")) {
                continue;
            }

            String relative = key.startsWith(prefix) ? key.substring(prefix.length()) : key;
            if (relative.isEmpty())
                continue;

            String url = s3.getPublicUrl(key);
            String[] parts = relative.split("/");
            insertIntoTree(tree, parts, 0, url);
        }

        return new TruckFilesResponse(truckModel, tree);
    }

    // ── New management methods ────────────────────────────────────────────────

    public List<String> listTruckModels() {
        return s3.listTruckModels();
    }

    public List<String> listHarnessIds(String truckModel) {
        return s3.listHarnessIds(truckModel);
    }

    public void deleteTruck(String truckModel) throws Exception {
        s3.deleteTruck(truckModel);
    }

    public void deleteHarness(String truckModel, String harnessId) throws Exception {
        s3.deleteHarness(truckModel, harnessId);
    }

    public void deleteHarnessFile(String truckModel, String harnessId,
            String category, String fileName) throws Exception {
        s3.deleteHarnessFile(truckModel, harnessId, category, fileName);
    }

    public UploadedFileResponse replaceHarnessFile(String truckModel, String harnessId,
            String category, MultipartFile file) throws Exception {
        String key = buildHarnessFileKey(truckModel, harnessId, category,
                Optional.ofNullable(file.getOriginalFilename()).orElse("file"));
        s3.upload(key, file);
        return new UploadedFileResponse(key, s3.getPublicUrl(key));
    }

    private String getExtension(MultipartFile file) {
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        return name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "bin";
    }

    private String buildHarnessFileKey(String truckModel, String harnessId,
            String category, String originalFilename) {
        String ext = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                : "bin";
        return switch (category.toLowerCase()) {
            case "info" -> String.format("cdn/v1/%s/harnesses/%s/info/%s_info.%s",
                    truckModel, harnessId, harnessId, ext);
            case "troubleshooting" -> String.format("cdn/v1/%s/harnesses/%s/troubleshooting/%s_troubleshooting.%s",
                    truckModel, harnessId, harnessId, ext);
            case "wiring" -> String.format("cdn/v1/%s/harnesses/%s/wiring/%s_wiring.%s",
                    truckModel, harnessId, harnessId, ext);
            case "powerflow" -> String.format("cdn/v1/%s/harnesses/%s/powerflow/%s_powerflow.%s",
                    truckModel, harnessId, harnessId, ext);
            case "repair" -> String.format("cdn/v1/%s/harnesses/%s/repair/%s_repair.%s",
                    truckModel, harnessId, harnessId, ext);
            default -> String.format("cdn/v1/%s/harnesses/%s/%s/%s",
                    truckModel, harnessId, category, originalFilename);
        };
    }

    public UploadedFileResponse upload3dModel(String truckModel, String harnessId, MultipartFile file)
            throws Exception {
        return s3.upload3dModel(truckModel, harnessId, file);
    }

    public HarnessValidationResult validateHarness(String truckModel, String harnessId) {
        List<String> existing = s3.listKeys(truckModel, harnessId);
        List<String> required = List.of(
                String.format("cdn/v1/%s/harnesses/%s/info/%s_info.pdf", truckModel, harnessId, harnessId),
                String.format("cdn/v1/%s/harnesses/%s/troubleshooting/%s_troubleshooting.pdf",
                        truckModel, harnessId, harnessId),
                String.format("cdn/v1/%s/harnesses/%s/wiring/%s_wiring.pdf", truckModel, harnessId, harnessId));
        List<String> missing = required.stream()
                .filter(req -> !existing.contains(req))
                .collect(Collectors.toList());
        return new HarnessValidationResult(truckModel, harnessId, missing.isEmpty(), missing);
    }

    @SuppressWarnings("unchecked")
    private void insertIntoTree(Map<String, Object> node, String[] parts, int index, String url) {
        String segment = parts[index];
        if (index == parts.length - 1) {
            node.put(segment, url);
        } else {
            node.computeIfAbsent(segment, k -> new LinkedHashMap<String, Object>());
            insertIntoTree((Map<String, Object>) node.get(segment), parts, index + 1, url);
        }
    }
}
