package com.sni.bokaticowork.features.payment.mapper.interfaces;

import com.sni.bokaticowork.features.payment.dto.response.CashMovementResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionResponse;
import com.sni.bokaticowork.features.payment.mapper.decorator.CashRegisterMapperDecorator;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(CashRegisterMapperDecorator.class)
public interface CashRegisterMapper {

    default CashRegisterResponse toCashRegisterResponse(CashRegister cashRegister) {
        return null;
    }

    default CashSessionResponse toCashSessionResponse(CashSession cashSession) {
        return null;
    }

    default CashMovementResponse toCashMovementResponse(CashMovement cashMovement) {
        return null;
    }
}
