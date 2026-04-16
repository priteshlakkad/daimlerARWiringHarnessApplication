package com.harness.controller;

import com.harness.dtos.VinValidationData;
import com.harness.dtos.VinValidationRequest;
import com.harness.dtos.VinValidationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * VIN Validation API Controller
 *
 * Provides an endpoint to validate VIN numbers.
 * The response is currently fixed as per requirements.
 */
@RestController
@RequestMapping("/api/v1/vin")
@Tag(name = "VIN Validation", description = "Endpoints for validating vehicle identification numbers")
public class VinController {

    @PostMapping("/validate")
    @Operation(summary = "Validate VIN number", description = "Validates the provided VIN number and returns truck details. Currently returns a fixed response.")
    public ResponseEntity<VinValidationResponse> validateVin(@RequestBody VinValidationRequest request) {
        // As per requirements, the response is fixed for any VIN number request
        VinValidationData data = new VinValidationData("Daimler FH16", "FH16_FH16-750");
        VinValidationResponse response = new VinValidationResponse("success", data);

        return ResponseEntity.ok(response);
    }
}
