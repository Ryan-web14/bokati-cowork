package com.sni.bokaticowork.features.inventory.stock.mapper.interfaces;

import com.sni.bokaticowork.features.inventory.stock.dto.response.StockLevelResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockMovementResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockReservationResponse;
import com.sni.bokaticowork.features.inventory.stock.mapper.decorator.StockMapperDecorator;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.model.StockMovement;
import com.sni.bokaticowork.features.inventory.stock.model.StockReservation;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(StockMapperDecorator.class)
public interface StockMapper {

    StockLevelResponse toLevelResponse(StockLevel level);

    StockMovementResponse toMovementResponse(StockMovement movement);

    StockReservationResponse toReservationResponse(StockReservation reservation);
}
