package com.sni.bokaticowork.features.document.kyc.mapper.decorator;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentTypeRepository;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseNoteResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDocumentResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDocumentOcrResultResponse;
import com.sni.bokaticowork.features.document.kyc.mapper.interfaces.KycMapper;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycCaseNote;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.model.KycDocumentOcrResult;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public abstract class KycMapperDecorator implements KycMapper {
    @Autowired
    @Qualifier("delegate")
    private KycMapper delegate;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private DocumentTypeRepository documentTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.api-base-url:}")
    private String apiBaseUrl;

    @Override
    public KycCaseResponse toResponse(KycCase kycCase) {
        KycCaseResponse response = delegate.toResponse(kycCase);
        if (kycCase == null || response == null) {
            return response;
        }
        enrichOwnerInfo(kycCase, response);
        response.setReviewedByEmail(resolveUserEmail(kycCase.getReviewedBy()));
        response.setAssignedToEmail(resolveUserEmail(kycCase.getAssignedTo()));
        return response;
    }

    @Override
    public KycDocumentResponse toDocumentResponse(KycDocument document) {
        KycDocumentResponse response = delegate.toDocumentResponse(document);
        if (document == null || response == null || document.getDocument() == null) {
            return response;
        }
        String documentCode = document.getDocument().getCode();
        response.setDocumentCode(documentCode);
        response.setFileName(document.getDocument().getFileName());
        response.setFileSize(document.getDocument().getFileSize());
        response.setMimeType(document.getDocument().getMimeType());
        if (StringUtils.hasText(documentCode) && StringUtils.hasText(document.getDocument().getFileUrl())) {
            response.setPreviewUrl(documentPreviewUrl(documentCode));
            response.setDownloadUrl(documentDownloadUrl(documentCode));
        }

        response.setOwnerName(resolveOwnerName(document.getOwnerType(), document.getOwnerId()));

        DocumentType docType = StringUtils.hasText(document.getDocumentType())
                ? documentTypeRepository.findByCode(document.getDocumentType()).orElse(null)
                : null;
        response.setRequiresBackSide(docType != null ? docType.getRequiresBackSide() : Boolean.FALSE);

        Document backDoc = document.getBackDocument();
        if (backDoc != null) {
            String backCode = backDoc.getCode();
            response.setBackDocumentCode(backCode);
            response.setBackFileName(backDoc.getFileName());
            response.setBackFileSize(backDoc.getFileSize());
            response.setBackMimeType(backDoc.getMimeType());
            if (StringUtils.hasText(backCode) && StringUtils.hasText(backDoc.getFileUrl())) {
                response.setBackPreviewUrl(documentPreviewUrl(backCode));
                response.setBackDownloadUrl(documentDownloadUrl(backCode));
            }
        }

        return response;
    }

    @Override
    public KycCaseNoteResponse toNoteResponse(KycCaseNote note) {
        KycCaseNoteResponse response = delegate.toNoteResponse(note);
        if (note != null && note.getKycCase() != null) {
            response.setKycCaseCode(note.getKycCase().getCode());
        }
        if (note != null) {
            response.setAuthorEmail(resolveUserEmail(note.getAuthorId()));
        }
        return response;
    }

    @Override
    public KycDocumentOcrResultResponse toOcrResultResponse(KycDocumentOcrResult result) {
        KycDocumentOcrResultResponse response = delegate.toOcrResultResponse(result);
        if (result != null && result.getKycDocument() != null && result.getKycDocument().getDocument() != null) {
            response.setDocumentCode(result.getKycDocument().getDocument().getCode());
        }
        return response;
    }

    private void enrichOwnerInfo(KycCase kycCase, KycCaseResponse response) {
        try {
            switch (kycCase.getOwnerType()) {
                case MEMBER -> memberRepository.findById(kycCase.getOwnerId()).ifPresent(member -> {
                    response.setOwnerName(member.getDisplayName());
                    response.setOwnerCode(member.getMemberId());
                });
                case CUSTOMER -> customerRepository.findById(kycCase.getOwnerId()).ifPresent(customer -> {
                    response.setOwnerName(customerName(customer));
                    response.setOwnerCode(customer.getCustomerId());
                });
                case BUSINESS -> businessRepository.findById(kycCase.getOwnerId()).ifPresent(business -> {
                    response.setOwnerName(business.getName());
                    response.setOwnerCode(business.getCode());
                });
                default -> {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String customerName(Customer customer) {
        if (StringUtils.hasText(customer.getCompanyName())) {
            return customer.getCompanyName();
        }
        String firstName = customer.getFirstname() == null ? "" : customer.getFirstname();
        String lastName = customer.getLastname() == null ? "" : customer.getLastname();
        return (firstName + " " + lastName).trim();
    }

    private String resolveUserEmail(Long userId) {
        if (userId == null) return null;
        try {
            return userRepository.findById(userId).map(u -> u.getEmail()).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveOwnerName(DocumentOwnerType ownerType, Long ownerId) {
        if (ownerType == null || ownerId == null) return null;
        try {
            return switch (ownerType) {
                case MEMBER -> memberRepository.findById(ownerId).map(m -> m.getDisplayName()).orElse(null);
                case CUSTOMER -> customerRepository.findById(ownerId).map(this::customerName).orElse(null);
                case BUSINESS -> businessRepository.findById(ownerId).map(b -> b.getName()).orElse(null);
                default -> null;
            };
        } catch (Exception ignored) {
            return null;
        }
    }

    private String documentPreviewUrl(String documentCode) {
        return apiUrl(ApiPath.V1 + "/documents/" + documentCode + "/preview");
    }

    private String documentDownloadUrl(String documentCode) {
        return apiUrl(ApiPath.V1 + "/documents/" + documentCode + "/download");
    }

    private String apiUrl(String path) {
        if (!StringUtils.hasText(apiBaseUrl)) {
            return path;
        }
        return apiBaseUrl.replaceAll("/+$", "") + path;
    }
}
