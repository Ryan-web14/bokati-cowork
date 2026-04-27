package com.sni.bokaticowork.features.subscription.seat.mapper.decorator;

import com.sni.bokaticowork.features.subscription.seat.dto.CreateSubscriptionSeatRequest;
import com.sni.bokaticowork.features.subscription.seat.dto.SubscriptionSeatResponse;
import com.sni.bokaticowork.features.subscription.seat.enums.SeatStatus;
import com.sni.bokaticowork.features.subscription.seat.mapper.interfaces.SubscriptionSeatMapper;
import com.sni.bokaticowork.features.subscription.seat.model.SubscriptionSeat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public abstract class SubscriptionSeatMapperDecorator implements SubscriptionSeatMapper {

    @Autowired
    @Qualifier("delegate")
    private SubscriptionSeatMapper delegate;

    @Override
    public SubscriptionSeat toEntity(CreateSubscriptionSeatRequest request) {
        SubscriptionSeat seat = delegate.toEntity(request);
        seat.setStatus(SeatStatus.ACTIVE);
        seat.setInvitedAt(Instant.now());
        seat.setActivatedAt(Instant.now());
        return seat;
    }

    @Override
    public SubscriptionSeatResponse toResponse(SubscriptionSeat seat) {
        return new SubscriptionSeatResponse(
                seat.getId(),
                seat.getSubscription().getSubscriptionNumber(),
                seat.getMemberCode(),
                seat.getMember().getDisplayName(),
                seat.getRole(),
                seat.getStatus(),
                seat.getInvitedAt(),
                seat.getActivatedAt(),
                seat.getRemovedAt()
        );
    }
}
