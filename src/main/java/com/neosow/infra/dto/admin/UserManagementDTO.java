package com.neosow.infra.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserManagementDTO {
    private UUID id;
    private String email;
    private boolean enabled;
    private List<String> roles;
    private LocalDateTime createdAt;
    private UUID parentAdminId;
    private String status;
}
