package com.harness.controller;

import com.harness.service.HarnessService;
import com.harness.dtos.UploadHarnessRequest;
import com.harness.dtos.UploadHarnessResponse;
import com.harness.dtos.UploadedFileResponse;
import com.harness.dtos.GetHarnessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Harness File Management API Controller
 *
 * This controller provides endpoints for uploading and retrieving harness files.
 * The underlying storage backend is determined by the 'app.use-s3' property:
 *
 * - app.use-s3=true  → Files stored in AWS S3 bucket
 * - app.use-s3=false → Files stored locally on the filesystem (default)
 *
 * This design allows the same API endpoints to work seamlessly with different
 * storage backends without needing duplicate controllers or endpoints.
 */
@RestController
@RequestMapping("/harnesses")
public class HarnessController {

    @Autowired
    HarnessService harnessService;

    @PostMapping(value = "/{harnessId}", consumes = "multipart/form-data")
    @Operation(
            summary = "Upload harness files",
            description = "Uploads PDFs, videos, JSON and bundle files for a harness",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Files uploaded successfully"),
                    @ApiResponse(responseCode = "500", description = "Upload failed")
            }
    )
    public ResponseEntity<UploadHarnessResponse> upload(
            @Parameter(description = "Harness ID", example = "HARN123")
            @PathVariable String harnessId,
            @ModelAttribute UploadHarnessRequest request
    ) {
        try {
            List<String> uploadedKeys = harnessService.uploadFiles(harnessId, request);
            return ResponseEntity.ok(new UploadHarnessResponse(harnessId, uploadedKeys));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new UploadHarnessResponse(harnessId, List.of()));
        }
    }



    @GetMapping("/{harnessId}")
    @Operation(
            summary = "Get harness files",
            description = "Returns all uploaded harness files with presigned URLs"
    )
    public ResponseEntity<GetHarnessResponse> getHarness(
            @PathVariable String harnessId,
            @RequestParam(defaultValue = "900") int ttlSeconds
    ) {
        List<UploadedFileResponse> files = harnessService.getHarnessFiles(harnessId, ttlSeconds);
        return ResponseEntity.ok(new GetHarnessResponse(harnessId, files));
    }
}
