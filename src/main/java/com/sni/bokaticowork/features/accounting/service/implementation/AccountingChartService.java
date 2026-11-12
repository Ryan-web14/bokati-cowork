package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.dto.AccountingResponses;
import com.sni.bokaticowork.features.accounting.enums.AccountNature;
import com.sni.bokaticowork.features.accounting.enums.AccountSide;
import com.sni.bokaticowork.features.accounting.enums.AccountStatus;
import com.sni.bokaticowork.features.accounting.enums.JournalType;
import com.sni.bokaticowork.features.accounting.enums.ThirdPartyType;
import com.sni.bokaticowork.features.accounting.enums.TreasuryType;
import com.sni.bokaticowork.features.accounting.model.AccAccount;
import com.sni.bokaticowork.features.accounting.model.AccAccountHistory;
import com.sni.bokaticowork.features.accounting.model.AccJournal;
import com.sni.bokaticowork.features.accounting.model.AccThirdPartyAccount;
import com.sni.bokaticowork.features.accounting.repository.AccAccountHistoryRepository;
import com.sni.bokaticowork.features.accounting.repository.AccAccountRepository;
import com.sni.bokaticowork.features.accounting.repository.AccEntryLineRepository;
import com.sni.bokaticowork.features.accounting.repository.AccJournalRepository;
import com.sni.bokaticowork.features.accounting.repository.AccReferenceChartRepository;
import com.sni.bokaticowork.features.accounting.repository.AccThirdPartyAccountRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.EntryNumbering;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Plan de comptes (plan, section 4.1) : creation d'un compte sous un parent, numero propose,
 * proprietes heritees, controles, historique ; desactivation au lieu de suppression ; comptes
 * auxiliaires de tiers crees a la premiere ecriture.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AccountingChartService {

    private static final String MANAGE = "ACCOUNTING_ACCOUNT:MANAGE";

    private final AccAccountRepository accountRepository;
    private final AccAccountHistoryRepository historyRepository;
    private final AccReferenceChartRepository referenceRepository;
    private final AccThirdPartyAccountRepository thirdPartyRepository;
    private final AccEntryLineRepository lineRepository;
    private final AccJournalRepository journalRepository;
    private final AccountingSettings settings;
    private final AccountingNotifier notifier;
    private final CashActorResolver actors;

    @Transactional(readOnly = true)
    public List<AccountingResponses.Account> list(String search, String classCode, boolean includeInactive) {
        String term = StringUtils.hasText(search) ? normalize(search) : null;
        List<AccAccount> accounts = accountRepository.findAllByOrderByNumberAsc();
        return accounts.stream()
                .filter(account -> includeInactive || account.getStatus() == AccountStatus.ACTIVE || account.getStatus() == AccountStatus.PENDING_APPROVAL)
                .filter(account -> classCode == null || classCode.equals(account.getClassCode()))
                .filter(account -> term == null || account.getNumber().startsWith(term) || normalize(account.getLabel()).contains(term))
                .map(account -> toResponse(account, null, accounts))
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountingResponses.Account get(String number) {
        AccAccount account = account(number);
        return toResponse(account, hasPostings(account.getNumber()), accountRepository.findByParentNumberOrderByNumberAsc(account.getNumber()));
    }

    /** Valeurs proposees pour un compte a creer sous ce parent : numero libre et proprietes heritees. */
    @Transactional(readOnly = true)
    public AccountingResponses.AccountProposal proposal(String parentNumber) {
        AccAccount parent = account(parentNumber);
        return new AccountingResponses.AccountProposal(parent.getNumber(), nextFreeNumber(parent), parent.getClassCode(), parent.getNature(),
                parent.getNormalSide(), parent.getReconcilable(), parent.getThirdPartyType(), parent.getTreasuryType(),
                parent.getAnalyticRequired(), parent.getTaxCode(), parent.getStatementLineCode(), settings.accountCreationNeedsApproval());
    }

    public AccountingResponses.Account create(AccountingRequests.CreateAccount request) {
        String actor = actors.resolve("SYSTEM");
        AccAccount parent = StringUtils.hasText(request.parentNumber()) ? account(request.parentNumber()) : null;
        String number = StringUtils.hasText(request.number()) ? request.number().trim() : parent == null ? null : nextFreeNumber(parent);
        if (number == null) {
            throw new BadRequestException("Choisissez le compte parent, ou indiquez le numero d'un compte principal");
        }
        if (!number.matches("[1-9][0-9]{0,11}")) {
            throw new BadRequestException("Un numero de compte est fait de chiffres et commence par le chiffre de sa classe (1 a 9)");
        }
        if (accountRepository.findByNumber(number).isPresent()) {
            throw new BadRequestException("Le compte " + number + " existe deja");
        }
        if (parent == null) {
            // Nouveau compte principal : permis s'il est dans la nomenclature de reference, sinon
            // reserve a l'administrateur finance, avec un motif.
            parent = accountRepository.findAllByOrderByNumberAsc().stream()
                    .filter(candidate -> number.startsWith(candidate.getNumber()) && !candidate.getNumber().equals(number))
                    .reduce((first, second) -> second.getNumber().length() > first.getNumber().length() ? second : first)
                    .orElseThrow(() -> new BadRequestException("Le compte " + number + " ne se rattache a aucun compte du plan"));
            if (referenceRepository.findById(number).isEmpty()) {
                if (!actors.hasAuthority(MANAGE)) {
                    throw new ForbiddenException("Le compte " + number + " n'est pas dans la nomenclature SYSCOHADA de reference :"
                            + " sa creation est reservee a l'administrateur finance");
                }
                if (!StringUtils.hasText(request.reason())) {
                    throw new BadRequestException("Indiquez le motif de la creation d'un compte principal hors nomenclature");
                }
            }
        }
        if (!number.startsWith(parent.getNumber()) || number.length() <= parent.getNumber().length()) {
            throw new BadRequestException("Le compte " + number + " doit se trouver sous son parent " + parent.getNumber()
                    + " : il commence par " + parent.getNumber() + " et il est plus long");
        }
        if (parent.getStatus() == AccountStatus.INACTIVE || parent.getStatus() == AccountStatus.MERGED) {
            throw new BadRequestException("Le compte parent " + parent.getNumber() + " est desactive");
        }
        String statementLine = StringUtils.hasText(request.statementLineCode()) ? request.statementLineCode().trim() : parent.getStatementLineCode();
        if (!StringUtils.hasText(statementLine)) {
            throw new BadRequestException("Un compte sans poste d'etat financier ne peut pas etre cree : les etats seraient incomplets");
        }
        boolean needsApproval = settings.accountCreationNeedsApproval() && !actors.hasAuthority(MANAGE);
        AccAccount account = accountRepository.save(AccAccount.builder()
                .number(number)
                .label(request.label().trim())
                .parentNumber(parent.getNumber())
                .classCode(number.substring(0, 1))
                .nature(parent.getNature())
                .normalSide(parent.getNormalSide())
                .reconcilable(request.reconcilable() != null ? request.reconcilable() : parent.getReconcilable())
                .thirdPartyType(request.thirdPartyType() != null ? request.thirdPartyType() : parent.getThirdPartyType())
                .treasuryType(request.treasuryType() != null ? request.treasuryType() : parent.getTreasuryType())
                .analyticRequired(request.analyticRequired() != null ? request.analyticRequired() : parent.getAnalyticRequired())
                .taxCode(StringUtils.hasText(request.taxCode()) ? request.taxCode().trim() : parent.getTaxCode())
                .statementLineCode(statementLine)
                .status(needsApproval ? AccountStatus.PENDING_APPROVAL : AccountStatus.ACTIVE)
                .createdBy(actor)
                .build());
        history(account.getNumber(), "created", null, account.getLabel(), request.reason(), actor);
        if (account.getTreasuryType() != null) {
            ensureTreasuryJournal(account, actor);
        }
        notifier.notifyAccountants("Nouveau compte comptable · " + account.getNumber(),
                actor + " a cree le compte " + account.getNumber() + " « " + account.getLabel() + " » sous " + parent.getNumber()
                        + (needsApproval ? " ; il attend votre validation." : "."), account.getNumber(), "/accounting/accounts/" + account.getNumber(), null);
        return get(account.getNumber());
    }

    /** Un compte de tresorerie a son journal : il est cree avec le compte s'il n'existe pas. */
    private void ensureTreasuryJournal(AccAccount account, String actor) {
        boolean exists = journalRepository.findAllByOrderByCodeAsc().stream()
                .anyMatch(journal -> account.getNumber().equals(journal.getTreasuryAccountNumber()));
        if (exists) {
            return;
        }
        String prefix = account.getTreasuryType() == TreasuryType.CASH ? "CA" : account.getTreasuryType() == TreasuryType.BANK ? "BQ" : "MM";
        JournalType type = account.getTreasuryType() == TreasuryType.CASH ? JournalType.CASH
                : account.getTreasuryType() == TreasuryType.BANK ? JournalType.BANK : JournalType.MOBILE_MONEY;
        String code = prefix + "-" + account.getNumber();
        if (journalRepository.findByCode(code).isPresent()) {
            return;
        }
        journalRepository.save(AccJournal.builder().code(code).label(account.getLabel()).type(type)
                .treasuryAccountNumber(account.getNumber()).autoValidate(true).createdBy(actor).build());
    }

    public AccountingResponses.Account update(String number, AccountingRequests.UpdateAccount request) {
        AccAccount account = account(number);
        String actor = actors.resolve("SYSTEM");
        boolean posted = hasPostings(account.getNumber());
        List<Runnable> structural = new ArrayList<>();
        if (StringUtils.hasText(request.label()) && !request.label().trim().equals(account.getLabel())) {
            history(number, "label", account.getLabel(), request.label().trim(), request.reason(), actor);
            account.setLabel(request.label().trim());
        }
        change(posted, "reconcilable", account.getReconcilable(), request.reconcilable(), account::setReconcilable, number, request.reason(), actor, true);
        change(posted, "thirdPartyType", account.getThirdPartyType(), request.thirdPartyType(), account::setThirdPartyType, number, request.reason(), actor, true);
        change(posted, "treasuryType", account.getTreasuryType(), request.treasuryType(), account::setTreasuryType, number, request.reason(), actor, true);
        change(posted, "analyticRequired", account.getAnalyticRequired(), request.analyticRequired(), account::setAnalyticRequired, number, request.reason(), actor, false);
        change(posted, "taxCode", account.getTaxCode(), request.taxCode(), account::setTaxCode, number, request.reason(), actor, false);
        change(posted, "statementLineCode", account.getStatementLineCode(), request.statementLineCode(), account::setStatementLineCode, number, request.reason(), actor, false);
        structural.forEach(Runnable::run);
        account.setUpdatedBy(actor);
        accountRepository.save(account);
        return get(number);
    }

    private <T> void change(boolean posted, String field, T oldValue, T newValue, Consumer<T> setter, String number, String reason,
                            String actor, boolean frozenOncePosted) {
        if (newValue == null || Objects.equals(oldValue, newValue)) {
            return;
        }
        if (posted && frozenOncePosted) {
            throw new BadRequestException("Le compte " + number + " porte deja des ecritures validees : « " + field + " » ne change plus");
        }
        history(number, field, oldValue == null ? null : oldValue.toString(), newValue.toString(), reason, actor);
        setter.accept(newValue);
    }

    public AccountingResponses.Account deactivate(String number, String reason) {
        AccAccount account = account(number);
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Indiquez le motif de la desactivation");
        }
        if (accountRepository.findByParentNumberOrderByNumberAsc(number).stream()
                .anyMatch(child -> child.getStatus() == AccountStatus.ACTIVE)) {
            throw new BadRequestException("Desactivez d'abord les comptes divisionnaires actifs de " + number);
        }
        String actor = actors.resolve("SYSTEM");
        history(number, "status", account.getStatus().name(), AccountStatus.INACTIVE.name(), reason, actor);
        account.setStatus(AccountStatus.INACTIVE);
        account.setUpdatedBy(actor);
        accountRepository.save(account);
        return get(number);
    }

    public AccountingResponses.Account reactivate(String number) {
        AccAccount account = account(number);
        if (account.getStatus() == AccountStatus.MERGED) {
            throw new BadRequestException("Un compte fusionne ne se reactive pas");
        }
        String actor = actors.resolve("SYSTEM");
        history(number, "status", account.getStatus().name(), AccountStatus.ACTIVE.name(), null, actor);
        account.setStatus(AccountStatus.ACTIVE);
        if (account.getApprovedBy() == null) {
            account.setApprovedBy(actor);
            account.setApprovedAt(Instant.now());
        }
        accountRepository.save(account);
        return get(number);
    }

    @Transactional(readOnly = true)
    public List<AccountingResponses.AccountHistory> history(String number) {
        return historyRepository.findByAccountNumberOrderByCreatedAtDesc(number).stream()
                .map(entry -> new AccountingResponses.AccountHistory(entry.getField(), entry.getOldValue(), entry.getNewValue(),
                        entry.getReason(), entry.getChangedBy(), entry.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccountingResponses.ThirdParty> thirdParties(String number) {
        return thirdPartyRepository.findByAccountNumberOrderByPartyCodeAsc(number).stream()
                .map(party -> new AccountingResponses.ThirdParty(party.getAccountNumber(), party.getPartyType(), party.getPartyCode(),
                        party.getLabel(), party.getCreatedAt()))
                .toList();
    }

    /** Compte auxiliaire du tiers, cree a sa premiere ecriture. */
    public void registerThirdParty(String accountNumber, ThirdPartyType type, String partyCode, String label) {
        if (!StringUtils.hasText(partyCode)) {
            return;
        }
        if (thirdPartyRepository.findByAccountNumberAndPartyCode(accountNumber, partyCode.trim()).isEmpty()) {
            thirdPartyRepository.save(AccThirdPartyAccount.builder().accountNumber(accountNumber)
                    .partyType(type == null ? ThirdPartyType.OTHER : type).partyCode(partyCode.trim()).label(label).build());
        }
    }

    /** Le compte du plan qui porte le numero ; inconnu : erreur lisible. */
    public AccAccount account(String number) {
        if (!StringUtils.hasText(number)) {
            throw new BadRequestException("Numero de compte manquant");
        }
        return accountRepository.findByNumber(number.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Compte comptable introuvable : " + number));
    }

    public Optional<AccAccount> find(String number) {
        return StringUtils.hasText(number) ? accountRepository.findByNumber(number.trim()) : Optional.empty();
    }

    public boolean hasPostings(String number) {
        return lineRepository.countPostedOnAccount(number, EntryNumbering.NUMBERED) > 0;
    }

    /** Prochain numero libre sous le parent : un chiffre de plus, puis deux. */
    String nextFreeNumber(AccAccount parent) {
        for (int digit = 1; digit <= 9; digit++) {
            String candidate = parent.getNumber() + digit;
            if (accountRepository.findByNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        for (int value = 1; value <= 99; value++) {
            String candidate = parent.getNumber() + String.format("%02d", value);
            if (accountRepository.findByNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new BadRequestException("Plus de numero libre sous " + parent.getNumber());
    }

    private void history(String number, String field, String oldValue, String newValue, String reason, String actor) {
        historyRepository.save(AccAccountHistory.builder().accountNumber(number).field(field).oldValue(oldValue).newValue(newValue)
                .reason(StringUtils.hasText(reason) ? reason.trim() : null).changedBy(actor).build());
    }

    private AccountingResponses.Account toResponse(AccAccount account, Boolean posted, List<AccAccount> universe) {
        long children = universe.stream().filter(candidate -> account.getNumber().equals(candidate.getParentNumber())).count();
        return new AccountingResponses.Account(account.getNumber(), account.getLabel(), account.getParentNumber(), account.getClassCode(),
                account.getNature(), account.getNormalSide(), account.getReconcilable(), account.getThirdPartyType(), account.getTreasuryType(),
                account.getAnalyticRequired(), account.getTaxCode(), account.getStatementLineCode(), account.getStatus(), account.getMergedInto(),
                posted, (int) children, account.getCreatedBy(), account.getCreatedAt());
    }

    private static String normalize(String value) {
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    static boolean isAnalyticOrOffBalance(AccAccount account) {
        return account.getNature() == AccountNature.ANALYTIC || account.getNature() == AccountNature.OFF_BALANCE;
    }

    static AccountSide side(AccAccount account) {
        return account.getNormalSide();
    }
}
