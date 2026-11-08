package com.sni.bokaticowork.features.document.explorer.service;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentCategory;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentFolder;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentFolderRepository;
import com.sni.bokaticowork.features.document.explorer.repository.GedExplorerQueries;
import com.sni.bokaticowork.features.document.explorer.service.GedFilingPlan.Level;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Les dossiers du plan de classement, crees a la demande.
 *
 * <p>Dans sa propre transaction : un depot ne doit jamais echouer parce que son classement a
 * echoue, et deux depots simultanes qui creent le meme dossier se departagent par l'index unique
 * de la cle systeme. Le perdant relit le dossier cree par l'autre.</p>
 */
@Component
@RequiredArgsConstructor
public class GedSystemFolders {

    private final DocumentFolderRepository folderRepository;
    private final GedExplorerQueries queries;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;

    /** L'identifiant du dossier ou ranger ce document, cree au besoin ; vide s'il n'a pas de place imposee. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long folderFor(DocumentOwnerType ownerType, Long ownerId, DocumentCategory category, String typeCode, Instant uploadedAt) {
        List<Level> path = GedFilingPlan.pathFor(ownerType, ownerId, ownerLabel(ownerType, ownerId), category, typeCode, uploadedAt);
        DocumentFolder parent = null;
        for (Level level : path) {
            parent = ensure(level, parent);
        }
        return parent == null ? null : parent.getId();
    }

    private DocumentFolder ensure(Level level, DocumentFolder parent) {
        DocumentFolder existing = folderRepository.findBySystemKey(level.key()).orElse(null);
        if (existing != null) {
            return existing;
        }
        // Un dossier du meme nom, cree a la main au meme endroit, devient celui du plan.
        DocumentFolder adopted = queries.childNamed(parent, level.name())
                .filter(folder -> folder.getSystemKey() == null)
                .orElse(null);
        if (adopted != null) {
            adopted.setSystemKey(level.key());
            return folderRepository.saveAndFlush(adopted);
        }
        DocumentFolder folder = DocumentFolder.builder()
                .code(sequenceGenerator.next("FOLDER", LocalDate.now()))
                .name(level.name())
                .parent(parent)
                .space(DocumentSpace.GENERIC)
                .depth(parent == null ? 0 : parent.getDepth() + 1)
                .sortOrder(0)
                .systemKey(level.key())
                .build();
        folderRepository.save(folder);
        folder.setPath((parent == null ? "/" : parent.getPath()) + folder.getId() + "/");
        return folderRepository.saveAndFlush(folder);
    }

    /** « Awa Ndoye · MBR-000012 » : le nom pour reconnaitre, le code pour ne pas confondre. */
    private String ownerLabel(DocumentOwnerType ownerType, Long ownerId) {
        if (ownerType == null || ownerId == null) {
            return null;
        }
        String label = switch (ownerType) {
            case MEMBER -> memberRepository.findById(ownerId)
                    .map(member -> join(member.getDisplayName(), member.getMemberId())).orElse(null);
            case CUSTOMER -> customerRepository.findById(ownerId)
                    .map(customer -> join(StringUtils.hasText(customer.getCompanyName()) ? customer.getCompanyName()
                            : ((customer.getFirstname() == null ? "" : customer.getFirstname()) + " "
                            + (customer.getLastname() == null ? "" : customer.getLastname())).trim(), customer.getCustomerId()))
                    .orElse(null);
            case BUSINESS -> businessRepository.findById(ownerId)
                    .map(business -> join(business.getName(), business.getCode())).orElse(null);
            default -> null;
        };
        return label != null ? label : ownerType.name() + " " + ownerId;
    }

    private static String join(String name, String code) {
        String cleanName = StringUtils.hasText(name) ? name.trim() : null;
        String cleanCode = StringUtils.hasText(code) ? code.trim() : null;
        if (cleanName == null) return cleanCode;
        if (cleanCode == null) return cleanName;
        String label = cleanName + " · " + cleanCode;
        return label.length() > 300 ? label.substring(0, 300) : label;
    }
}
