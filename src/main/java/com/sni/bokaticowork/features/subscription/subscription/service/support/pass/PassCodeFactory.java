package com.sni.bokaticowork.features.subscription.subscription.service.support.pass;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PassCodeFactory {

    private final SequenceGeneratorFacade sequenceGenerator;

    public String nextPassNumber(SubscriberType subscriberType, PassPlanVersion version) {
        return sequenceGenerator.next("pass_purchase");
    }
}
