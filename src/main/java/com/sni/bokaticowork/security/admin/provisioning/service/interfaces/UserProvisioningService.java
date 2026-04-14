package com.sni.bokaticowork.security.admin.provisioning.service.interfaces;

import com.sni.bokaticowork.features.client.member.dto.request.CreateMemberRequest;
import com.sni.bokaticowork.features.client.member.dto.response.MemberResponse;
import com.sni.bokaticowork.security.admin.user.dto.request.UserRequest;
import com.sni.bokaticowork.security.admin.user.model.Users;

public interface UserProvisioningService {

    Users initializeGlobalAdmin(UserRequest request);

    Users createStaff(UserRequest request, String assignedBy);

    MemberResponse createMemberByAdmin(CreateMemberRequest request, String assignedBy);

    MemberResponse registerMemberFromPortal(CreateMemberRequest request);

    MemberResponse activatePortalMember(String email, String assignedBy);

    void completePortalVerification(String email, String assignedBy);
}
