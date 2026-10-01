package com.sni.bokaticowork.features.payment.compliance.service;

import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.payment.compliance.model.ScreeningCheck;
import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.control.service.WalletRiskFlagService;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

/**
 * Ce que les autres modules disent a la conformite · apres coup, jamais en travers.
 *
 * <p>Une inscription qui reussit est controlee sur les listes une fois validee ; un changement de
 * numero de telephone est signale et declenche un recontrole. Tout part apres le commit de
 * l'operation qui l'a provoque : la conformite regarde, elle ne retient pas la main. Et si elle
 * echoue, l'operation est deja faite · c'est voulu, un module de surveillance qui pourrait faire
 * echouer une inscription serait un moyen simple de la faire echouer.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComplianceSignals {

    private final ScreeningService screeningService;
    private final WalletRiskFlagService flagService;
    private final WalletAccountRepository walletRepository;
    private final com.sni.bokaticowork.features.client.member.repository.repo.MemberProfileRepository profileRepository;

    /** A l'entree en relation · le controle des listes, avec sa preuve. */
    public void memberRegistered(Member member) {
        afterCommit(() -> screeningService.screen(ScreeningService.SUBJECT_MEMBER, member.getMemberId(),
                fullName(member), birthYear(member), ScreeningCheck.Trigger.ONBOARDING, "SYSTEM"));
    }

    /**
     * Un changement significatif · le numero de telephone.
     *
     * <p>Un numero qui change est un moyen de connexion et de rechargement qui change. Le signaler
     * ne suspecte personne : la revue verra que c'est le titulaire, et passera. Mais un compte
     * repris par quelqu'un d'autre commence presque toujours par la.</p>
     */
    public void memberPhoneChanged(Member member, String previousPhone, String newPhone, String actor) {
        afterCommit(() -> {
            walletOf(member).ifPresent(wallet -> flagService.raise(wallet, WalletRiskFlag.Type.PHONE_CHANGED,
                    WalletRiskFlag.Severity.MEDIUM,
                    "Numéro changé de " + previousPhone + " à " + newPhone + " par " + actor, newPhone));
            screeningService.screen(ScreeningService.SUBJECT_MEMBER, member.getMemberId(),
                    fullName(member), birthYear(member), ScreeningCheck.Trigger.PHONE_CHANGE, actor);
        });
    }

    private Optional<WalletAccount> walletOf(Member member) {
        return walletRepository.list("MEMBER", member.getMemberId(), PageRequest.of(0, 1)).stream().findFirst();
    }

    private void afterCommit(Runnable task) {
        Runnable guarded = () -> {
            try {
                task.run();
            } catch (RuntimeException ex) {
                log.error("Conformite · signal en echec, l'operation d'origine est deja faite", ex);
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            guarded.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                guarded.run();
            }
        });
    }

    static String fullName(Member member) {
        String first = member.getFirstname() == null ? "" : member.getFirstname();
        String last = member.getLastname() == null ? "" : member.getLastname();
        return (first + " " + last).trim();
    }

    /** L'annee de naissance vit dans le profil · absente, le rapprochement se fait sur le nom seul. */
    private Integer birthYear(Member member) {
        return profileRepository.findByMember_MemberId(member.getMemberId())
                .map(profile -> profile.getBirthDate() == null ? null : profile.getBirthDate().getYear())
                .orElse(null);
    }
}
