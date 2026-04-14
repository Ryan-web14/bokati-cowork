package com.sni.bokaticowork.features.contract.mapper.interfaces;

import com.sni.bokaticowork.features.contract.dto.response.ContractPartyResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractResponse;
import com.sni.bokaticowork.features.contract.mapper.decorator.ContractMapperDecorator;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.model.ContractParty;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(ContractMapperDecorator.class)
public interface ContractMapper {
    @Mapping(target = "businessCode", ignore = true)
    @Mapping(target = "parties", ignore = true)
    ContractResponse toResponse(Contract contract);

    ContractPartyResponse toPartyResponse(ContractParty party);
}
