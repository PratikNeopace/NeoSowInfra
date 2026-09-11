package com.neosow.infra.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyCodeRequest {
    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Verification code is required")
    private String code;
}
