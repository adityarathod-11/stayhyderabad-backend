package com.stayhyderabad.service;

import com.stayhyderabad.dto.AuthDto.*;
import com.stayhyderabad.entity.OwnerProfile;
import com.stayhyderabad.entity.Role;
import com.stayhyderabad.entity.User;
import com.stayhyderabad.entity.UserStatus;
import com.stayhyderabad.exception.BadRequestException;
import com.stayhyderabad.exception.ResourceNotFoundException;
import com.stayhyderabad.repository.OwnerProfileRepository;
import com.stayhyderabad.repository.UserRepository;
import com.stayhyderabad.security.JwtTokenProvider;
import com.stayhyderabad.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            OwnerProfileRepository ownerProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.ownerProfileRepository = ownerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BadRequestException("Your account has been suspended. Please contact support.");
        }

        String jwt = tokenProvider.generateToken(principal);
        return new AuthResponse(
                jwt,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getPhone()
        );
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("An account with this email already exists");
        }

        // Strict role security: Never allow creation of ROLE_ADMIN via registration endpoint!
        Role requestedRole = request.role() != null ? request.role() : Role.ROLE_USER;
        if (requestedRole == Role.ROLE_ADMIN) {
            throw new BadRequestException("Administrator accounts cannot be self-registered");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone().trim());
        user.setRole(requestedRole);
        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);

        // If registering as OWNER, create initial owner profile
        if (requestedRole == Role.ROLE_OWNER) {
            OwnerProfile profile = new OwnerProfile();
            profile.setUser(savedUser);
            profile.setIsVerified(false);
            ownerProfileRepository.save(profile);
        }

        UserPrincipal principal = UserPrincipal.create(savedUser);
        String jwt = tokenProvider.generateToken(principal);

        return new AuthResponse(
                jwt,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getRole().name(),
                savedUser.getPhone()
        );
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String businessName = null;
        if (user.getRole() == Role.ROLE_OWNER) {
            businessName = ownerProfileRepository.findByUserId(userId)
                    .map(OwnerProfile::getBusinessName)
                    .orElse(null);
        }

        return new UserProfileDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getAvatarUrl(),
                businessName
        );
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, String fullName, String phone, String businessName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (phone != null && !phone.isBlank()) {
            user.setPhone(phone.trim());
        }

        if (user.getRole() == Role.ROLE_OWNER && businessName != null) {
            OwnerProfile profile = ownerProfileRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        OwnerProfile p = new OwnerProfile();
                        p.setUser(user);
                        return p;
                    });
            profile.setBusinessName(businessName.trim());
            ownerProfileRepository.save(profile);
        }

        User updated = userRepository.save(user);
        return getProfile(updated.getId());
    }
}
