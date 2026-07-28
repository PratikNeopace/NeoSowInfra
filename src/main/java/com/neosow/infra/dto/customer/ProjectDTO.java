package com.neosow.infra.dto.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDTO {
    private UUID id;
    @NotBlank(message = "Type of work is required")
    private String workType;
    private BigDecimal carpetArea;
    private String areaUnit;
    private BigDecimal builtUpArea;
    private BigDecimal budget;
    private String timeline;
}
