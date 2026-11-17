package com.sni.bokaticowork.features.document.documentMaster.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.CreateDocumentSignatureRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentReviewDecisionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.SignDocumentRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.WorkflowActionRequest;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSignatureStatus;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentShareLink;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentSignature;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentWorkflow;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentWorkflowInstance;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentWorkflowStep;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentFolderRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentShareLinkRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentSignatureRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentWorkflowActionRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentWorkflowInstanceRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentWorkflowRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentWorkflowStepRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentShareService;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentSignatureServiceImpl;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentWorkflowService;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot KG5 : circuits d'approbation reels, signature menee a son terme, partage interne. */
class DocumentKg5Test {

    private final DocumentWorkflowRepository workflowRepository = mock(DocumentWorkflowRepository.class);
    private final DocumentWorkflowStepRepository stepRepository = mock(DocumentWorkflowStepRepository.class);
    private final DocumentWorkflowInstanceRepository instanceRepository = mock(DocumentWorkflowInstanceRepository.class);
    private final DocumentWorkflowActionRepository actionRepository = mock(DocumentWorkflowActionRepository.class);
    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final DocumentService documentService = mock(DocumentService.class);

    private final DocumentWorkflowService workflows = new DocumentWorkflowService(workflowRepository, stepRepository,
            instanceRepository, actionRepository, documentRepository, userRepository, mock(SequenceGeneratorFacade.class));

