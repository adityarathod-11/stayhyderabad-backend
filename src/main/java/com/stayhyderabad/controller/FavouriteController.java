package com.stayhyderabad.controller;

import com.stayhyderabad.dto.PropertyDto.PropertySummaryDto;
import com.stayhyderabad.security.UserPrincipal;
import com.stayhyderabad.service.FavouriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/favourites")
@Tag(name = "Favourites", description = "User saved hostel bookmarks")
public class FavouriteController {

    private final FavouriteService favouriteService;

    public FavouriteController(FavouriteService favouriteService) {
        this.favouriteService = favouriteService;
    }

    @PostMapping("/{propertyId}")
    @Operation(summary = "Toggle property bookmark/favourite for current user")
    public ResponseEntity<Map<String, Boolean>> toggleFavourite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long propertyId) {
        boolean isFav = favouriteService.toggleFavourite(principal.getId(), propertyId);
        return ResponseEntity.ok(Map.of("isFavourite", isFav));
    }

    @GetMapping
    @Operation(summary = "Get all saved properties for current user")
    public ResponseEntity<Page<PropertySummaryDto>> getUserFavourites(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(favouriteService.getUserFavourites(principal.getId(), pageable));
    }

    @GetMapping("/{propertyId}/check")
    @Operation(summary = "Check if a property is in current user's favourites")
    public ResponseEntity<Map<String, Boolean>> checkFavourite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long propertyId) {
        Long userId = (principal != null) ? principal.getId() : null;
        boolean isFav = favouriteService.isFavourite(userId, propertyId);
        return ResponseEntity.ok(Map.of("isFavourite", isFav));
    }
}
