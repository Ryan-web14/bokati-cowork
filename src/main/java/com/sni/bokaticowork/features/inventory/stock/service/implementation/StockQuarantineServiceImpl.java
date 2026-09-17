package com.sni.bokaticowork.features.inventory.stock.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.inventory.stock.dto.request.LotBlockRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.LotQuarantineRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.LotReleaseRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockLotResponse;
import com.sni.bokaticowork.features.inventory.stock.mapper.decorator.StockLotResponseFactory;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.model.StockLot;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLotRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockQuarantineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockQuarantineServiceImpl implements StockQuarantineService {

    private final StockLotRepository lotRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StockLotResponseFactory lotResponseFactory;

    @Override
    public StockLotResponse quarantine(Long lotId, LotQuarantineRequest request) {
        StockLot lot = findOrThrow(lotId);
        if (Boolean.TRUE.equals(lot.getQuarantined())) {
            throw new BadRequestException("Lot " + lot.getLotNumber() + " is already quarantined");
        }

        lot.setQuarantined(Boolean.TRUE);
        lot.setQuarantineReason(request.getReason().trim());
        lot.setQuarantinedAt(Instant.now());
        lot.setQuarantinedBy(trimToNull(request.getQuarantinedBy()));
        lot.setReleaseApprovedBy(null);
        lot.setReleasedAt(null);
        lotRepository.save(lot);

        refreshImmobilisedQuantity(lot);
        log.info("Lot {} quarantined by {} : {}", lot.getLotNumber(), request.getQuarantinedBy(), request.getReason());
        return lotResponseFactory.toResponse(lot);
    }

    @Override
    public StockLotResponse release(Long lotId, LotReleaseRequest request) {
        StockLot lot = findOrThrow(lotId);
        if (!Boolean.TRUE.equals(lot.getQuarantined())) {
            throw new BadRequestException("Lot " + lot.getLotNumber() + " is not quarantined");
        }

        // Double regard : liberer un lot declare non conforme ne doit pas pouvoir se faire seul.
        if (sameActor(request.getRequestedBy(), request.getApprovedBy())) {
            throw new BadRequestException(
                    "Releasing a quarantined lot requires an approver different from the requester");
        }

        lot.setQuarantined(Boolean.FALSE);
        lot.setReleaseApprovedBy(request.getApprovedBy().trim());
        lot.setReleasedAt(Instant.now());
        lotRepository.save(lot);

        refreshImmobilisedQuantity(lot);
        log.info("Lot {} released, requested by {} approved by {}",
                lot.getLotNumber(), request.getRequestedBy(), request.getApprovedBy());
        return lotResponseFactory.toResponse(lot);
    }

    @Override
    public StockLotResponse block(Long lotId, LotBlockRequest request) {
        StockLot lot = findOrThrow(lotId);
        if (Boolean.TRUE.equals(lot.getBlocked())) {
            throw new BadRequestException("Lot " + lot.getLotNumber() + " is already blocked");
        }

        lot.setBlocked(Boolean.TRUE);
        lot.setBlockReasonType(request.getReasonType());
        lot.setBlockReason(request.getReason().trim());
        lot.setBlockedAt(Instant.now());
        lot.setBlockedBy(trimToNull(request.getBlockedBy()));
        lotRepository.save(lot);

        refreshImmobilisedQuantity(lot);
        log.info("Lot {} blocked by {} : {} ({})", lot.getLotNumber(), request.getBlockedBy(),
                request.getReason(), request.getReasonType());
        return lotResponseFactory.toResponse(lot);
    }

    @Override
    public StockLotResponse unblock(Long lotId, String unblockedBy) {
        StockLot lot = findOrThrow(lotId);
        if (!Boolean.TRUE.equals(lot.getBlocked())) {
            throw new BadRequestException("Lot " + lot.getLotNumber() + " is not blocked");
        }

        lot.setBlocked(Boolean.FALSE);
        lot.setBlockReasonType(null);
        lot.setBlockReason(null);
        lot.setBlockedAt(null);
        lot.setBlockedBy(null);
        lotRepository.save(lot);

        refreshImmobilisedQuantity(lot);
        log.info("Lot {} unblocked by {}", lot.getLotNumber(), unblockedBy);
        return lotResponseFactory.toResponse(lot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockLotResponse> listImmobilised(String itemCode, String locationCode) {
        return lotRepository.findImmobilised(normalize(itemCode), normalize(locationCode)).stream()
                .map(lotResponseFactory::toResponse)
                .toList();
    }

    /**
     * Recalcule la quantite immobilisee du niveau de stock concerne.
     *
     * <p>C est ce qui rend coherentes les deux vues : la consommation de lots ecartait deja les lots
     * en quarantaine, mais le controle de suffisance portait sur le disponible du niveau. Sans ce
     * recalcul, une sortie passerait le controle puis echouerait faute de lot consommable.</p>
     */
    private void refreshImmobilisedQuantity(StockLot lot) {
        StockLevel level = stockLevelRepository
                .findByItemAndLocationForUpdate(lot.getItem(), lot.getLocation())
                .orElse(null);
        if (level == null) {
            return;
        }

        BigDecimal immobilised = lotRepository
                .sumImmobilisedQuantity(lot.getItem(), lot.getLocation());
        level.setQuantityQuarantined(immobilised == null ? BigDecimal.ZERO : immobilised);
        level.recalculateAvailable();
        stockLevelRepository.save(level);
    }

    private boolean sameActor(String requestedBy, String approvedBy) {
        if (!StringUtils.hasText(requestedBy) || !StringUtils.hasText(approvedBy)) {
            return true;
        }
        return requestedBy.trim().equalsIgnoreCase(approvedBy.trim());
    }

    private StockLot findOrThrow(Long lotId) {
        return lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock lot not found"));
    }

    private String normalize(String value) {
        return StringUtils.hasText(value)
                ? value.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+", "-")
                    .replaceAll("^-|-$", "").toUpperCase(Locale.ROOT)
                : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
