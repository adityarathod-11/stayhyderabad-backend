package com.stayhyderabad.controller;

import com.stayhyderabad.dto.PropertyDto.*;
import com.stayhyderabad.entity.GenderCategory;
import com.stayhyderabad.entity.SharingType;
import com.stayhyderabad.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/properties")
@Tag(name = "Properties", description = "Public endpoints for exploring, searching, and viewing hostel listings")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping
    @Operation(summary = "Search properties with filters, sorting, and pagination")
    public ResponseEntity<Page<PropertySummaryDto>> searchProperties(
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) BigDecimal minRent,
            @RequestParam(required = false) BigDecimal maxRent,
            @RequestParam(required = false) GenderCategory genderCategory,
            @RequestParam(required = false) SharingType sharingType,
            @RequestParam(required = false) Boolean foodIncluded,
            @RequestParam(required = false) Boolean hasAc,
            @RequestParam(required = false) Boolean hasAttachedBathroom,
            @RequestParam(required = false) Boolean availableBedsOnly,
            @RequestParam(required = false, defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Sort sort = switch (sortBy) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "startingMonthlyRent");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "startingMonthlyRent");
            case "updated" -> Sort.by(Sort.Direction.DESC, "lastAvailabilityUpdate");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(page, size, sort);
        PropertySearchCriteria criteria = new PropertySearchCriteria(
                locality, minRent, maxRent, genderCategory, sharingType,
                foodIncluded, hasAc, hasAttachedBathroom, availableBedsOnly, sortBy
        );

        return ResponseEntity.ok(propertyService.searchProperties(criteria, pageable));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured verified properties for home showcase")
    public ResponseEntity<List<PropertySummaryDto>> getFeaturedProperties() {
        return ResponseEntity.ok(propertyService.getFeaturedProperties());
    }

    @GetMapping("/localities")
    @Operation(summary = "Get all distinct active localities in Hyderabad")
    public ResponseEntity<List<String>> getLocalities() {
        return ResponseEntity.ok(propertyService.getPublishedLocalities());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get complete property details by SEO slug")
    public ResponseEntity<PropertyDetailDto> getPropertyBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(propertyService.getPropertyBySlug(slug));
    }

    @GetMapping("/{id}/similar")
    @Operation(summary = "Get similar properties nearby")
    public ResponseEntity<List<PropertySummaryDto>> getSimilarProperties(
            @PathVariable Long id,
            @RequestParam(required = false) String locality) {
        return ResponseEntity.ok(propertyService.getSimilarProperties(locality, id));
    }
}
