package com.sni.bokaticowork.features.client.member.mapper.interfaces;

import com.sni.bokaticowork.features.client.member.dto.request.CreateMemberRequest;
import com.sni.bokaticowork.features.client.member.dto.request.UpdateMemberProfileRequest;
import com.sni.bokaticowork.features.client.member.dto.request.UpdateMemberRequest;
import com.sni.bokaticowork.features.client.member.dto.response.MemberProfileResponse;
import com.sni.bokaticowork.features.client.member.dto.response.MemberResponse;
import com.sni.bokaticowork.features.client.member.dto.response.MemberSummaryResponse;
import com.sni.bokaticowork.features.client.member.mapper.decorator.MemberMapperDecorator;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.model.MemberProfile;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(MemberMapperDecorator.class)
public interface MemberMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "memberId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "portalAccess", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Member toEntity(CreateMemberRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "memberId", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Member updateEntity(@MappingTarget Member member, UpdateMemberRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "member", ignore = true)
    void   updateProfile(@MappingTarget MemberProfile profile, UpdateMemberProfileRequest request);

    default MemberProfileResponse toProfileResponse(MemberProfile profile) {
        if (profile == null) {
            return null;
        }
        return MemberProfileResponse.builder()
                .memberId(profile.getMember().getMemberId())
                .firstName(profile.getMember().getFirstname())
                .lastName(profile.getMember().getLastname())
                .email(profile.getMember().getEmail())
                .phone(profile.getMember().getPhone())
                .whatsappPhone(profile.getMember().getWhatsappPhone())
                .birthDate(profile.getBirthDate())
                .jobTitle(profile.getJobTitle())
                .companyRole(profile.getCompanyRole())
                .address(profile.getAddress())
                .city(profile.getCity())
                .country(profile.getCountry())
                .build();
    }

    default MemberSummaryResponse toSummary(Member member) {
        return MemberSummaryResponse.builder()
                .memberId(member.getMemberId())
                .customerId(member.getCustomer().getCustomerId())
                .fullName(member.getDisplayName().trim())
                .email(member.getEmail())
                .phone(member.getPhone())
                .status(member.getStatus().name())
                .portalAccess(member.getPortalAccess())
                .build();
    }

    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "fullname", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "portalAccess", ignore = true)
    MemberResponse toResponse(Member member);
}
