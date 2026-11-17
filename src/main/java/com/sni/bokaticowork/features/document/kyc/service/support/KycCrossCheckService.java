package com.sni.bokaticowork.features.document.kyc.service.support;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.enums.KycCrossRuleType;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycCrossValidationRule;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.model.KycDocumentOcrResult;
import com.sni.bokaticowork.features.document.kyc.repository.KycCrossValidationRuleRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentOcrResultRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Evalue les regles de controle croise d'un dossier KYC.
 *
 * <p>Chaque regle active donne un resultat par piece visee : {@code PASSED}, {@code FAILED} ou
 * {@code SKIPPED} (donnee absente, OCR indisponible). Seules les regles bloquantes en echec
 * empechent l'approbation ; les autres sont des signaux pour le reviseur.</p>
 */
@Component
@RequiredArgsConstructor
public class KycCrossCheckService {

    public enum Outcome { PASSED, FAILED, SKIPPED }

    /** Resultat d'une regle sur une piece (ou sur un couple de pieces). */
    public record Result(String ruleCode, String ruleName, KycCrossRuleType ruleType, boolean blocking,
                         Outcome outcome, String documentType, String detail) {
    }

    private static final Set<KycDocumentVerificationStatus> LIVE = Set.of(
            KycDocumentVerificationStatus.PENDING, KycDocumentVerificationStatus.VERIFIED);

    private final KycCrossValidationRuleRepository ruleRepository;
    private final KycDocumentRepository pieceRepository;
    private final KycDocumentOcrResultRepository ocrRepository;
    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;

    public List<Result> evaluate(KycCase kycCase) {
        return evaluate(kycCase, LocalDate.now());
    }

    public List<Result> evaluate(KycCase kycCase, LocalDate today) {
        List<KycDocument> pieces = pieceRepository.findAllByKycCaseOrderByIdAsc(kycCase).stream()
                .filter(piece -> LIVE.contains(piece.getStatus()))
                .toList();
        List<Result> results = new ArrayList<>();
        for (KycCrossValidationRule rule : ruleRepository.findAllByActiveTrueOrderByIdAsc()) {
            if (rule.getRuleType() == KycCrossRuleType.FIELD_MATCH) {
                results.add(fieldMatch(rule, pieces));
                continue;
            }
            for (KycDocument piece : pieces) {
                if (!targets(rule.getDocumentTypeCode1(), piece.getDocumentType())) {
                    continue;
                }
                Result result = single(rule, piece, kycCase, today);
                if (result != null) {
                    results.add(result);
                }
            }
        }
        return results;
    }

    /** Les regles bloquantes en echec. */
    public static List<Result> blockingFailures(List<Result> results) {
        return results.stream().filter(r -> r.blocking() && r.outcome() == Outcome.FAILED).toList();
    }

