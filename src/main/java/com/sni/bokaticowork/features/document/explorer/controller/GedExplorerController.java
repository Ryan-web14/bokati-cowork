package com.sni.bokaticowork.features.document.explorer.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFolderTreeNode;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedCopyRequest;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedCreateFolderRequest;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedItem;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedListing;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedMoveRequest;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedRenameRequest;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedSelectionRequest;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedTrashBatch;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedTrashResult;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedUploadResult;
import com.sni.bokaticowork.features.document.explorer.service.GedExplorerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * L'explorateur de fichiers de la GED, pour le back-office.
 *
 * <p>Droits : lecture {@code DOCUMENT:READ}, depot {@code DOCUMENT:UPLOAD}, tout le reste
 * {@code DOCUMENT:WRITE} (voir {@code AdminApiAuthorizationManager}).</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/ged")
@RequiredArgsConstructor
public class GedExplorerController {

    private final GedExplorerService service;

    /** Le contenu d'un dossier (ou de la racine sans {@code folderCode}) : dossiers puis fichiers. */
    @GetMapping("/items")
    public ResponseEntity<GedListing> items(
            @RequestParam(required = false) String folderCode,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(service.list(folderCode, search, sort, direction, page, size));
    }

    @GetMapping("/tree")
    public ResponseEntity<List<DocumentFolderTreeNode>> tree() {
        return ResponseEntity.ok(service.tree());
    }

    @PostMapping("/folders")
    public ResponseEntity<GedItem> createFolder(@Valid @RequestBody GedCreateFolderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createFolder(request));
    }

    @PatchMapping("/folders/{code}")
    public ResponseEntity<GedItem> renameFolder(@PathVariable String code, @Valid @RequestBody GedRenameRequest request) {
        return ResponseEntity.ok(service.renameFolder(code, request.name()));
    }

    /**
     * Depose un ou plusieurs fichiers. {@code relativePaths} (un par fichier, dans le meme ordre)
     * recree l'arborescence d'un dossier depose. La taille totale d'une requete est limitee
     * (10 Mo) : pour beaucoup de fichiers, envoyer une requete par fichier.
     */
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GedUploadResult> upload(
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(required = false) String folderCode,
            @RequestParam(required = false) List<String> relativePaths,
            @RequestParam(required = false) String documentTypeCode,
            @RequestParam(required = false) DocumentOwnerType ownerType,
            @RequestParam(required = false) String ownerCode) {
        GedUploadResult result = service.upload(files, folderCode, relativePaths, documentTypeCode, ownerType, ownerCode);
        HttpStatus status = result.uploaded().isEmpty() ? HttpStatus.BAD_REQUEST : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result);
    }

    @PatchMapping("/files/{code}")
    public ResponseEntity<GedItem> renameFile(@PathVariable String code, @Valid @RequestBody GedRenameRequest request) {
        return ResponseEntity.ok(service.renameFile(code, request.name()));
    }

    @PostMapping("/move")
    public ResponseEntity<Void> move(@RequestBody GedMoveRequest request) {
        service.move(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/copy")
    public ResponseEntity<GedUploadResult> copy(@RequestBody GedCopyRequest request) {
        GedUploadResult result = service.copy(request);
        HttpStatus status = result.uploaded().isEmpty() ? HttpStatus.BAD_REQUEST : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result);
    }

    @PostMapping("/trash")
    public ResponseEntity<GedTrashResult> trash(@RequestBody GedSelectionRequest request) {
        return ResponseEntity.ok(service.trash(request));
    }

    @GetMapping("/trash")
    public ResponseEntity<List<GedTrashBatch>> trashContent() {
        return ResponseEntity.ok(service.trashContent());
    }

    @PostMapping("/trash/{batch}/restore")
    public ResponseEntity<GedTrashResult> restore(@PathVariable String batch) {
        return ResponseEntity.ok(service.restore(batch));
    }
}
