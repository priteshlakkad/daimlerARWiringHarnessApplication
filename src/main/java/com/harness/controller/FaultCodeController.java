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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FaultCodes File Management API
 *
 * Base path: /cdn/v1/{truckModel}/faultcodes
 *
 * Endpoints:
 * POST  /{truckModel}/faultcodes/{faultcodeId}/upload   — Upload file(s) under a faultcode ID.
 * GET   /{truckModel}/faultcodes                        — List ALL faultcode files (backward compatible).
 * GET   /{truckModel}/faultcodes/{faultcodeId}          — List files for a specific faultcode ID.
 * DELETE /{truckModel}/faultcodes                       — Delete all faultcode files.
 * DELETE /{truckModel}/faultcodes/{faultcodeId}         — Delete a faultcode ID folder.
 * DELETE /{truckModel}/faultcodes/{faultcodeId}/{fileName} — Delete a specific file.
 * PUT   /{truckModel}/faultcodes/{faultcodeId}/{fileName}  — Replace/update a specific file.
 */
@RestController
@RequestMapping("/api/v1/cdn/v1")
@Log4j2
@Tag(name = "FaultCodes", description = "Endpoints for managing faultcode file uploads")
public class FaultCodeController {

    private final S3ServiceBase storage;

    public FaultCodeController(S3ServiceBase storage) {
        this.storage = storage;
    }

