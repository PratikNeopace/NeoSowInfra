package com.neosow.infra.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalResponseDTO {
    private UUID id;
    private String type;
    private UUID submittedBy;
    private String status;
    private UUID reviewedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
