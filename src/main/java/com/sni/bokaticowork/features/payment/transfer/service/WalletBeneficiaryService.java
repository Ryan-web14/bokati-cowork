package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.transfer.model.WalletBeneficiary;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletBeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Les destinataires enregistres d'un portefeuille.
 *
 * <p>Enregistrer un destinataire n'exige pas le code : ce n'est pas une operation, c'est un signet.
 * Le code viendra au transfert, comme toujours · un signet ne fait jamais sortir d'argent.</p>
 */
@Service
@RequiredArgsConstructor
public class WalletBeneficiaryService {

    private final WalletBeneficiaryRepository beneficiaryRepository;
    private final com.sni.bokaticowork.features.payment.repository.WalletAccountRepository walletRepository;
    private final WalletCounterpartyResolver counterpartyResolver;

    @Transactional(readOnly = true)
    public List<WalletBeneficiary> list(WalletAccount owner) {
        return beneficiaryRepository.findByOwnerWallet_IdOrderByAliasAsc(owner.getId());
    }

    @Transactional
    public WalletBeneficiary add(WalletAccount owner, String counterpartyReference, String alias) {
        if (!StringUtils.hasText(alias)) {
            throw new BadRequestException("Donnez un nom à ce destinataire");
        }
        WalletCounterpartyResolver.Counterparty counterparty =
                counterpartyResolver.resolve(counterpartyReference, owner.getCurrency());
        WalletAccount target = counterparty.wallet();
        if (target.getId().equals(owner.getId())) {
            throw new BadRequestException("Vous ne pouvez pas vous enregistrer vous-même comme destinataire");
        }
        return beneficiaryRepository.findByOwnerWallet_IdAndBeneficiaryWallet_Id(owner.getId(), target.getId())
                .map(existing -> {
                    // Deja enregistre · on renomme plutot que de refuser, c'est ce que voulait le titulaire.
                    existing.setAlias(alias.trim());
                    return beneficiaryRepository.save(existing);
                })
                .orElseGet(() -> beneficiaryRepository.save(WalletBeneficiary.builder()
                        // Relus ici · la vue rendue nomme le portefeuille du destinataire, et une
                        // instance venue d'avant la session ne se laisse plus lire apres elle.
                        .ownerWallet(managed(owner))
                        .beneficiaryWallet(managed(target))
                        .alias(alias.trim())
                        .build()));
    }

    /** Le portefeuille tel que cette transaction le connait · jamais l'exemplaire venu d'avant. */
    private WalletAccount managed(WalletAccount wallet) {
        return wallet == null || wallet.getId() == null
                ? wallet
                : walletRepository.findById(wallet.getId()).orElse(wallet);
    }

    @Transactional
    public void remove(WalletAccount owner, Long beneficiaryId) {
        WalletBeneficiary beneficiary = beneficiaryRepository.findByIdAndOwnerWallet_Id(beneficiaryId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Destinataire introuvable"));
        beneficiaryRepository.delete(beneficiary);
    }
}
