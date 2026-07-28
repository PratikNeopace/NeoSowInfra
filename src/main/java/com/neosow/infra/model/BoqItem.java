package com.neosow.infra.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "boq_items")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoqItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "main_heading")
    private String mainHeading;

    @Column(name = "sub_heading")
    private String subHeading;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String width;

    @Column(length = 50)
    private String height;

    @Column(length = 50)
    private String depth;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal qty;

    @Column(length = 50)
    private String unit;

    @Column(name = "no_of_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal noOfUnit;

    @Column(name = "total_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalQty;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal rate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "admin_id")
    private UUID adminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BoqItemStatus status = BoqItemStatus.APPROVED;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "uploaded_role", length = 50)
    private String uploadedRole;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "is_new_value", nullable = false)
    @Builder.Default
    private boolean isNewValue = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
