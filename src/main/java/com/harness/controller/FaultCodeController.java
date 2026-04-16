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
 * POST /upload — Upload a faultcode file.
 * GET / — List all files in the folder.
 * DELETE /{fileName} — Delete a particular file in that folder.
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

    @PostMapping(value = "/{truckModel}/faultcodes/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload faultcode files", description = "Uploads multiple files for the given truck model in the /faultcodes folder.", responses = {
            @ApiResponse(responseCode = "200", description = "Files uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "No files provided"),
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
                    storage.uploadFaultCodeFile(truckModel, file);
                }
            }

            List<String> fileKeys = storage.getFaultCodeFiles(truckModel);
            List<Map<String, String>> fileDetails = fileKeys.stream().map(key -> {
                Map<String, String> fileInfo = new HashMap<>();
                fileInfo.put("key", key);
                fileInfo.put("url", storage.getPublicUrl(key));
                String fileName = key.contains("/") ? key.substring(key.lastIndexOf("/") + 1) : key;
                fileInfo.put("fileName", fileName);
                return fileInfo;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("truckModel", truckModel);
            response.put("files", fileDetails);
            response.put("count", fileDetails.size());

            log.info("FaultCode files uploaded for truckModel={}, count={}", truckModel, files.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to upload FaultCode files for truckModel={}", truckModel, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{truckModel}/faultcodes")
    @Operation(summary = "List all faultcode files", description = "Returns a list of all files stored in the /faultcodes folder for the given truck model.", responses = {
            @ApiResponse(responseCode = "200", description = "List of files returned")
    })
    public ResponseEntity<Map<String, Object>> listFiles(
            @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel) {

        List<String> fileKeys = storage.getFaultCodeFiles(truckModel);

        List<Map<String, String>> files = fileKeys.stream().map(key -> {
            Map<String, String> fileInfo = new HashMap<>();
            fileInfo.put("key", key);
            fileInfo.put("url", storage.getPublicUrl(key));
            String fileName = key.contains("/") ? key.substring(key.lastIndexOf("/") + 1) : key;
            fileInfo.put("fileName", fileName);
            return fileInfo;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("truckModel", truckModel);
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

    @DeleteMapping("/{truckModel}/faultcodes/{fileName}")
    @Operation(summary = "Delete a faultcode file", description = "Deletes the specified file from the /faultcodes folder for the given truck model.", responses = {
            @ApiResponse(responseCode = "200", description = "File deleted successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Deletion failed")
    })
    public ResponseEntity<Map<String, Object>> deleteFile(
            @Parameter(name = "truckModel", description = "Truck model identifier") @PathVariable("truckModel") String truckModel,
            @Parameter(name = "fileName", description = "Name of the file to delete") @PathVariable("fileName") String fileName) {

        try {
            storage.deleteFaultCodeFile(truckModel, fileName);
            log.info("FaultCode file deleted for truckModel={}, fileName={}", truckModel, fileName);
            return ResponseEntity.ok(Map.of("success", true, "message", "File deleted successfully"));
        } catch (java.io.IOException e) {
            log.warn("FaultCode file not found for deletion: truckModel={}, fileName={}", truckModel, fileName);
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete FaultCode file for truckModel={}, fileName={}", truckModel, fileName, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Deletion failed: " + e.getMessage()));
        }
    }
}
