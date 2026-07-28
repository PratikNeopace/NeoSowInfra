package com.neosow.infra.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApprovalRequestDTO {
    @NotBlank(message = "Approval type is required")
    private String type;
}
