package com.neosow.infra.dto.quotation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationDTO {
    private UUID id;
    private UUID parentQuotationId;
    private String status;
    @NotNull(message = "Customer ID is required")
    private UUID customerId;
    private String customerName; // Handy for UI presentation
    @NotNull(message = "Project unit setting is required")
    private String projectUnit;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal discountPercent;
    private boolean includeGst;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    @NotEmpty(message = "Quotation must have at least one item")
    @Valid
    private List<QuotationItemDTO> items;
    private LocalDateTime createdAt;
    private String createdBy;
}
