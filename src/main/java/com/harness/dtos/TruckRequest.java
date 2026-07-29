package com.harness.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TruckRequest {
    private String vin;
    private String truckName;
    private String truckModel;
}
