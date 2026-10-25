package com.stayhyderabad.dto;

import com.stayhyderabad.entity.BookingStatus;
import com.stayhyderabad.entity.SharingType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class BookingDto {

    public record BookingRequest(
            @NotNull(message = "Property ID is required")
            Long propertyId,

            @NotNull(message = "Room Type ID is required")
            Long roomTypeId,

            @NotNull(message = "Preferred move-in date is required")
            @FutureOrPresent(message = "Move-in date cannot be in the past")
            LocalDate preferredMoveInDate,

            @Min(value = 1, message = "At least 1 person required")
            @Max(value = 4, message = "Maximum 4 persons per request")
            Integer numPersons,

            @NotBlank(message = "Applicant name is required")
            String applicantName,

            @NotBlank(message = "Phone number is required")
            String applicantPhone,

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email")
            String applicantEmail,

            String message
    ) {}

    public record BookingResponse(
            Long id,
            String bookingReference,
            Long propertyId,
            String propertyName,
            String propertySlug,
            String locality,
            String propertyPrimaryImage,
            Long roomTypeId,
            SharingType sharingType,
            LocalDate preferredMoveInDate,
            Integer numPersons,
            String applicantName,
            String applicantPhone,
            String applicantEmail,
            String message,
            BigDecimal monthlyRentLocked,
            BigDecimal depositLocked,
            BookingStatus status,
            String ownerNote,
            Boolean bedsAllocated,
            OffsetDateTime createdAt
    ) {}

    public record BookingStatusUpdate(
            @NotNull(message = "Status is required")
            BookingStatus status,
            String ownerNote
    ) {}

    public record EnquiryRequest(
            @NotNull(message = "Property ID is required")
            Long propertyId,

            @NotBlank(message = "Name is required")
            String name,

            @NotBlank(message = "Phone is required")
            String phone,

            String email,

            @NotBlank(message = "Message is required")
            String message,

            Boolean isWhatsapp
    ) {}

    public record EnquiryResponse(
            Long id,
            Long propertyId,
            String propertyName,
            String name,
            String phone,
            String email,
            String message,
            Boolean isWhatsapp,
            OffsetDateTime createdAt
    ) {}
}
