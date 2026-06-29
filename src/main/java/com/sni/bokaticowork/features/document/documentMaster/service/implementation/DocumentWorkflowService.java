package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

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
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        DocumentWorkflow workflow = findWorkflow(workflowCode);

        instanceRepository.findByDocumentAndStatusIn(document, List.of("PENDING", "IN_PROGRESS"))
                .ifPresent(i -> { throw new BadRequestException("Document already has an active workflow"); });

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
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        DocumentWorkflowInstance instance = instanceRepository.findByDocumentAndStatusIn(
                        document, List.of("PENDING", "IN_PROGRESS", "APPROVED", "REJECTED"))
                .orElseThrow(() -> new ResourceNotFoundException("No workflow found for this document"));
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
        Document document = documentRepository.findByCode(documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        DocumentWorkflowInstance instance = instanceRepository.findByDocumentAndStatusIn(
                        document, List.of("PENDING", "IN_PROGRESS"))
                .orElseThrow(() -> new BadRequestException("No active workflow for this document"));

        DocumentWorkflowAction wfAction = DocumentWorkflowAction.builder()
                .instance(instance)
                .stepOrder(instance.getCurrentStep())
                .action(action)
                .actorId(currentUserId())
                .comment(request != null ? request.getComment() : null)
                .build();
        actionRepository.save(wfAction);

        if ("REJECT".equals(action)) {
            instance.setStatus("REJECTED");
            instance.setCompletedAt(Instant.now());
        } else {
            List<DocumentWorkflowStep> steps = stepRepository.findAllByWorkflowOrderByStepOrderAsc(instance.getWorkflow());
            int maxStep = steps.stream().mapToInt(DocumentWorkflowStep::getStepOrder).max().orElse(1);
            if (instance.getCurrentStep() >= maxStep) {
                instance.setStatus("APPROVED");
                instance.setCompletedAt(Instant.now());
            } else {
                instance.setCurrentStep(instance.getCurrentStep() + 1);
            }
        }
        instanceRepository.save(instance);
        return toInstanceResponse(instance);
    }

    private DocumentWorkflow findWorkflow(String code) {
        return workflowRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + code));
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
                            .actorEmail(userRepository.findById(a.getActorId()).map(u -> u.getEmail()).orElse(null))
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
