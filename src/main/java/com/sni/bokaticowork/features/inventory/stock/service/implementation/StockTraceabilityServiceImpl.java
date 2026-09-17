package com.sni.bokaticowork.features.inventory.stock.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.inventory.stock.dto.response.LotTraceabilityResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
import com.sni.bokaticowork.features.inventory.stock.model.StockLot;
import com.sni.bokaticowork.features.inventory.stock.model.StockLotGenealogy;
import com.sni.bokaticowork.features.inventory.stock.model.StockMovement;
import com.sni.bokaticowork.features.inventory.stock.model.StockMovementLot;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLotGenealogyRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLotRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockMovementLotRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockTraceabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockTraceabilityServiceImpl implements StockTraceabilityService {

    private final StockLotRepository lotRepository;
    private final StockMovementLotRepository movementLotRepository;
    private final StockLotGenealogyRepository genealogyRepository;

    @Override
    public LotTraceabilityResponse trace(String lotNumber) {
        if (!StringUtils.hasText(lotNumber)) {
            throw new BadRequestException("Lot number is required");
        }
        String normalized = lotNumber.trim().toUpperCase(Locale.ROOT);

        List<StockLot> lots = lotRepository.findAllByLotNumberAndActiveTrue(normalized);
        List<StockMovementLot> movementLines = movementLotRepository.findAllByLotNumber(normalized);

        List<LotTraceabilityResponse.UpstreamEvent> upstream = new ArrayList<>();
        List<LotTraceabilityResponse.DownstreamEvent> downstream = new ArrayList<>();

        for (StockMovementLot line : movementLines) {
            StockMovement movement = line.getMovement();
            // Une entree raconte d ou vient le lot, une sortie ou un transfert ou il est parti.
            if (movement.getMovementType() == StockMovementType.IN
                    || movement.getMovementType() == StockMovementType.ADJUSTMENT_IN) {
                upstream.add(toUpstream(line, movement));
            } else {
                downstream.add(toDownstream(line, movement));
            }
        }

        List<StockLotGenealogy> parentLinks = lots.isEmpty() ? List.of() : genealogyRepository.findByChildren(lots);
        List<StockLotGenealogy> childLinks = lots.isEmpty() ? List.of() : genealogyRepository.findByParents(lots);

        return LotTraceabilityResponse.builder()
                .lotNumber(normalized)
                .resolved(!lots.isEmpty() || !movementLines.isEmpty())
                .positions(lots.stream().map(this::toPosition).toList())
                .upstream(upstream)
                .downstream(downstream)
                .parents(parentLinks.stream().map(link -> toLink(link, link.getParentLot())).toList())
                .children(childLinks.stream().map(link -> toLink(link, link.getChildLot())).toList())
                .build();
    }

    private LotTraceabilityResponse.LotPosition toPosition(StockLot lot) {
        return LotTraceabilityResponse.LotPosition.builder()
                .lotId(lot.getId())
                .itemCode(lot.getItem().getItemCode())
                .itemName(lot.getItem().getName())
                .locationCode(lot.getLocation().getLocationCode())
                .initialQuantity(lot.getInitialQuantity())
                .remainingQuantity(lot.getRemainingQuantity())
                .expiryDate(lot.getExpiryDate())
                .receivedAt(lot.getReceivedAt())
                .quarantined(lot.getQuarantined())
                .quarantineReason(lot.getQuarantineReason())
                .blocked(lot.getBlocked())
                .blockReason(lot.getBlockReason())
                .ownershipType(lot.getOwnershipType() == null ? null : lot.getOwnershipType().name())
                .ownerCode(lot.getOwnerCode())
                .build();
    }

    private LotTraceabilityResponse.UpstreamEvent toUpstream(StockMovementLot line, StockMovement movement) {
        return LotTraceabilityResponse.UpstreamEvent.builder()
                .movementCode(movement.getMovementCode())
                .movementType(movement.getMovementType().name())
                .itemCode(movement.getItem().getItemCode())
                .locationCode(movement.getLocationTo() == null ? null : movement.getLocationTo().getLocationCode())
                .quantity(line.getQuantity())
                .unitCost(movement.getUnitCost())
                .referenceType(movement.getReferenceType() == null ? null : movement.getReferenceType().name())
                .referenceCode(movement.getReferenceCode())
                .performedBy(movement.getPerformedBy())
                .performedAt(movement.getPerformedAt())
                .build();
    }

    private LotTraceabilityResponse.DownstreamEvent toDownstream(StockMovementLot line, StockMovement movement) {
        return LotTraceabilityResponse.DownstreamEvent.builder()
                .movementCode(movement.getMovementCode())
                .movementType(movement.getMovementType().name())
                .itemCode(movement.getItem().getItemCode())
                .fromLocationCode(movement.getLocationFrom() == null ? null : movement.getLocationFrom().getLocationCode())
                .toLocationCode(movement.getLocationTo() == null ? null : movement.getLocationTo().getLocationCode())
                .quantity(line.getQuantity())
                .reasonCode(movement.getReasonCode() == null ? null : movement.getReasonCode().name())
                .referenceType(movement.getReferenceType() == null ? null : movement.getReferenceType().name())
                .referenceCode(movement.getReferenceCode())
                .performedBy(movement.getPerformedBy())
                .performedAt(movement.getPerformedAt())
                .reversed(movement.getReversed())
                .build();
    }

    private LotTraceabilityResponse.GenealogyLink toLink(StockLotGenealogy link, StockLot other) {
        return LotTraceabilityResponse.GenealogyLink.builder()
                .lotNumber(other.getLotNumber())
                .itemCode(other.getItem().getItemCode())
                .relation(link.getRelation())
                .quantity(link.getQuantity())
                .sourceMovementCode(link.getSourceMovementCode())
                .createdAt(link.getCreatedAt())
                .build();
    }
}
