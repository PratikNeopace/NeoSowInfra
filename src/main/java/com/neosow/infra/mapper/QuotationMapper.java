package com.neosow.infra.mapper;

import com.neosow.infra.dto.quotation.QuotationDTO;
import com.neosow.infra.dto.quotation.QuotationItemDTO;
import com.neosow.infra.model.Quotation;
import com.neosow.infra.model.QuotationItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QuotationMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    QuotationDTO toDto(Quotation quotation);

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    Quotation toEntity(QuotationDTO quotationDto);

    QuotationItemDTO toDto(QuotationItem item);

    @Mapping(target = "quotation", ignore = true)
    QuotationItem toEntity(QuotationItemDTO itemDto);
}
