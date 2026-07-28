package com.neosow.infra.controller;

import com.neosow.infra.dto.customer.CustomerDTO;
import com.neosow.infra.dto.quotation.QuotationDTO;
import com.neosow.infra.service.CustomerService;
import com.neosow.infra.service.PdfService;
import com.neosow.infra.service.QuotationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quotations")
@RequiredArgsConstructor
@Slf4j
public class QuotationController {

    private final QuotationService quotationService;
    private final CustomerService customerService;
    private final PdfService pdfService;

    @PostMapping
    public ResponseEntity<QuotationDTO> createQuotation(@Valid @RequestBody QuotationDTO quotationDto) {
        log.info("REST request to save Quotation for customer ID: {}", quotationDto.getCustomerId());
        QuotationDTO result = quotationService.createQuotation(quotationDto);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuotationDTO> getQuotation(@PathVariable UUID id) {
        log.info("REST request to get Quotation by ID: {}", id);
        QuotationDTO result = quotationService.getQuotationById(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<QuotationDTO>> getQuotationsByCustomer(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST request to get paginated Quotations for customer ID: {}", customerId);
        Page<QuotationDTO> result = quotationService.getQuotationsByCustomerId(customerId, page, size);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuotation(@PathVariable UUID id) {
        log.info("REST request to delete Quotation: {}", id);
        quotationService.deleteQuotation(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateQuotationStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        log.info("REST request to update Quotation ID {} status to {}", id, status);
        quotationService.updateQuotationStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getQuotationPdf(@PathVariable UUID id) {
        log.info("REST request to download PDF for Quotation: {}", id);
        QuotationDTO quotation = quotationService.getQuotationById(id);
        CustomerDTO customer = customerService.getCustomerById(quotation.getCustomerId());

        Map<String, Object> data = new HashMap<>();
        data.put("quotation", quotation);
        data.put("customer", customer);

        byte[] pdfBytes = pdfService.generatePdf("pdf/quotation-template", data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "quotation_" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
