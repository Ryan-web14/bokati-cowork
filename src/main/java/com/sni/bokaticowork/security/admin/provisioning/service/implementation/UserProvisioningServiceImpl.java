package com.sni.bokaticowork.security.admin.provisioning.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.client.member.dto.request.CreateMemberRequest;
import com.sni.bokaticowork.features.client.member.dto.request.UpdateStatusRequest;
import com.sni.bokaticowork.features.client.member.dto.response.MemberResponse;
import com.sni.bokaticowork.features.client.member.enums.MemberStatus;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import com.sni.bokaticowork.security.admin.provisioning.service.interfaces.UserProvisioningService;
import com.sni.bokaticowork.security.admin.role.service.interfaces.RoleUserService;
import com.sni.bokaticowork.security.admin.user.dto.request.UserRequest;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProvisioningServiceImpl implements UserProvisioningService {

    private static final String SYSTEM_ASSIGNER = "SYSTEM";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String STAFF_ROLE = "STAFF";
    private static final String MEMBER_ROLE = "MEMBER";

    private final UserService userService;
    private final MemberService memberService;
    private final RoleUserService roleUserService;

    @Override
    public Users initializeGlobalAdmin(UserRequest request) {
//        if (roleUserService.hasAnyUserAssignedToRole(ADMIN_ROLE)) {
//            throw new BadRequestException("Global admin has already been initialized");
//        }

        Users admin = userService.createUser(request);
        roleUserService.addRoleToUser(admin.getId(), ADMIN_ROLE, SYSTEM_ASSIGNER);
        userService.activateUser(admin.getEmail());
        return admin;
    }

    @Override
    public Users createStaff(UserRequest request, String assignedBy) {
        Users staff = userService.createUser(request);
        roleUserService.addRoleToUser(staff.getId(), STAFF_ROLE, normalizeAssigner(assignedBy));
        userService.activateUser(staff.getEmail());
        return staff;
    }

    @Override
    public MemberResponse createMemberByAdmin(CreateMemberRequest request, String assignedBy) {
        MemberResponse memberResponse = memberService.create(request, true);
        Member member = memberService.getByEmailForService(memberResponse.getEmail());
        roleUserService.addRoleToUser(member.getUser().getId(), MEMBER_ROLE, normalizeAssigner(assignedBy));
        return memberService.getByMemberId(member.getMemberId());
    }

    @Override
    public MemberResponse registerMemberFromPortal(CreateMemberRequest request) {
        return memberService.create(request, false);
    }

    @Override
    public MemberResponse activatePortalMember(String email, String assignedBy) {
        Member member = memberService.getByEmailForService(email);
        roleUserService.addRoleToUser(member.getUser().getId(), MEMBER_ROLE, normalizeAssigner(assignedBy));
        userService.activateUser(email);
        // Activer sans attendre le code, c'est se porter garant de l'adresse · on le note sous le
        // nom de celui qui l'a fait, pour que la verification ait toujours un auteur.
        userService.markEmailVerified(email, normalizeAssigner(assignedBy));
        memberService.ChangeStatus(member.getMemberId(), new UpdateStatusRequest(MemberStatus.ACTIVE.name()));
        return memberService.getByMemberId(member.getMemberId());
    }

    @Override
    public void completePortalVerification(String email, String assignedBy) {
        try {
            Member member = memberService.getByEmailForService(email);
            if (member.getStatus() != MemberStatus.ACTIVE || !member.getPortalAccess()) {
                activatePortalMember(email, assignedBy);
            } else {
                roleUserService.addRoleToUser(member.getUser().getId(), MEMBER_ROLE, normalizeAssigner(assignedBy));
            }
            userService.markEmailVerified(email, normalizeAssigner(assignedBy));
        } catch (ResourceNotFoundException ignored) {
            // Non-member users can still authenticate through OTT without portal provisioning.
        }
    }

    private String normalizeAssigner(String assignedBy) {
        return assignedBy == null || assignedBy.isBlank() ? SYSTEM_ASSIGNER : assignedBy;
    }
}
