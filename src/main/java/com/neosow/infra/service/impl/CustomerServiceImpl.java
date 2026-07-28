package com.neosow.infra.service.impl;

import com.neosow.infra.dto.customer.CustomerDTO;
import com.neosow.infra.exception.ResourceNotFoundException;
import com.neosow.infra.mapper.CustomerMapper;
import com.neosow.infra.model.Customer;
import com.neosow.infra.model.FamilyMember;
import com.neosow.infra.model.Project;
import com.neosow.infra.model.User;
import com.neosow.infra.repository.CustomerRepository;
import com.neosow.infra.repository.UserRepository;
import com.neosow.infra.service.CustomerService;
import com.neosow.infra.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CustomerDTO createCustomer(CustomerDTO customerDto) {
        log.info("Creating customer with name: {}", customerDto.getName());
        
        Customer customer = customerMapper.toEntity(customerDto);
        
        // Link project back-reference
        if (customerDto.getProject() != null) {
            Project project = customerMapper.toEntity(customerDto.getProject());
            customer.setProject(project);
        }
        
        // Link family members back-references
        if (customerDto.getFamilyMembers() != null) {
            for (var memDto : customerDto.getFamilyMembers()) {
                FamilyMember member = customerMapper.toEntity(memDto);
                customer.addFamilyMember(member);
            }
        }

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer saved successfully with ID: {}", savedCustomer.getId());
        return customerMapper.toDto(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(UUID id) {
        log.info("Fetching customer with ID: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
        checkOwnership(customer);
        return customerMapper.toDto(customer);
    }

    @Override
    @Transactional
    public CustomerDTO updateCustomer(UUID id, CustomerDTO customerDto) {
        log.info("Updating customer with ID: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
        checkOwnership(customer);
        
        customer.setName(customerDto.getName());
        customer.setPhone(customerDto.getPhone());
        customer.setAddress(customerDto.getAddress());
        customer.setBirthDate(customerDto.getBirthDate());
        customer.setAnniversaryDate(customerDto.getAnniversaryDate());
        customer.setDrawingPlanUrl(customerDto.getDrawingPlanUrl());

        // Update project
        if (customerDto.getProject() != null) {
            Project project = customerMapper.toEntity(customerDto.getProject());
            customer.setProject(project);
        } else {
            customer.setProject(null);
        }

        // Update family members
        customer.getFamilyMembers().clear();
        if (customerDto.getFamilyMembers() != null) {
            for (var memDto : customerDto.getFamilyMembers()) {
                FamilyMember member = customerMapper.toEntity(memDto);
                customer.addFamilyMember(member);
            }
        }

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer updated successfully with ID: {}", savedCustomer.getId());
        return customerMapper.toDto(savedCustomer);
    }

    @Override
    @Transactional
    public void deleteCustomer(UUID id) {
        log.info("Deleting customer with ID: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
        checkOwnership(customer);
        customerRepository.delete(customer);
        log.info("Customer deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerDTO> getCustomers(String query, int page, int size) {
        log.info("Fetching paginated customers list. Query: {}", query);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        Set<String> emails = getAccessibleEmails(auth);

        Page<Customer> customers;
        if (query == null || query.trim().isEmpty()) {
            customers = customerRepository.findAllFiltered(isSuperAdmin, emails, pageable);
        } else {
            customers = customerRepository.searchFiltered(isSuperAdmin, emails, query.trim(), pageable);
        }
        return customers.map(customerMapper::toDto);
    }

    private void checkOwnership(Customer customer) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return;
        }
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (isSuperAdmin) {
            return;
        }
        Set<String> emails = getAccessibleEmails(auth);
        if (customer.getCreatedBy() == null || !emails.contains(customer.getCreatedBy())) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to access this customer");
        }
    }

    private Set<String> getAccessibleEmails(Authentication auth) {
        String email = auth.getName();
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (isSuperAdmin) {
            return Collections.emptySet();
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        Set<String> emails = new HashSet<>();
        emails.add(email);

        if (isAdmin) {
            User currentUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
            List<String> subEmails = userRepository.findEmailsByParentAdminId(currentUser.getId());
            emails.addAll(subEmails);
        }
        return emails;
    }
}
