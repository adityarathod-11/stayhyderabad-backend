package com.stayhyderabad.controller;

import com.stayhyderabad.dto.AdminDto.OwnerDashboardStats;
import com.stayhyderabad.dto.BookingDto.*;
import com.stayhyderabad.dto.PropertyDto.*;
import com.stayhyderabad.security.UserPrincipal;
import com.stayhyderabad.service.AdminService;
import com.stayhyderabad.service.BookingService;
import com.stayhyderabad.service.EnquiryService;
import com.stayhyderabad.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
@Tag(name = "Owner Dashboard", description = "Property management, vacancies, photos, and booking requests for hostel owners")
public class OwnerPropertyController {

    private final PropertyService propertyService;
    private final BookingService bookingService;
    private final EnquiryService enquiryService;
    private final AdminService adminService;

    public OwnerPropertyController(
            PropertyService propertyService,
            BookingService bookingService,
            EnquiryService enquiryService,
            AdminService adminService) {
        this.propertyService = propertyService;
        this.bookingService = bookingService;
        this.enquiryService = enquiryService;
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get owner dashboard statistics")
    public ResponseEntity<OwnerDashboardStats> getOwnerStats(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adminService.getOwnerStats(principal.getId()));
    }

    @GetMapping("/properties")
    @Operation(summary = "Get properties owned by the authenticated owner")
    public ResponseEntity<Page<PropertySummaryDto>> getOwnerProperties(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(propertyService.getOwnerProperties(principal.getId(), pageable));
    }

    @GetMapping("/properties/{id}")
    @Operation(summary = "Get specific owner property details")
    public ResponseEntity<PropertyDetailDto> getPropertyById(@PathVariable Long id) {
        return ResponseEntity.ok(propertyService.getPropertyById(id));
    }

    @PostMapping("/properties")
    @Operation(summary = "Create a new hostel or PG listing")
    public ResponseEntity<PropertyDetailDto> createProperty(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PropertyCreateUpdateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(propertyService.createProperty(principal.getId(), request));
    }

    @PutMapping("/properties/{id}")
    @Operation(summary = "Update an existing hostel listing")
    public ResponseEntity<PropertyDetailDto> updateProperty(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody PropertyCreateUpdateRequest request) {
        return ResponseEntity.ok(propertyService.updateProperty(principal.getId(), id, request));
    }

    @PatchMapping("/properties/{id}/publish")
    @Operation(summary = "Toggle property published / draft status")
    public ResponseEntity<PropertyDetailDto> togglePublishStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(propertyService.togglePublishStatus(principal.getId(), id));
    }

    @PostMapping(value = "/properties/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload property photograph to Cloudinary")
    public ResponseEntity<PropertyImageDto> uploadImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "category", required = false, defaultValue = "GENERAL") String category,
            @RequestParam(value = "isPrimary", required = false, defaultValue = "false") Boolean isPrimary) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(propertyService.uploadPropertyImage(principal.getId(), id, file, caption, category, isPrimary));
    }

    @DeleteMapping("/properties/{id}/images/{imageId}")
    @Operation(summary = "Delete property photograph")
    public ResponseEntity<Void> deleteImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long imageId) {
        propertyService.deletePropertyImage(principal.getId(), id, imageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bookings")
    @Operation(summary = "Get booking requests submitted for the owner's properties")
    public ResponseEntity<Page<BookingResponse>> getOwnerBookings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bookingService.getOwnerBookings(principal.getId(), pageable));
    }

    @PatchMapping("/bookings/{bookingId}")
    @Operation(summary = "Accept, reject, or complete a booking request")
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId,
            @Valid @RequestBody BookingStatusUpdate request) {
        return ResponseEntity.ok(bookingService.updateBookingStatusByOwner(principal.getId(), bookingId, request));
    }

    @GetMapping("/enquiries")
    @Operation(summary = "Get guest enquiries received for owner's properties")
    public ResponseEntity<Page<EnquiryResponse>> getOwnerEnquiries(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(enquiryService.getOwnerEnquiries(principal.getId(), pageable));
    }
}
