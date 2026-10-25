package com.stayhyderabad.service;

import com.stayhyderabad.dto.AdminDto.*;
import com.stayhyderabad.dto.AuthDto.UserProfileDto;
import com.stayhyderabad.dto.PropertyDto.PropertyDetailDto;
import com.stayhyderabad.dto.PropertyDto.PropertySummaryDto;
import com.stayhyderabad.entity.*;
import com.stayhyderabad.exception.ResourceNotFoundException;
import com.stayhyderabad.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;
    private final EnquiryRepository enquiryRepository;
    private final ReportRepository reportRepository;
    private final AuditLogRepository auditLogRepository;
    private final PropertyService propertyService;
    private final AuthService authService;

    public AdminService(
            UserRepository userRepository,
            PropertyRepository propertyRepository,
            BookingRepository bookingRepository,
            EnquiryRepository enquiryRepository,
            ReportRepository reportRepository,
            AuditLogRepository auditLogRepository,
            PropertyService propertyService,
            AuthService authService) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.bookingRepository = bookingRepository;
        this.enquiryRepository = enquiryRepository;
        this.reportRepository = reportRepository;
        this.auditLogRepository = auditLogRepository;
        this.propertyService = propertyService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardStats getAdminStats() {
        return new AdminDashboardStats(
                userRepository.countByRole(Role.ROLE_USER),
                userRepository.countByRole(Role.ROLE_OWNER),
                propertyRepository.count(),
                propertyRepository.countByStatus(PropertyStatus.PENDING_APPROVAL),
                propertyRepository.countByStatus(PropertyStatus.PUBLISHED),
                bookingRepository.count(),
                reportRepository.countByStatus("PENDING")
        );
    }

    @Transactional(readOnly = true)
    public OwnerDashboardStats getOwnerStats(Long ownerId) {
        return new OwnerDashboardStats(
                propertyRepository.findByOwnerId(ownerId, Pageable.unpaged()).stream().filter(p -> p.getStatus() == PropertyStatus.PUBLISHED).count(),
                propertyRepository.countByOwnerId(ownerId),
                bookingRepository.countByPropertyOwnerIdAndStatus(ownerId, BookingStatus.PENDING),
                bookingRepository.countByPropertyOwnerIdAndStatus(ownerId, BookingStatus.ACCEPTED),
                enquiryRepository.countByPropertyOwnerId(ownerId),
                propertyRepository.sumAvailableBedsByOwnerId(ownerId)
        );
    }

    @Transactional(readOnly = true)
    public Page<PropertySummaryDto> getAllProperties(Pageable pageable) {
        return propertyRepository.findAll(pageable).map(propertyService::mapToSummary);
    }

    @Transactional
    public PropertyDetailDto moderateProperty(Long adminId, Long propertyId, PropertyModerationRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        PropertyStatus oldStatus = property.getStatus();
        property.setStatus(request.status());
        Property saved = propertyRepository.save(property);

        User admin = userRepository.findById(adminId).orElse(null);
        auditLogRepository.save(new AuditLog(
                admin,
                "MODERATE_PROPERTY",
                "Property",
                propertyId,
                "Changed status from " + oldStatus + " to " + request.status() + (request.moderationNote() != null ? " Note: " + request.moderationNote() : "")
        ));

        return propertyService.mapToDetail(saved);
    }

    @Transactional
    public PropertyDetailDto toggleFeatured(Long adminId, Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        property.setIsFeatured(!property.getIsFeatured());
        Property saved = propertyRepository.save(property);

        User admin = userRepository.findById(adminId).orElse(null);
        auditLogRepository.save(new AuditLog(
                admin,
                "TOGGLE_FEATURED",
                "Property",
                propertyId,
                "Featured flag toggled to: " + saved.getIsFeatured()
        ));

        return propertyService.mapToDetail(saved);
    }

    @Transactional(readOnly = true)
    public Page<UserProfileDto> getUsers(Role role, Pageable pageable) {
        if (role != null) {
            return userRepository.findAllByRole(role, pageable).map(u -> authService.getProfile(u.getId()));
        }
        return userRepository.findAll(pageable).map(u -> authService.getProfile(u.getId()));
    }

    @Transactional
    public UserProfileDto updateUserStatus(Long adminId, Long targetUserId, UserStatus status) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserStatus old = user.getStatus();
        user.setStatus(status);
        userRepository.save(user);

        User admin = userRepository.findById(adminId).orElse(null);
        auditLogRepository.save(new AuditLog(
                admin,
                "UPDATE_USER_STATUS",
                "User",
                targetUserId,
                "User status changed from " + old + " to " + status
        ));

        return authService.getProfile(user.getId());
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
