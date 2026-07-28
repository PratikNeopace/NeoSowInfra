package com.neosow.infra.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardDTO {
    private long totalUsers;
    private long totalCustomers;
    private long totalQuotations;
    private BigDecimal totalQuotationAmount;
}
