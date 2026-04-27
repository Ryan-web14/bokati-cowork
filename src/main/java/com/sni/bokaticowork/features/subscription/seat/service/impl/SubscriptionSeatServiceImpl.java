package com.sni.bokaticowork.features.subscription.seat.service.impl;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.seat.dto.CreateSubscriptionSeatRequest;
import com.sni.bokaticowork.features.subscription.seat.dto.SubscriptionSeatResponse;
import com.sni.bokaticowork.features.subscription.seat.enums.SeatStatus;
import com.sni.bokaticowork.features.subscription.seat.mapper.interfaces.SubscriptionSeatMapper;
import com.sni.bokaticowork.features.subscription.seat.model.SubscriptionSeat;
import com.sni.bokaticowork.features.subscription.seat.repository.SubscriptionSeatRepository;
import com.sni.bokaticowork.features.subscription.seat.repository.specification.SubscriptionSeatCriteria;
import com.sni.bokaticowork.features.subscription.seat.repository.specification.SubscriptionSeatSpecification;
import com.sni.bokaticowork.features.subscription.seat.service.SubscriptionSeatService;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionSeatServiceImpl implements SubscriptionSeatService {

    private final SubscriptionSeatRepository seatRepository;
    private final SubscriptionService subscriptionService;
    private final MemberRepository memberRepository;
    private final SubscriptionSeatMapper seatMapper;

    @Override
    public SubscriptionSeatResponse add(String subscriptionNumber, CreateSubscriptionSeatRequest request) {
        Subscription subscription = subscriptionService.getForService(subscriptionNumber);
        Member member = memberRepository.findByMemberIdAndDeletedFalse(request.memberCode())
                .orElseThrow(() -> new ResourceNotFoundException("Member " + request.memberCode() + " not found"));
        if (seatRepository.findActiveSeat(subscription.getId(), member.getMemberId()).isPresent()) {
            throw new ConflictException("subscription seat", "member already has an active seat");
        }

        SubscriptionSeat seat = seatMapper.toEntity(request);
        seat.setSubscription(subscription);
        seat.setMember(member);
        seat.setMemberCode(member.getMemberId());
        return seatMapper.toResponse(seatRepository.save(seat));
    }

    @Override
    public SubscriptionSeatResponse remove(String subscriptionNumber, String memberCode) {
        Subscription subscription = subscriptionService.getForService(subscriptionNumber);
        SubscriptionSeat seat = seatRepository.findActiveSeat(subscription.getId(), memberCode)
                .orElseThrow(() -> new ResourceNotFoundException("Active subscription seat not found"));
        seat.setStatus(SeatStatus.REMOVED);
        seat.setRemovedAt(Instant.now());
        return seatMapper.toResponse(seatRepository.save(seat));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<SubscriptionSeatResponse> list(String subscriptionNumber, String memberCode, SeatStatus status, Pageable pageable) {
        return new PaginatedResponse<>(seatRepository.findAll(
                SubscriptionSeatSpecification.search(SubscriptionSeatCriteria.builder()
                        .subscriptionNumber(subscriptionNumber)
                        .memberCode(memberCode)
                        .status(status)
                        .build()),
                pageable
        ).map(seatMapper::toResponse));
    }
}
