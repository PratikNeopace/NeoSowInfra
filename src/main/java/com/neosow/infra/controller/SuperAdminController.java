package com.neosow.infra.controller;

import com.neosow.infra.dto.admin.ApprovalResponseDTO;
import com.neosow.infra.dto.admin.UserManagementDTO;
import com.neosow.infra.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
@Slf4j
public class SuperAdminController {

    private final AdminUserService adminUserService;

    @GetMapping("/admins")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<UserManagementDTO>> getAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST request to view all registered Admins");
        Page<UserManagementDTO> admins = adminUserService.getAdmins(page, size);
        return ResponseEntity.ok(admins);
    }

    @PutMapping("/admins/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> toggleAdminStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestParam(required = false) String securityCode) {
        log.info("REST request to change status of Admin ID {} to {} with security code: {}", id, status, securityCode);
        adminUserService.toggleAdminStatus(id, status, securityCode);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/approvals")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<ApprovalResponseDTO>> getApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST request to view all approvals");
        Page<ApprovalResponseDTO> approvals = adminUserService.getApprovals(page, size);
        return ResponseEntity.ok(approvals);
    }

    @PutMapping("/approvals/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApprovalResponseDTO> processApproval(
            @PathVariable UUID id,
            @RequestParam String status) {
        log.info("REST request to process Approval ID {} with status {}", id, status);
        ApprovalResponseDTO response = adminUserService.processApproval(id, status);
        return ResponseEntity.ok(response);
    }
}
