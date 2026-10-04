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
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProvisioningServiceImpl implements UserProvisioningService {

    private static final String SYSTEM_ASSIGNER = "SYSTEM";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String STAFF_ROLE = "STAFF";
    private static final String MEMBER_ROLE = "MEMBER";

    /** Sans secret configure, l'amorcage est ferme · c'est le defaut voulu. */
    @Value("${app.security.bootstrap.secret:}")
    private String bootstrapSecret;

    private final UserService userService;
    private final MemberService memberService;
    private final RoleUserService roleUserService;

    /**
     * L'amorcage du premier administrateur · une seule fois, et avec un secret.
     *
     * <p>Le garde-fou qui refermait cet amorcage etait commente, et la route est publique. Une
     * seule requete POST non authentifiee rendait donc un compte ADMIN active, a n'importe qui,
     * autant de fois qu'il le voulait. C'etait la faille la plus directement exploitable du
     * systeme, et elle ne dependait d'aucun reglage.</p>
     *
     * <p>Trois verrous desormais, et il faut les trois :</p>
     * <ol>
     *   <li>un <b>secret d'amorcage</b> fourni dans la demande et confronte a
     *       {@code app.security.bootstrap.secret} · sans secret configure, l'amorcage est
     *       entierement ferme, ce qui est le bon etat par defaut ;</li>
     *   <li>l'absence de tout administrateur · des qu'il en existe un, la porte se referme
     *       definitivement et seul un administrateur cree les suivants ;</li>
     *   <li>un journal explicite · un amorcage est un evenement unique dans la vie du systeme,
     *       il doit se voir dans les journaux.</li>
     * </ol>
     */
    @Override
    public Users initializeGlobalAdmin(UserRequest request) {
        assertBootstrapAllowed(request == null ? null : request.getBootstrapSecret());

        Users admin = userService.createUser(request);
        roleUserService.addRoleToUser(admin.getId(), ADMIN_ROLE, SYSTEM_ASSIGNER);
        userService.activateUser(admin.getEmail());
        log.warn("Amorcage · premier administrateur cree pour {} · la route d'amorcage est desormais fermee",
                admin.getEmail());
        return admin;
    }

    private void assertBootstrapAllowed(String providedSecret) {
        if (!StringUtils.hasText(bootstrapSecret)) {
            // Aucun secret configure · l'amorcage n'est pas « ouvert par defaut », il est ferme.
            log.warn("Tentative d'amorcage refusee · aucun secret d'amorcage n'est configure");
            throw new ForbiddenException("L'amorçage n'est pas ouvert.");
        }
        if (!MessageDigest.isEqual(
                bootstrapSecret.trim().getBytes(StandardCharsets.UTF_8),
                (providedSecret == null ? "" : providedSecret.trim()).getBytes(StandardCharsets.UTF_8))) {
            log.warn("Tentative d'amorcage refusee · secret d'amorcage invalide");
            throw new ForbiddenException("L'amorçage n'est pas ouvert.");
        }
        if (roleUserService.hasAnyUserAssignedToRole(ADMIN_ROLE)) {
            log.warn("Tentative d'amorcage refusee · un administrateur existe deja");
            throw new ForbiddenException("L'amorçage a déjà été effectué.");
        }
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
