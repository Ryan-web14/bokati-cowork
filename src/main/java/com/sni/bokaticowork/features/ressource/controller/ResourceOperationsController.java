package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.service.support.ResourceOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lot R5d : modeles de taches, incidents et historique de maintenance (equipe). */
@RestController
@RequiredArgsConstructor
public class ResourceOperationsController {

    private static final String TEMPLATES = ApiPath.V1 + "/resource-task-templates";
    private static final String INCIDENTS = ApiPath.V1 + "/resource-incidents";

    private final ResourceOperations operations;
    private final com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository documents;

    public record ResolveRequest(String resolution, Boolean reopen) {
    }

    @GetMapping(TEMPLATES)
    public ResponseEntity<List<ResourceOperations.TemplateResponse>> templates() {
        return ResponseEntity.ok(operations.templates());
    }

    @PostMapping(TEMPLATES)
    @Audited(module = "RESOURCE", action = "CREATE_TASK_TEMPLATE", ressource = "resource_task_template")
    public ResponseEntity<ResourceOperations.TemplateResponse> createTemplate(@RequestBody ResourceOperations.TemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(operations.createTemplate(request));
    }

    @PatchMapping(TEMPLATES + "/{id}")
    @Audited(module = "RESOURCE", action = "UPDATE_TASK_TEMPLATE", ressource = "resource_task_template")
    public ResponseEntity<ResourceOperations.TemplateResponse> updateTemplate(@PathVariable Long id,
                                                                             @RequestBody ResourceOperations.TemplateRequest request) {
        return ResponseEntity.ok(operations.updateTemplate(id, request));
    }

    @DeleteMapping(TEMPLATES + "/{id}")
    @Audited(module = "RESOURCE", action = "DELETE_TASK_TEMPLATE", ressource = "resource_task_template")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        operations.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    /** Incidents ouverts et en cours par defaut ; par ressource, tous. */
    @GetMapping(INCIDENTS)
    public ResponseEntity<List<ResourceOperations.IncidentResponse>> incidents(@RequestParam(required = false) String resourceCode,
                                                                               @RequestParam(required = false) String status) {
        return ResponseEntity.ok(operations.list(resourceCode, status));
    }

    @GetMapping(INCIDENTS + "/{code}")
    public ResponseEntity<ResourceOperations.IncidentResponse> incident(@PathVariable String code) {
        return ResponseEntity.ok(operations.get(code));
    }

    /** Signalement de l'equipe ; {@code block} met la ressource en maintenance. */
    @PostMapping(INCIDENTS)
    @Audited(module = "RESOURCE", action = "REPORT_INCIDENT", ressource = "resource_incident")
    public ResponseEntity<ResourceOperations.IncidentResponse> report(@RequestBody ResourceOperations.IncidentRequest request,
                                                                      Authentication authentication) {
        String actor = authentication == null ? null : authentication.getName();
        // Lot B0 (ecart Bn-19) : les photos sont des documents existants.
        if (request != null && request.photoDocumentCodes() != null) {
            for (String code : request.photoDocumentCodes()) {
                if (code != null && !code.isBlank() && documents.findByCode(code.trim()).isEmpty()) {
                    throw new com.sni.bokaticowork.core.exception.customs.BadRequestException("Document introuvable : " + code.trim());
                }
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(operations.report(request, new ResourceOperations.Reporter("TEAM", actor, actor, null, null)));
    }

    @PostMapping(INCIDENTS + "/{code}/start")
    @Audited(module = "RESOURCE", action = "START_INCIDENT", ressource = "resource_incident")
    public ResponseEntity<ResourceOperations.IncidentResponse> start(@PathVariable String code) {
        return ResponseEntity.ok(operations.start(code));
    }

    @PostMapping(INCIDENTS + "/{code}/resolve")
    @Audited(module = "RESOURCE", action = "RESOLVE_INCIDENT", ressource = "resource_incident")
    public ResponseEntity<ResourceOperations.IncidentResponse> resolve(@PathVariable String code, @RequestBody ResolveRequest request,
                                                                       Authentication authentication) {
        return ResponseEntity.ok(operations.resolve(code, request.resolution(), Boolean.TRUE.equals(request.reopen()),
                authentication == null ? null : authentication.getName()));
    }

    @GetMapping(ApiPath.V1 + "/resources/{code}/maintenance-history")
    public ResponseEntity<List<ResourceOperations.HistoryEntry>> history(@PathVariable String code) {
        return ResponseEntity.ok(operations.history(code));
    }
}
