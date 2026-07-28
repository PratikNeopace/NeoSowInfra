package com.neosow.infra.mapper;

import com.neosow.infra.dto.customer.CustomerDTO;
import com.neosow.infra.dto.customer.FamilyMemberDTO;
import com.neosow.infra.dto.customer.ProjectDTO;
import com.neosow.infra.model.Customer;
import com.neosow.infra.model.FamilyMember;
import com.neosow.infra.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerDTO toDto(Customer customer);

    @Mapping(target = "familyMembers", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "quotations", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    Customer toEntity(CustomerDTO customerDto);

    FamilyMemberDTO toDto(FamilyMember familyMember);
    
    @Mapping(target = "customer", ignore = true)
    FamilyMember toEntity(FamilyMemberDTO familyMemberDto);

    ProjectDTO toDto(Project project);
    
    @Mapping(target = "customer", ignore = true)
    Project toEntity(ProjectDTO projectDto);
}
