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

/**
 * EndDevice File Management API
 *
 * Base path: /enddevices/{truckModel}/{enddeviceId}
 *
 * Endpoints:
 * POST /enddevices/{truckModel}/{enddeviceId}/upload — Upload a file for the
 * end device.
 * File is replaced if it already exists in the folder. Only one file is
 * maintained.
 *
 * GET /enddevices/{truckModel}/{enddeviceId}/info — Return S3 key and public
 * URL for the file.
 */
@RestController
@RequestMapping("/api/v1/enddevices")
@Log4j2
@Tag(name = "EndDevices", description = "Endpoints for managing file uploads for end devices")
public class EndDeviceController {

        private final S3ServiceBase storage;

        public EndDeviceController(S3ServiceBase storage) {
                this.storage = storage;
        }

        // ── POST /enddevices/{truckModel}/upload ────────────────────────

        @PostMapping(value = "/{truckModel}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Upload files for an end device", description = "Uploads multiple files for the given truck model. "
                        + "Files are stored at cdn/v1/{truckModel}/enddevices/{originalFilename}.", responses = {
                                        @ApiResponse(responseCode = "200", description = "Files uploaded successfully"),
                                        @ApiResponse(responseCode = "400", description = "No file provided"),
                                        @ApiResponse(responseCode = "500", description = "Upload failed")
                        })
        public ResponseEntity<Map<String, Object>> uploadFiles(
                        @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel,
                        @RequestParam("files") List<MultipartFile> files) {

                if (files == null || files.isEmpty()) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("success", false, "message",
                                                        "At least one file must be provided"));
                }

                try {
                        for (MultipartFile file : files) {
                                if (file != null && !file.isEmpty()) {
                                        storage.uploadEndDeviceFile(truckModel, file);
                                }
                        }

                        List<String> fileKeys = storage.getEndDeviceFiles(truckModel);
                        List<Map<String, String>> fileDetails = fileKeys.stream()
                                        .map(key -> Map.of(
                                                        "fileKey", key,
                                                        "fileUrl", storage.getPublicUrl(key)))
                                        .collect(Collectors.toList());

                        log.info("EndDevice files uploaded for truckModel={}, count={}",
                                        truckModel, files.size());

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "truckModel", truckModel,
                                        "files", fileDetails));

                } catch (Exception e) {
                        log.error("Failed to upload EndDevice files for truckModel={}",
                                        truckModel, e);
                        return ResponseEntity.internalServerError()
                                        .body(Map.of("success", false, "message", "Upload failed: " + e.getMessage()));
                }
        }

        // ── GET /enddevices/{truckModel}/info ───────────────────────────

        @GetMapping("/{truckModel}/info")
        @Operation(summary = "Get EndDevice files info", description = "Returns the list of storage keys and public URLs for all files "
                        + "associated with the given truck model.", responses = {
                                        @ApiResponse(responseCode = "200", description = "Info returned")
                        })
        public ResponseEntity<Map<String, Object>> getInfo(
                        @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel) {

                List<String> fileKeys = storage.getEndDeviceFiles(truckModel);
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

        // ── DELETE /enddevices/{truckModel} ────────────────────────────

        @DeleteMapping("/{truckModel}")
        @Operation(summary = "Delete ALL end device files for a truck model", description = "Deletes every file under cdn/v1/{truckModel}/enddevices/ in a single operation.", responses = {
                        @ApiResponse(responseCode = "200", description = "All end device files deleted"),
                        @ApiResponse(responseCode = "500", description = "Deletion failed")
        })
        public ResponseEntity<Map<String, Object>> deleteAllEndDeviceFiles(
                        @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel) {
                try {
                        storage.deleteAllEndDeviceFiles(truckModel);
                        log.info("All EndDevice files deleted for truckModel={}", truckModel);
                        return ResponseEntity.ok(Map.of("success", true,
                                        "message", "All end device files deleted for: " + truckModel));
                } catch (Exception e) {
                        log.error("Failed to delete all EndDevice files for truckModel={}", truckModel, e);
                        return ResponseEntity.internalServerError()
                                        .body(Map.of("success", false, "message",
                                                        "Deletion failed: " + e.getMessage()));
                }
        }

        // ── DELETE /enddevices/{truckModel}/files/{fileName} ───────────

        @DeleteMapping("/{truckModel}/files/{fileName}")
        @Operation(summary = "Delete an EndDevice file", description = "Deletes a specific file for the given truck model.", responses = {
                        @ApiResponse(responseCode = "200", description = "File deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "File not found"),
                        @ApiResponse(description = "Deletion failed")
        })
        public ResponseEntity<Map<String, Object>> deleteFile(
                        @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel,
                        @Parameter(name = "fileName", description = "Name of the file to delete") @PathVariable("fileName") String fileName) {
                try {
                        storage.deleteEndDeviceFile(truckModel, fileName);
                        log.info("EndDevice file deleted: truckModel={}, fileName={}", truckModel, fileName);
                        return ResponseEntity.ok(Map.of("success", true, "message", "File deleted successfully"));
                } catch (Exception e) {
                        log.error("Failed to delete EndDevice file: truckModel={}, fileName={}", truckModel, fileName,
                                        e);
                        return ResponseEntity.internalServerError()
                                        .body(Map.of("success", false, "message",
                                                        "Deletion failed: " + e.getMessage()));
                }
        }
}
