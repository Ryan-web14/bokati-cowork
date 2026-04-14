package com.sni.bokaticowork.features.client.member.service.interfaces;

import com.sni.bokaticowork.features.client.member.dto.request.MemberProfileRequest;
import com.sni.bokaticowork.features.client.member.dto.request.UpdateMemberProfileRequest;
import com.sni.bokaticowork.features.client.member.dto.response.MemberProfileResponse;
import com.sni.bokaticowork.features.client.member.model.Member;

public interface MemberProfileService {

    void createProfile(Member member, MemberProfileRequest profile);
    MemberProfileResponse updateProfile(String memberId, UpdateMemberProfileRequest profile);
    MemberProfileResponse getProfile(String memberId);
    MemberProfileResponse getMyProfile(String email);
    MemberProfileResponse updateMyProfile(String email, UpdateMemberProfileRequest profile);


}
