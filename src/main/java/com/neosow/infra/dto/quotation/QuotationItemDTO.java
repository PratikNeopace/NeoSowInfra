package com.neosow.infra.dto.quotation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class QuotationItemDTO {
    private UUID id;
    @NotBlank(message = "Category is required")
    private String category;
    private String subcategory;
    private String description;
    private String width;
    private String height;
    private String depth;
    private String unit;
    private BigDecimal qty;
    private BigDecimal noOfUnit;
    private BigDecimal totalQty;
    @NotNull(message = "Unit rate is required")
    private BigDecimal unitRate;
    private BigDecimal amount;
}
