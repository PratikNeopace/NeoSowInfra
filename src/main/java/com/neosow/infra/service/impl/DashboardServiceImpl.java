package com.neosow.infra.service.impl;

import com.neosow.infra.dto.dashboard.AdminDashboardDTO;
import com.neosow.infra.dto.dashboard.SuperAdminDashboardDTO;
import com.neosow.infra.dto.dashboard.UserDashboardDTO;
import com.neosow.infra.exception.ResourceNotFoundException;
import com.neosow.infra.model.ApprovalStatus;
import com.neosow.infra.model.ERole;
import com.neosow.infra.model.User;
import com.neosow.infra.repository.ApprovalRepository;
import com.neosow.infra.repository.CustomerRepository;
import com.neosow.infra.repository.QuotationRepository;
import com.neosow.infra.repository.UserRepository;
import com.neosow.infra.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;
    private final ApprovalRepository approvalRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDashboardDTO getUserDashboard() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Fetching user dashboard stats for: {}", email);

        long totalCustomers = customerRepository.countByCreatedBy(email);
        long totalQuotations = quotationRepository.countByCreatedBy(email);
        BigDecimal totalQuotationAmount = quotationRepository.sumTotalAmountByCreatedBy(email);

        return UserDashboardDTO.builder()
                .totalCustomers(totalCustomers)
                .totalQuotations(totalQuotations)
                .totalQuotationAmount(totalQuotationAmount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardDTO getAdminDashboard() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Fetching admin dashboard stats for: {}", email);

        User currentAdmin = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found"));

        long totalUsers = userRepository.countByParentAdminId(currentAdmin.getId());

        List<String> emails = new ArrayList<>();
        emails.add(email);
        List<String> subEmails = userRepository.findEmailsByParentAdminId(currentAdmin.getId());
        emails.addAll(subEmails);

        long totalCustomers = customerRepository.countByCreatedByIn(emails);
        long totalQuotations = quotationRepository.countByCreatedByIn(emails);
        BigDecimal totalQuotationAmount = quotationRepository.sumTotalAmountByCreatedByIn(emails);

        return AdminDashboardDTO.builder()
                .totalUsers(totalUsers)
                .totalCustomers(totalCustomers)
                .totalQuotations(totalQuotations)
                .totalQuotationAmount(totalQuotationAmount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SuperAdminDashboardDTO getSuperAdminDashboard() {
        log.info("Fetching super admin dashboard stats");

        long totalAdmins = userRepository.countByRoleName(ERole.ROLE_ADMIN);
        long totalUsers = userRepository.countByRoleName(ERole.ROLE_USER);
        long totalCustomers = customerRepository.count();
        long totalQuotations = quotationRepository.count();
        BigDecimal totalQuotationAmount = quotationRepository.sumAllTotalAmount();
        long totalPendingApprovals = approvalRepository.countByStatus(ApprovalStatus.PENDING);

        return SuperAdminDashboardDTO.builder()
                .totalAdmins(totalAdmins)
                .totalUsers(totalUsers)
                .totalCustomers(totalCustomers)
                .totalQuotations(totalQuotations)
                .totalQuotationAmount(totalQuotationAmount)
                .totalPendingApprovals(totalPendingApprovals)
                .build();
    }
}
