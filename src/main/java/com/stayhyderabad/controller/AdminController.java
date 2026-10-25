package com.stayhyderabad.controller;

import com.stayhyderabad.dto.AdminDto.*;
import com.stayhyderabad.dto.AuthDto.UserProfileDto;
import com.stayhyderabad.dto.PropertyDto.PropertyDetailDto;
import com.stayhyderabad.dto.PropertyDto.PropertySummaryDto;
import com.stayhyderabad.entity.AuditLog;
import com.stayhyderabad.entity.Role;
import com.stayhyderabad.security.UserPrincipal;
import com.stayhyderabad.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Portal", description = "Administrative moderation, platform statistics, user management, and audit logs")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get platform-wide admin statistics")
    public ResponseEntity<AdminDashboardStats> getAdminStats() {
        return ResponseEntity.ok(adminService.getAdminStats());
    }

    @GetMapping("/properties")
    @Operation(summary = "Get all platform properties with moderation status")
    public ResponseEntity<Page<PropertySummaryDto>> getAllProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(adminService.getAllProperties(pageable));
    }

    @PatchMapping("/properties/{id}/moderate")
    @Operation(summary = "Moderate a property listing (Approve, Reject, Suspend, Archive)")
    public ResponseEntity<PropertyDetailDto> moderateProperty(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody PropertyModerationRequest request) {
        return ResponseEntity.ok(adminService.moderateProperty(principal.getId(), id, request));
    }

    @PatchMapping("/properties/{id}/feature")
    @Operation(summary = "Toggle property featured status on homepage")
    public ResponseEntity<PropertyDetailDto> toggleFeatured(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleFeatured(principal.getId(), id));
    }

    @GetMapping("/users")
    @Operation(summary = "List users filtered by role")
    public ResponseEntity<Page<UserProfileDto>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(adminService.getUsers(role, pageable));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Update user status (ACTIVE, SUSPENDED)")
    public ResponseEntity<UserProfileDto> updateUserStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateUserStatus(principal.getId(), id, request.status()));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "View administrative audit logs")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getAuditLogs(pageable));
    }
}