    private Result single(KycCrossValidationRule rule, KycDocument piece, KycCase kycCase, LocalDate today) {
        LocalDate expiry = piece.getExpiryDate() != null ? piece.getExpiryDate()
                : piece.getDocument() == null ? null : piece.getDocument().getExpiryDate();
        LocalDate issue = piece.getIssueDate() != null ? piece.getIssueDate()
                : piece.getDocument() == null ? null : piece.getDocument().getIssueDate();
        return switch (rule.getRuleType()) {
            case DOCUMENT_NOT_EXPIRED -> expiry == null ? null
                    : result(rule, piece, expiry.isBefore(today) ? Outcome.FAILED : Outcome.PASSED,
                    expiry.isBefore(today) ? "Pièce échue le " + expiry : "Valable jusqu'au " + expiry);
            case ISSUE_BEFORE_EXPIRY -> {
                if (issue == null) yield null;
                if (issue.isAfter(today)) yield result(rule, piece, Outcome.FAILED, "Date d'émission dans le futur (" + issue + ")");
                if (expiry != null && !issue.isBefore(expiry)) yield result(rule, piece, Outcome.FAILED,
                        "Émission (" + issue + ") postérieure à l'expiration (" + expiry + ")");
                yield result(rule, piece, Outcome.PASSED, "Émise le " + issue);
            }
            case MIN_VALIDITY -> {
                if (expiry == null || expiry.isBefore(today)) yield null;
                int days = parseInt(rule.getParameter(), 30);
                long left = ChronoUnit.DAYS.between(today, expiry);
                yield result(rule, piece, left < days ? Outcome.FAILED : Outcome.PASSED,
                        left < days ? "Plus que " + left + " jour(s) de validité (minimum " + days + ")" : left + " jour(s) de validité");
            }
            case UNIQUE_DOCUMENT_NUMBER -> {
                if (!StringUtils.hasText(piece.getDocumentNumber())) yield null;
                List<KycDocument> same = pieceRepository.findAllByDocumentNumberIgnoreCase(piece.getDocumentNumber().trim()).stream()
                        .filter(other -> !Objects.equals(other.getId(), piece.getId()))
                        .filter(other -> other.getOwnerType() != piece.getOwnerType() || !Objects.equals(other.getOwnerId(), piece.getOwnerId()))
                        .filter(other -> Objects.equals(other.getDocumentType(), piece.getDocumentType()))
                        .toList();
                yield result(rule, piece, same.isEmpty() ? Outcome.PASSED : Outcome.FAILED,
                        same.isEmpty() ? "Numéro propre au titulaire"
                                : "Numéro déjà utilisé par " + same.size() + " autre(s) titulaire(s)");
            }
            case DECLARED_VS_OCR -> {
                Optional<KycDocumentOcrResult> ocr = ocrRepository.findByKycDocument(piece).filter(KycCrossCheckService::read);
                String declared = "expiry_date".equals(rule.getFieldToCompare())
                        ? (piece.getExpiryDate() == null ? null : piece.getExpiryDate().toString()) : piece.getDocumentNumber();
                String extracted = ocr.map(o -> ocrValue(o, rule.getFieldToCompare())).orElse(null);
                yield compare(rule, piece, declared, extracted, "saisi", "lu sur la pièce");
            }
            case PROFILE_MATCH -> {
                String profile = profileValue(kycCase, rule.getFieldToCompare());
                String pieceValue = switch (String.valueOf(rule.getFieldToCompare())) {
                    case "niu", "rccm" -> piece.getDocumentNumber();
                    default -> ocrRepository.findByKycDocument(piece).filter(KycCrossCheckService::read)
                            .map(o -> ocrValue(o, rule.getFieldToCompare())).orElse(null);
                };
                yield compare(rule, piece, pieceValue, profile, "de la pièce", "de la fiche");
            }
            default -> null;
        };
    }

    private Result fieldMatch(KycCrossValidationRule rule, List<KycDocument> pieces) {
        KycDocument first = pieces.stream().filter(p -> targets(rule.getDocumentTypeCode1(), p.getDocumentType())).findFirst().orElse(null);
        KycDocument second = pieces.stream().filter(p -> targets(rule.getDocumentTypeCode2(), p.getDocumentType())).findFirst().orElse(null);
        if (first == null || second == null) {
            return new Result(rule.getCode(), label(rule), rule.getRuleType(), Boolean.TRUE.equals(rule.getBlocking()),
                    Outcome.SKIPPED, rule.getDocumentTypeCode1(), "Une des deux pièces manque");
        }
        String a = ocrRepository.findByKycDocument(first).filter(KycCrossCheckService::read).map(o -> ocrValue(o, rule.getFieldToCompare())).orElse(null);
        String b = ocrRepository.findByKycDocument(second).filter(KycCrossCheckService::read).map(o -> ocrValue(o, rule.getFieldToCompare())).orElse(null);
        return compare(rule, first, a, b, "sur " + first.getDocumentType(), "sur " + second.getDocumentType());
    }

