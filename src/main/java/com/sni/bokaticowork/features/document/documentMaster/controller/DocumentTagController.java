package com.sni.bokaticowork.features.document.documentMaster.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentTagAssignRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentTagRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentTagUpdateRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentTagResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentMetadataService;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DocumentTagController {

    private final DocumentTagService tagService;
    private final DocumentMetadataService metadataService;

    // ── Tag catalog ──────────────────────────────────────────────────────────

    @PostMapping(ApiPath.V1 + "/document-tags")
    public ResponseEntity<DocumentTagResponse> createTag(@Valid @RequestBody DocumentTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.create(request));
    }

    @PutMapping(ApiPath.V1 + "/document-tags/{code}")
    public ResponseEntity<DocumentTagResponse> updateTag(
            @PathVariable String code,
            @Valid @RequestBody DocumentTagUpdateRequest request) {
        return ResponseEntity.ok(tagService.update(code, request));
    }

    @GetMapping(ApiPath.V1 + "/document-tags/{code}")
    public ResponseEntity<DocumentTagResponse> getTag(@PathVariable String code) {
        return ResponseEntity.ok(tagService.getByCode(code));
    }

    @GetMapping(ApiPath.V1 + "/document-tags")
    public ResponseEntity<List<DocumentTagResponse>> listTags(
            @RequestParam(required = false) DocumentSpace space) {
        return ResponseEntity.ok(tagService.list(space));
    }

    @DeleteMapping(ApiPath.V1 + "/document-tags/{code}")
    public ResponseEntity<Void> deleteTag(@PathVariable String code) {
        tagService.delete(code);
        return ResponseEntity.noContent().build();
    }

    // ── Tag assignments ───────────────────────────────────────────────────────

    @PostMapping(ApiPath.V1 + "/documents/{code}/tags")
    public ResponseEntity<List<DocumentTagResponse>> assignTags(
            @PathVariable String code,
            @Valid @RequestBody DocumentTagAssignRequest request) {
        return ResponseEntity.ok(tagService.assignTags(code, request));
    }

    @DeleteMapping(ApiPath.V1 + "/documents/{code}/tags/{tagCode}")
    public ResponseEntity<Void> removeTag(@PathVariable String code, @PathVariable String tagCode) {
        tagService.removeTag(code, tagCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(ApiPath.V1 + "/documents/{code}/tags")
    public ResponseEntity<List<DocumentTagResponse>> getDocumentTags(@PathVariable String code) {
        return ResponseEntity.ok(tagService.getDocumentTags(code));
    }

    // ── Metadata ──────────────────────────────────────────────────────────────

    @PatchMapping(ApiPath.V1 + "/documents/{code}/metadata")
    public ResponseEntity<Map<String, String>> setMetadata(
            @PathVariable String code,
            @RequestBody Map<String, String> metadata) {
        return ResponseEntity.ok(metadataService.setMetadata(code, metadata));
    }

    @GetMapping(ApiPath.V1 + "/documents/{code}/metadata")
    public ResponseEntity<Map<String, String>> getMetadata(@PathVariable String code) {
        return ResponseEntity.ok(metadataService.getMetadata(code));
    }

    @DeleteMapping(ApiPath.V1 + "/documents/{code}/metadata/{key}")
    public ResponseEntity<Void> deleteMetadataKey(@PathVariable String code, @PathVariable String key) {
        metadataService.deleteKey(code, key);
        return ResponseEntity.noContent().build();
    }
}
