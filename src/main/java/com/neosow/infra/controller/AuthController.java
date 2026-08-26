package com.neosow.infra.controller;

import com.neosow.infra.dto.auth.*;
import com.neosow.infra.exception.BadRequestException;
import com.neosow.infra.model.ERole;
import com.neosow.infra.model.Role;
import com.neosow.infra.model.User;
import com.neosow.infra.repository.RoleRepository;
import com.neosow.infra.repository.UserRepository;
import com.neosow.infra.security.JWTTokenProvider;
import com.neosow.infra.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTTokenProvider tokenProvider;

    @PostMapping({"/login", "/signin"})
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Authentication request received for user: {}", loginRequest.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return ResponseEntity.ok(JwtResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken)
                .id(userDetails.getId())
                .email(userDetails.getEmail())
                .roles(roles)
                .build());
    }

    @PostMapping("/signup")
    public ResponseEntity<String> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        log.info("Registration request received for user: {}", signUpRequest.getEmail());

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new BadRequestException("Error: Email is already in use!");
        }

        // Create new user's account
        User user = User.builder()
                .email(signUpRequest.getEmail())
                .passwordHash(passwordEncoder.encode(signUpRequest.getPassword()))
                .enabled(true)
                .build();

        // Assign default USER role
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role ROLE_USER is not found in database. Check Liquibase configuration."));
        user.setRoles(new HashSet<>(Collections.singletonList(userRole)));

        // For auditing: setting system as default createdBy since they are not authenticated yet
        user.setCreatedBy("System");

        userRepository.save(user);
        log.info("User registered successfully: {}", signUpRequest.getEmail());

        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        if (tokenProvider.validateToken(requestRefreshToken)) {
            String email = tokenProvider.getUsernameFromToken(requestRefreshToken);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadRequestException("Invalid refresh token: user not found"));

            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList());

            String accessToken = tokenProvider.generateAccessTokenFromUsernameAndRoles(email, roles);

            log.info("Tokens successfully refreshed for user: {}", email);
            return ResponseEntity.ok(TokenRefreshResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(requestRefreshToken)
                    .build());
        } else {
            throw new BadRequestException("Invalid or expired refresh token");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password requested for email: {}", request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User with this email does not exist."));

        // Generate 6-digit code
        String code = String.format("%06d", (int) (Math.random() * 1000000));
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        // Log the verification code for testing/development
        log.info("FORGOT_PASSWORD_CODE for user {}: {}", user.getEmail(), code);

        return ResponseEntity.ok("Verification code has been sent to your email.");
    }

    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        log.info("Verification code check requested for email: {}", request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found."));

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(request.getCode())) {
            throw new BadRequestException("Invalid verification code.");
        }

        if (user.getVerificationCodeExpiresAt() == null || user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification code has expired.");
        }

        return ResponseEntity.ok("Verification code verified successfully!");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password requested for email: {}", request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found."));

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(request.getCode())) {
            throw new BadRequestException("Invalid verification code.");
        }

        if (user.getVerificationCodeExpiresAt() == null || user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification code has expired.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        log.info("Password successfully reset for user: {}", user.getEmail());
        return ResponseEntity.ok("Password has been reset successfully.");
    }
}
