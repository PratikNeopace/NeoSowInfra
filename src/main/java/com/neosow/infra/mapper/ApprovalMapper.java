package com.neosow.infra.mapper;

import com.neosow.infra.dto.admin.ApprovalResponseDTO;
import com.neosow.infra.model.Approval;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApprovalMapper {
    ApprovalResponseDTO toDto(Approval approval);
}
