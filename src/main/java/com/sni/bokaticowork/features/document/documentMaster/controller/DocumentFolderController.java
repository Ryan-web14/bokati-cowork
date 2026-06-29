package com.sni.bokaticowork.features.document.documentMaster.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.CreateFolderRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.MoveDocumentToFolderRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.MoveFolderRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.UpdateFolderRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.BreadcrumbItem;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFolderTreeNode;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.FolderDetailResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentFolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/document-folders")
public class DocumentFolderController {

    private final DocumentFolderService service;

    @PostMapping
    @Audited(module = "DOCUMENT", action = "CREATE_FOLDER", ressource = "document_folder")
    public ResponseEntity<FolderDetailResponse> create(@Valid @RequestBody CreateFolderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{code}")
    public ResponseEntity<FolderDetailResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.getByCode(code));
    }

    @PatchMapping("/{code}")
    @Audited(module = "DOCUMENT", action = "UPDATE_FOLDER", ressource = "document_folder")
    public ResponseEntity<FolderDetailResponse> update(
            @PathVariable String code,
            @Valid @RequestBody UpdateFolderRequest request) {
        return ResponseEntity.ok(service.update(code, request));
    }

    @DeleteMapping("/{code}")
    @Audited(module = "DOCUMENT", action = "DELETE_FOLDER", ressource = "document_folder")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{code}/move")
    @Audited(module = "DOCUMENT", action = "MOVE_FOLDER", ressource = "document_folder")
    public ResponseEntity<FolderDetailResponse> move(
            @PathVariable String code,
            @Valid @RequestBody MoveFolderRequest request) {
        return ResponseEntity.ok(service.move(code, request));
    }

    @GetMapping("/roots")
    public ResponseEntity<List<FolderDetailResponse>> roots(
            @RequestParam(required = false) DocumentSpace space) {
        return ResponseEntity.ok(service.getRoots(space));
    }

    @GetMapping("/{code}/children")
    public ResponseEntity<List<FolderDetailResponse>> children(@PathVariable String code) {
        return ResponseEntity.ok(service.getChildren(code));
    }

    @GetMapping("/{code}/tree")
    public ResponseEntity<DocumentFolderTreeNode> tree(@PathVariable String code) {
        return ResponseEntity.ok(service.getTree(code));
    }

    @GetMapping("/{code}/documents")
    public ResponseEntity<PaginatedResponse<DocumentResponse>> documents(
            @PathVariable String code,
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.getDocuments(code, pageable));
    }

    @GetMapping("/{code}/breadcrumb")
    public ResponseEntity<List<BreadcrumbItem>> breadcrumb(@PathVariable String code) {
        return ResponseEntity.ok(service.getBreadcrumb(code));
    }

    @PostMapping("/{documentCode}/move-document")
    @Audited(module = "DOCUMENT", action = "MOVE_DOCUMENT", ressource = "document")
    public ResponseEntity<DocumentResponse> moveDocument(
            @PathVariable String documentCode,
            @Valid @RequestBody MoveDocumentToFolderRequest request) {
        return ResponseEntity.ok(service.moveDocument(documentCode, request.getFolderCode()));
    }
}
