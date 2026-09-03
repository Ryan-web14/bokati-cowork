package com.sni.bokaticowork.features.client.member.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.maintenance.MemberPurgeService;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.dto.request.CreateMemberRequest;
import com.sni.bokaticowork.features.client.member.dto.request.KycGracePeriodRequest;
import com.sni.bokaticowork.features.client.member.dto.request.TransferMemberCustomerRequest;
import com.sni.bokaticowork.features.client.member.dto.request.UpdateMemberProfileRequest;
import com.sni.bokaticowork.features.client.member.dto.request.UpdateMemberRequest;
import com.sni.bokaticowork.features.client.member.dto.request.UpdateStatusRequest;
import com.sni.bokaticowork.features.client.member.dto.response.MemberProfileResponse;
import com.sni.bokaticowork.features.client.member.dto.response.MemberResponse;
import com.sni.bokaticowork.features.client.member.dto.response.MemberSummaryResponse;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberProfileService;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import com.sni.bokaticowork.features.client.member.repository.specification.criteria.MemberSearchCriteria;
import com.sni.bokaticowork.security.admin.provisioning.dto.request.PortalMemberActivationRequest;
import com.sni.bokaticowork.security.admin.provisioning.service.interfaces.UserProvisioningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;

@RestController
@RequestMapping(ApiPath.V1 + "/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberProfileService memberProfileService;
    private final UserProvisioningService userProvisioningService;
    private final MemberPurgeService memberPurgeService;

    //Todo revenir sur l'activation portail et le profil membre

        @PostMapping
        @Audited(module = "MEMBER", action = "CREATE", ressource = "member")
        @Idempotent(operation = "MEMBER_CREATE")
        public ResponseEntity<MemberResponse> create(@Valid @RequestBody CreateMemberRequest request) {
            return ResponseEntity.status(HttpStatus.CREATED).body(userProvisioningService.registerMemberFromPortal(request));
        }

        @PostMapping("/admin/create")
        @Audited(module = "MEMBER", action = "ADMIN_CREATE", ressource = "member")
        @Idempotent(operation = "MEMBER_ADMIN_CREATE")
        public ResponseEntity<MemberResponse> createByAdmin(@Valid @RequestBody CreateMemberRequest request,
                                                            Authentication authentication){
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(userProvisioningService.createMemberByAdmin(request, getActor(authentication)));
        }

        @GetMapping("/{id}")
        public ResponseEntity<MemberResponse> get(@PathVariable String id) {
            return ResponseEntity.ok(memberService.getByMemberId(id));
        }

        @GetMapping("/by-email")
        public ResponseEntity<MemberResponse> getByEmail(@RequestParam String email) {
            return ResponseEntity.ok(memberService.getByEmail(email));
        }

        @GetMapping
        public ResponseEntity<PaginatedResponse> list(
                @RequestParam(required = false) String customerId,
                @RequestParam(defaultValue = "false") boolean summary,
                @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

            if (customerId != null) {
                return ResponseEntity.ok(memberService.listSummaryByCustomer(customerId, pageable));
            }

            if (summary) {
                return ResponseEntity.ok(memberService.listSummary(pageable));
            }

            return ResponseEntity.ok(memberService.list(pageable));
        }

        @GetMapping("/summary")
        public ResponseEntity<PaginatedResponse<MemberSummaryResponse>> listSummary(
                @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
            return ResponseEntity.ok(memberService.listSummary(pageable));
        }

        @GetMapping("/summary/all")
        public ResponseEntity<java.util.List<MemberSummaryResponse>> listAllSummary() {
            return ResponseEntity.ok(memberService.getAllMembersSummary());
        }

        @PatchMapping("/{id}")
        @Audited(module = "MEMBER", action = "UPDATE", ressource = "member")
        @Idempotent(operation = "MEMBER_UPDATE")
        public ResponseEntity<Void> update(
                @PathVariable String id,
                @Valid @RequestBody UpdateMemberRequest request) {
            memberService.update(id, request);
            return ResponseEntity.noContent().build();
        }

        @PatchMapping("/{id}/status")
        @Audited(module = "MEMBER", action = "CHANGE_STATUS", ressource = "member")
        @Idempotent(operation = "MEMBER_CHANGE_STATUS")
        public ResponseEntity<Void> changeStatus(
                @PathVariable String id,
                @Valid @RequestBody UpdateStatusRequest request) {
            memberService.ChangeStatus(id, request);
            return ResponseEntity.noContent().build();
        }

        @GetMapping("/{id}/profile")
        public ResponseEntity<MemberProfileResponse> getProfile(@PathVariable String id) {
            return ResponseEntity.ok(memberProfileService.getProfile(id));
        }

        @GetMapping("/me/profile")
        public ResponseEntity<MemberProfileResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
            return ResponseEntity.ok(memberProfileService.getMyProfile(resolvePrincipalEmail(principal)));
        }

        @PutMapping("/{id}/profile")
        @Audited(module = "MEMBER", action = "UPDATE_PROFILE", ressource = "member_profile")
        @Idempotent(operation = "MEMBER_PROFILE_UPDATE")
        public ResponseEntity<MemberProfileResponse> updateProfile(
                @PathVariable String id,
                @Valid @RequestBody UpdateMemberProfileRequest request) {
            return ResponseEntity.ok(memberProfileService.updateProfile(id, request));
        }

        @PutMapping("/me/profile")
        @Audited(module = "MEMBER", action = "UPDATE_MY_PROFILE", ressource = "member_profile")
        @Idempotent(operation = "MEMBER_MY_PROFILE_UPDATE", required = false)
        public ResponseEntity<MemberProfileResponse> updateMyProfile(
                @AuthenticationPrincipal UserPrincipal principal,
                @Valid @RequestBody UpdateMemberProfileRequest request) {
            return ResponseEntity.ok(memberProfileService.updateMyProfile(resolvePrincipalEmail(principal), request));
        }

        @PatchMapping("/{id}/portal-access/enable")
        @Audited(module = "MEMBER", action = "ENABLE_PORTAL_ACCESS", ressource = "member")
        @Idempotent(operation = "MEMBER_ENABLE_PORTAL_ACCESS", requestBodyArgIndex = -1)
        public ResponseEntity<Void> enablePortalAccess(@PathVariable String id) {
            memberService.enablePortalAccess(id);
            return ResponseEntity.noContent().build();
        }

        @PatchMapping("/{id}/portal-access/disable")
        @Audited(module = "MEMBER", action = "DISABLE_PORTAL_ACCESS", ressource = "member")
        @Idempotent(operation = "MEMBER_DISABLE_PORTAL_ACCESS", requestBodyArgIndex = -1)
        public ResponseEntity<Void> disablePortalAccess(@PathVariable String id) {
            memberService.disablePortalAccess(id);
            return ResponseEntity.noContent().build();
        }

        @PatchMapping("/{id}/kyc-grace-period")
        @Audited(module = "MEMBER", action = "SET_KYC_GRACE_PERIOD", ressource = "member")
        public ResponseEntity<Void> setKycGracePeriod(
                @PathVariable String id,
                @Valid @RequestBody KycGracePeriodRequest request) {
            memberService.setKycGracePeriodDays(id, request.getGracePeriodDays());
            return ResponseEntity.noContent().build();
        }

        @PatchMapping("/{id}/transfer-customer")
        @Audited(module = "MEMBER", action = "TRANSFER_CUSTOMER", ressource = "member")
        @Idempotent(operation = "MEMBER_TRANSFER_CUSTOMER")
        public ResponseEntity<MemberResponse> transferCustomer(
                @PathVariable String id,
                @Valid @RequestBody TransferMemberCustomerRequest request) {
            return ResponseEntity.ok(memberService.transferCustomer(id, request.customerId()));
        }

    @GetMapping({"/search/by-name", "/search/basic"})
    public ResponseEntity<java.util.List<MemberSummaryResponse>> basicSearch(
            @RequestParam("query") String query) {
        return ResponseEntity.ok(memberService.basicSearch(query));
    }

    @PostMapping("/search")
    public ResponseEntity<PaginatedResponse<MemberSummaryResponse>> search(
            @RequestBody(required = false) MemberSearchCriteria criteria,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(memberService.search(criteria, pageable));
    }


    @PostMapping("/portal/complete")
    @Audited(module = "MEMBER", action = "COMPLETE_PORTAL_REGISTRATION", ressource = "member")
    @Idempotent(operation = "MEMBER_COMPLETE_PORTAL_REGISTRATION")
    public ResponseEntity<MemberResponse> completePortalRegistration(
            @Valid @RequestBody PortalMemberActivationRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                userProvisioningService.activatePortalMember(request.email(), getActor(authentication))
        );
    }

    private String getActor(Authentication authentication) {
        return authentication == null || authentication.getName() == null || authentication.getName().isBlank()
                ? "SYSTEM"
                : authentication.getName();
    }

    private String resolvePrincipalEmail(UserPrincipal principal) {
        if (principal == null || principal.getUsername() == null || principal.getUsername().isBlank()) {
            throw new IllegalStateException("Authenticated user email is required");
        }
        return principal.getUsername();
    }
    /**
     * Retire un membre. Par defaut il est archive : le membre passe en ARCHIVED, ses factures,
     * contrats et mouvements restent en base et restent opposables.
     *
     * <p>Avec {@code purge=true}, le membre et toutes ses donnees sont supprimes definitivement
     * - compte utilisateur, factures, portefeuille, contrats, abonnements, reservations · et les
     * montants correspondants disparaissent des agregats comptables. C'est irreversible et sans
     * sauvegarde : reserve au retrait de jeux de test restes en production.
     *
     * <p>La purge n'est pas le defaut a dessein. Ce point d'entree est celui que l'interface
     * appelle pour retirer un membre ordinaire, et une suppression definitive declenchee par ce
     * bouton detruirait la facturation d'un client reel. Previsualiser d'abord avec
     * {@code GET /admin/maintenance/members/{code}/purge-preview}.
     */
    @DeleteMapping("/{id}")
    @Audited(module = "MEMBER", action = "ARCHIVE", ressource = "member")
    public ResponseEntity<?> archive(@PathVariable String id,
                                     @RequestParam(defaultValue = "false") boolean purge,
                                     @RequestParam(required = false) String reason) {
        if (purge) {
            return ResponseEntity.ok(memberPurgeService.purge(id, reason));
        }
        memberService.delete(id);
        return ResponseEntity.noContent().build();
    }

    }
