package com.sni.bokaticowork.features.event.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.event.dto.EventRegistrationDtos.EventRegistrationResponse;
import com.sni.bokaticowork.features.event.dto.EventRegistrationDtos.EventSummaryResponse;
import com.sni.bokaticowork.features.event.dto.EventRegistrationDtos.PublicEventRegistrationRequest;
import com.sni.bokaticowork.features.event.dto.EventRegistrationDtos.RejectEventRegistrationRequest;
import com.sni.bokaticowork.features.event.enums.RegistrationStatus;
import org.springframework.data.domain.Pageable;

public interface EventRegistrationService {

    EventSummaryResponse getEvent(String eventCode);

    EventRegistrationResponse register(String eventCode, PublicEventRegistrationRequest request);

    PaginatedResponse<EventRegistrationResponse> search(String eventCode, RegistrationStatus status,
                                                          String searchText, Pageable pageable);

    EventRegistrationResponse get(Long id);

    EventRegistrationResponse validate(Long id);

    EventRegistrationResponse reject(Long id, RejectEventRegistrationRequest request);
}