    private final Document document = Document.builder().code("DOC-1").title("Bail").space(DocumentSpace.GENERIC)
            .status(DocumentStatus.PENDING_REVIEW).build();
    private final DocumentWorkflow workflow = DocumentWorkflow.builder().code("WF-1").name("Direction").build();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(long id, String email, String... roles) {
        Users user = Users.builder().id(id).email(email).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(user), null,
                java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList()));
    }

    private DocumentWorkflowInstance running(int currentStep, DocumentWorkflowStep... steps) throws Exception {
        Method setter = DocumentWorkflowService.class.getDeclaredMethod("setDocumentService", DocumentService.class);
        setter.setAccessible(true);
        setter.invoke(workflows, documentService);
        DocumentWorkflowInstance instance = DocumentWorkflowInstance.builder().workflow(workflow).document(document)
                .currentStep(currentStep).status("IN_PROGRESS").startedAt(Instant.now()).build();
        when(documentRepository.findByCode("DOC-1")).thenReturn(Optional.of(document));
        when(instanceRepository.findByDocumentAndStatusIn(eq(document), anyList())).thenReturn(Optional.of(instance));
        when(stepRepository.findAllByWorkflowOrderByStepOrderAsc(workflow)).thenReturn(List.of(steps));
        return instance;
    }

    private static DocumentWorkflowStep step(int order, String type, String value) {
        return DocumentWorkflowStep.builder().stepOrder(order).stepName("Etape " + order).approverType(type).approverValue(value).build();
    }

    private static WorkflowActionRequest comment(String text) {
        WorkflowActionRequest request = new WorkflowActionRequest();
        request.setComment(text);
        return request;
    }

    @Test
    void anInactiveWorkflowDoesNotStart() {
        workflow.setActive(false);
        when(documentRepository.findByCode("DOC-1")).thenReturn(Optional.of(document));
        when(workflowRepository.findByCode("WF-1")).thenReturn(Optional.of(workflow));

        assertThatThrownBy(() -> workflows.startWorkflow("DOC-1", "WF-1")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void aWorkflowOfAnotherSpaceDoesNotStart() {
        workflow.setSpace(DocumentSpace.KYC_SPACE);
        when(documentRepository.findByCode("DOC-1")).thenReturn(Optional.of(document));
        when(workflowRepository.findByCode("WF-1")).thenReturn(Optional.of(workflow));

        assertThatThrownBy(() -> workflows.startWorkflow("DOC-1", "WF-1")).hasMessageContaining("KYC_SPACE");
    }

    @Test
    void onlyTheStepApproverDecides() throws Exception {
        running(1, step(1, "USER", "chef@bokati.cg"));
        login(7, "autre@bokati.cg", "ROLE_ADMIN");

        assertThatThrownBy(() -> workflows.approve("DOC-1", comment("ok"))).isInstanceOf(ForbiddenException.class);
        verify(actionRepository, never()).save(any());
    }

    @Test
    void anIntermediateApprovalMovesToTheNextStepOnly() throws Exception {
        DocumentWorkflowInstance instance = running(1, step(1, "ROLE", "MANAGER"), step(2, "USER", "dg@bokati.cg"));
        login(7, "chef@bokati.cg", "ROLE_MANAGER");

        workflows.approve("DOC-1", comment("vu"));

        assertThat(instance.getCurrentStep()).isEqualTo(2);
        assertThat(instance.getStatus()).isEqualTo("IN_PROGRESS");
        verify(documentService, never()).approve(any(), any());
    }

    @Test
    void theLastApprovalApprovesTheDocument() throws Exception {
        DocumentWorkflowInstance instance = running(2, step(1, "ROLE", "MANAGER"), step(2, "USER", "dg@bokati.cg"));
        login(9, "dg@bokati.cg");

        workflows.approve("DOC-1", comment("bon pour accord"));

        assertThat(instance.getStatus()).isEqualTo("APPROVED");
        verify(documentService).approve(eq("DOC-1"), any(DocumentReviewDecisionRequest.class));
    }

    @Test
    void aRejectionNeedsAReasonAndRejectsTheDocument() throws Exception {
        DocumentWorkflowInstance instance = running(1, step(1, "USER", "9"));
        login(9, "dg@bokati.cg");

        assertThatThrownBy(() -> workflows.reject("DOC-1", comment(" "))).isInstanceOf(BadRequestException.class);

        workflows.reject("DOC-1", comment("clause manquante"));
        assertThat(instance.getStatus()).isEqualTo("REJECTED");
        verify(documentService).reject(eq("DOC-1"), any(DocumentReviewDecisionRequest.class));
    }

    @Test
    void theStatusReadsTheLatestInstance() {
        DocumentWorkflowInstance latest = DocumentWorkflowInstance.builder().workflow(workflow).document(document)
                .currentStep(1).status("REJECTED").startedAt(Instant.now()).build();
        when(documentRepository.findByCode("DOC-1")).thenReturn(Optional.of(document));
        when(instanceRepository.findFirstByDocumentOrderByStartedAtDesc(document)).thenReturn(Optional.of(latest));

        assertThat(workflows.getStatus("DOC-1").getStatus()).isEqualTo("REJECTED");
    }

    @Test
    void aSignatureIsNotRequestedOnARejectedDocument() {
        DocumentSignatureRepository signatures = mock(DocumentSignatureRepository.class);
        DocumentSignatureServiceImpl service = new DocumentSignatureServiceImpl(documentRepository, signatures, mock(OutboxService.class));
        document.setStatus(DocumentStatus.REJECTED);
        when(documentRepository.findByCode("DOC-1")).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.requestSignature("DOC-1",
                new CreateDocumentSignatureRequest("USER", 9L, "Directeur", "dg@bokati.cg")))
                .isInstanceOf(BadRequestException.class);
        verify(signatures, never()).save(any());
    }

    @Test
    void theLastSignatureSignsTheDocument() {
        DocumentSignatureRepository signatures = mock(DocumentSignatureRepository.class);
        DocumentSignatureServiceImpl service = new DocumentSignatureServiceImpl(documentRepository, signatures, mock(OutboxService.class));
        document.setStatus(DocumentStatus.APPROVED);
        DocumentSignature pending = DocumentSignature.builder().id(3L).document(document).signerType("USER").signerId(9L)
                .signerName("Directeur").signatureStatus(DocumentSignatureStatus.PENDING).build();
        when(signatures.findById(3L)).thenReturn(Optional.of(pending));
        when(signatures.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(signatures.findAllByDocumentAndSignatureStatus(document, DocumentSignatureStatus.PENDING)).thenReturn(List.of());
        login(9, "dg@bokati.cg");

        service.sign("DOC-1", 3L, new SignDocumentRequest(true, "trace"), "127.0.0.1", "test");

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.SIGNED);
        verify(documentRepository).save(document);
    }

    @Test
    void onlyTheAuthorOrAnAdminRevokesAShareLink() {
        DocumentShareLinkRepository links = mock(DocumentShareLinkRepository.class);
        DocumentShareService shares = new DocumentShareService(links, documentRepository, mock(DocumentFolderRepository.class),
                userRepository, mock(PasswordEncoder.class));
        DocumentShareLink link = DocumentShareLink.builder().token("tok").document(document).createdBy(5L)
                .expiresAt(Instant.now().plusSeconds(3600)).build();
        when(links.findByTokenAndActiveTrue("tok")).thenReturn(Optional.of(link));

        login(7, "autre@bokati.cg", "ROLE_STAFF");
        assertThatThrownBy(() -> shares.revoke("tok")).isInstanceOf(ForbiddenException.class);

        login(5, "auteur@bokati.cg", "ROLE_STAFF");
        shares.revoke("tok");
        assertThat(link.getActive()).isFalse();
    }
}
