package com.neosow.infra.dto.boq;

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
public class ImportJobResponseDTO {
    private UUID id;
    private String uploadedBy;
    private String role;
    private String fileName;
    private String status;
    private int totalRows;
    private int successRows;
    private int failedRows;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
