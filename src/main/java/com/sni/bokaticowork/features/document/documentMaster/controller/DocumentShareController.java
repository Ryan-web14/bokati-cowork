package com.sni.bokaticowork.features.document.documentMaster.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.CreateShareLinkRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.ShareLinkResponse;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1)
public class DocumentShareController {

    private final DocumentShareService service;
    private final com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService documentService;
    private final com.sni.bokaticowork.features.document.explorer.access.GedAccessService gedAccess;

    @PostMapping("/documents/{documentCode}/share")
    @Audited(module = "DOCUMENT", action = "CREATE_SHARE_LINK", ressource = "document")
    public ResponseEntity<ShareLinkResponse> shareDocument(
            @PathVariable String documentCode,
            @Valid @RequestBody CreateShareLinkRequest request) {
        gedAccess.requireDocumentCode(documentCode, com.sni.bokaticowork.features.document.explorer.access.GedAccessLevel.DOWNLOAD);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.shareDocument(documentCode, request));
    }

    @PostMapping("/document-folders/{folderCode}/share")
    @Audited(module = "DOCUMENT", action = "CREATE_SHARE_LINK", ressource = "document_folder")
    public ResponseEntity<ShareLinkResponse> shareFolder(
            @PathVariable String folderCode,
            @Valid @RequestBody CreateShareLinkRequest request) {
        gedAccess.requireFolderCode(folderCode, com.sni.bokaticowork.features.document.explorer.access.GedAccessLevel.DOWNLOAD);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.shareFolder(folderCode, request));
    }

    /**
     * Lot KG5 : partage interne. Le lien exige la connexion (plus de route publique) et les droits
     * sur le document ou le dossier vise (dossiers restreints compris). Mot de passe eventuel dans
     * l'en-tete {@code X-Share-Password} (plus dans l'URL).
     */
    @GetMapping("/shares/{token}")
    public ResponseEntity<ShareLinkResponse> access(@PathVariable String token,
                                                     @RequestHeader(value = "X-Share-Password", required = false) String password) {
        requireTargetAccess(token, com.sni.bokaticowork.features.document.explorer.access.GedAccessLevel.READ);
        ShareLinkResponse response = service.access(token, password);
        service.recordAccess(token);
        return ResponseEntity.ok(response);
    }

    /** Lot KG5 : le fichier d'un lien de document, si le lien le permet (compte, expiration, telechargement). */
    @GetMapping("/shares/{token}/file")
    public ResponseEntity<byte[]> file(@PathVariable String token,
                                       @RequestHeader(value = "X-Share-Password", required = false) String password) {
        requireTargetAccess(token, com.sni.bokaticowork.features.document.explorer.access.GedAccessLevel.DOWNLOAD);
        String documentCode = service.fileDocumentCode(token, password);
        com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFileResult result = documentService.getFileWithMeta(documentCode);
        service.recordAccess(token);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType(result.mimeType()));
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename(result.fileName()).build());
        headers.setContentLength(result.content().length);
        return ResponseEntity.ok().headers(headers).body(result.content());
    }

    private void requireTargetAccess(String token, com.sni.bokaticowork.features.document.explorer.access.GedAccessLevel level) {
        var link = service.activeLink(token);
        if (link.getDocument() != null) {
            gedAccess.requireDocumentCode(link.getDocument().getCode(), level);
        } else if (link.getFolder() != null) {
            gedAccess.requireFolderCode(link.getFolder().getCode(), level);
        }
    }

    @DeleteMapping("/shares/{token}")
    @Audited(module = "DOCUMENT", action = "REVOKE_SHARE_LINK", ressource = "document")
    public ResponseEntity<Void> revoke(@PathVariable String token) {
        service.revoke(token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/documents/{documentCode}/shares")
    public ResponseEntity<List<ShareLinkResponse>> listByDocument(@PathVariable String documentCode) {
        gedAccess.requireDocumentCode(documentCode, com.sni.bokaticowork.features.document.explorer.access.GedAccessLevel.READ);
        return ResponseEntity.ok(service.listByDocument(documentCode));
    }
}
