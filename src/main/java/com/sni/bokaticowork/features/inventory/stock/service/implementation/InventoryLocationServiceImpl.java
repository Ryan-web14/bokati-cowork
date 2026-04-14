package com.sni.bokaticowork.features.inventory.stock.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.inventory.stock.dto.request.InventoryLocationRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.InventoryLocationResponse;
import com.sni.bokaticowork.features.inventory.stock.mapper.interfaces.InventoryLocationMapper;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.repository.InventoryLocationRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryLocationServiceImpl implements InventoryLocationService {

    private final InventoryLocationRepository repository;
    private final InventoryLocationMapper mapper;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Override
    public InventoryLocationResponse create(InventoryLocationRequest request) {
        String code = normalizeCode(StringUtils.hasText(request.getLocationCode()) ? request.getLocationCode() : codeWithMillis());
        if (repository.existsByLocationCode(code)) {
            throw new ResourceAlreadyExistException("Inventory location already exists");
        }
        InventoryLocation entity = mapper.toEntity(request);
        entity.setLocationCode(code);
        apply(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public InventoryLocationResponse update(String locationCode, InventoryLocationRequest request) {
        InventoryLocation entity = findByLocationCodeOrThrow(locationCode);
        apply(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryLocationResponse get(String locationCode) {
        return mapper.toResponse(findByLocationCodeOrThrow(locationCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLocationResponse> list(Boolean active) {
        List<InventoryLocation> locations = active == null
                ? repository.findAllByOrderByNameAsc()
                : Boolean.TRUE.equals(active)
                ? repository.findAllByActiveTrueOrderByNameAsc()
                : repository.findAllByOrderByNameAsc().stream().filter(item -> Boolean.FALSE.equals(item.getActive())).toList();
        return locations.stream().map(mapper::toResponse).toList();
    }

    @Override
    public InventoryLocationResponse activate(String locationCode) {
        InventoryLocation entity = findByLocationCodeOrThrow(locationCode);
        entity.setActive(Boolean.TRUE);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public InventoryLocationResponse deactivate(String locationCode) {
        InventoryLocation entity = findByLocationCodeOrThrow(locationCode);
        entity.setActive(Boolean.FALSE);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryLocation findByLocationCodeOrThrow(String locationCode) {
        return repository.findByLocationCode(normalizeCode(locationCode))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found"));
    }

    private void apply(InventoryLocation entity, InventoryLocationRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Inventory location name is required");
        }
        if (request.getLocationType() == null) {
            throw new BadRequestException("Inventory location type is required");
        }
        entity.setName(request.getName().trim());
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setLocationType(request.getLocationType());
        entity.setBusinessCode(normalizeOptionalCode(request.getBusinessCode()));
        entity.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        entity.setParentLocation(StringUtils.hasText(request.getParentLocationCode())
                ? findByLocationCodeOrThrow(request.getParentLocationCode())
                : null);
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Inventory location code is required");
        }
        return value.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+", "-")
                .replaceAll("^-|-$", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalCode(String value) {
        return StringUtils.hasText(value) ? normalizeCode(value) : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String codeWithMillis() {
        return sequenceGenerator.next("inventory_location") + "-" + System.currentTimeMillis();
    }
}
