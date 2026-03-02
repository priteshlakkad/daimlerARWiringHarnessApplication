package com.harness.controller;

import com.harness.service.LocalStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/files")
@Log4j2
@ConditionalOnProperty(name = "app.use-s3", havingValue = "false", matchIfMissing = true)
public class FileDownloadController {

    @Autowired(required = false)
    private LocalStorageService localStorageService;

    @GetMapping("/download")
    @Operation(
            summary = "Download file from local storage",
            description = "Downloads a file that was uploaded to local storage",
            responses = {
                    @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
                    @ApiResponse(responseCode = "404", description = "File not found"),
                    @ApiResponse(responseCode = "500", description = "Download failed")
            }
    )
    public ResponseEntity<?> downloadFile(
            @Parameter(description = "File key/path", example = "cdn/v1/harnesses/HARN123/info/HARN123_info.pdf")
            @RequestParam String key
    ) {
        try {
            if (localStorageService == null) {
                return ResponseEntity.badRequest().body("Local storage service not available");
            }

            byte[] fileContent = localStorageService.downloadFile(key);
            String fileExtension = localStorageService.getFileExtension(key);
            MediaType mediaType = getMediaType(fileExtension);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(fileContent);
        } catch (IOException e) {
            log.error("Failed to download file with key={}", key, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error downloading file with key={}", key, e);
            return ResponseEntity.internalServerError().body("Failed to download file");
        }
    }

    /**
     * Determine the media type based on file extension
     */
    private MediaType getMediaType(String fileExtension) {
        return switch (fileExtension.toLowerCase()) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "mp4" -> MediaType.valueOf("video/mp4");
            case "json" -> MediaType.APPLICATION_JSON;
            case "bundle" -> MediaType.valueOf("application/x-bundle");
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "zip" -> MediaType.valueOf("application/zip");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}

