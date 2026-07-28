package com.neosow.infra.service.impl;

import com.neosow.infra.dto.quotation.QuotationDTO;
import com.neosow.infra.exception.BadRequestException;
import com.neosow.infra.exception.ResourceNotFoundException;
import com.neosow.infra.mapper.QuotationMapper;
import com.neosow.infra.model.Customer;
import com.neosow.infra.model.Quotation;
import com.neosow.infra.model.QuotationItem;
import com.neosow.infra.model.QuotationStatus;
import com.neosow.infra.model.User;
import com.neosow.infra.repository.CustomerRepository;
import com.neosow.infra.repository.QuotationRepository;
import com.neosow.infra.repository.UserRepository;
import com.neosow.infra.service.QuotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotationServiceImpl implements QuotationService {

    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final QuotationMapper quotationMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public QuotationDTO createQuotation(QuotationDTO quotationDto) {
        log.info("Creating quotation for customer ID: {}", quotationDto.getCustomerId());
        
        Customer customer = customerRepository.findById(quotationDto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + quotationDto.getCustomerId()));
        checkCustomerOwnership(customer);

        Quotation quotation = quotationMapper.toEntity(quotationDto);
        quotation.setCustomer(customer);
        if (quotationDto.getStatus() != null) {
            try {
                quotation.setStatus(QuotationStatus.valueOf(quotationDto.getStatus().toUpperCase().trim()));
            } catch (IllegalArgumentException e) {
                quotation.setStatus(QuotationStatus.ENQUIRY);
            }
        } else {
            quotation.setStatus(QuotationStatus.ENQUIRY);
        }

        if (quotationDto.getParentQuotationId() != null) {
            boolean parentExists = quotationRepository.existsById(quotationDto.getParentQuotationId());
            if (!parentExists) {
                throw new ResourceNotFoundException("Parent quotation not found with ID: " + quotationDto.getParentQuotationId());
            }
            quotation.setParentQuotationId(quotationDto.getParentQuotationId());
        }

        BigDecimal calculatedSubtotal = BigDecimal.ZERO;

        for (var itemDto : quotationDto.getItems()) {
            QuotationItem item = quotationMapper.toEntity(itemDto);

            // Compute dimension values for quantity calculations
            double w = parseDimension(itemDto.getWidth(), quotationDto.getProjectUnit());
            double h = parseDimension(itemDto.getHeight(), quotationDto.getProjectUnit());
            double d = parseDimension(itemDto.getDepth(), quotationDto.getProjectUnit());

            double qty = calculateQuantity(w, h, d, itemDto.getUnit());
            item.setQty(BigDecimal.valueOf(qty).setScale(2, RoundingMode.HALF_UP));

            // Multiplier noOfUnit defaults to 1.0 if not specified
            BigDecimal noOfUnit = itemDto.getNoOfUnit() != null ? itemDto.getNoOfUnit() : BigDecimal.ONE;
            item.setNoOfUnit(noOfUnit);

            // Calculate totalQty: qty * noOfUnit
            BigDecimal totalQty = item.getQty().multiply(noOfUnit).setScale(2, RoundingMode.HALF_UP);
            item.setTotalQty(totalQty);

            // Calculate item amount: totalQty * rate
            BigDecimal rate = itemDto.getUnitRate();
            BigDecimal amount = totalQty.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            item.setAmount(amount);

            calculatedSubtotal = calculatedSubtotal.add(amount);
            quotation.addItem(item);
        }

        quotation.setSubtotal(calculatedSubtotal.setScale(2, RoundingMode.HALF_UP));
        
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal discountPercent = quotationDto.getDiscountPercent();
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            // Percent discount: discount = subtotal * (percent / 100)
            discount = calculatedSubtotal.multiply(discountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            quotation.setDiscountPercent(discountPercent);
        } else {
            // Flat discount
            discount = quotationDto.getDiscount() != null ? quotationDto.getDiscount() : BigDecimal.ZERO;
            quotation.setDiscountPercent(null);
        }
        quotation.setDiscount(discount);

        BigDecimal netSubtotal = calculatedSubtotal.subtract(discount);
        if (netSubtotal.compareTo(BigDecimal.ZERO) < 0) {
            netSubtotal = BigDecimal.ZERO;
        }

        // Calculate GST (18%)
        BigDecimal gstAmount = BigDecimal.ZERO;
        if (quotationDto.isIncludeGst()) {
            gstAmount = netSubtotal.multiply(BigDecimal.valueOf(0.18)).setScale(2, RoundingMode.HALF_UP);
        }
        quotation.setIncludeGst(quotationDto.isIncludeGst());
        quotation.setGstAmount(gstAmount);

        // Final total
        BigDecimal totalAmount = netSubtotal.add(gstAmount).setScale(2, RoundingMode.HALF_UP);
        quotation.setTotalAmount(totalAmount);

        Quotation savedQuotation = quotationRepository.save(quotation);
        log.info("Quotation saved successfully with ID: {}", savedQuotation.getId());
        return quotationMapper.toDto(savedQuotation);
    }

    @Override
    @Transactional(readOnly = true)
    public QuotationDTO getQuotationById(UUID id) {
        log.info("Fetching quotation with ID: {}", id);
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found with ID: " + id));
        checkQuotationOwnership(quotation);
        return quotationMapper.toDto(quotation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuotationDTO> getQuotationsByCustomerId(UUID customerId, int page, int size) {
        log.info("Fetching paginated quotations for customer ID: {}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
        checkCustomerOwnership(customer);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        Set<String> emails = getAccessibleEmails(auth);

        return quotationRepository.findByCustomerIdFiltered(isSuperAdmin, emails, customerId, pageable)
                .map(quotationMapper::toDto);
    }

    @Override
    @Transactional
    public void deleteQuotation(UUID id) {
        log.info("Deleting quotation with ID: {}", id);
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found with ID: " + id));
        checkQuotationOwnership(quotation);
        quotationRepository.delete(quotation);
        log.info("Quotation deleted successfully: {}", id);
    }

    @Override
    @Transactional
    public void updateQuotationStatus(UUID id, String statusStr) {
        log.info("Updating status of quotation ID: {} to {}", id, statusStr);
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found with ID: " + id));
        checkQuotationOwnership(quotation);
        
        if (statusStr == null) {
            throw new BadRequestException("Status parameter is required");
        }

        try {
            QuotationStatus status = QuotationStatus.valueOf(statusStr.toUpperCase().trim());
            quotation.setStatus(status);
            quotationRepository.save(quotation);
            log.info("Quotation ID {} status updated successfully to {}", id, status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid quotation status: " + statusStr);
        }
    }

    private void checkCustomerOwnership(Customer customer) {
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

    private void checkQuotationOwnership(Quotation quotation) {
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
        if (quotation.getCreatedBy() == null || !emails.contains(quotation.getCreatedBy())) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to access this quotation");
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

    // Helper functions matching JS prototype rules
    private double parseDimension(String val, String projectUnit) {
        if (val == null || val.trim().isEmpty()) {
            return 0.0;
        }
        String s = val.trim();

        double feet = 0.0;
        double inches = 0.0;
        boolean isFtInchPattern = false;

        // Standard decimal validation
        if (s.matches("^[-+]?[0-9]*\\.?[0-9]+$")) {
            if ("Ft Inch".equalsIgnoreCase(projectUnit)) {
                double decimalVal = Double.parseDouble(s);
                feet = Math.floor(decimalVal);
                inches = (decimalVal - feet) * 12.0;
                isFtInchPattern = true;
            } else {
                return Double.parseDouble(s);
            }
        } else {
            // Feet-inch pattern (e.g. 5' 6" or 5ft 6in)
            try {
                String[] parts = s.split("'|ft|FT");
                if (parts.length > 0) {
                    feet = Double.parseDouble(parts[0].trim());
                    if (parts.length > 1) {
                        String inchStr = parts[1].replaceAll("[\"inIN\\s]", "");
                        if (!inchStr.isEmpty()) {
                            inches = Double.parseDouble(inchStr);
                        }
                    }
                    isFtInchPattern = true;
                }
            } catch (Exception e) {
                log.warn("Failed to parse feet-inch string: '{}', falling back to safe parsing.", val);
            }
        }

        if (isFtInchPattern && "Ft Inch".equalsIgnoreCase(projectUnit)) {
            // Apply rounding:
            // 0-3 => 3 inches (0.25 ft)
            // 4-6 => 6 inches (0.50 ft)
            // 7-9 => 9 inches (0.75 ft)
            // 10-12 => 12 inches (1.0 ft)
            double roundedInches = 3.0;
            double ft = feet;
            if (inches <= 3.0) {
                roundedInches = 3.0;
            } else if (inches <= 6.0) {
                roundedInches = 6.0;
            } else if (inches <= 9.0) {
                roundedInches = 9.0;
            } else {
                ft += 1.0;
                roundedInches = 0.0;
            }
            return ft + (roundedInches / 12.0);
        }

        // Fallback/standard parsing
        try {
            String[] parts = s.split("'|ft|FT");
            if (parts.length > 0) {
                double f = Double.parseDouble(parts[0].trim());
                double inch = 0.0;
                if (parts.length > 1) {
                    String inchStr = parts[1].replaceAll("[\"inIN\\s]", "");
                    if (!inchStr.isEmpty()) {
                        inch = Double.parseDouble(inchStr);
                    }
                }
                return f + (inch / 12.0);
            }
        } catch (Exception e) {
            // Ignore
        }

        try {
            return Double.parseDouble(s.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double calculateQuantity(double width, double height, double depth, String unit) {
        if (unit == null) {
            return 1.0;
        }
        String normUnit = unit.toUpperCase().replace(".", "").replace(" ", "");

        if (normUnit.contains("SQFT") || normUnit.contains("SQMTR")) {
            // Condition 1: Multiply two highest values
            double[] dims = {width, height, depth};
            Arrays.sort(dims);
            return dims[1] * dims[2];
        } else if (normUnit.contains("RFT") || normUnit.contains("RMTR")) {
            // Condition 2: Highest single value
            return Math.max(width, Math.max(height, depth));
        } else if (normUnit.contains("CUFT") || normUnit.contains("CUMTR") || 
                   normUnit.contains("NO") || normUnit.contains("NUM") || 
                   normUnit.contains("JOB")) {
            // Condition 3, 4, 5: Multiply all three
            double product = width * height * depth;
            if ((normUnit.contains("NO") || normUnit.contains("NUM") || normUnit.contains("JOB")) && 
                width == 0.0 && height == 0.0 && depth == 0.0) {
                return 1.0;
            }
            return product;
        } else {
            return 1.0;
        }
    }
}
