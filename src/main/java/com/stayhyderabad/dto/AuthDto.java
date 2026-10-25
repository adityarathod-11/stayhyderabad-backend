package com.stayhyderabad.dto;

import com.stayhyderabad.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDto {

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record RegisterRequest(
            @NotBlank(message = "Full name is required")
            @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
            String fullName,

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Phone number is required")
            @Size(min = 10, max = 15, message = "Phone number must be valid")
            String phone,

            @NotBlank(message = "Password is required")
            @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
            String password,

            Role role // USER or OWNER. (ADMIN is rejected at service level)
    ) {}

    public record AuthResponse(
            String token,
            String tokenType,
            Long userId,
            String email,
            String fullName,
            String role,
            String phone
    ) {
        public AuthResponse(String token, Long userId, String email, String fullName, String role, String phone) {
            this(token, "Bearer", userId, email, fullName, role, phone);
        }
    }

    public record UserProfileDto(
            Long id,
            String email,
            String fullName,
            String phone,
            String role,
            String status,
            String avatarUrl,
            String businessName
    ) {}
}
