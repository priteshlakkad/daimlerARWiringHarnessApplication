package com.harness.service;

import com.harness.dtos.UploadedFileResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;
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
            String key = String.format("cdn/v1/%s/harnesses/%s/info/%s_info.pdf", truckModel, harnessId, harnessId);
            s3.upload(key, request.getInfo());
            uploadedKeys.add(key);
        }

        // 2. Troubleshooting PDF
        if (request.getTroubleshooting() != null) {
            String key = String.format("cdn/v1/%s/harnesses/%s/troubleshooting/%s_troubleshooting.pdf", truckModel,
                    harnessId, harnessId);
            s3.upload(key, request.getTroubleshooting());
            uploadedKeys.add(key);
        }

        // 3. Wiring PDF
        if (request.getWiring() != null) {
            String key = String.format("cdn/v1/%s/harnesses/%s/wiring/%s_wiring.pdf", truckModel, harnessId, harnessId);
            s3.upload(key, request.getWiring());
            uploadedKeys.add(key);
        }

        // 4. Powerflow video
        if (request.getPowerFlow() != null) {
            String key = String.format("cdn/v1/%s/harnesses/%s/powerflow/%s_powerflow.mp4", truckModel, harnessId,
                    harnessId);
            s3.upload(key, request.getPowerFlow());
            uploadedKeys.add(key);
        }

        // 5. Repair video
        if (request.getRepair() != null) {
            String key = String.format("cdn/v1/%s/harnesses/%s/repair/%s_repair.mp4", truckModel, harnessId, harnessId);
            s3.upload(key, request.getRepair());
            uploadedKeys.add(key);
        }

        // 6. Enddevices JSON
        if (request.getEndDevices() != null) {
            String key = String.format("cdn/v1/%s/harnesses/%s/enddevices.json", truckModel, harnessId);
            s3.upload(key, request.getEndDevices());
            uploadedKeys.add(key);
        }

        // 7. iOS Unity bundle
        if (request.getIosBundle() != null) {
            String key = String.format("cdn/v1/%s/harnesses/%s/bundles/ios/%s.bundle", truckModel, harnessId,
                    harnessId);
            s3.upload(key, request.getIosBundle());
            uploadedKeys.add(key);
        }

        // 8. Knowledge PDFs (optional)
        if (request.getKnowledge() != null) {
            for (MultipartFile file : request.getKnowledge()) {
                String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("knowledge.pdf");
                String purpose = originalName.contains(".") ? originalName.substring(0, originalName.lastIndexOf('.'))
                        : originalName;
                String key = String.format("cdn/v1/%s/harnesses/%s/knowledge/%s_%s.pdf", truckModel, harnessId,
                        harnessId, purpose);
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
}
