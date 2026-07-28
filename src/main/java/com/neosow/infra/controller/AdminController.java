package com.neosow.infra.controller;

import com.neosow.infra.dto.admin.ApprovalResponseDTO;
import com.neosow.infra.dto.admin.UserCreationRequest;
import com.neosow.infra.dto.admin.UserManagementDTO;
import com.neosow.infra.dto.dashboard.AdminDashboardDTO;
import com.neosow.infra.dto.admin.ApprovalRequestDTO;
import com.neosow.infra.service.AdminUserService;
import com.neosow.infra.service.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminUserService adminUserService;
    private final DashboardService dashboardService;

    @GetMapping("/users")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<UserManagementDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST request to view all registered users");
        Page<UserManagementDTO> users = adminUserService.getUsers(page, size);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<UserManagementDTO> createUser(@Valid @RequestBody UserCreationRequest request) {
        log.info("REST request to create user: {}", request.getEmail());
        UserManagementDTO user = adminUserService.createUser(request);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @PutMapping("/users/{id}/toggle")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> toggleUserStatus(
            @PathVariable UUID id,
            @RequestParam(required = false) String securityCode) {
        log.info("REST request to toggle user status for ID: {} with security code: {}", id, securityCode);
        adminUserService.toggleUserStatus(id, securityCode);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        log.info("REST request to delete user ID: {}", id);
        adminUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rates")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApprovalResponseDTO> submitRateChange() {
        log.info("REST request to submit rate change approval");
        ApprovalRequestDTO request = new ApprovalRequestDTO();
        request.setType("RATE_CHANGE");
        ApprovalResponseDTO response = adminUserService.submitApproval(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/approvals")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApprovalResponseDTO> submitApproval(@Valid @RequestBody ApprovalRequestDTO request) {
        log.info("REST request to submit approval: {}", request.getType());
        ApprovalResponseDTO response = adminUserService.submitApproval(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/approvals")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<ApprovalResponseDTO>> getApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST request to view submitted approvals");
        Page<ApprovalResponseDTO> approvals = adminUserService.getApprovals(page, size);
        return ResponseEntity.ok(approvals);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<AdminDashboardDTO> getDashboardStats() {
        log.info("REST request to retrieve dashboard statistics for Admin");
        AdminDashboardDTO stats = dashboardService.getAdminDashboard();
        return ResponseEntity.ok(stats);
    }
}
