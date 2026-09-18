package com.sni.bokaticowork.features.portal.wallet.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.portal.wallet.dto.SetWalletPinRequest;
import com.sni.bokaticowork.features.portal.wallet.dto.WalletSecurityStatusResponse;
import com.sni.bokaticowork.features.portal.wallet.service.ClientWalletSecurityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/client/wallet")
@RequiredArgsConstructor
public class ClientWalletSecurityController {

    private final ClientContextService clientContextService;
    private final ClientWalletSecurityService securityService;

    /** Etat du code secret et des plafonds · a lire avant de proposer une operation, pas apres. */
    @GetMapping("/{walletNumber}/security")
    public ResponseEntity<WalletSecurityStatusResponse> status(@PathVariable String walletNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(securityService.status(member, walletNumber));
    }

    @PostMapping("/{walletNumber}/security/pin")
    public ResponseEntity<WalletSecurityStatusResponse> setPin(@PathVariable String walletNumber,
                                                               @Valid @RequestBody SetWalletPinRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(securityService.setPin(member, walletNumber, request));
    }
}
