package com.sni.bokaticowork.features.client.member.service.implementation;

import com.sni.bokaticowork.features.client.member.dto.request.MemberProfileRequest;
import com.sni.bokaticowork.features.client.member.dto.request.UpdateMemberProfileRequest;
import com.sni.bokaticowork.features.client.member.dto.response.MemberProfileResponse;
import com.sni.bokaticowork.features.client.member.mapper.interfaces.MemberMapper;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.model.MemberProfile;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberProfileRepository;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberProfileService;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class MemberProfileServiceImpl implements MemberProfileService {

    private final MemberProfileRepository profileRepository;
    private final MemberMapper memberMapper;
    private final MemberService memberService;

    @Override
    public void createProfile(Member member, MemberProfileRequest request) {

        MemberProfile profile = MemberProfile.builder()
                .member(member)
                //.profilePictureUrl(null)
                .jobTitle(trimToNull(request.getJobTitle()))
                .birthDate(request.getBirthDate())
                .address(trimToNull(request.getAddress()))
                .city(trimToNull(request.getCity()))
                .country(trimToNull(request.getCountry()))
                .companyRole(trimToNull(request.getCompanyRole()))
                .build();

        profileRepository.save(profile);
    }

    @Override
    public MemberProfileResponse updateProfile(String memberId, UpdateMemberProfileRequest request) {
        Member member = memberService.getByMemberIdForService(memberId);

        MemberProfile profile = profileRepository.findByMember_MemberId(memberId)
                .orElseGet(() -> MemberProfile.builder().member(member).build());

        memberMapper.updateProfile(profile, request);
        profileRepository.save(profile);

        return memberMapper.toProfileResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberProfileResponse getProfile(String memberId) {
        Member member = memberService.getByMemberIdForService(memberId);

        return profileRepository.findByMember_MemberId(memberId)
                .map(memberMapper::toProfileResponse)
                .orElseGet(() -> MemberProfileResponse.builder()
                        .memberId(member.getMemberId())
                        .firstName(member.getFirstname())
                        .lastName(member.getLastname())
                        .email(member.getEmail())
                        .phone(member.getPhone())
                        .whatsappPhone(member.getWhatsappPhone())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public MemberProfileResponse getMyProfile(String email) {
        Member member = memberService.getByEmailForService(email);
        return getProfile(member.getMemberId());
    }

    @Override
    public MemberProfileResponse updateMyProfile(String email, UpdateMemberProfileRequest request) {
        Member member = memberService.getByEmailForService(email);
        return updateProfile(member.getMemberId(), request);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