    @PostMapping(value = "/{truckModel}/faultcodes/{faultcodeId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload faultcode files", description = "Uploads multiple files for the given truck model and faultcode ID.", responses = {
            @ApiResponse(responseCode = "200", description = "Files uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "No files provided"),
            @ApiResponse(responseCode = "500", description = "Upload failed")
    })
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel,
            @Parameter(name = "faultcodeId", description = "Fault code identifier") @PathVariable("faultcodeId") String faultcodeId,
            @RequestParam("files") List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "At least one file must be provided"));
        }

        try {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    storage.uploadFaultCodeFile(truckModel, faultcodeId, file);
                }
            }

            List<String> fileKeys = storage.getFaultCodeFilesByFaultcodeId(truckModel, faultcodeId);
            List<Map<String, String>> fileDetails = toFileDetails(fileKeys);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("truckModel", truckModel);
            response.put("faultcodeId", faultcodeId);
            response.put("files", fileDetails);
            response.put("count", fileDetails.size());

            log.info("FaultCode files uploaded for truckModel={}, faultcodeId={}, count={}", truckModel, faultcodeId, files.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to upload FaultCode files for truckModel={}, faultcodeId={}", truckModel, faultcodeId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{truckModel}/faultcodes")
    @Operation(summary = "List all faultcode files", description = "Returns all files under /faultcodes for the given truck model (backward compatible — includes legacy flat files and new nested files).", responses = {
            @ApiResponse(responseCode = "200", description = "List of files returned")
    })
    public ResponseEntity<Map<String, Object>> listFiles(
            @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel) {

        List<String> fileKeys = storage.getFaultCodeFiles(truckModel);
        List<Map<String, String>> files = toFileDetails(fileKeys);

        Map<String, Object> response = new HashMap<>();
        response.put("truckModel", truckModel);
        response.put("files", files);
        response.put("count", files.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{truckModel}/faultcodes/{faultcodeId}")
    @Operation(summary = "List files for a faultcode ID", description = "Returns all files stored under the given faultcode ID.", responses = {
            @ApiResponse(responseCode = "200", description = "List of files returned")
    })
    public ResponseEntity<Map<String, Object>> listFilesByFaultcodeId(
            @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel,
            @Parameter(name = "faultcodeId", description = "Fault code identifier") @PathVariable("faultcodeId") String faultcodeId) {

        List<String> fileKeys = storage.getFaultCodeFilesByFaultcodeId(truckModel, faultcodeId);
        List<Map<String, String>> files = toFileDetails(fileKeys);

        Map<String, Object> response = new HashMap<>();
        response.put("truckModel", truckModel);
        response.put("faultcodeId", faultcodeId);
        response.put("files", files);
        response.put("count", files.size());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{truckModel}/faultcodes")
    @Operation(summary = "Delete ALL faultcode files for a truck model", description = "Deletes every file under cdn/v1/{truckModel}/faultcodes/ in a single operation.", responses = {
            @ApiResponse(responseCode = "200", description = "All faultcodes deleted"),
            @ApiResponse(responseCode = "500", description = "Deletion failed")
    })
    public ResponseEntity<Map<String, Object>> deleteAllFaultCodeFiles(
            @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel) {
        try {
            storage.deleteAllFaultCodeFiles(truckModel);
            log.info("All faultcode files deleted for truckModel={}", truckModel);
            return ResponseEntity.ok(Map.of("success", true,
                    "message", "All faultcode files deleted for: " + truckModel));
        } catch (Exception e) {
            log.error("Failed to delete all faultcode files for truckModel={}", truckModel, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Deletion failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{truckModel}/faultcodes/{faultcodeId}")
    @Operation(summary = "Delete a faultcode ID folder", description = "Deletes all files under cdn/v1/{truckModel}/faultcodes/{faultcodeId}/.", responses = {
            @ApiResponse(responseCode = "200", description = "Faultcode folder deleted"),
            @ApiResponse(responseCode = "500", description = "Deletion failed")
    })
    public ResponseEntity<Map<String, Object>> deleteFaultCodeFolder(
            @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel,
            @Parameter(name = "faultcodeId", description = "Fault code identifier") @PathVariable("faultcodeId") String faultcodeId) {
        try {
            storage.deleteFaultCodeFolder(truckModel, faultcodeId);
            log.info("FaultCode folder deleted for truckModel={}, faultcodeId={}", truckModel, faultcodeId);
            return ResponseEntity.ok(Map.of("success", true,
                    "message", "FaultCode folder deleted: " + faultcodeId));
        } catch (Exception e) {
            log.error("Failed to delete faultcode folder for truckModel={}, faultcodeId={}", truckModel, faultcodeId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Deletion failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{truckModel}/faultcodes/{faultcodeId}/{fileName}")
    @Operation(summary = "Delete a faultcode file", description = "Deletes the specified file from cdn/v1/{truckModel}/faultcodes/{faultcodeId}/.", responses = {
            @ApiResponse(responseCode = "200", description = "File deleted successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Deletion failed")
    })
    public ResponseEntity<Map<String, Object>> deleteFile(
            @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel,
            @Parameter(name = "faultcodeId", description = "Fault code identifier") @PathVariable("faultcodeId") String faultcodeId,
            @Parameter(name = "fileName", description = "Name of the file to delete") @PathVariable("fileName") String fileName) {

        try {
            storage.deleteFaultCodeFile(truckModel, faultcodeId, fileName);
            log.info("FaultCode file deleted for truckModel={}, faultcodeId={}, fileName={}", truckModel, faultcodeId, fileName);
            return ResponseEntity.ok(Map.of("success", true, "message", "File deleted successfully"));
        } catch (java.io.IOException e) {
            log.warn("FaultCode file not found: truckModel={}, faultcodeId={}, fileName={}", truckModel, faultcodeId, fileName);
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete FaultCode file for truckModel={}, faultcodeId={}, fileName={}", truckModel, faultcodeId, fileName, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Deletion failed: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/{truckModel}/faultcodes/{faultcodeId}/{fileName}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update a faultcode file", description = "Replaces the specified file at cdn/v1/{truckModel}/faultcodes/{faultcodeId}/{fileName}.", responses = {
            @ApiResponse(responseCode = "200", description = "File updated successfully"),
            @ApiResponse(responseCode = "400", description = "No file provided"),
            @ApiResponse(responseCode = "500", description = "Update failed")
    })
    public ResponseEntity<Map<String, Object>> updateFile(
            @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel,
            @Parameter(name = "faultcodeId", description = "Fault code identifier") @PathVariable("faultcodeId") String faultcodeId,
            @Parameter(name = "fileName", description = "Name of the file to replace") @PathVariable("fileName") String fileName,
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "No file provided"));
        }

        try {
            String key = "cdn/v1/" + truckModel + "/faultcodes/" + faultcodeId + "/" + fileName;
            storage.upload(key, file);
            log.info("FaultCode file updated for truckModel={}, faultcodeId={}, fileName={}", truckModel, faultcodeId, fileName);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "truckModel", truckModel,
                    "faultcodeId", faultcodeId,
                    "fileName", fileName,
                    "key", key,
                    "url", storage.getPublicUrl(key)));
        } catch (Exception e) {
            log.error("Failed to update FaultCode file for truckModel={}, faultcodeId={}, fileName={}", truckModel, faultcodeId, fileName, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Update failed: " + e.getMessage()));
        }
    }

    private List<Map<String, String>> toFileDetails(List<String> keys) {
        return keys.stream().map(key -> {
            Map<String, String> info = new HashMap<>();
            info.put("key", key);
            info.put("url", storage.getPublicUrl(key));
            info.put("fileName", key.contains("/") ? key.substring(key.lastIndexOf("/") + 1) : key);
            return info;
        }).collect(Collectors.toList());
    }
}
