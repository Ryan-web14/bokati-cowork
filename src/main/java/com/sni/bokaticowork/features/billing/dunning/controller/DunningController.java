package com.sni.bokaticowork.features.billing.dunning.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dunning.model.DunningNotice;
import com.sni.bokaticowork.features.billing.dunning.model.DunningPolicy;
import com.sni.bokaticowork.features.billing.dunning.model.DunningStep;
import com.sni.bokaticowork.features.billing.dunning.service.DunningPolicyService;
import com.sni.bokaticowork.features.billing.dunning.worker.DunningPolicyWorker;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Les relances d'impayes · une politique par segment, des paliers, une trace par facture. */
@RestController
@RequestMapping(ApiPath.V1 + "/billing/dunning")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')")
public class DunningController {

    private final DunningPolicyService policyService;
    private final DunningPolicyWorker worker;

    public record StepRequest(Integer daysAfterDue, DunningStep.Action action, DunningStep.Channel channel, String subjectTemplate, String messageTemplate) {
    }

    public record PolicyRequest(@NotBlank String policyCode, @NotBlank String name, DunningPolicy.Segment segment, DunningPolicy.Tone tone,
                                Boolean active, @NotEmpty List<StepRequest> steps) {
    }

    public record StepView(Integer stepOrder, Integer daysAfterDue, DunningStep.Action action, DunningStep.Channel channel, String subjectTemplate, String messageTemplate) {
        static StepView of(DunningStep s) {
            return new StepView(s.getStepOrder(), s.getDaysAfterDue(), s.getAction(), s.getChannel(), s.getSubjectTemplate(), s.getMessageTemplate());
        }
    }

    public record PolicyView(String policyCode, String name, DunningPolicy.Segment segment, DunningPolicy.Tone tone, boolean active,
                             List<StepView> steps, Instant updatedAt) {
        static PolicyView of(DunningPolicy p) {
            return new PolicyView(p.getPolicyCode(), p.getName(), p.getSegment(), p.getTone(), Boolean.TRUE.equals(p.getActive()),
                    p.getSteps().stream().map(StepView::of).toList(), p.getUpdatedAt());
        }
    }

    public record NoticeView(String documentNumber, String policyCode, Integer stepOrder, DunningStep.Action action, String customerType,
                             String customerCode, String subscriptionNumber, Integer daysOverdue, BigDecimal balanceDue,
                             DunningNotice.Outcome outcome, String detail, Instant executedAt) {
        static NoticeView of(DunningNotice n) {
            return new NoticeView(n.getDocumentNumber(), n.getPolicyCode(), n.getStep().getStepOrder(), n.getAction(), n.getCustomerType(),
                    n.getCustomerCode(), n.getSubscriptionNumber(), n.getDaysOverdue(), n.getBalanceDue(), n.getOutcome(), n.getDetail(), n.getExecutedAt());
        }
    }

    @GetMapping("/policies")
    public ResponseEntity<List<PolicyView>> policies() {
        return ResponseEntity.ok(policyService.list().stream().map(PolicyView::of).toList());
    }

    @GetMapping("/policies/{policyCode}")
    public ResponseEntity<PolicyView> policy(@PathVariable String policyCode) {
        return ResponseEntity.ok(PolicyView.of(policyService.get(policyCode)));
    }

    @PutMapping("/policies")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<PolicyView> save(@Valid @RequestBody PolicyRequest r) {
        return ResponseEntity.ok(PolicyView.of(policyService.save(new DunningPolicyService.PolicySpec(r.policyCode(), r.name(), r.segment(), r.tone(),
                r.active(), r.steps().stream().map(s -> new DunningPolicyService.StepSpec(s.daysAfterDue(), s.action(), s.channel(),
                        s.subjectTemplate(), s.messageTemplate())).toList()))));
    }

    @PostMapping("/policies/{policyCode}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<PolicyView> deactivate(@PathVariable String policyCode) {
        return ResponseEntity.ok(PolicyView.of(policyService.deactivate(policyCode)));
    }

    @GetMapping("/notices")
    public ResponseEntity<List<NoticeView>> recent() {
        return ResponseEntity.ok(policyService.recent().stream().map(NoticeView::of).toList());
    }

    @GetMapping("/notices/documents/{documentNumber}")
    public ResponseEntity<List<NoticeView>> ofDocument(@PathVariable String documentNumber) {
        return ResponseEntity.ok(policyService.noticesOf(documentNumber).stream().map(NoticeView::of).toList());
    }

    @GetMapping("/notices/customers/{customerType}/{customerCode}")
    public ResponseEntity<List<NoticeView>> ofCustomer(@PathVariable String customerType, @PathVariable String customerCode) {
        return ResponseEntity.ok(policyService.noticesOfCustomer(customerType, customerCode).stream().map(NoticeView::of).toList());
    }

    /** Un passage a la main · ce que le worker fera de toute facon demain matin. */
    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<DunningPolicyWorker.Outcome> run() {
        return ResponseEntity.ok(worker.run());
    }
}
