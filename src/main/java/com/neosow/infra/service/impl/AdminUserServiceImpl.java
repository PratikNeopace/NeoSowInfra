package com.neosow.infra.service.impl;

import com.neosow.infra.dto.admin.ApprovalRequestDTO;
import com.neosow.infra.dto.admin.ApprovalResponseDTO;
import com.neosow.infra.dto.admin.UserCreationRequest;
import com.neosow.infra.dto.admin.UserManagementDTO;
import com.neosow.infra.exception.BadRequestException;
import com.neosow.infra.exception.ResourceNotFoundException;
import com.neosow.infra.mapper.ApprovalMapper;
import com.neosow.infra.mapper.UserMapper;
import com.neosow.infra.model.*;
import com.neosow.infra.repository.ApprovalRepository;
import com.neosow.infra.repository.RoleRepository;
import com.neosow.infra.repository.UserRepository;
import com.neosow.infra.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ApprovalRepository approvalRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ApprovalMapper approvalMapper;

    @Override
    @Transactional
    public UserManagementDTO createUser(UserCreationRequest request) {
        log.info("Creating user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Error: Email is already in use!");
        }

        String currentAdminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentAdminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found"));

        boolean isSuperAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_SUPER_ADMIN);

        Set<Role> roles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            ERole eRole;
            try {
                eRole = ERole.valueOf(roleName);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Error: Invalid role name: " + roleName);
            }

            // Standard ADMIN cannot create ADMIN or SUPER_ADMIN users
            if (!isSuperAdmin && (eRole == ERole.ROLE_ADMIN || eRole == ERole.ROLE_SUPER_ADMIN)) {
                log.warn("Admin '{}' tried to create a user with privileged role: {}", currentAdminEmail, eRole);
                throw new AccessDeniedException("Only SUPER_ADMIN can create ADMIN or SUPER_ADMIN users.");
            }

            Role role = roleRepository.findByName(eRole)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + eRole));
            roles.add(role);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .roles(roles)
                .status(UserStatus.ACTIVE)
                .build();
        user.setCreatedBy(currentAdminEmail);

        if (!isSuperAdmin) {
            user.setParentAdminId(currentUser.getId());
        }

        User savedUser = userRepository.save(user);
        log.info("User created successfully: {}", savedUser.getEmail());
        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserManagementDTO> getUsers(int page, int size) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        boolean isSuperAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_SUPER_ADMIN);

        Page<User> usersPage;
        if (isSuperAdmin) {
            List<User> allUsers = userRepository.findAll();
            List<User> sortedUsers = sortUsersHierarchically(allUsers);

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), sortedUsers.size());
            
            List<User> pageContent = new ArrayList<>();
            if (start < sortedUsers.size()) {
                pageContent = sortedUsers.subList(start, end);
            }
            usersPage = new PageImpl<>(pageContent, pageable, sortedUsers.size());
        } else {
            usersPage = userRepository.findByParentAdminId(currentUser.getId(), pageable);
        }
        return usersPage.map(userMapper::toDto);
    }

    private List<User> sortUsersHierarchically(List<User> allUsers) {
        List<User> superAdmins = new ArrayList<>();
        List<User> admins = new ArrayList<>();
        List<User> regularUsers = new ArrayList<>();

        for (User u : allUsers) {
            boolean isSuper = u.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_SUPER_ADMIN);
            boolean isAdmin = u.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_ADMIN) && !isSuper;

            if (isSuper) {
                superAdmins.add(u);
            } else if (isAdmin) {
                admins.add(u);
            } else {
                regularUsers.add(u);
            }
        }

        superAdmins.sort(Comparator.comparing(u -> u.getEmail().toLowerCase()));
        admins.sort(Comparator.comparing(u -> u.getEmail().toLowerCase()));
        regularUsers.sort(Comparator.comparing(u -> u.getEmail().toLowerCase()));

        List<User> sorted = new ArrayList<>();
        sorted.addAll(superAdmins);

        Set<UUID> addedUserIds = new HashSet<>();
        for (User admin : admins) {
            sorted.add(admin);
            for (User user : regularUsers) {
                if (admin.getId().equals(user.getParentAdminId())) {
                    sorted.add(user);
                    addedUserIds.add(user.getId());
                }
            }
        }

        for (User user : regularUsers) {
            if (!addedUserIds.contains(user.getId())) {
                sorted.add(user);
            }
        }

        return sorted;
    }

    @Override
    @Transactional
    public void toggleUserStatus(UUID id, String securityCode) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found"));

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        boolean isSuperAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_SUPER_ADMIN);

        boolean userIsPrivileged = user.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_ADMIN || r.getName() == ERole.ROLE_SUPER_ADMIN);

        if (!isSuperAdmin) {
            if (userIsPrivileged || !currentUser.getId().equals(user.getParentAdminId())) {
                throw new AccessDeniedException("Not authorized to manage this user");
            }
        }

        // Security check to avoid blocking Super Admin by mistake
        boolean targetIsSuperAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_SUPER_ADMIN);
        if (targetIsSuperAdmin && user.isEnabled()) {
            if (securityCode == null || !securityCode.equals("1998")) {
                throw new BadRequestException("Blocking a Super Admin requires the correct security code");
            }
        }

        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        log.info("User ID {} status toggled. Enabled: {}", id, user.isEnabled());
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        userRepository.delete(user);
        log.info("User ID {} deleted", id);
    }

    @Override
    @Transactional
    public ApprovalResponseDTO submitApproval(ApprovalRequestDTO request) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found"));

        ApprovalType type;
        try {
            type = ApprovalType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid approval type: " + request.getType());
        }

        Approval approval = Approval.builder()
                .type(type)
                .submittedBy(currentUser.getId())
                .status(ApprovalStatus.PENDING)
                .build();

        Approval savedApproval = approvalRepository.save(approval);
        log.info("Approval request submitted of type: {}", type);
        return approvalMapper.toDto(savedApproval);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalResponseDTO> getApprovals(int page, int size) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        boolean isSuperAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_SUPER_ADMIN);

        Page<Approval> approvalsPage;
        if (isSuperAdmin) {
            approvalsPage = approvalRepository.findAll(pageable);
        } else {
            approvalsPage = approvalRepository.findBySubmittedBy(currentUser.getId(), pageable);
        }
        return approvalsPage.map(approvalMapper::toDto);
    }

    @Override
    @Transactional
    public ApprovalResponseDTO processApproval(UUID approvalId, String status) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found"));

        boolean isSuperAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_SUPER_ADMIN);
        if (!isSuperAdmin) {
            throw new AccessDeniedException("Only SUPER_ADMIN can review approval requests.");
        }

        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found"));

        ApprovalStatus approvalStatus;
        try {
            approvalStatus = ApprovalStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid approval status: " + status);
        }

        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("Approval request is already processed");
        }

        approval.setStatus(approvalStatus);
        approval.setReviewedBy(currentUser.getId());
        
        Approval savedApproval = approvalRepository.save(approval);
        log.info("Approval ID {} reviewed and set to {}", approvalId, approvalStatus);
        return approvalMapper.toDto(savedApproval);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserManagementDTO> getAdmins(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return userRepository.findAllByRoleName(ERole.ROLE_ADMIN, pageable).map(userMapper::toDto);
    }

    @Override
    @Transactional
    public void toggleAdminStatus(UUID adminId, String status, String securityCode) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found"));

        boolean isSuperAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_SUPER_ADMIN);
        if (!isSuperAdmin) {
            throw new AccessDeniedException("Only SUPER_ADMIN can manage admin statuses.");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with ID: " + adminId));

        boolean isAdmin = admin.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_ADMIN);
        if (!isAdmin) {
            throw new BadRequestException("Target user is not an Admin");
        }

        UserStatus userStatus;
        try {
            userStatus = UserStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + status);
        }

        // Security check to avoid blocking Super Admin by mistake
        boolean targetIsSuperAdmin = admin.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_SUPER_ADMIN);
        if (targetIsSuperAdmin && userStatus == UserStatus.BLOCKED) {
            if (securityCode == null || !securityCode.equals("1998")) {
                throw new BadRequestException("Blocking a Super Admin requires the correct security code");
            }
        }

        admin.setStatus(userStatus);
        admin.setEnabled(userStatus == UserStatus.ACTIVE);
        userRepository.save(admin);
        log.info("Admin ID {} status changed to {}", adminId, userStatus);
    }
}
