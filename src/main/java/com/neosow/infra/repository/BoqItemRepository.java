package com.neosow.infra.repository;

import com.neosow.infra.dto.boq.ApprovedBoqItemDTO;
import com.neosow.infra.model.BoqItem;
import com.neosow.infra.model.BoqItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.neosow.infra.model.QuotationType;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoqItemRepository extends JpaRepository<BoqItem, UUID> {
    List<BoqItem> findByJobId(UUID jobId);

    boolean existsBySubHeadingIgnoreCase(String subHeading);

    Page<BoqItem> findByStatus(BoqItemStatus status, Pageable pageable);

    Optional<BoqItem> findFirstBySubHeadingIgnoreCase(String subHeading);

    @Query("SELECT new com.neosow.infra.dto.boq.ApprovedBoqItemDTO(b.mainHeading, b.subHeading, b.description, b.unit, b.rate, b.isNewValue, b.quotationType) FROM BoqItem b WHERE b.status = com.neosow.infra.model.BoqItemStatus.APPROVED")
    List<ApprovedBoqItemDTO> findApprovedBoqItems();

    @Query("SELECT new com.neosow.infra.dto.boq.ApprovedBoqItemDTO(b.mainHeading, b.subHeading, b.description, b.unit, b.rate, b.isNewValue, b.quotationType) FROM BoqItem b WHERE b.status = com.neosow.infra.model.BoqItemStatus.APPROVED AND b.quotationType = :quotationType")
    List<ApprovedBoqItemDTO> findApprovedBoqItemsByQuotationType(@Param("quotationType") QuotationType quotationType);

    @Query("SELECT b FROM BoqItem b WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(b.mainHeading) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.subHeading) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<BoqItem> findAllWithSearch(String search, Pageable pageable);
}
