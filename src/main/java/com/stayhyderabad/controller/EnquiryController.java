package com.stayhyderabad.controller;

import com.stayhyderabad.dto.BookingDto.*;
import com.stayhyderabad.security.UserPrincipal;
import com.stayhyderabad.service.EnquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enquiries")
@Tag(name = "Enquiries", description = "Public guest enquiries and WhatsApp contact triggers")
public class EnquiryController {

    private final EnquiryService enquiryService;

    public EnquiryController(EnquiryService enquiryService) {
        this.enquiryService = enquiryService;
    }

    @PostMapping
    @Operation(summary = "Submit a general enquiry or record WhatsApp click")
    public ResponseEntity<EnquiryResponse> submitEnquiry(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EnquiryRequest request) {
        Long userId = (principal != null) ? principal.getId() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(enquiryService.submitEnquiry(userId, request));
    }
}
