package com.neosow.infra.dto.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyMemberDTO {
    private UUID id;
    private String type;
    @NotBlank(message = "Family member name is required")
    private String name;
    private String contact;
    private String email;
    private LocalDate birthdate;
    private boolean designApproval;
    private boolean financeApproval;
}
