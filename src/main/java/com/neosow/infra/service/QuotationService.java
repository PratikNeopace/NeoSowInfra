package com.neosow.infra.service;

import com.neosow.infra.dto.quotation.QuotationDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface QuotationService {
    QuotationDTO createQuotation(QuotationDTO quotationDto);
    QuotationDTO getQuotationById(UUID id);
    Page<QuotationDTO> getQuotationsByCustomerId(UUID customerId, int page, int size);
    void deleteQuotation(UUID id);
    void updateQuotationStatus(UUID id, String status);
}
