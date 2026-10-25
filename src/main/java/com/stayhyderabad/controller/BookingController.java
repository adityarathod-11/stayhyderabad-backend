package com.stayhyderabad.controller;

import com.stayhyderabad.dto.BookingDto.*;
import com.stayhyderabad.security.UserPrincipal;
import com.stayhyderabad.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Endpoints for submitting and managing hostel booking requests")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @Operation(summary = "Submit a new booking request for a room category")
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBookingRequest(principal.getId(), request));
    }

    @GetMapping
    @Operation(summary = "Get booking requests submitted by the authenticated user")
    public ResponseEntity<Page<BookingResponse>> getUserBookings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bookingService.getUserBookings(principal.getId(), pageable));
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Get booking details by unique reference code")
    public ResponseEntity<BookingResponse> getBookingByReference(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String reference) {
        return ResponseEntity.ok(bookingService.getBookingByReference(principal.getId(), reference));
    }

    @PatchMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel an eligible booking request")
    public ResponseEntity<BookingResponse> cancelBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Cancelled by guest";
        return ResponseEntity.ok(bookingService.cancelBookingByUser(principal.getId(), bookingId, reason));
    }
}
