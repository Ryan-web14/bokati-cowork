package com.sni.bokaticowork.features.fund.service;

import org.springframework.beans.factory.annotation.Autowired;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.fund.enums.*;
import com.sni.bokaticowork.features.fund.model.FundRequest;
import com.sni.bokaticowork.features.fund.model.FundRequestLine;
import com.sni.bokaticowork.features.fund.repository.FundRequestLineRepository;
import com.sni.bokaticowork.features.fund.repository.FundRequestRepository;
import com.sni.bokaticowork.features.payment.enums.CashRequestStatus;
import com.sni.bokaticowork.features.payment.enums.CashRequestType;
import com.sni.bokaticowork.features.payment.model.CashRequest;
import com.sni.bokaticowork.features.payment.repository.CashRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Suivi dans le temps des demandes de fonds (lots F5 et F6) : rappels aux valideurs et escalade,
 * expiration des demandes approuvees non decaissees, relances de justification (J-1, J, J+3 puis
 * responsable), et reprise des anciennes demandes de caisse.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FundLifecycleService {

    private final FundRequestRepository requestRepository;
    private final FundRequestLineRepository lineRepository;
    private final CashRequestRepository legacyRepository;
    private final FundRequestService requests;
    private final FundNotifier notifier;
    private final SequenceGeneratorFacade sequences;

    @Value("${fund.approval.reminder-hours:24}")
    private long reminderHours;
    @Value("${fund.approval.escalation-days:3}")
    private long escalationDays;

    private static final String APPROVAL_REMINDER_HOURS_KEY = "fund.approval.reminder-hours";
    private static final String APPROVAL_ESCALATION_DAYS_KEY = "fund.approval.escalation-days";

    private PlatformSettings platformSettings;

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    private long reminderHours() {
        return platformSettings == null ? reminderHours : platformSettings.number(APPROVAL_REMINDER_HOURS_KEY, reminderHours);
    }

    private long escalationDays() {
        return platformSettings == null ? escalationDays : platformSettings.number(APPROVAL_ESCALATION_DAYS_KEY, escalationDays);
    }

    public Map<String, Integer> runPeriodic() {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(FundRequestService.ZONE);
        int reminded = 0;
        int escalated = 0;
        int expired = 0;
        int justification = 0;
        for (FundRequest request : requestRepository.findByStatusInOrderByCreatedAtAsc(EnumSet.of(FundRequestStatus.IN_APPROVAL))) {
            Instant since = request.getLevelSince() == null ? request.getSubmittedAt() : request.getLevelSince();
            if (since == null) {
                continue;
            }
            boolean due = Duration.between(since, now).toHours() >= reminderHours()
                    && (request.getApproverRemindedAt() == null || Duration.between(request.getApproverRemindedAt(), now).toHours() >= reminderHours());
            if (due) {
                notifier.toApprovers(request, request.getCurrentLevel());
                if (Duration.between(since, now).toDays() >= escalationDays()) {
                    notifier.toSupervisor(request, "La demande " + request.getNumber() + " attend sa validation de niveau " + request.getCurrentLevel()
                            + " depuis " + Duration.between(since, now).toDays() + " jours.");
                    escalated++;
                }
                request.setApproverRemindedAt(now);
                requestRepository.save(request);
                reminded++;
            }
        }
        for (FundRequest request : requestRepository.findByStatusInOrderByCreatedAtAsc(EnumSet.of(FundRequestStatus.APPROVED))) {
            if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(now) && request.getDisbursedAmount().signum() == 0) {
                request.setStatus(FundRequestStatus.EXPIRED);
                requestRepository.save(request);
                requests.record(request, request.getCurrentLevel(), FundApprovalDecision.EXPIRED, requests.amount(request), null,
                        "Approuvee sans decaissement dans le delai", null);
                requests.releaseBudget(request, true);
                notifier.toRequester(request, "Votre demande " + request.getNumber() + " a expire faute de decaissement.");
                expired++;
            }
        }
        for (FundRequest request : requestRepository.findByStatusInOrderByCreatedAtAsc(EnumSet.of(FundRequestStatus.TO_JUSTIFY,
                FundRequestStatus.PARTIALLY_JUSTIFIED))) {
            LocalDate dueDate = request.getJustificationDueDate();
            if (dueDate == null || (request.getJustificationRemindedAt() != null
                    && request.getJustificationRemindedAt().atZone(FundRequestService.ZONE).toLocalDate().equals(today))) {
                continue;
            }
            int done = request.getJustificationReminders();
            LocalDate next = switch (done) {
                case 0 -> dueDate.minusDays(1);
                case 1 -> dueDate;
                case 2 -> dueDate.plusDays(3);
                default -> dueDate.plusDays(3L + 7L * (done - 2));
            };
            if (today.isBefore(next)) {
                continue;
            }
            String message = "Il reste " + FundRequestService.plain(requests.remainingToJustify(request)) + " XAF a justifier sur l'avance "
                    + request.getNumber() + " (echeance " + dueDate + ").";
            notifier.toBeneficiary(request, message);
            if (done >= 2) {
                notifier.toSupervisor(request, request.getBeneficiaryName() + " : " + message);
            }
            request.setJustificationReminders(done + 1);
            request.setJustificationRemindedAt(now);
            requestRepository.save(request);
            justification++;
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("approverReminders", reminded);
        result.put("escalations", escalated);
        result.put("expired", expired);
        result.put("justificationReminders", justification);
        return result;
    }

    /**
     * Reprise des anciennes demandes de caisse : celles en attente deviennent des demandes soumises
     * (niveau 1) ; les avances executees deviennent des avances a justifier du montant decaisse.
     */
    public Map<String, Integer> importLegacy() {
        int pending = 0;
        int advances = 0;
        for (CashRequest legacy : legacyRepository.findAll()) {
            if (requestRepository.findByLegacyRequestNumber(legacy.getRequestNumber()).isPresent()) {
                continue;
            }
            boolean isPending = legacy.getStatus() == CashRequestStatus.PENDING;
            boolean openAdvance = legacy.getStatus() == CashRequestStatus.EXECUTED && legacy.getRequestType() == CashRequestType.CASH_ADVANCE;
            if (!isPending && !openAdvance) {
                continue;
            }
            FundRequestType type = switch (legacy.getRequestType()) {
                case CASH_ADVANCE -> FundRequestType.ADVANCE;
                case EXPENSE_REIMBURSEMENT -> FundRequestType.EXPENSE_REFUND;
                case REMISE_DE_FONDS -> FundRequestType.FUND_REMITTANCE;
                default -> FundRequestType.DIRECT_EXPENSE;
            };
            String requester = legacy.getRequestedBy() == null ? "SYSTEM" : legacy.getRequestedBy();
            Instant at = legacy.getRequestedAt() == null ? Instant.now() : legacy.getRequestedAt();
            FundRequest request = requestRepository.save(FundRequest.builder().number(sequences.next("fund_request")).requestType(type)
                    .status(isPending ? FundRequestStatus.IN_APPROVAL : FundRequestStatus.TO_JUSTIFY).requester(requester)
                    .beneficiaryType(FundBeneficiaryType.SELF).beneficiaryCode(requester).beneficiaryName(requester)
                    .registerCode(legacy.getCashSession() == null ? null : legacy.getCashSession().getCashRegister().getRegisterCode())
                    .subject(legacy.getReason() == null ? "Reprise de " + legacy.getRequestNumber() : legacy.getReason())
                    .totalAmount(FundRequestService.money(legacy.getAmount()))
                    .approvedAmount(isPending ? null : FundRequestService.money(legacy.getAmount()))
                    .disbursedAmount(isPending ? java.math.BigDecimal.ZERO : FundRequestService.money(legacy.getAmount()))
                    .requiredLevels(requests.levelsFor(FundRequestService.money(legacy.getAmount()))).currentLevel(isPending ? 1 : 0)
                    .submittedAt(at).levelSince(Instant.now()).disbursedAt(isPending ? null : legacy.getExecutedAt())
                    .justificationDueDate(isPending ? null : LocalDate.now(FundRequestService.ZONE).plusDays(7))
                    .legacyRequestNumber(legacy.getRequestNumber()).build());
            lineRepository.save(FundRequestLine.builder().requestId(request.getId()).lineNo(1)
                    .natureCode(type == FundRequestType.ADVANCE ? FundRequestService.ADVANCE_NATURE : null)
                    .designation(request.getSubject()).amount(request.getTotalAmount()).build());
            if (isPending) {
                pending++;
            } else {
                advances++;
            }
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("pendingRequests", pending);
        result.put("openAdvances", advances);
        return result;
    }
}
