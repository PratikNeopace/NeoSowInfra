package com.neosow.infra;

import com.neosow.infra.controller.AuthController;
import com.neosow.infra.dto.auth.*;
import com.neosow.infra.exception.BadRequestException;
import com.neosow.infra.model.User;
import com.neosow.infra.repository.RoleRepository;
import com.neosow.infra.repository.UserRepository;
import com.neosow.infra.security.JWTTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JWTTokenProvider tokenProvider;

    @InjectMocks
    private AuthController authController;

    @Test
    void testForgotPassword_Success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@email.com");
        User user = User.builder().email("test@email.com").build();

        when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(user));

        ResponseEntity<String> response = authController.forgotPassword(request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(user.getVerificationCode());
        assertNotNull(user.getVerificationCodeExpiresAt());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testVerifyCode_Success() {
        VerifyCodeRequest request = new VerifyCodeRequest("test@email.com", "123456");
        User user = User.builder()
                .email("test@email.com")
                .verificationCode("123456")
                .verificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(user));

        ResponseEntity<String> response = authController.verifyCode(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Verification code verified successfully!", response.getBody());
    }

    @Test
    void testVerifyCode_Expired() {
        VerifyCodeRequest request = new VerifyCodeRequest("test@email.com", "123456");
        User user = User.builder()
                .email("test@email.com")
                .verificationCode("123456")
                .verificationCodeExpiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> authController.verifyCode(request));
    }

    @Test
    void testResetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest("test@email.com", "123456", "newPassword");
        User user = User.builder()
                .email("test@email.com")
                .verificationCode("123456")
                .verificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("hashedPassword");

        ResponseEntity<String> response = authController.resetPassword(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("hashedPassword", user.getPasswordHash());
        assertNull(user.getVerificationCode());
        assertNull(user.getVerificationCodeExpiresAt());
        verify(userRepository, times(1)).save(user);
    }
}
