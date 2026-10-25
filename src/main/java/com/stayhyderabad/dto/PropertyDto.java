package com.stayhyderabad.dto;

import com.stayhyderabad.entity.FoodType;
import com.stayhyderabad.entity.GenderCategory;
import com.stayhyderabad.entity.PropertyStatus;
import com.stayhyderabad.entity.SharingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

public class PropertyDto {

    public record RoomTypeDto(
            Long id,
            SharingType sharingType,
            BigDecimal monthlyRent,
            BigDecimal securityDeposit,
            Integer totalBeds,
            Integer availableBeds,
            Boolean hasAc,
            Boolean hasAttachedBathroom,
            Boolean hasBalcony
    ) {}

    public record RoomTypeRequest(
            @NotNull SharingType sharingType,
            @NotNull @DecimalMin("1000.00") BigDecimal monthlyRent,
            @NotNull @DecimalMin("0.00") BigDecimal securityDeposit,
            @NotNull Integer totalBeds,
            @NotNull Integer availableBeds,
            Boolean hasAc,
            Boolean hasAttachedBathroom,
            Boolean hasBalcony
    ) {}

    public record PropertyImageDto(
            Long id,
            String imageUrl,
            String thumbnailUrl,
            String caption,
            String category,
            Boolean isPrimary,
            Integer displayOrder
    ) {}

    public record AmenityDto(
            Long id,
            String name,
            String iconName,
            String category,
            String description
    ) {}

    public record PropertySummaryDto(
            Long id,
            String name,
            String slug,
            GenderCategory genderCategory,
            String locality,
            String landmark,
            BigDecimal startingMonthlyRent,
            BigDecimal securityDeposit,
            Integer availableBeds,
            Integer totalBeds,
            Boolean foodIncluded,
            FoodType foodType,
            String primaryImageUrl,
            int imageCount,
            List<SharingType> sharingTypes,
            List<String> keyAmenities,
            PropertyStatus status,
            Boolean isFeatured,
            OffsetDateTime lastAvailabilityUpdate
    ) {}

    public record PropertyDetailDto(
            Long id,
            String name,
            String slug,
            GenderCategory genderCategory,
            String locality,
            String addressLine,
            String landmark,
            BigDecimal latitude,
            BigDecimal longitude,
            String description,
            String houseRules,
            String gateClosingTime,
            Boolean foodIncluded,
            FoodType foodType,
            BigDecimal startingMonthlyRent,
            BigDecimal securityDeposit,
            BigDecimal maintenanceCharge,
            Integer noticePeriodDays,
            PropertyStatus status,
            Boolean isFeatured,
            Integer totalBeds,
            Integer availableBeds,
            String contactPhone,
            String whatsappNumber,
            OffsetDateTime lastAvailabilityUpdate,
            List<PropertyImageDto> images,
            List<RoomTypeDto> roomTypes,
            Set<AmenityDto> amenities,
            Long ownerId,
            String ownerName,
            String ownerBusinessName
    ) {}

    public record PropertyCreateUpdateRequest(
            @NotBlank(message = "Property name is required")
            String name,

            @NotNull(message = "Gender category is required")
            GenderCategory genderCategory,

            @NotBlank(message = "Locality is required")
            String locality,

            @NotBlank(message = "Address line is required")
            String addressLine,

            String landmark,
            BigDecimal latitude,
            BigDecimal longitude,
            String description,
            String houseRules,
            String gateClosingTime,
            Boolean foodIncluded,
            FoodType foodType,

            @NotNull(message = "Starting monthly rent is required")
            @DecimalMin("1000.00")
            BigDecimal startingMonthlyRent,

            @NotNull(message = "Security deposit is required")
            @DecimalMin("0.00")
            BigDecimal securityDeposit,

            BigDecimal maintenanceCharge,
            Integer noticePeriodDays,
            String contactPhone,
            String whatsappNumber,

            List<RoomTypeRequest> roomTypes,
            List<Long> amenityIds
    ) {}

    public record PropertySearchCriteria(
            String locality,
            BigDecimal minRent,
            BigDecimal maxRent,
            GenderCategory genderCategory,
            SharingType sharingType,
            Boolean foodIncluded,
            Boolean hasAc,
            Boolean hasAttachedBathroom,
            Boolean availableBedsOnly,
            String sortBy // price_asc, price_desc, newest, updated
    ) {}
}
