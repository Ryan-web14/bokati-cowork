package com.sni.bokaticowork.features.contract.mapper.decorator;

import com.sni.bokaticowork.features.contract.dto.response.ContractResponse;
import com.sni.bokaticowork.features.contract.mapper.interfaces.ContractMapper;
import com.sni.bokaticowork.features.contract.model.Contract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class ContractMapperDecorator implements ContractMapper {
    @Autowired
    @Qualifier("delegate")
    private ContractMapper delegate;

    @Override
    public ContractResponse toResponse(Contract contract) {
        ContractResponse response = delegate.toResponse(contract);
        if (contract != null && contract.getBusiness() != null) {
            response.setBusinessCode(contract.getBusiness().getCode());
        }
        return response;
    }
}
