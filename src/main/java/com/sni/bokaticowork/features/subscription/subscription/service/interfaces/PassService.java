package com.sni.bokaticowork.features.subscription.subscription.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePassRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PassResponse;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.PassSearchCriteria;
import org.springframework.data.domain.Pageable;

public interface PassService {

    PassResponse create(CreatePassRequest request);

    PassResponse get(String passNumber);

    Pass getForService(String passNumber);

    PaginatedResponse<PassResponse> list(PassSearchCriteria criteria, Pageable pageable);

    PassResponse cancel(String passNumber, String reason);

    int expirePasses();
}
