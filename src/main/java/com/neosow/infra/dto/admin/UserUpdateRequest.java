package com.neosow.infra.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class UserUpdateRequest {
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be less than 100 characters")
    private String email;

    @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
    private String password;

    @Size(max = 20, message = "Phone number must be less than 20 characters")
    private String phone;

    private List<String> roles;
    private Boolean enabled;
}
