package com.neosow.infra.dto.boq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportErrorDTO {
    private UUID id;
    private UUID jobId;
    private int rowNumber;
    private String errorMessage;
    private String rawData;
}
