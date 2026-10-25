package com.stayhyderabad;

import com.stayhyderabad.dto.AuthDto.RegisterRequest;
import com.stayhyderabad.entity.Role;
import com.stayhyderabad.entity.User;
import com.stayhyderabad.exception.BadRequestException;
import com.stayhyderabad.repository.OwnerProfileRepository;
import com.stayhyderabad.repository.UserRepository;
import com.stayhyderabad.security.JwtTokenProvider;
import com.stayhyderabad.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OwnerProfileRepository ownerProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authenticationManager,
                userRepository,
                ownerProfileRepository,
                passwordEncoder,
                tokenProvider
        );
    }

    @Test
    void register_ShouldRejectAdminRoleRegistration() {
        RegisterRequest request = new RegisterRequest(
                "Sneaky Admin",
                "attacker@example.com",
                "9999999999",
                "password123",
                Role.ROLE_ADMIN // Should be strictly rejected
        );

        when(userRepository.existsByEmail("attacker@example.com")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("Administrator accounts cannot be self-registered"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "Duplicate User",
                "existing@example.com",
                "9876543210",
                "password123",
                Role.ROLE_USER
        );

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void register_ShouldHashPasswordAndSaveUser() {
        RegisterRequest request = new RegisterRequest(
                "Real User",
                "newuser@example.com",
                "9876543210",
                "plainPassword",
                Role.ROLE_USER
        );

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("$2a$10$hashedPasswordHere");

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setEmail("newuser@example.com");
        savedUser.setFullName("Real User");
        savedUser.setPhone("9876543210");
        savedUser.setRole(Role.ROLE_USER);
        savedUser.setPasswordHash("$2a$10$hashedPasswordHere");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateToken(any())).thenReturn("mock.jwt.token");

        var response = authService.register(request);

        assertNotNull(response);
        assertEquals("newuser@example.com", response.email());
        assertEquals("ROLE_USER", response.role());
        verify(passwordEncoder).encode("plainPassword");
        verify(userRepository).save(any(User.class));
    }
}
