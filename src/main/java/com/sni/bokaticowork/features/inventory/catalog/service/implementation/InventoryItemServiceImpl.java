package com.sni.bokaticowork.features.inventory.catalog.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemResponse;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryCategory;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryUnit;
import com.sni.bokaticowork.features.inventory.catalog.mapper.interfaces.InventoryItemMapper;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.specification.InventoryItemSpecification;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryCategoryService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryUnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryItemServiceImpl implements InventoryItemService, InventoryItemLookupService {

    private final InventoryItemRepository repository;
    private final InventoryCategoryService categoryService;
    private final InventoryUnitService unitService;
    private final InventoryItemMapper mapper;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Override
    public InventoryItemResponse create(InventoryItemRequest request) {
        InventoryItem entity = mapper.toEntity(request);
        entity.setItemCode(normalizeCode(StringUtils.hasText(request.getItemCode())
                ? request.getItemCode()
                : codeWithMillis("inventory_item")));
        assertUniqueCreate(entity.getItemCode(), request);
        apply(entity, request);
        repository.save(entity);
        return mapper.toResponse(entity);
    }

    @Override
    public InventoryItemResponse update(String itemCode, InventoryItemRequest request) {
        InventoryItem entity = findByItemCodeOrThrow(itemCode);
        apply(entity, request);
        repository.save(entity);
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemResponse get(String itemCode) {
        return mapper.toResponse(findByItemCodeOrThrow(itemCode));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> search(String query, String categoryCode, InventoryItemType itemType, Boolean active, Pageable pageable) {
        if (!StringUtils.hasText(query)) {
            return repository.findAll(InventoryItemSpecification.filters(categoryCode, itemType, active), pageable)
                    .map(mapper::toResponse);
        }
        return repository.nativeSearch(trimToNull(query), normalizeOptionalCode(categoryCode),
                        itemType == null ? null : itemType.name(), active, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public InventoryItemResponse activate(String itemCode) {
        InventoryItem entity = findByItemCodeOrThrow(itemCode);
        entity.setActive(Boolean.TRUE);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public InventoryItemResponse deactivate(String itemCode) {
        InventoryItem entity = findByItemCodeOrThrow(itemCode);
        entity.setActive(Boolean.FALSE);
        return mapper.toResponse(repository.save(entity));
    }

    private void apply(InventoryItem entity, InventoryItemRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Inventory item name is required");
        }
        if (request.getItemType() == null) {
            throw new BadRequestException("Inventory item type is required");
        }

        entity.setName(request.getName().trim());
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setPsku(resolvePsku(entity, request.getPsku()));
        entity.setShortCode(normalizeOptionalCode(StringUtils.hasText(request.getShortCode()) ? request.getShortCode() : request.getName()));
        entity.setDisplayCode(normalizeDisplayCode(request.getDisplayCode(), entity));
        entity.setIdentificationCode(normalizeOptionalCode(StringUtils.hasText(request.getIdentificationCode())
                ? request.getIdentificationCode()
                : entity.getDisplayCode()));
        entity.setSpecification(trimToNull(request.getSpecification()));
        entity.setCategory(StringUtils.hasText(request.getCategoryCode()) ? categoryService.findByCodeOrThrow(request.getCategoryCode()) : null);
        entity.setUnit(StringUtils.hasText(request.getUnitCode()) ? unitService.findByCodeOrThrow(request.getUnitCode()) : null);
        entity.setItemType(request.getItemType());
        entity.setTrackingType(request.getTrackingType() == null ? defaultTrackingType(request.getItemType()) : request.getTrackingType());
        entity.setDefaultCost(request.getDefaultCost());
        entity.setSalePrice(request.getSalePrice());
        entity.setTaxable(defaultBoolean(request.getTaxable(), false));
        entity.setAllowNegativeStock(defaultBoolean(request.getAllowNegativeStock(), false));
        entity.setRequiresExpiryDate(defaultBoolean(request.getRequiresExpiryDate(), false));
        entity.setRequiresLotNumber(defaultBoolean(request.getRequiresLotNumber(), false));
        entity.setRequiresSerialNumber(defaultBoolean(request.getRequiresSerialNumber(), request.getItemType() == InventoryItemType.ASSET));
        entity.setActive(defaultBoolean(request.getActive(), true));
        validateUniqueBusinessCodes(entity);
        entity.setSearchText(buildSearchText(entity));
    }

    private void assertUniqueCreate(String itemCode, InventoryItemRequest request) {
        if (repository.existsByItemCode(itemCode)) {
            throw new ResourceAlreadyExistException("Inventory item code already exists");
        }
        if (StringUtils.hasText(request.getPsku()) && repository.existsByPsku(normalizeOptionalCode(request.getPsku()))) {
            throw new ResourceAlreadyExistException("Inventory item PSKU already exists");
        }
    }

    private void validateUniqueBusinessCodes(InventoryItem entity) {
        if (repository.existsConflictingIdentification(
                entity.getId(),
                entity.getPsku(),
                entity.getShortCode(),
                entity.getDisplayCode(),
                entity.getIdentificationCode())) {
            throw new ResourceAlreadyExistException("Inventory identification code already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItem findByItemCodeOrThrow(String itemCode) {
        return repository.findByItemCode(normalizeCode(itemCode))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
    }

    private String buildSearchText(InventoryItem entity) {
        return String.join(" ",
                nullToBlank(entity.getItemCode()),
                nullToBlank(entity.getName()),
                nullToBlank(entity.getDescription()),
                nullToBlank(entity.getPsku()),
                nullToBlank(entity.getShortCode()),
                nullToBlank(entity.getDisplayCode()),
                nullToBlank(entity.getIdentificationCode()),
                nullToBlank(entity.getSpecification()),
                entity.getCategory() == null ? "" : nullToBlank(entity.getCategory().getCode()),
                entity.getCategory() == null ? "" : nullToBlank(entity.getCategory().getName())
        ).toLowerCase(Locale.ROOT);
    }

    private InventoryTrackingType defaultTrackingType(InventoryItemType itemType) {
        return itemType == InventoryItemType.ASSET ? InventoryTrackingType.SERIAL : InventoryTrackingType.QUANTITY;
    }

    private String normalizeDisplayCode(String requested, InventoryItem entity) {
        if (StringUtils.hasText(requested)) {
            return normalizeCode(requested);
        }
        if (StringUtils.hasText(entity.getShortCode())) {
            return entity.getShortCode();
        }
        if (StringUtils.hasText(entity.getPsku())) {
            return entity.getPsku();
        }
        return entity.getItemCode();
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Inventory code is required");
        }
        return value.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+", "-")
                .replaceAll("^-|-$", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalCode(String value) {
        return StringUtils.hasText(value) ? normalizeCode(value) : null;
    }

    private String resolvePsku(InventoryItem entity, String requestedPsku) {
        if (StringUtils.hasText(requestedPsku)) {
            String value = requestedPsku.trim();
            if (!value.matches("\\d{12}")) {
                throw new BadRequestException("Inventory item PSKU must contain exactly 12 digits");
            }
            return value;
        }
        if (StringUtils.hasText(entity.getPsku())) {
            return entity.getPsku();
        }
        return generatePsku();
    }

    private String generatePsku() {
        long base = System.currentTimeMillis() % 1_000_000_000_000L;
        String psku = String.format(Locale.ROOT, "%012d", base);
        while (repository.existsByPsku(psku)) {
            base = (base + 1) % 1_000_000_000_000L;
            psku = String.format(Locale.ROOT, "%012d", base);
        }
        return psku;
    }

    private String codeWithMillis(String sequenceCode) {
        return sequenceGenerator.next(sequenceCode) + "-" + System.currentTimeMillis();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Boolean defaultBoolean(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
