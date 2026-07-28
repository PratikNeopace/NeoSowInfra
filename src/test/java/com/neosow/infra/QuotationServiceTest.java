package com.neosow.infra;

import com.neosow.infra.dto.quotation.QuotationDTO;
import com.neosow.infra.dto.quotation.QuotationItemDTO;
import com.neosow.infra.model.Customer;
import com.neosow.infra.repository.CustomerRepository;
import com.neosow.infra.repository.QuotationRepository;
import com.neosow.infra.service.impl.QuotationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.neosow.infra.mapper.QuotationMapper;
import com.neosow.infra.model.Quotation;
import com.neosow.infra.model.QuotationItem;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class QuotationServiceTest {

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private QuotationMapper quotationMapper;

    @InjectMocks
    private QuotationServiceImpl quotationService;

    private Customer testCustomer;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        testCustomer = new Customer();
        testCustomer.setId(customerId);
        testCustomer.setName("Test Client");
        testCustomer.setPhone("1234567890");
    }

    @Test
    void testCreateQuotationWithSqftQuantityRules() {
        // Arrange
        QuotationItemDTO itemDto = QuotationItemDTO.builder()
                .category("partitions")
                .subcategory("Glass Partition")
                .width("5' 6\"") // 5.5 ft
                .height("5' 10\"") // 5' 10" rounds to 6.0 ft
                .depth("0")
                .unit("SQ.FT.") // Area rule: Multiply two highest (5.5 * 6 = 33)
                .unitRate(BigDecimal.valueOf(100)) // 33 * 100 = 3300
                .build();

        QuotationDTO inputDto = QuotationDTO.builder()
                .customerId(customerId)
                .projectUnit("Ft Inch")
                .discount(BigDecimal.ZERO)
                .includeGst(true) // 3300 * 0.18 = 594
                .items(Collections.singletonList(itemDto))
                .build();

        Quotation mockQuotation = new Quotation();
        mockQuotation.setItems(new java.util.ArrayList<>());

        Mockito.when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        Mockito.when(quotationMapper.toEntity(any(QuotationDTO.class))).thenReturn(mockQuotation);
        Mockito.when(quotationMapper.toEntity(any(QuotationItemDTO.class))).thenReturn(new QuotationItem());
        Mockito.when(quotationRepository.save(any(Quotation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(quotationMapper.toDto(any(Quotation.class))).thenAnswer(invocation -> {
            Quotation q = invocation.getArgument(0);
            return QuotationDTO.builder()
                    .subtotal(q.getSubtotal())
                    .discount(q.getDiscount())
                    .gstAmount(q.getGstAmount())
                    .totalAmount(q.getTotalAmount())
                    .build();
        });

        // Act
        QuotationDTO result = quotationService.createQuotation(inputDto);

        // Assert
        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(3300.00).compareTo(result.getSubtotal()), "Subtotal should be 3300");
        assertEquals(0, BigDecimal.valueOf(594.00).compareTo(result.getGstAmount()), "GST should be 594");
        assertEquals(0, BigDecimal.valueOf(3894.00).compareTo(result.getTotalAmount()), "Total amount should be 3894");
    }

    @Test
    void testCreateQuotationWithRftQuantityRules() {
        // Arrange
        QuotationItemDTO itemDto = QuotationItemDTO.builder()
                .category("partitions")
                .width("3' 10\"") // rounds to 4.0 ft
                .height("7' 10\"") // rounds to 8.0 ft (highest)
                .depth("1' 10\"") // rounds to 2.0 ft
                .unit("R.FT.") // Running length rule: max of three (8.0)
                .unitRate(BigDecimal.valueOf(50)) // 8 * 50 = 400
                .build();

        QuotationDTO inputDto = QuotationDTO.builder()
                .customerId(customerId)
                .projectUnit("Ft Inch")
                .discount(BigDecimal.valueOf(100)) // 400 - 100 = 300
                .includeGst(true) // 300 * 0.18 = 54
                .items(Collections.singletonList(itemDto))
                .build();

        Quotation mockQuotation = new Quotation();
        mockQuotation.setItems(new java.util.ArrayList<>());

        Mockito.when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        Mockito.when(quotationMapper.toEntity(any(QuotationDTO.class))).thenReturn(mockQuotation);
        Mockito.when(quotationMapper.toEntity(any(QuotationItemDTO.class))).thenReturn(new QuotationItem());
        Mockito.when(quotationRepository.save(any(Quotation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(quotationMapper.toDto(any(Quotation.class))).thenAnswer(invocation -> {
            Quotation q = invocation.getArgument(0);
            return QuotationDTO.builder()
                    .subtotal(q.getSubtotal())
                    .discount(q.getDiscount())
                    .gstAmount(q.getGstAmount())
                    .totalAmount(q.getTotalAmount())
                    .build();
        });

        // Act
        QuotationDTO result = quotationService.createQuotation(inputDto);

        // Assert
        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(400.00).compareTo(result.getSubtotal()), "Subtotal should be 400");
        assertEquals(0, BigDecimal.valueOf(54.00).compareTo(result.getGstAmount()), "GST should be 54");
        assertEquals(0, BigDecimal.valueOf(354.00).compareTo(result.getTotalAmount()), "Total amount should be 354");
    }

    @Test
    void testCreateQuotationRevision() {
        UUID parentQuoteId = UUID.randomUUID();
        QuotationItemDTO itemDto = QuotationItemDTO.builder()
                .category("partitions")
                .unit("SQ.FT.")
                .unitRate(BigDecimal.valueOf(100))
                .build();

        QuotationDTO inputDto = QuotationDTO.builder()
                .customerId(customerId)
                .parentQuotationId(parentQuoteId)
                .projectUnit("Ft Inch")
                .discount(BigDecimal.ZERO)
                .items(Collections.singletonList(itemDto))
                .build();

        Quotation mockQuotation = new Quotation();
        mockQuotation.setItems(new java.util.ArrayList<>());

        Mockito.when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        Mockito.when(quotationMapper.toEntity(any(QuotationDTO.class))).thenReturn(mockQuotation);
        Mockito.when(quotationMapper.toEntity(any(QuotationItemDTO.class))).thenReturn(new QuotationItem());
        Mockito.when(quotationRepository.existsById(parentQuoteId)).thenReturn(true);
        Mockito.when(quotationRepository.save(any(Quotation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(quotationMapper.toDto(any(Quotation.class))).thenAnswer(invocation -> {
            Quotation q = invocation.getArgument(0);
            return QuotationDTO.builder()
                    .parentQuotationId(q.getParentQuotationId())
                    .build();
        });

        // Act
        QuotationDTO result = quotationService.createQuotation(inputDto);

        // Assert
        assertNotNull(result);
        assertEquals(parentQuoteId, result.getParentQuotationId(), "Parent quotation ID should be mapped correctly");
    }

    @Test
    void testUpdateQuotationStatus() {
        UUID quoteId = UUID.randomUUID();
        Quotation mockQuotation = new Quotation();
        mockQuotation.setId(quoteId);
        mockQuotation.setStatus(com.neosow.infra.model.QuotationStatus.ENQUIRY);
        mockQuotation.setCustomer(testCustomer);

        Mockito.when(quotationRepository.findById(quoteId)).thenReturn(Optional.of(mockQuotation));
        Mockito.when(quotationRepository.save(any(Quotation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        quotationService.updateQuotationStatus(quoteId, "Ongoing");

        // Assert
        assertEquals(com.neosow.infra.model.QuotationStatus.ONGOING, mockQuotation.getStatus(), "Quotation status should be updated to ONGOING");
    }

    @Test
    void testCreateQuotationWithPercentageDiscount() {
        QuotationItemDTO itemDto = QuotationItemDTO.builder()
                .category("partitions")
                .unit("JOB")
                .unitRate(BigDecimal.valueOf(100))
                .build();

        QuotationDTO inputDto = QuotationDTO.builder()
                .customerId(customerId)
                .projectUnit("Ft Inch")
                .discountPercent(BigDecimal.valueOf(10)) // 10% discount
                .includeGst(false)
                .items(Collections.singletonList(itemDto))
                .build();

        Quotation mockQuotation = new Quotation();
        mockQuotation.setItems(new java.util.ArrayList<>());

        Mockito.when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        Mockito.when(quotationMapper.toEntity(any(QuotationDTO.class))).thenReturn(mockQuotation);
        Mockito.when(quotationMapper.toEntity(any(QuotationItemDTO.class))).thenReturn(new QuotationItem());
        Mockito.when(quotationRepository.save(any(Quotation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(quotationMapper.toDto(any(Quotation.class))).thenAnswer(invocation -> {
            Quotation q = invocation.getArgument(0);
            return QuotationDTO.builder()
                    .subtotal(q.getSubtotal())
                    .discount(q.getDiscount())
                    .discountPercent(q.getDiscountPercent())
                    .totalAmount(q.getTotalAmount())
                    .build();
        });

        // Act
        QuotationDTO result = quotationService.createQuotation(inputDto);

        // Assert
        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(10.00).compareTo(result.getDiscount()), "Discount should be calculated as 10% of 100 which is 10");
        assertEquals(0, BigDecimal.valueOf(90.00).compareTo(result.getTotalAmount()), "Total amount should be 90");
        assertEquals(0, BigDecimal.valueOf(10).compareTo(result.getDiscountPercent()), "Discount percent should be 10");
    }
}

