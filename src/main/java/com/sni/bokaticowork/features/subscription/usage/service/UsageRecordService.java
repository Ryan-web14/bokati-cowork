package com.sni.bokaticowork.features.subscription.usage.service;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.usage.dto.CreateUsageRecordRequest;
import com.sni.bokaticowork.features.subscription.usage.dto.UsageRecordResponse;
import com.sni.bokaticowork.features.subscription.usage.enums.UsageRecordStatus;
import org.springframework.data.domain.Pageable;

public interface UsageRecordService {

    UsageRecordResponse record(CreateUsageRecordRequest request);

    PaginatedResponse<UsageRecordResponse> list(SubscriberType ownerType,
                                                String ownerCode,
                                                String entitlementCode,
                                                String referenceType,
                                                String referenceId,
                                                UsageRecordStatus status,
                                                Pageable pageable);
}
