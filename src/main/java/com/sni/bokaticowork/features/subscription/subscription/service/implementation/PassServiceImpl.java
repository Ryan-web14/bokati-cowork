package com.sni.bokaticowork.features.subscription.subscription.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePassRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PassResponse;
import com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces.SubscriptionPassMapper;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.PassSearchCriteria;
import com.sni.bokaticowork.features.subscription.repository.specification.specification.PassSpecification;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.PassService;
import com.sni.bokaticowork.features.subscription.subscription.service.support.pass.PassCreationOperator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.pass.PassLifecycleOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
@RequiredArgsConstructor
public class PassServiceImpl implements PassService {

    private final PassRepository passRepository;
    private final SubscriptionPassMapper responseMapper;
    private final PassCreationOperator creationOperator;
    private final PassLifecycleOperator lifecycleOperator;

    @Override
    public PassResponse create(CreatePassRequest request) {
        return responseMapper.toResponse(creationOperator.create(request));
    }

    @Override
    @Transactional(readOnly = true)
    public PassResponse get(String passNumber) {
        return responseMapper.toResponse(getForService(passNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public Pass getForService(String passNumber) {
        if (!StringUtils.hasText(passNumber)) {
            throw new BadRequestException("Pass number is required");
        }
        return passRepository.findByPassNumber(passNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Pass " + passNumber + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<PassResponse> list(PassSearchCriteria criteria, Pageable pageable) {
        return new PaginatedResponse<>(passRepository.findAll(PassSpecification.search(criteria), pageable)
                .map(responseMapper::toResponse));
    }

    @Override
    public PassResponse cancel(String passNumber, String reason) {
        return responseMapper.toResponse(lifecycleOperator.cancel(getForService(passNumber), reason));
    }

    @Override
    public int expirePasses() {
        return lifecycleOperator.expirePasses();
    }
}
