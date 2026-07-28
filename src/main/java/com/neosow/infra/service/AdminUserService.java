package com.neosow.infra.service;

import com.neosow.infra.dto.admin.ApprovalRequestDTO;
import com.neosow.infra.dto.admin.ApprovalResponseDTO;
import com.neosow.infra.dto.admin.UserCreationRequest;
import com.neosow.infra.dto.admin.UserManagementDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface AdminUserService {
    UserManagementDTO createUser(UserCreationRequest request);
    Page<UserManagementDTO> getUsers(int page, int size);
    void toggleUserStatus(UUID id, String securityCode);
    void deleteUser(UUID id);
    
    ApprovalResponseDTO submitApproval(ApprovalRequestDTO request);
    Page<ApprovalResponseDTO> getApprovals(int page, int size);
    ApprovalResponseDTO processApproval(UUID approvalId, String status);
    
    Page<UserManagementDTO> getAdmins(int page, int size);
    void toggleAdminStatus(UUID adminId, String status, String securityCode);
}
