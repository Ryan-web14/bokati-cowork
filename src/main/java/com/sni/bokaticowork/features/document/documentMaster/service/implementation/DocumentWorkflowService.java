package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import org.springframework.util.StringUtils;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.CreateWorkflowRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.WorkflowActionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.WorkflowInstanceResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.WorkflowResponse;
import com.sni.bokaticowork.features.document.documentMaster.model.*;
import com.sni.bokaticowork.features.document.documentMaster.repository.*;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentWorkflowService {

    private final DocumentWorkflowRepository workflowRepository;
    private final DocumentWorkflowStepRepository stepRepository;
    private final DocumentWorkflowInstanceRepository instanceRepository;
    private final DocumentWorkflowActionRepository actionRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    public WorkflowResponse create(CreateWorkflowRequest request) {
        DocumentWorkflow workflow = DocumentWorkflow.builder()
                .code(sequenceGenerator.next("WORKFLOW", LocalDate.now()))
                .name(request.getName().trim())
                .description(request.getDescription())
                .workflowType(request.getWorkflowType() != null ? request.getWorkflowType() : "SIMPLE")
                .space(request.getSpace())
                .documentTypeCode(request.getDocumentTypeCode())
                .createdBy(currentUserId())
                .build();
        workflowRepository.save(workflow);

        for (int i = 0; i < request.getSteps().size(); i++) {
            CreateWorkflowRequest.WorkflowStepRequest stepReq = request.getSteps().get(i);
            DocumentWorkflowStep step = DocumentWorkflowStep.builder()
                    .workflow(workflow)
                    .stepOrder(i + 1)
                    .stepName(stepReq.getStepName().trim())
                    .approverType(stepReq.getApproverType().trim())
                    .approverValue(stepReq.getApproverValue().trim())
                    .required(stepReq.getRequired() != null ? stepReq.getRequired() : Boolean.TRUE)
                    .autoApproveDays(stepReq.getAutoApproveDays())
                    .build();
            stepRepository.save(step);
        }
        return toResponse(workflow);
    }

    @Transactional(readOnly = true)
    public WorkflowResponse getByCode(String code) {
        return toResponse(findWorkflow(code));
    }

    @Transactional(readOnly = true)
    public List<WorkflowResponse> list(Boolean activeOnly) {
        List<DocumentWorkflow> workflows = Boolean.TRUE.equals(activeOnly)
                ? workflowRepository.findAllByActiveTrueOrderByNameAsc()
                : workflowRepository.findAllByOrderByNameAsc();
        return workflows.stream().map(this::toResponse).toList();
    }

    public void delete(String code) {
        DocumentWorkflow workflow = findWorkflow(code);
        workflow.setActive(false);
        workflowRepository.save(workflow);
    }

    public WorkflowInstanceResponse startWorkflow(String documentCode, String workflowCode) {
        Document document = documentRepository.findByCode(documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable"));
        DocumentWorkflow workflow = findWorkflow(workflowCode);
        // Lot KG5 : un circuit inactif, d'un autre espace ou d'un autre type ne demarre pas ;
        // un document deja tranche non plus.
        if (!Boolean.TRUE.equals(workflow.getActive())) {
            throw new BadRequestException("Le circuit " + workflow.getCode() + " est désactivé");
        }
        if (workflow.getSpace() != null && workflow.getSpace() != document.getSpace()) {
            throw new BadRequestException("Le circuit " + workflow.getCode() + " concerne l'espace " + workflow.getSpace()
                    + ", pas l'espace " + document.getSpace() + " du document");
        }
        if (StringUtils.hasText(workflow.getDocumentTypeCode()) && (document.getDocumentType() == null
                || !workflow.getDocumentTypeCode().equalsIgnoreCase(document.getDocumentType().getCode()))) {
            throw new BadRequestException("Le circuit " + workflow.getCode() + " concerne le type " + workflow.getDocumentTypeCode());
        }
        if (java.util.EnumSet.of(DocumentStatus.APPROVED, DocumentStatus.SIGNED, DocumentStatus.REJECTED, DocumentStatus.ARCHIVED,
                DocumentStatus.EXPIRED, DocumentStatus.SUPERSEDED).contains(document.getStatus())) {
            throw new BadRequestException("Le document est " + document.getStatus() + " : il ne passe plus en circuit d'approbation");
        }

        instanceRepository.findByDocumentAndStatusIn(document, List.of("PENDING", "IN_PROGRESS"))
                .ifPresent(i -> { throw new BadRequestException("Ce document a déjà un circuit d'approbation en cours"); });

        DocumentWorkflowInstance instance = DocumentWorkflowInstance.builder()
                .workflow(workflow)
                .document(document)
                .currentStep(1)
                .status("IN_PROGRESS")
                .startedBy(currentUserId())
                .build();
        instanceRepository.save(instance);
        return toInstanceResponse(instance);
    }

    public WorkflowInstanceResponse approve(String documentCode, WorkflowActionRequest request) {
        return processAction(documentCode, "APPROVE", request);
    }

    public WorkflowInstanceResponse reject(String documentCode, WorkflowActionRequest request) {
        return processAction(documentCode, "REJECT", request);
    }

    @Transactional(readOnly = true)
    public WorkflowInstanceResponse getStatus(String documentCode) {
        Document document = documentRepository.findByCode(documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable"));
        // Lot KG5 : la derniere instance (avant : erreur 500 des le deuxieme circuit du document).
        DocumentWorkflowInstance instance = instanceRepository.findFirstByDocumentOrderByStartedAtDesc(document)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun circuit d'approbation pour ce document"));
        return toInstanceResponse(instance);
    }

    @Transactional(readOnly = true)
    public List<WorkflowInstanceResponse> myPending() {
        String email = currentUserEmail();
        List<String> roles = currentUserRoles();
        return instanceRepository.findPendingForApprover(email, roles)
                .stream().map(this::toInstanceResponse).toList();
    }

    private WorkflowInstanceResponse processAction(String documentCode, String action, WorkflowActionRequest request) {
        return processAction(documentCode, action, request, false);
    }

    private com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService documentService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setDocumentService(@org.springframework.context.annotation.Lazy
                            com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService documentService) {
        this.documentService = documentService;
    }

    /** Decision du circuit sur le document, par l'approbation normale (trace, KYC, evenements). */
    private void decideDocument(Document document, boolean approved, String comment) {
        if (documentService == null || document.getStatus() != DocumentStatus.PENDING_REVIEW && document.getStatus() != DocumentStatus.UPLOADED
                && document.getStatus() != DocumentStatus.NEEDS_CORRECTION) {
            return;
        }
        var decision = new com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentReviewDecisionRequest();
        decision.setComment(comment);
        if (!approved) {
            decision.setRejectionReasonDetail(comment);
        }
        com.sni.bokaticowork.features.document.documentMaster.security.DocumentReviewGuard.asWorkflow(() -> {
            if (approved) {
                documentService.approve(document.getCode(), decision);
            } else {
                documentService.reject(document.getCode(), decision);
            }
        });
    }

    /** Le compte connecte est-il l'approbateur de l'etape (compte designe, par courriel ou identifiant, ou role) ? */
    private boolean isApprover(DocumentWorkflowStep step) {
        String value = step.getApproverValue() == null ? "" : step.getApproverValue().trim();
        if ("USER".equalsIgnoreCase(step.getApproverType())) {
            Long id = currentUserId();
            return value.equalsIgnoreCase(currentUserEmail()) || (id != null && value.equals(String.valueOf(id)));
        }
        if ("ROLE".equalsIgnoreCase(step.getApproverType())) {
            String bare = value.toUpperCase(java.util.Locale.ROOT).replaceFirst("^ROLE_", "");
            return currentUserRoles().stream().anyMatch(r -> r.equalsIgnoreCase("ROLE_" + bare) || r.equalsIgnoreCase(bare));
        }
        return false;
    }

    /**
     * Lot KG5 : etapes dont le delai {@code autoApproveDays} est ecoule, approuvees par le systeme
     * (tache du cycle de vie des documents). Une transaction par instance.
     *
     * @return nombre d'etapes approuvees
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public int autoApproveDueSteps() {
        int count = 0;
        for (DocumentWorkflowInstance instance : instanceRepository.findAllByStatusInOrderByStartedAtAsc(List.of("PENDING", "IN_PROGRESS"))) {
            try {
                DocumentWorkflowStep step = stepRepository.findAllByWorkflowOrderByStepOrderAsc(instance.getWorkflow()).stream()
                        .filter(s -> s.getStepOrder().equals(instance.getCurrentStep())).findFirst().orElse(null);
                if (step == null || step.getAutoApproveDays() == null || step.getAutoApproveDays() <= 0) {
                    continue;
                }
                Instant since = actionRepository.findAllByInstanceOrderByActedAtAsc(instance).stream()
                        .map(DocumentWorkflowAction::getActedAt).reduce((a, b) -> b).orElse(instance.getStartedAt());
                if (since == null || since.plus(step.getAutoApproveDays(), java.time.temporal.ChronoUnit.DAYS).isAfter(Instant.now())) {
                    continue;
                }
                WorkflowActionRequest auto = new WorkflowActionRequest();
                auto.setComment("Étape approuvée automatiquement après " + step.getAutoApproveDays() + " jour(s)");
                self().processAction(instance.getDocument().getCode(), "APPROVE", auto, true);
                count++;
            } catch (RuntimeException ex) {
                org.slf4j.LoggerFactory.getLogger(DocumentWorkflowService.class)
                        .warn("Approbation automatique de l'etape impossible pour {} : {}", instance.getId(), ex.getMessage());
            }
        }
        return count;
    }

    private org.springframework.beans.factory.ObjectProvider<DocumentWorkflowService> selfProvider;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setSelf(org.springframework.beans.factory.ObjectProvider<DocumentWorkflowService> selfProvider) {
        this.selfProvider = selfProvider;
    }

    private DocumentWorkflowService self() {
        return selfProvider == null ? this : selfProvider.getObject();
    }

    WorkflowInstanceResponse processAction(String documentCode, String action, WorkflowActionRequest request, boolean systemActor) {
        Document document = documentRepository.findByCode(documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable"));
        DocumentWorkflowInstance instance = instanceRepository.findByDocumentAndStatusIn(
                        document, List.of("PENDING", "IN_PROGRESS"))
                .orElseThrow(() -> new BadRequestException("Aucun circuit d'approbation en cours pour ce document"));
        List<DocumentWorkflowStep> allSteps = stepRepository.findAllByWorkflowOrderByStepOrderAsc(instance.getWorkflow());
        DocumentWorkflowStep current = allSteps.stream().filter(s -> s.getStepOrder().equals(instance.getCurrentStep()))
                .findFirst().orElse(null);
        // Lot KG5 : seul l'approbateur de l'etape (compte designe ou role) decide ; avant, n'importe qui.
        if (!systemActor && current != null && !isApprover(current)) {
            throw new com.sni.bokaticowork.core.exception.customs.ForbiddenException(
                    "Cette étape (" + current.getStepName() + ") revient à " + current.getApproverValue());
        }
        if ("REJECT".equals(action) && (request == null || !StringUtils.hasText(request.getComment()))) {
            throw new BadRequestException("Le motif du refus est requis");
        }

        DocumentWorkflowAction wfAction = DocumentWorkflowAction.builder()
                .instance(instance)
                .stepOrder(instance.getCurrentStep())
                .action(action)
                .actorId(currentUserId())
                .comment(request != null ? request.getComment() : null)
                .build();
        actionRepository.save(wfAction);

        String comment = request != null ? request.getComment() : null;
        if ("REJECT".equals(action)) {
            instance.setStatus("REJECTED");
            instance.setCompletedAt(Instant.now());
            instanceRepository.save(instance);
            // Lot KG5 : un refus refuse le document.
            decideDocument(document, false, "Refusé par le circuit " + instance.getWorkflow().getName() + " : " + comment);
        } else {
            int maxStep = allSteps.stream().mapToInt(DocumentWorkflowStep::getStepOrder).max().orElse(1);
            if (instance.getCurrentStep() >= maxStep) {
                instance.setStatus("APPROVED");
                instance.setCompletedAt(Instant.now());
                instanceRepository.save(instance);
                // Lot KG5 : la derniere approbation approuve le document.
                decideDocument(document, true, "Approuvé par le circuit " + instance.getWorkflow().getName()
                        + (StringUtils.hasText(comment) ? " : " + comment : ""));
            } else {
                instance.setCurrentStep(instance.getCurrentStep() + 1);
                instanceRepository.save(instance);
            }
        }
        return toInstanceResponse(instance);
    }

    private DocumentWorkflow findWorkflow(String code) {
        return workflowRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Circuit d'approbation introuvable : " + code));
    }

    private WorkflowResponse toResponse(DocumentWorkflow wf) {
        List<DocumentWorkflowStep> steps = stepRepository.findAllByWorkflowOrderByStepOrderAsc(wf);
        return WorkflowResponse.builder()
                .code(wf.getCode()).name(wf.getName()).description(wf.getDescription())
                .workflowType(wf.getWorkflowType()).space(wf.getSpace()).documentTypeCode(wf.getDocumentTypeCode())
                .active(wf.getActive()).createdAt(wf.getCreatedAt())
                .steps(steps.stream().map(s -> WorkflowResponse.StepResponse.builder()
                        .id(s.getId()).stepOrder(s.getStepOrder()).stepName(s.getStepName())
                        .approverType(s.getApproverType()).approverValue(s.getApproverValue())
                        .required(s.getRequired()).autoApproveDays(s.getAutoApproveDays())
                        .build()).toList())
                .build();
    }

    private WorkflowInstanceResponse toInstanceResponse(DocumentWorkflowInstance instance) {
        List<DocumentWorkflowStep> steps = stepRepository.findAllByWorkflowOrderByStepOrderAsc(instance.getWorkflow());
        String currentStepName = steps.stream()
                .filter(s -> s.getStepOrder().equals(instance.getCurrentStep()))
                .map(DocumentWorkflowStep::getStepName).findFirst().orElse(null);
        List<DocumentWorkflowAction> actions = actionRepository.findAllByInstanceOrderByActedAtAsc(instance);

        return WorkflowInstanceResponse.builder()
                .id(instance.getId())
                .workflowCode(instance.getWorkflow().getCode())
                .workflowName(instance.getWorkflow().getName())
                .documentCode(instance.getDocument().getCode())
                .documentTitle(instance.getDocument().getTitle())
                .currentStep(instance.getCurrentStep())
                .currentStepName(currentStepName)
                .status(instance.getStatus())
                .startedAt(instance.getStartedAt())
                .completedAt(instance.getCompletedAt())
                .actions(actions.stream().map(a -> {
                    String stepName = steps.stream().filter(s -> s.getStepOrder().equals(a.getStepOrder()))
                            .map(DocumentWorkflowStep::getStepName).findFirst().orElse(null);
                    return WorkflowInstanceResponse.ActionResponse.builder()
                            .stepOrder(a.getStepOrder()).stepName(stepName).action(a.getAction())
                            .actorId(a.getActorId())
                            .actorEmail(a.getActorId() == null ? null : userRepository.findById(a.getActorId()).map(u -> u.getEmail()).orElse(null))
                            .actedAt(a.getActedAt()).comment(a.getComment()).build();
                }).toList())
                .build();
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) return p.getUser().getId();
        return null;
    }

    private String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) return p.getUser().getEmail();
        return "";
    }

    private List<String> currentUserRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return List.of();
        return auth.getAuthorities().stream().map(a -> a.getAuthority()).toList();
    }
}
