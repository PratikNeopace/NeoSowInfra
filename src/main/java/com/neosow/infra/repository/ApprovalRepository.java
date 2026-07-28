package com.neosow.infra.repository;

import com.neosow.infra.model.Approval;
import com.neosow.infra.model.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, UUID> {
    Page<Approval> findBySubmittedBy(UUID submittedBy, Pageable pageable);
    long countByStatus(ApprovalStatus status);
}
