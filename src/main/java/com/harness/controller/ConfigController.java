package com.harness.controller;

import com.harness.service.S3ServiceBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/config")
@Log4j2
@Tag(name = "Config", description = "Endpoints for managing config folder files per truck model")
public class ConfigController {

    private final S3ServiceBase storage;

    public ConfigController(S3ServiceBase storage) {
        this.storage = storage;
    }

    // ── POST /config/{truckModel}/upload ────────────────────────────────

    @PostMapping(value = "/{truckModel}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload config files for a truck model",
            description = "Uploads multiple files into cdn/v1/{truckModel}/config/.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Files uploaded successfully"),
                    @ApiResponse(responseCode = "400", description = "No file provided"),
                    @ApiResponse(responseCode = "500", description = "Upload failed")
            })
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel,
            @RequestParam("files") List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "At least one file must be provided"));
        }

        try {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    storage.uploadConfigFile(truckModel, file);
                }
            }

            List<String> fileKeys = storage.getConfigFiles(truckModel);
            List<Map<String, String>> fileDetails = fileKeys.stream()
                    .map(key -> Map.of(
                            "fileKey", key,
                            "fileUrl", storage.getPublicUrl(key)))
                    .collect(Collectors.toList());

            log.info("Config files uploaded for truckModel={}, count={}", truckModel, files.size());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "truckModel", truckModel,
                    "files", fileDetails));

        } catch (Exception e) {
            log.error("Failed to upload config files for truckModel={}", truckModel, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Upload failed: " + e.getMessage()));
        }
    }

    // ── GET /config/{truckModel}/info ────────────────────────────────────

    @GetMapping("/{truckModel}/info")
    @Operation(summary = "Get config files info",
            description = "Returns the list of storage keys and public URLs for all config files of the given truck model.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Info returned")
            })
    public ResponseEntity<Map<String, Object>> getInfo(
            @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel) {

        List<String> fileKeys = storage.getConfigFiles(truckModel);
        List<Map<String, String>> fileDetails = fileKeys.stream()
                .map(key -> Map.of(
                        "fileKey", key,
                        "fileUrl", storage.getPublicUrl(key)))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "truckModel", truckModel,
                "files", fileDetails));
    }

    // ── DELETE /config/{truckModel}/files/{fileName} ─────────────────────

    @DeleteMapping("/{truckModel}/files/{fileName}")
    @Operation(summary = "Delete a specific config file",
            description = "Deletes a single file at cdn/v1/{truckModel}/config/{fileName}.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "File deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "File not found"),
                    @ApiResponse(responseCode = "500", description = "Deletion failed")
            })
    public ResponseEntity<Map<String, Object>> deleteFile(
            @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel,
            @Parameter(name = "fileName", description = "Name of the file to delete") @PathVariable("fileName") String fileName) {
        try {
            storage.deleteConfigFile(truckModel, fileName);
            log.info("Config file deleted: truckModel={}, fileName={}", truckModel, fileName);
            return ResponseEntity.ok(Map.of("success", true, "message", "File deleted successfully"));
        } catch (Exception e) {
            log.error("Failed to delete config file: truckModel={}, fileName={}", truckModel, fileName, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Deletion failed: " + e.getMessage()));
        }
    }

    // ── DELETE /config/{truckModel} ──────────────────────────────────────

    @DeleteMapping("/{truckModel}")
    @Operation(summary = "Delete ALL config files for a truck model",
            description = "Deletes every file under cdn/v1/{truckModel}/config/.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "All config files deleted"),
                    @ApiResponse(responseCode = "500", description = "Deletion failed")
            })
    public ResponseEntity<Map<String, Object>> deleteAllConfigFiles(
            @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel) {
        try {
            storage.deleteAllConfigFiles(truckModel);
            log.info("All config files deleted for truckModel={}", truckModel);
            return ResponseEntity.ok(Map.of("success", true,
                    "message", "All config files deleted for: " + truckModel));
        } catch (Exception e) {
            log.error("Failed to delete all config files for truckModel={}", truckModel, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Deletion failed: " + e.getMessage()));
        }
    }
}
