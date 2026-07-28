package com.neosow.infra.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "customer")
@EqualsAndHashCode(exclude = "customer")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @Column(name = "work_type", nullable = false, length = 100)
    private String workType;

    @Column(name = "carpet_area", precision = 10, scale = 2)
    private BigDecimal carpetArea;

    @Column(name = "area_unit", length = 10)
    private String areaUnit;

    @Column(name = "built_up_area", precision = 10, scale = 2)
    private BigDecimal builtUpArea;

    @Column(precision = 15, scale = 2)
    private BigDecimal budget;

    @Column(length = 100)
    private String timeline;
}
