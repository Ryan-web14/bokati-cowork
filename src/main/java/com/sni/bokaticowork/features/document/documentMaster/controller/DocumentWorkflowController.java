package com.sni.bokaticowork.features.document.documentMaster.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.CreateWorkflowRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.WorkflowActionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.WorkflowInstanceResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.WorkflowResponse;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1)
public class DocumentWorkflowController {

    private final DocumentWorkflowService service;

    @PostMapping("/document-workflows")
    @Audited(module = "DOCUMENT", action = "CREATE_WORKFLOW", ressource = "document_workflow")
    public ResponseEntity<WorkflowResponse> create(@Valid @RequestBody CreateWorkflowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/document-workflows/{code}")
    public ResponseEntity<WorkflowResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.getByCode(code));
    }

    @GetMapping("/document-workflows")
    public ResponseEntity<List<WorkflowResponse>> list(@RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(service.list(activeOnly));
    }

    @DeleteMapping("/document-workflows/{code}")
    @Audited(module = "DOCUMENT", action = "DELETE_WORKFLOW", ressource = "document_workflow")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/documents/{documentCode}/start-workflow")
    @Audited(module = "DOCUMENT", action = "START_WORKFLOW", ressource = "document")
    public ResponseEntity<WorkflowInstanceResponse> startWorkflow(
            @PathVariable String documentCode,
            @RequestParam String workflowCode) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.startWorkflow(documentCode, workflowCode));
    }

    @PostMapping("/documents/{documentCode}/workflow/approve")
    @Audited(module = "DOCUMENT", action = "WORKFLOW_APPROVE", ressource = "document")
    public ResponseEntity<WorkflowInstanceResponse> approve(
            @PathVariable String documentCode,
            @RequestBody(required = false) WorkflowActionRequest request) {
        return ResponseEntity.ok(service.approve(documentCode, request));
    }

    @PostMapping("/documents/{documentCode}/workflow/reject")
    @Audited(module = "DOCUMENT", action = "WORKFLOW_REJECT", ressource = "document")
    public ResponseEntity<WorkflowInstanceResponse> reject(
            @PathVariable String documentCode,
            @RequestBody(required = false) WorkflowActionRequest request) {
        return ResponseEntity.ok(service.reject(documentCode, request));
    }

    @GetMapping("/documents/{documentCode}/workflow/status")
    public ResponseEntity<WorkflowInstanceResponse> status(@PathVariable String documentCode) {
        return ResponseEntity.ok(service.getStatus(documentCode));
    }

    @GetMapping("/document-workflows/my-pending")
    public ResponseEntity<List<WorkflowInstanceResponse>> myPending() {
        return ResponseEntity.ok(service.myPending());
    }
}
