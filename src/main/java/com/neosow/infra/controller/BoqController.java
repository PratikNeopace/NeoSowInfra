package com.neosow.infra.controller;

import com.neosow.infra.dto.boq.ApprovedBoqItemDTO;
import com.neosow.infra.dto.boq.BoqItemDTO;
import com.neosow.infra.dto.boq.ImportJobResponseDTO;
import com.neosow.infra.service.BoqImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.neosow.infra.model.QuotationType;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/boq")
@RequiredArgsConstructor
@Slf4j
public class BoqController {

    private final BoqImportService boqImportService;

    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ImportJobResponseDTO> importBoq(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "quotationType", required = false) QuotationType quotationType) {
        log.info("REST request to import BOQ spreadsheet: {} with quotationType: {}", file.getOriginalFilename(), quotationType);
        ImportJobResponseDTO result = boqImportService.importBoq(file, quotationType);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @GetMapping("/imports")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<ImportJobResponseDTO>> getImportJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST request to list import jobs list. Page: {}, Size: {}", page, size);
        Page<ImportJobResponseDTO> result = boqImportService.getImportJobs(page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<InputStreamResource> downloadTemplate() {
        log.info("REST request to download empty BOQ import template spreadsheet");
        ByteArrayInputStream in = boqImportService.generateTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=boq_import_template.xlsx");
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(new InputStreamResource(in));
    }

    @GetMapping("/imports/{jobId}/summary")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<InputStreamResource> downloadSummary(@PathVariable UUID jobId) {
        log.info("REST request to download import job summary spreadsheet for ID: {}", jobId);
        ByteArrayInputStream in = boqImportService.generateSummary(jobId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=boq_import_summary_" + jobId + ".xlsx");
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(new InputStreamResource(in));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<BoqItemDTO>> getPendingBoqItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST request to get pending BOQ items list. Page: {}, Size: {}", page, size);
        Page<BoqItemDTO> result = boqImportService.getPendingBoqItems(page, size);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<BoqItemDTO> approveBoqItem(@PathVariable UUID id) {
        log.info("REST request to approve BOQ Item ID: {}", id);
        BoqItemDTO result = boqImportService.approveBoqItem(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<BoqItemDTO> rejectBoqItem(@PathVariable UUID id) {
        log.info("REST request to reject BOQ Item ID: {}", id);
        BoqItemDTO result = boqImportService.rejectBoqItem(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/approved")
    public ResponseEntity<List<ApprovedBoqItemDTO>> getApprovedBoqItems(
            @RequestParam(required = false) QuotationType quotationType) {
        log.info("REST request to retrieve approved BOQ items for dropdown listing. Filter by quotationType: {}", quotationType);
        List<ApprovedBoqItemDTO> result = boqImportService.getApprovedBoqItems(quotationType);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<BoqItemDTO> createManualBoqItem(@RequestBody BoqItemDTO boqItemDTO) {
        log.info("REST request to manually create BOQ item: {}", boqItemDTO);
        BoqItemDTO result = boqImportService.createManualBoqItem(boqItemDTO);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @GetMapping("/items")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<BoqItemDTO>> getAllBoqItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        log.info("REST request to get all BOQ items. Page: {}, Size: {}, Search: {}", page, size, search);
        Page<BoqItemDTO> result = boqImportService.getAllBoqItems(page, size, search);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<BoqItemDTO> updateBoqItem(@PathVariable UUID id, @RequestBody BoqItemDTO boqItemDTO) {
        log.info("REST request to update BOQ item ID: {}, body: {}", id, boqItemDTO);
        BoqItemDTO result = boqImportService.updateBoqItem(id, boqItemDTO);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deleteBoqItem(@PathVariable UUID id) {
        log.info("REST request to delete BOQ item ID: {}", id);
        boqImportService.deleteBoqItem(id);
        return ResponseEntity.noContent().build();
    }
}





