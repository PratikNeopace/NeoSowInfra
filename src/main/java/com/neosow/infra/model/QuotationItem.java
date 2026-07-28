package com.neosow.infra.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "quotation_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "quotation")
@EqualsAndHashCode(exclude = "quotation")
public class QuotationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    @JsonIgnore
    private Quotation quotation;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 255)
    private String subcategory;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String width;

    @Column(length = 50)
    private String height;

    @Column(length = 50)
    private String depth;

    @Column(length = 20)
    private String unit;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal qty;

    @Column(name = "no_of_unit", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal noOfUnit = BigDecimal.ONE;

    @Column(name = "total_qty", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalQty = BigDecimal.ZERO;

    @Column(name = "unit_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitRate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
}
