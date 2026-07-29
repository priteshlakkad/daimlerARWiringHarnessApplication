package com.harness.controller;

import com.harness.dtos.VinValidationData;
import com.harness.dtos.VinValidationRequest;
import com.harness.dtos.VinValidationResponse;
import com.harness.service.TruckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vin")
@Tag(name = "VIN Validation", description = "Endpoints for validating vehicle identification numbers")
@RequiredArgsConstructor
public class VinController {

    private final TruckService truckService;

    @PostMapping("/validate")
    @Operation(summary = "Validate VIN number", description = "Validates the provided VIN number and returns truck details from the database")
    public ResponseEntity<VinValidationResponse> validateVin(@RequestBody VinValidationRequest request) {
        return truckService.findByVin(request.getVin())
                .map(truck -> ResponseEntity.ok(new VinValidationResponse("success",
                        new VinValidationData(truck.getTruckName(), truck.getTruckModel()))))
                .orElse(ResponseEntity.status(404)
                        .body(new VinValidationResponse("not_found", null)));
    }
}
