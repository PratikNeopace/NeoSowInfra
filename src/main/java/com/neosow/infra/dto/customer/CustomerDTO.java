package com.neosow.infra.dto.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {
    private UUID id;

    @NotBlank(message = "Customer name is required")
    private String name;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String address;
    private LocalDate birthDate;
    private LocalDate anniversaryDate;
    private String drawingPlanUrl;

    @Valid
    private List<FamilyMemberDTO> familyMembers;

    @NotNull(message = "Project details are required")
    @Valid
    private ProjectDTO project;

    private String createdBy;
}
