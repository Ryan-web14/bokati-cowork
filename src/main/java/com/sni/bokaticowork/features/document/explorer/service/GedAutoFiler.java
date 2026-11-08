package com.sni.bokaticowork.features.document.explorer.service;

import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentFolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Range un document dans le plan de classement des sa creation.
 *
 * <p>Un document deja place (depot dans un dossier choisi, ou deplace a la main) n'est jamais
 * deplace. Un echec de classement n'empeche pas le depot : le document reste a la racine et la
 * reprise periodique le rangera.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GedAutoFiler {

    private final GedSystemFolders systemFolders;
    private final DocumentFolderRepository folderRepository;

    /** Place le document dans son dossier ; a appeler avant son enregistrement final. */
    public void file(Document document) {
        if (document.getFolder() != null) {
            return;
        }
        Long folderId = folderIdFor(document);
        if (folderId != null) {
            document.setFolder(folderRepository.getReferenceById(folderId));
        }
    }

    /** Le dossier du plan pour ce document, cree au besoin ; vide s'il n'en a pas, ou en cas d'echec. */
    public Long folderIdFor(Document document) {
        String typeCode = document.getDocumentType() == null ? null : document.getDocumentType().getCode();
        try {
            return attempt(document, typeCode);
        } catch (DataIntegrityViolationException race) {
            // Un depot simultane a cree le meme dossier : il existe maintenant, on le relit.
            try {
                return attempt(document, typeCode);
            } catch (RuntimeException failure) {
                log.warn("Classement GED impossible pour {} · {}", document.getCode(), failure.getMessage());
                return null;
            }
        } catch (RuntimeException failure) {
            log.warn("Classement GED impossible pour {} · {}", document.getCode(), failure.getMessage());
            return null;
        }
    }

    private Long attempt(Document document, String typeCode) {
        return systemFolders.folderFor(document.getOwnerType(), document.getOwnerId(), document.getCategory(),
                typeCode, document.getUploadedAt());
    }
}
