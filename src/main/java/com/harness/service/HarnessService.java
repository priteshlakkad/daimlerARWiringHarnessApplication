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

    private final S3ServiceBase  s3;

    public HarnessService(S3ServiceBase  s3) {
        this.s3 = s3;
    }

    public List<String> uploadFiles(String harnessId, com.harness.dtos.UploadHarnessRequest request) throws Exception {
        List<String> uploadedKeys = new ArrayList<>();

        // 1. Info PDF
        if (request.getInfo() != null && !request.getInfo().isEmpty()) {
            String key = String.format("cdn/v1/harnesses/%s/info/%s_info.pdf", harnessId, harnessId);
            s3.upload(key, request.getInfo());
            uploadedKeys.add(key);
        }

        // 2. Troubleshooting PDF
        if (request.getTroubleshooting() != null) {
            String key = String.format("cdn/v1/harnesses/%s/troubleshooting/%s_troubleshooting.pdf", harnessId, harnessId);
            s3.upload(key, request.getTroubleshooting());
            uploadedKeys.add(key);
        }

        // 3. Wiring PDF
        if (request.getWiring() != null) {
            String key = String.format("cdn/v1/harnesses/%s/wiring/%s_wiring.pdf", harnessId, harnessId);
            s3.upload(key, request.getWiring());
            uploadedKeys.add(key);
        }

        // 4. Powerflow video
        if (request.getPowerFlow() != null) {
            String key = String.format("cdn/v1/harnesses/%s/powerflow/%s_powerflow.mp4", harnessId, harnessId);
            s3.upload(key, request.getPowerFlow());
            uploadedKeys.add(key);
        }

        // 5. Repair video
        if (request.getRepair() != null) {
            String key = String.format("cdn/v1/harnesses/%s/repair/%s_repair.mp4", harnessId, harnessId);
            s3.upload(key, request.getRepair());
            uploadedKeys.add(key);
        }

        // 6. Enddevices JSON
        if (request.getEndDevices() != null) {
            String key = String.format("cdn/v1/harnesses/%s/enddevices.json", harnessId);
            s3.upload(key, request.getEndDevices());
            uploadedKeys.add(key);
        }

        // 7. iOS Unity bundle
        if (request.getIosBundle() != null) {
            String key = String.format("cdn/v1/harnesses/%s/bundles/ios/%s.bundle", harnessId, harnessId);
            s3.upload(key, request.getIosBundle());
            uploadedKeys.add(key);
        }

        // 8. Knowledge PDFs (optional)
        if (request.getKnowledge() != null) {
            for (MultipartFile file : request.getKnowledge()) {
                String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("knowledge.pdf");
                String purpose = originalName.contains(".") ? originalName.substring(0, originalName.lastIndexOf('.')) : originalName;
                String key = String.format("cdn/v1/harnesses/%s/knowledge/%s_%s.pdf", harnessId, harnessId, purpose);
                s3.upload(key, file);
                uploadedKeys.add(key);
            }
        }

        return uploadedKeys;
    }

    public List<UploadedFileResponse> getHarnessFiles(String harnessId, int ttlSeconds) {
        return s3.listKeys(harnessId).stream()
                .map(key -> new UploadedFileResponse(key, s3.presign(key, ttlSeconds)))
                .collect(Collectors.toList());
    }
}
