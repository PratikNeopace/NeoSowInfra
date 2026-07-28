package com.neosow.infra.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "family_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "customer")
@EqualsAndHashCode(exclude = "customer")
public class FamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @Column(length = 50)
    private String type;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String contact;

    @Column(length = 100)
    private String email;

    private LocalDate birthdate;

    @Column(name = "design_approval")
    @Builder.Default
    private boolean designApproval = false;

    @Column(name = "finance_approval")
    @Builder.Default
    private boolean financeApproval = false;
}
