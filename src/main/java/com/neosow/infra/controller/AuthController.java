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

    @org.springframework.beans.factory.annotation.Value("${sms.way2smart.api-url}")
    private String smsApiUrl;

    @org.springframework.beans.factory.annotation.Value("${sms.way2smart.api-key}")
    private String smsApiKey;

    @org.springframework.beans.factory.annotation.Value("${sms.way2smart.sender}")
    private String smsSender;

    @org.springframework.beans.factory.annotation.Value("${sms.way2smart.dlt-entity-id}")
    private String smsDltEntityId;

    @org.springframework.beans.factory.annotation.Value("${sms.way2smart.dlt-temp-id}")
    private String smsDltTempId;

    @org.springframework.beans.factory.annotation.Value("${sms.way2smart.app-signature:Q2TwnGW50lc}")
    private String smsAppSignature;

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
                .phone(signUpRequest.getPhone())
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
        log.info("Forgot password requested for phone: {}", request.getPhone());
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BadRequestException("User with this phone number does not exist."));

        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new BadRequestException("No registered mobile number found for this account. Please contact your administrator.");
        }

        // Generate 6-digit code
        String code = String.format("%06d", (int) (Math.random() * 1000000));
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        // Send OTP via SMS
        sendSMSOtp(user.getPhone(), code);

        return ResponseEntity.ok("Verification code has been sent to your registered mobile number.");
    }

    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        log.info("Verification code check requested for phone: {}", request.getPhone());
        User user = userRepository.findByPhone(request.getPhone())
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
        log.info("Reset password requested for phone: {}", request.getPhone());
        User user = userRepository.findByPhone(request.getPhone())
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

    private String normalizeIndianMobile(String mobile) {
        if (mobile == null) return null;

        String digits = mobile.replaceAll("\\D", "");

        if (digits.length() == 10) {
            return "+91" + digits;
        }

        if (digits.startsWith("91") && digits.length() == 12) {
            return "+91" + digits.substring(2);
        }

        return null;
    }

    private void sendSMSOtp(String mobile, String otpValue) {
        try {
            String formattedMobile = normalizeIndianMobile(mobile);
            if (formattedMobile == null) {
                log.warn("Invalid mobile number format for OTP send: {}", mobile);
                return;
            }

            String tenDigitMobile = formattedMobile;
            if (formattedMobile.startsWith("+91")) {
                tenDigitMobile = formattedMobile.substring(3);
            } else if (formattedMobile.startsWith("91") && formattedMobile.length() == 12) {
                tenDigitMobile = formattedMobile.substring(2);
            }

            String cleanAppSig = (smsAppSignature != null) ? smsAppSignature.trim() : "";
            String message = String.format("Dear Customer your Otp for login is %s . This code will expire in 10 minutes . - Neopace Team %s", otpValue, cleanAppSig).trim();

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String finalUrl = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(smsApiUrl)
                    .queryParam("sender", smsSender)
                    .queryParam("numbers", tenDigitMobile)
                    .queryParam("message", message)
                    .queryParam("messagetype", "TXT")
                    .queryParam("reponse", "Y")
                    .queryParam("apikey", smsApiKey)
                    .queryParam("dltentityid", smsDltEntityId)
                    .queryParam("dlttempid", smsDltTempId)
                    .build()
                    .toUriString();

            log.info("Sending SMS OTP to {}. API URL: {}", tenDigitMobile, finalUrl);

            org.springframework.http.ResponseEntity<String> response = restTemplate.getForEntity(finalUrl, String.class);
            log.info("SMS Send response status: {}, body: {}", response.getStatusCode(), response.getBody());

        } catch (Exception e) {
            log.error("Failed to send SMS OTP to " + mobile, e);
            throw new BadRequestException("Failed to send SMS OTP: " + e.getMessage());
        }
    }
}
