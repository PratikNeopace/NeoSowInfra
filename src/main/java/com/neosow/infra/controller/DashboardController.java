package com.neosow.infra.controller;

import com.neosow.infra.dto.dashboard.AdminDashboardDTO;
import com.neosow.infra.dto.dashboard.SuperAdminDashboardDTO;
import com.neosow.infra.dto.dashboard.UserDashboardDTO;
import com.neosow.infra.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/user")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<UserDashboardDTO> getUserDashboard() {
        log.info("REST request to retrieve User dashboard statistics");
        UserDashboardDTO stats = dashboardService.getUserDashboard();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<AdminDashboardDTO> getAdminDashboard() {
        log.info("REST request to retrieve Admin dashboard statistics");
        AdminDashboardDTO stats = dashboardService.getAdminDashboard();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/super-admin")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<SuperAdminDashboardDTO> getSuperAdminDashboard() {
        log.info("REST request to retrieve Super Admin dashboard statistics");
        SuperAdminDashboardDTO stats = dashboardService.getSuperAdminDashboard();
        return ResponseEntity.ok(stats);
    }
}
