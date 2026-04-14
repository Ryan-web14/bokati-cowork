package com.sni.bokaticowork.features.inventory.catalog.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryUnitRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryUnitResponse;
import com.sni.bokaticowork.features.inventory.catalog.mapper.interfaces.InventoryUnitMapper;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryUnit;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryUnitRepository;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryUnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryUnitServiceImpl implements InventoryUnitService {

    private final InventoryUnitRepository repository;
    private final InventoryUnitMapper mapper;

    @Override
    public InventoryUnitResponse create(InventoryUnitRequest request) {
        String code = normalizeCode(StringUtils.hasText(request.getCode()) ? request.getCode() : request.getName());
        if (repository.existsByCode(code)) {
            throw new ResourceAlreadyExistException("Inventory unit already exists");
        }
        InventoryUnit entity = mapper.toEntity(request);
        entity.setCode(code);
        apply(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public InventoryUnitResponse update(String code, InventoryUnitRequest request) {
        InventoryUnit entity = findByCodeOrThrow(code);
        apply(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryUnitResponse get(String code) {
        return mapper.toResponse(findByCodeOrThrow(code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUnitResponse> list(Boolean active) {
        List<InventoryUnit> units = active == null
                ? repository.findAllByOrderByNameAsc()
                : Boolean.TRUE.equals(active)
                ? repository.findAllByActiveTrueOrderByNameAsc()
                : repository.findAllByOrderByNameAsc().stream().filter(item -> Boolean.FALSE.equals(item.getActive())).toList();
        return units.stream().map(mapper::toResponse).toList();
    }

    @Override
    public InventoryUnitResponse activate(String code) {
        InventoryUnit entity = findByCodeOrThrow(code);
        entity.setActive(Boolean.TRUE);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public InventoryUnitResponse deactivate(String code) {
        InventoryUnit entity = findByCodeOrThrow(code);
        entity.setActive(Boolean.FALSE);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryUnit findByCodeOrThrow(String code) {
        return repository.findByCode(normalizeCode(code))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory unit not found"));
    }

    private void apply(InventoryUnit entity, InventoryUnitRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Inventory unit name is required");
        }
        if (request.getUnitType() == null) {
            throw new BadRequestException("Inventory unit type is required");
        }
        entity.setName(request.getName().trim());
        entity.setUnitType(request.getUnitType());
        entity.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Inventory unit code is required");
        }
        return value.trim().replaceAll("[^A-Za-z0-9]+", "_").replaceAll("_+", "_")
                .replaceAll("^_|_$", "").toUpperCase(Locale.ROOT);
    }
}
