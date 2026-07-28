package com.neosow.infra.mapper;

import com.neosow.infra.dto.boq.BoqItemDTO;
import com.neosow.infra.dto.boq.ImportErrorDTO;
import com.neosow.infra.dto.boq.ImportJobResponseDTO;
import com.neosow.infra.model.BoqItem;
import com.neosow.infra.model.ImportError;
import com.neosow.infra.model.ImportJob;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BoqMapper {

    @Mapping(target = "uploadedBy", ignore = true)
    ImportJobResponseDTO toDto(ImportJob job);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "uploadedBy", ignore = true)
    ImportJob toEntity(ImportJobResponseDTO dto);

    ImportErrorDTO toDto(ImportError error);
    ImportError toEntity(ImportErrorDTO dto);

    @Mapping(source = "newValue", target = "isNewValue")
    @Mapping(target = "uploadedBy", ignore = true)
    BoqItemDTO toDto(BoqItem item);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "newValue", target = "isNewValue")
    @Mapping(target = "uploadedBy", ignore = true)
    BoqItem toEntity(BoqItemDTO dto);
}
