package com.stayhyderabad.controller;

import com.stayhyderabad.dto.PropertyDto.AmenityDto;
import com.stayhyderabad.repository.AmenityRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/amenities")
@Tag(name = "Amenities", description = "Available hostel facilities and amenities metadata")
public class AmenityController {

    private final AmenityRepository amenityRepository;

    public AmenityController(AmenityRepository amenityRepository) {
        this.amenityRepository = amenityRepository;
    }

    @GetMapping
    @Operation(summary = "Get list of all standard accommodation amenities")
    public ResponseEntity<List<AmenityDto>> getAllAmenities() {
        List<AmenityDto> dtos = amenityRepository.findAll().stream()
                .map(a -> new AmenityDto(a.getId(), a.getName(), a.getIconName(), a.getCategory(), a.getDescription()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
