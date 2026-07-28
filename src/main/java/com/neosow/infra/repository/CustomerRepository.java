package com.neosow.infra.repository;

import com.neosow.infra.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Page<Customer> findByNameContainingIgnoreCaseOrPhoneContaining(String name, String phone, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE :superAdmin = true OR c.createdBy IN :emails")
    Page<Customer> findAllFiltered(@Param("superAdmin") boolean superAdmin, @Param("emails") Collection<String> emails, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE (:superAdmin = true OR c.createdBy IN :emails) AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Customer> searchFiltered(@Param("superAdmin") boolean superAdmin, @Param("emails") Collection<String> emails, @Param("search") String search, Pageable pageable);

    long countByCreatedBy(String email);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.createdBy IN :emails")
    long countByCreatedByIn(@Param("emails") Collection<String> emails);
}
