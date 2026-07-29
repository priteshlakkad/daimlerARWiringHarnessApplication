package com.harness.service;

import com.harness.dtos.TruckRequest;
import com.harness.model.Truck;
import com.harness.repository.TruckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TruckService {

    private final TruckRepository truckRepository;

    public Truck create(TruckRequest request) {
        Truck truck = Truck.builder()
                .vin(request.getVin())
                .truckName(request.getTruckName())
                .truckModel(request.getTruckModel())
                .build();
        return truckRepository.save(truck);
    }

    public List<Truck> getAll() {
        return truckRepository.findAll();
    }

    public Optional<Truck> getById(Long id) {
        return truckRepository.findById(id);
    }

    public Optional<Truck> findByVin(String vin) {
        return truckRepository.findByVin(vin);
    }

    public Optional<Truck> update(Long id, TruckRequest request) {
        return truckRepository.findById(id).map(truck -> {
            truck.setVin(request.getVin());
            truck.setTruckName(request.getTruckName());
            truck.setTruckModel(request.getTruckModel());
            return truckRepository.save(truck);
        });
    }

    public boolean delete(Long id) {
        if (truckRepository.existsById(id)) {
            truckRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
