package com.stayhyderabad.dto;

import com.stayhyderabad.entity.PropertyStatus;
import com.stayhyderabad.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public class AdminDto {

    public record PropertyModerationRequest(
            @NotNull PropertyStatus status,
            String moderationNote
    ) {}

    public record UserStatusUpdateRequest(
            @NotNull UserStatus status
    ) {}

    public record OwnerDashboardStats(
            long publishedProperties,
            long totalProperties,
            long pendingBookings,
            long acceptedBookings,
            long totalEnquiries,
            long availableBeds
    ) {}

    public record AdminDashboardStats(
            long totalUsers,
            long totalOwners,
            long totalProperties,
            long pendingProperties,
            long publishedProperties,
            long totalBookings,
            long pendingReports
    ) {}
}
