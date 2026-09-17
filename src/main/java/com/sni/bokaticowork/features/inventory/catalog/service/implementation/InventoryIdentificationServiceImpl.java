package com.sni.bokaticowork.features.inventory.catalog.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryBarcodeRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryPackagingRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryBarcodeResponse;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryPackagingResponse;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryBarcodeType;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryBarcode;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryPackaging;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryBarcodeRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryPackagingRepository;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryIdentificationService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryUnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryIdentificationServiceImpl implements InventoryIdentificationService {

    private final InventoryItemLookupService itemLookupService;
    private final InventoryUnitService unitService;
    private final InventoryBarcodeRepository barcodeRepository;
    private final InventoryPackagingRepository packagingRepository;

    @Override
    public InventoryBarcodeResponse addBarcode(String itemCode, InventoryBarcodeRequest request) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(itemCode);
        String value = normalizeBarcode(request.getBarcodeValue());
        validateBarcodeFormat(request.getBarcodeType(), value);

        if (barcodeRepository.existsByBarcodeValue(value)) {
            throw new ResourceAlreadyExistException("Barcode already assigned: " + value);
        }

        boolean requestedPrimary = Boolean.TRUE.equals(request.getPrimaryCode());
        boolean firstBarcode = barcodeRepository.findAllByItemOrderByPrimaryCodeDescBarcodeValueAsc(item).isEmpty();
        boolean primary = requestedPrimary || firstBarcode;
        if (primary) {
            clearPrimaryFlag(item);
        }

        InventoryBarcode barcode = InventoryBarcode.builder()
                .item(item)
                .barcodeType(request.getBarcodeType())
                .barcodeValue(value)
                .unitCode(resolveUnitCode(request.getUnitCode()))
                .quantity(request.getQuantity() == null ? BigDecimal.ONE : request.getQuantity())
                .primaryCode(primary)
                .active(request.getActive() == null ? Boolean.TRUE : request.getActive())
                .build();

        return toResponse(barcodeRepository.save(barcode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryBarcodeResponse> listBarcodes(String itemCode) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(itemCode);
        return barcodeRepository.findAllByItemOrderByPrimaryCodeDescBarcodeValueAsc(item).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public InventoryBarcodeResponse markPrimary(Long barcodeId) {
        InventoryBarcode barcode = barcodeRepository.findById(barcodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Barcode not found"));
        clearPrimaryFlag(barcode.getItem());
        barcode.setPrimaryCode(Boolean.TRUE);
        return toResponse(barcodeRepository.save(barcode));
    }

    @Override
    public void deleteBarcode(Long barcodeId) {
        InventoryBarcode barcode = barcodeRepository.findById(barcodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Barcode not found"));
        barcodeRepository.delete(barcode);
    }

    @Override
    public InventoryPackagingResponse addPackaging(String itemCode, InventoryPackagingRequest request) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(itemCode);

        packagingRepository.findByItemAndPackagingLevel(item, request.getPackagingLevel())
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistException(
                            "Packaging level already defined for this item: " + request.getPackagingLevel());
                });

        String barcodeValue = normalizeOptionalBarcode(request.getBarcodeValue());
        if (barcodeValue != null && barcodeRepository.existsByBarcodeValue(barcodeValue)) {
            throw new ResourceAlreadyExistException("Barcode already assigned: " + barcodeValue);
        }

        InventoryPackaging packaging = InventoryPackaging.builder()
                .item(item)
                .packagingLevel(request.getPackagingLevel())
                .name(trimToNull(request.getName()))
                .quantity(request.getQuantity())
                .barcodeValue(barcodeValue)
                .weightKg(request.getWeightKg())
                .lengthMm(request.getLengthMm())
                .widthMm(request.getWidthMm())
                .heightMm(request.getHeightMm())
                .active(request.getActive() == null ? Boolean.TRUE : request.getActive())
                .build();

        return toResponse(packagingRepository.save(packaging));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryPackagingResponse> listPackagings(String itemCode) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(itemCode);
        return packagingRepository.findAllByItemOrderByQuantityAsc(item).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void deletePackaging(Long packagingId) {
        InventoryPackaging packaging = packagingRepository.findById(packagingId)
                .orElseThrow(() -> new ResourceNotFoundException("Packaging not found"));
        packagingRepository.delete(packaging);
    }

    /**
     * Retire la marque de code principal aux codes existants de l'article.
     *
     * <p>L'index unique partiel en base interdit deux codes principaux : sans ce nettoyage prealable,
     * la promotion d'un nouveau code echouerait.</p>
     */
    private void clearPrimaryFlag(InventoryItem item) {
        List<InventoryBarcode> currentPrimaries = barcodeRepository.findAllByItemAndPrimaryCodeTrue(item);
        if (currentPrimaries.isEmpty()) {
            return;
        }
        currentPrimaries.forEach(existing -> existing.setPrimaryCode(Boolean.FALSE));
        barcodeRepository.saveAllAndFlush(currentPrimaries);
    }

    private void validateBarcodeFormat(InventoryBarcodeType type, String value) {
        if (type == null) {
            throw new BadRequestException("Barcode type is required");
        }
        if (!type.isFixedNumeric()) {
            return;
        }
        if (!value.matches("\\d{" + type.getExpectedDigits() + "}")) {
            throw new BadRequestException(
                    type.name() + " barcode must contain exactly " + type.getExpectedDigits() + " digits");
        }
    }

    private String resolveUnitCode(String unitCode) {
        if (!StringUtils.hasText(unitCode)) {
            return null;
        }
        // Passe par le service d'unite pour refuser un code inexistant plutot que de le stocker tel quel.
        return unitService.findByCodeOrThrow(unitCode).getCode();
    }

    private String normalizeBarcode(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Barcode value is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalBarcode(String value) {
        return StringUtils.hasText(value) ? normalizeBarcode(value) : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private InventoryBarcodeResponse toResponse(InventoryBarcode entity) {
        return InventoryBarcodeResponse.builder()
                .id(entity.getId())
                .itemCode(entity.getItem().getItemCode())
                .barcodeType(entity.getBarcodeType())
                .barcodeValue(entity.getBarcodeValue())
                .unitCode(entity.getUnitCode())
                .quantity(entity.getQuantity())
                .primaryCode(entity.getPrimaryCode())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private InventoryPackagingResponse toResponse(InventoryPackaging entity) {
        return InventoryPackagingResponse.builder()
                .id(entity.getId())
                .itemCode(entity.getItem().getItemCode())
                .packagingLevel(entity.getPackagingLevel())
                .name(entity.getName())
                .quantity(entity.getQuantity())
                .barcodeValue(entity.getBarcodeValue())
                .weightKg(entity.getWeightKg())
                .lengthMm(entity.getLengthMm())
                .widthMm(entity.getWidthMm())
                .heightMm(entity.getHeightMm())
                .active(entity.getActive())
                .build();
    }
}
