package com.sni.bokaticowork.features.portal.wallet.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.payment.limit.service.WalletKycLevelResolver;
import com.sni.bokaticowork.features.payment.limit.service.WalletLimitService;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.security.model.WalletCredential;
import com.sni.bokaticowork.features.payment.security.model.WalletSecurityPolicy;
import com.sni.bokaticowork.features.payment.security.repository.WalletCredentialRepository;
import com.sni.bokaticowork.features.payment.security.service.WalletSecurityService;
import com.sni.bokaticowork.features.portal.wallet.dto.SetWalletPinRequest;
import com.sni.bokaticowork.features.portal.wallet.dto.WalletSecurityStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

/**
 * Le code secret et les plafonds, vus par leur titulaire.
 *
 * <p>Toute lecture passe par la verification d'appartenance, et un portefeuille qui n'est pas le
 * sien est rendu « introuvable » plutot qu'« interdit » · repondre « interdit » confirmerait
 * l'existence du portefeuille a qui essaie des numeros.</p>
 */
@Service
@RequiredArgsConstructor
public class ClientWalletSecurityService {

    private static final String OWNER_TYPE = "MEMBER";

    private final WalletAccountRepository walletRepository;
    private final WalletCredentialRepository credentialRepository;
    private final WalletSecurityService securityService;
    private final WalletLimitService limitService;
    private final WalletKycLevelResolver kycLevelResolver;

    @Transactional(readOnly = true)
    public WalletSecurityStatusResponse status(Member member, String walletNumber) {
        WalletAccount wallet = ownedWallet(member, walletNumber);
        WalletSecurityPolicy policy = securityService.policyFor(wallet);
        Optional<WalletCredential> credential = credentialRepository.findByWalletId(wallet.getId());
        Instant now = Instant.now();

        boolean locked = credential.map(value -> value.lockedAt(now)).orElse(false);
        Long lockedMinutes = locked
                ? Math.max(1, Duration.between(now, credential.get().getLockedUntil()).toMinutes())
                : null;

        return new WalletSecurityStatusResponse(
                wallet.getWalletNumber(),
                credential.isPresent(),
                credential.map(value -> Boolean.TRUE.equals(value.getMustChangePin())).orElse(false),
                credential.map(value -> value.expiredAt(now)).orElse(false),
                locked,
                lockedMinutes,
                policy.getPinLength(),
                policy.requiredOperations().stream()
                        .map(Enum::name)
                        .sorted(Comparator.naturalOrder())
                        .toList(),
                limitService.snapshot(wallet, kycLevelResolver.levelOf(wallet)));
    }

    @Transactional
    public WalletSecurityStatusResponse setPin(Member member, String walletNumber, SetWalletPinRequest request) {
        WalletAccount wallet = ownedWallet(member, walletNumber);
        securityService.setPin(wallet, request.currentPin(), request.newPin());
        return status(member, walletNumber);
    }

    private WalletAccount ownedWallet(Member member, String walletNumber) {
        WalletAccount wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Portefeuille introuvable"));
        if (!OWNER_TYPE.equals(wallet.getOwnerType()) || !member.getMemberId().equals(wallet.getOwnerCode())) {
            throw new ResourceNotFoundException("Portefeuille introuvable");
        }
        return wallet;
    }
}
