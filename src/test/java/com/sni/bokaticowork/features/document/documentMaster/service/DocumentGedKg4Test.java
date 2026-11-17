package com.sni.bokaticowork.features.document.documentMaster.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentReviewDecisionRequest;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentBatchService;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentLockService;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentServiceImpl;
import com.sni.bokaticowork.features.document.explorer.lifecycle.GedHoldService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/** Lot KG4 : remplacement garde, verrou sur les decisions, archive bornee. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentGedKg4Test {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentLockService lockService;
    @Mock private GedHoldService holdService;
    @InjectMocks private DocumentServiceImpl service;

    private Document document(DocumentStatus status) {
        Document document = Document.builder().code("DOC-1").status(status).build();
        when(documentRepository.findByCode("DOC-1")).thenReturn(Optional.of(document));
        return document;
    }

    @ParameterizedTest
    @EnumSource(value = DocumentStatus.class, names = {"ARCHIVED", "EXPIRED", "SUPERSEDED", "SIGNED"})
    void aClosedDocumentIsNotReplaced(DocumentStatus status) {
        document(status);
        assertThatThrownBy(() -> service.replace("DOC-1", new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1})))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("déposez un nouveau document");
    }

    @Test
    void aDocumentLockedByAnotherCannotBeDecided() {
        Document document = document(DocumentStatus.PENDING_REVIEW);
        doThrow(new ConflictException("document", "verrouillé")).when(lockService).assertEditable(document);

        assertThatThrownBy(() -> service.approve("DOC-1", new DocumentReviewDecisionRequest()))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.reject("DOC-1", new DocumentReviewDecisionRequest()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void aZipEntryIsReadWithinItsLimit() throws Exception {
        byte[] ten = new byte[10];
        assertThat(ReflectionTestUtils.<byte[]>invokeMethod(DocumentBatchService.class, "readBounded",
                new ByteArrayInputStream(ten), 10L, "a.pdf")).hasSize(10);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(DocumentBatchService.class, "readBounded",
                new ByteArrayInputStream(new byte[11]), 10L, "bombe.pdf"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("bombe.pdf");
    }

    @Test
    void theZipLimitIsTheRealRequestLimit() {
        DocumentBatchService batch = new DocumentBatchService(null, null, null, null, null);
        assertThat(batch.effectiveZipLimit()).isEqualTo(10L * 1024 * 1024);
    }
}
