package com.neosow.infra.dto.boq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoqItemDTO {
    private UUID id;
    private UUID jobId;
    private String mainHeading;
    private String subHeading;
    private String description;
    private String width;
    private String height;
    private String depth;
    private BigDecimal qty;
    private String unit;
    private BigDecimal noOfUnit;
    private BigDecimal totalQty;
    private BigDecimal rate;
    private BigDecimal amount;
    private UUID createdBy;
    private UUID adminId;

    private String status;
    private String uploadedBy;
    private String uploadedRole;
    private UUID approvedBy;
    private LocalDateTime approvedAt;
    private boolean isNewValue;
}
