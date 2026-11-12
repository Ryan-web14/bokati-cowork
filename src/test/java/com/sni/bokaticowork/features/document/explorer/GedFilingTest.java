package com.sni.bokaticowork.features.document.explorer;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentCategory;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentFolder;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentFolderRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentFolderService;
import com.sni.bokaticowork.features.document.explorer.repository.GedExplorerQueries;
import com.sni.bokaticowork.features.document.explorer.service.GedFilingPlan;
import com.sni.bokaticowork.features.document.explorer.service.GedFilingPlan.Level;
import com.sni.bokaticowork.features.document.explorer.service.GedSystemFolders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Le plan de classement : chaque document trouve sa place d'apres son proprietaire et sa nature,
 * et les dossiers du plan sont crees une seule fois puis proteges.
 */
class GedFilingTest {

    private static final Instant MARCH_2026 = Instant.parse("2026-03-15T10:00:00Z");

    private static List<String> names(List<Level> path) {
        return path.stream().map(Level::name).toList();
    }

    @Test
    @DisplayName("Une piece KYC, un contrat et un document de domiciliation vont dans le dossier de leur membre")
    void personDocumentsGoUnderTheirOwner() {
        assertThat(names(GedFilingPlan.pathFor(DocumentOwnerType.MEMBER, 12L, "Awa Ndoye · MBR-12", DocumentCategory.KYC, "MEMBER_ID_CARD", MARCH_2026)))
                .containsExactly("Membres", "Awa Ndoye · MBR-12", "KYC");
        assertThat(names(GedFilingPlan.pathFor(DocumentOwnerType.CUSTOMER, 3L, "SNI · CUS-3", DocumentCategory.LEGAL, "CONTRACT_DRAFT", MARCH_2026)))
                .containsExactly("Clients", "SNI · CUS-3", "Contrats");
        assertThat(names(GedFilingPlan.pathFor(DocumentOwnerType.BUSINESS, 5L, "Elle a ose · BUS-5", DocumentCategory.OTHER, "DOMICILIATION_CERTIFICATE", MARCH_2026)))
                .containsExactly("Entreprises", "Elle a ose · BUS-5", "Domiciliation");
        assertThat(GedFilingPlan.pathFor(DocumentOwnerType.MEMBER, 12L, "x", DocumentCategory.KYC, null, MARCH_2026).get(2).key())
                .isEqualTo("owner:MEMBER:12:KYC");
    }

    @Test
    @DisplayName("Un document rattache a un objet se range par annee, factures et paiements ensemble")
    void objectDocumentsGoByYear() {
        assertThat(names(GedFilingPlan.pathFor(DocumentOwnerType.CONTRACT, 9L, null, DocumentCategory.LEGAL, "X", MARCH_2026)))
                .containsExactly("Contrats", "2026");
        List<Level> payment = GedFilingPlan.pathFor(DocumentOwnerType.PAYMENT, 1L, null, DocumentCategory.FINANCIAL, "X", MARCH_2026);
        List<Level> invoice = GedFilingPlan.pathFor(DocumentOwnerType.INVOICE, 2L, null, DocumentCategory.FINANCIAL, "X", MARCH_2026);
        assertThat(payment.getFirst().key()).isEqualTo(invoice.getFirst().key());
        assertThat(names(payment)).containsExactly("Facturation", "2026");
    }

    @Test
    @DisplayName("Un document interne n'a pas de place imposee")
    void internalDocumentsStayWhereTheyAre() {
        assertThat(GedFilingPlan.pathFor(DocumentOwnerType.INTERNAL, null, null, DocumentCategory.OTHER, "GENERAL", MARCH_2026)).isEmpty();
        assertThat(GedFilingPlan.pathFor(DocumentOwnerType.MEMBER, null, null, DocumentCategory.KYC, null, MARCH_2026)).isEmpty();
    }

    @Test
    @DisplayName("Un dossier du plan ne se renomme, ne se deplace ni ne se supprime")
    void systemFoldersAreProtected() {
        DocumentFolder system = DocumentFolder.builder().name("Membres").systemKey("root:MEMBER").build();
        assertThatThrownBy(() -> DocumentFolderService.requireUserFolder(system))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("plan de classement");
        DocumentFolderService.requireUserFolder(DocumentFolder.builder().name("Mes scans").build());
    }

    @Test
    @DisplayName("Les dossiers du plan sont crees une fois, retrouves ensuite, et un dossier homonyme cree a la main est repris")
    void systemFoldersAreCreatedOnceAndAdopted() {
        DocumentFolderRepository folders = mock(DocumentFolderRepository.class);
        GedExplorerQueries queries = mock(GedExplorerQueries.class);
        SequenceGeneratorFacade sequence = mock(SequenceGeneratorFacade.class);
        MemberRepository members = mock(MemberRepository.class);
        AtomicLong ids = new AtomicLong();
        List<DocumentFolder> created = new ArrayList<>();
        when(sequence.next(eq("FOLDER"), any())).thenAnswer(call -> "FLD-" + ids.get());
        when(folders.save(any())).thenAnswer(call -> {
            DocumentFolder folder = call.getArgument(0);
            if (folder.getId() == null) {
                folder.setId(ids.incrementAndGet());
                created.add(folder);
            }
            return folder;
        });
        when(folders.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(folders.findBySystemKey(anyString())).thenAnswer(call -> created.stream()
                .filter(folder -> call.getArgument(0).equals(folder.getSystemKey())).findFirst());
        // « Membres » existe deja, cree a la main a la racine.
        DocumentFolder manual = DocumentFolder.builder().id(100L).name("Membres").path("/100/").depth(0).build();
        when(queries.childNamed(any(), anyString())).thenAnswer(call ->
                "Membres".equals(call.getArgument(1)) && call.getArgument(0) == null ? Optional.of(manual) : Optional.empty());
        Member member = mock(Member.class);
        when(member.getDisplayName()).thenReturn("Awa Ndoye");
        when(member.getMemberId()).thenReturn("MBR-12");
        when(members.findById(12L)).thenReturn(Optional.of(member));
        when(folders.findBySystemKey("root:MEMBER")).thenAnswer(call ->
                "root:MEMBER".equals(manual.getSystemKey()) ? Optional.of(manual) : Optional.empty());

        GedSystemFolders systemFolders = new GedSystemFolders(folders, queries, sequence, members,
                mock(CustomerRepository.class), mock(BusinessRepository.class));

        Long first = systemFolders.folderFor(DocumentOwnerType.MEMBER, 12L, DocumentCategory.KYC, "MEMBER_ID_CARD", MARCH_2026);
        Long second = systemFolders.folderFor(DocumentOwnerType.MEMBER, 12L, DocumentCategory.KYC, "MEMBER_ID_CARD", MARCH_2026);

        assertThat(manual.getSystemKey()).isEqualTo("root:MEMBER");
        assertThat(created).extracting(DocumentFolder::getName).containsExactly("Awa Ndoye · MBR-12", "KYC");
        assertThat(created.get(1).getPath()).isEqualTo("/100/" + created.get(0).getId() + "/" + created.get(1).getId() + "/");
        assertThat(first).isEqualTo(second).isEqualTo(created.get(1).getId());
    }
}
