package com.neosow.infra.repository;

import com.neosow.infra.model.Quotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.math.BigDecimal;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, UUID> {
    Page<Quotation> findByCustomerId(UUID customerId, Pageable pageable);

    @Query("SELECT q FROM Quotation q WHERE (:superAdmin = true OR q.createdBy IN :emails) AND q.customer.id = :customerId")
    Page<Quotation> findByCustomerIdFiltered(@Param("superAdmin") boolean superAdmin, @Param("emails") Collection<String> emails, @Param("customerId") UUID customerId, Pageable pageable);

    long countByCreatedBy(String email);

    @Query("SELECT COUNT(q) FROM Quotation q WHERE q.createdBy IN :emails")
    long countByCreatedByIn(@Param("emails") Collection<String> emails);

    @Query("SELECT COALESCE(SUM(q.totalAmount), 0) FROM Quotation q WHERE q.createdBy = :email")
    BigDecimal sumTotalAmountByCreatedBy(@Param("email") String email);

    @Query("SELECT COALESCE(SUM(q.totalAmount), 0) FROM Quotation q WHERE q.createdBy IN :emails")
    BigDecimal sumTotalAmountByCreatedByIn(@Param("emails") Collection<String> emails);

    @Query("SELECT COALESCE(SUM(q.totalAmount), 0) FROM Quotation q")
    BigDecimal sumAllTotalAmount();
}
