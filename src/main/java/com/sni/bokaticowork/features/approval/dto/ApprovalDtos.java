package com.sni.bokaticowork.features.approval.dto;

import com.sni.bokaticowork.features.approval.enums.ApprovalEventType;
import com.sni.bokaticowork.features.approval.enums.ApprovalStatus;
import com.sni.bokaticowork.features.approval.enums.ApprovalStepStatus;
import com.sni.bokaticowork.features.approval.enums.ApproverRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Echanges des centres d'approbation (lot B15). */
public final class ApprovalDtos {

    private ApprovalDtos() {
    }

    /** Niveau demande : libelle, regle, profondeur hierarchique (N+1 = 1), droit, utilisateur nomme. */
    public record LevelSpec(@NotBlank @Size(max = 120) String label, @NotNull ApproverRule rule, Integer depth,
                            @Size(max = 80) String permission, @Size(max = 180) String userLogin, BigDecimal minAmount) {
    }

    /**
     * Soumission par un centre. {@code levels} vide : les niveaux viennent du circuit du type ;
     * {@code requester} vide : l'utilisateur connecte ; {@code startLevel} : reprise d'une demande
     * dont les premiers niveaux sont deja franchis.
     */
    public record Submission(String center, String requestType, String subjectType, String subjectRef, String title,
                             BigDecimal amount, String currency, String requester, String detailsJson, List<LevelSpec> levels,
                             Integer startLevel) {
    }

    /** Decision : commentaire, et montant reduit le cas echeant (montant avant et apres dans la frise). */
    public record Decision(@Size(max = 1000) String comment, BigDecimal reducedAmount) {
    }

    /**
     * Effet d'une approbation : niveaux {@code fromLevel} a {@code toLevel} approuves d'un coup
     * (ceux d'avant {@code toLevel} au titre de la validation en avance), demande approuvee ou non.
     */
    public record Outcome(String number, String subjectRef, ApprovalStatus status, int fromLevel, int toLevel, boolean finalApproval,
                          String actor, String delegatedFrom, BigDecimal amountBefore, BigDecimal amountAfter, String comment) {
    }

    public record StepView(Integer level, String label, ApproverRule rule, String expectedApprover, String expectedApproverName,
                           String permission, ApprovalStepStatus status, String decidedBy, String decidedByName, String delegatedFrom,
                           Instant decidedAt, String comment) {
    }

    public record EventView(Integer level, String actor, String actorName, ApprovalEventType event, String comment, BigDecimal amountBefore,
                            BigDecimal amountAfter, String delegatedFrom, Instant occurredAt) {
    }

    /**
     * Demande vue d'un ecran : ou elle en est, qui doit agir et depuis quand.
     *
     * @param waitingFor qui doit agir : nom du decideur attendu, ou libelle du droit requis
     * @param canDecide  l'utilisateur connecte peut-il decider (au niveau courant ou en avance)
     */
    public record RequestView(String number, String center, String requestType, String subjectType, String subjectRef, String title,
                              BigDecimal amount, String currency, String requester, String requesterName, ApprovalStatus status,
                              Integer requiredLevels, Integer currentLevel, String currentLabel, String waitingFor, Instant levelSince,
                              String lastComment, Instant submittedAt, Instant decidedAt, String decidedBy, String detailsJson,
                              boolean canDecide, List<StepView> steps, List<EventView> timeline) {
    }

    public record CircuitInput(@NotBlank @Size(max = 60) String code, @NotBlank @Size(max = 30) String center,
                               @NotBlank @Size(max = 60) String requestType, @NotBlank @Size(max = 160) String label, Boolean active,
                               @NotEmpty List<@Valid LevelSpec> levels) {
    }

    public record CircuitView(String code, String center, String requestType, String label, boolean active, List<LevelSpec> levels) {
    }

    public record HierarchyNode(String userCode, String email, String fullName, String jobTitle, String managerUserCode, String managerName) {
    }

    public record HierarchyUpdate(@NotBlank String userCode, String managerUserCode) {
    }

    public record DelegationInput(@NotBlank String delegate, String center, Integer level, @NotNull LocalDate startsOn,
                                  @NotNull LocalDate endsOn, @Size(max = 500) String reason) {
    }

    public record DelegationView(Long id, String delegator, String delegate, String center, Integer level, LocalDate startsOn,
                                 LocalDate endsOn, String reason) {
    }
}
