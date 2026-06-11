package com.sni.bokaticowork.features.client.member.mapper.decorator;

import com.sni.bokaticowork.features.client.member.dto.request.CreateMemberRequest;
import com.sni.bokaticowork.features.client.member.dto.request.UpdateMemberProfileRequest;
import com.sni.bokaticowork.features.client.member.dto.request.UpdateMemberRequest;
import com.sni.bokaticowork.features.client.member.dto.response.MemberResponse;
import com.sni.bokaticowork.features.client.member.dto.response.MemberSummaryResponse;
import com.sni.bokaticowork.features.client.member.mapper.interfaces.MemberMapper;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.model.MemberProfile;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.utils.format.Normalization;
import com.sni.bokaticowork.core.utils.validation.ValidationUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Component
@Slf4j
public abstract class MemberMapperDecorator implements MemberMapper {

    @Autowired
    @Qualifier("delegate")
    private MemberMapper delegate;
    
    public MemberMapperDecorator() {}

    @Override
    public Member toEntity(CreateMemberRequest request) {

        Member member =  delegate.toEntity(request);
        member.setFirstname(Normalization.normalizeFirstname(request.getFirstname()));
        member.setLastname(Normalization.normalizeLastname(request.getLastname()));
        member.setWhatsappPhone(
                request.getWhatsappPhone() != null && !request.getWhatsappPhone().isBlank()
                        ? request.getWhatsappPhone().trim()
                        : request.getPhone()
        );

        try{

            if(!ValidationUtils.validateEmail(member.getEmail())){
                throw new IllegalArgumentException("Invalid email");
            }

            member.setEmail(member.getEmail().trim().toLowerCase(Locale.ROOT));

        }catch (IllegalArgumentException e){
            throw new BadRequestException("Could not validate email");
        }

        return member;
    }

    //Todo implement this
    @Override
    public void updateProfile(MemberProfile profile, UpdateMemberProfileRequest request) {
        if (profile == null || request == null) {
            return;
        }

        if (request.getBirthDate() != null) {
            profile.setBirthDate(request.getBirthDate());
        }

        if (hasText(request.getJobTitle())) {
            profile.setJobTitle(request.getJobTitle().trim());
        }

        if (hasText(request.getCompanyRole())) {
            profile.setCompanyRole(request.getCompanyRole().trim());
        }

        if (hasText(request.getAddress())) {
            profile.setAddress(request.getAddress().trim());
        }

        if (hasText(request.getCity())) {
            profile.setCity(request.getCity().trim());
        }

        if (hasText(request.getCountry())) {
            profile.setCountry(request.getCountry().trim());
        }
    }

    @Override
    public Member updateEntity( Member member, UpdateMemberRequest request ) {
        if (member == null || request == null) {
            return member;
        }

        boolean updated = false;

        if (hasText(request.getFirstname()) && isSameValue(member.getFirstname(), request.getFirstname())) {
            member.setFirstname(request.getFirstname().trim());
            updated = true;
        }

        if (hasText(request.getLastname()) && isSameValue(member.getLastname(), request.getLastname())) {
            member.setLastname(request.getLastname().trim());
            updated = true;
        }

//        if (hasText(request.getCompanyName()) && isSameValue(member
//                .getCompanyName(), request.getCompanyName())) {
//            member
//                    .setCompanyName(request.getCompanyName().trim());
//            updated = true;
//        }

        if (hasText(request.getEmail()) && !isSameEmail(member.getEmail(), request.getEmail())) {
            member.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
            updated = true;
        }


        if (hasText(request.getPhone()) && isSameValue(member.getPhone(), request.getPhone())) {
            member
                    .setPhone(request.getPhone().trim());
            updated = true;
        }

        if (hasText(request.getWhatsappPhone()) && isSameValue(member.getWhatsappPhone(), request.getWhatsappPhone())) {
            member.setWhatsappPhone(request.getWhatsappPhone().trim());
            updated = true;
        }

        if (updated) {
            member.setUpdatedAt(Instant.now());
        }

        return member;
    }

    @Override
    public MemberResponse toResponse(Member member) {
        if (member == null) {
            return null;
        }
        MemberResponse response = delegate.toResponse(member);
        response.setCustomerId(safeCustomerId(member));
        response.setUserId(safeUserId(member));
        response.setFullname(member.getDisplayName());
        response.setStatus(member.getStatus() == null ? null : member.getStatus().name());
        response.setPortalAccess(Boolean.TRUE.equals(member.getPortalAccess()));
        return response;
    }

    @Override
    public MemberSummaryResponse toSummary(Member member) {
        if (member == null) {
            return null;
        }
        return MemberSummaryResponse.builder()
                .memberId(member.getMemberId())
                .customerId(safeCustomerId(member))
                .fullName(member.getDisplayName().trim())
                .email(member.getEmail())
                .phone(member.getPhone())
                .status(member.getStatus() == null ? null : member.getStatus().name())
                .portalAccess(member.getPortalAccess())
                .build();
    }

    private String safeCustomerId(Member member) {
        try {
            return member.getCustomer() == null ? null : member.getCustomer().getCustomerId();
        } catch (EntityNotFoundException ex) {
            log.warn("Member {} references a missing or deleted customer", member.getMemberId(), ex);
            return null;
        }
    }

    private String safeUserId(Member member) {
        try {
            return member.getUser() == null ? null : member.getUser().getUserId();
        } catch (EntityNotFoundException ex) {
            log.warn("Member {} references a missing or deleted user", member.getMemberId(), ex);
            return null;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isSameValue(String current, String incoming) {
        return !Objects.equals(
                current == null ? null : current.trim(),
                incoming == null ? null : incoming.trim()
        );
    }

    private static boolean isSameEmail(String current, String incoming) {
        if (current == null && incoming == null) {
            return true;
        }
        if (current == null || incoming == null) {
            return false;
        }
        return current.trim().equalsIgnoreCase(incoming.trim());
    }

}