    private Result compare(KycCrossValidationRule rule, KycDocument piece, String left, String right, String leftLabel, String rightLabel) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return result(rule, piece, Outcome.SKIPPED, "Donnée absente (" + (StringUtils.hasText(left) ? rightLabel : leftLabel) + ")");
        }
        boolean same = normalize(left).equals(normalize(right));
        return result(rule, piece, same ? Outcome.PASSED : Outcome.FAILED,
                same ? "Concordant" : "Écart : « " + left.trim() + " » " + leftLabel + ", « " + right.trim() + " » " + rightLabel);
    }

    private String profileValue(KycCase kycCase, String field) {
        if (kycCase.getOwnerType() == DocumentOwnerType.MEMBER) {
            Member member = memberRepository.findById(kycCase.getOwnerId()).orElse(null);
            if (member == null) return null;
            return switch (String.valueOf(field)) {
                case "last_name" -> member.getLastname();
                case "first_name" -> member.getFirstname();
                default -> null;
            };
        }
        if (kycCase.getOwnerType() == DocumentOwnerType.CUSTOMER) {
            Customer customer = customerRepository.findById(kycCase.getOwnerId()).orElse(null);
            if (customer == null) return null;
            BusinessEntity business = customer.getBusinessEntity();
            return switch (String.valueOf(field)) {
                case "last_name" -> customer.getLastname();
                case "first_name" -> customer.getFirstname();
                case "niu" -> business == null ? null : business.getNiuNumber();
                case "rccm" -> business == null ? null : business.getRccmNumber();
                default -> null;
            };
        }
        if (kycCase.getOwnerType() == DocumentOwnerType.BUSINESS) {
            BusinessEntity business = businessRepository.findById(kycCase.getOwnerId()).orElse(null);
            if (business == null) return null;
            return switch (String.valueOf(field)) {
                case "niu" -> business.getNiuNumber();
                case "rccm" -> business.getRccmNumber();
                default -> null;
            };
        }
        return null;
    }

    /** Une lecture exploitable (OCR passe). */
    private static boolean read(KycDocumentOcrResult result) {
        return result.getRawOcrJson() != null && result.getRawOcrJson().replace(" ", "").contains("\"status\":\"SUCCESS\"");
    }

    private static String ocrValue(KycDocumentOcrResult result, String field) {
        return switch (String.valueOf(field)) {
            case "first_name" -> result.getExtractedFirstName();
            case "last_name" -> result.getExtractedLastName();
            case "date_of_birth" -> result.getExtractedDateOfBirth() == null ? null : result.getExtractedDateOfBirth().toString();
            case "expiry_date" -> result.getExtractedExpiryDate() == null ? null : result.getExtractedExpiryDate().toString();
            case "document_number" -> result.getExtractedDocumentNumber();
            case "nationality" -> result.getExtractedNationality();
            default -> null;
        };
    }

    private Result result(KycCrossValidationRule rule, KycDocument piece, Outcome outcome, String detail) {
        return new Result(rule.getCode(), label(rule), rule.getRuleType(), Boolean.TRUE.equals(rule.getBlocking()),
                outcome, piece.getDocumentType(), detail);
    }

    private static String label(KycCrossValidationRule rule) {
        return StringUtils.hasText(rule.getName()) ? rule.getName()
                : rule.getRuleType() + (rule.getFieldToCompare() == null ? "" : " " + rule.getFieldToCompare());
    }

    /** Vide = toutes les pieces ; sinon liste de codes separes par des virgules. */
    static boolean targets(String codes, String documentType) {
        if (!StringUtils.hasText(codes)) {
            return true;
        }
        if (documentType == null) {
            return false;
        }
        return Arrays.stream(codes.split(",")).map(String::trim).collect(Collectors.toSet())
                .contains(documentType.trim().toUpperCase(Locale.ROOT));
    }

    /** Majuscules, sans accents ni separateurs : « N° 12-345 » = « 12345 ». */
    static String normalize(String value) {
        String noAccents = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
