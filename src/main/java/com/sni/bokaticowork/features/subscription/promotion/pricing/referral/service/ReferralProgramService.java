package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.repository.PromotionRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.LinkView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.MyReferralView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.OpenProgramView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.ProgramRequest;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.ProgramView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.PromotionRef;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.PublicLinkView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.ReferralView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralProgramStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralQualificationRule;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.Referral;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.ReferralLink;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.ReferralProgram;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository.ReferralLinkRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository.ReferralProgramRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository.ReferralRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Les programmes de parrainage, leurs liens et leurs parrainages, tels que le back-office,
 * l'espace client et la page publique d'un lien les voient.
 *
 * <p>L'enregistrement d'un parrainage, sa qualification et les recompenses restent dans
 * {@link ReferralService} ; ici, on cree et pilote les programmes, on distribue les liens et on
 * lit. Les vues sont construites dans la transaction : programme, promotions et lien sont des
 * associations paresseuses.</p>
 */
@Service
@RequiredArgsConstructor
public class ReferralProgramService {

    /** Le parrain designe depuis l'espace client : le membre connecte. */
    public static final String MEMBER = "MEMBER";
    private static final int MAX_PAGE_SIZE = 100;

    private final ReferralProgramRepository programRepository;
    private final ReferralLinkRepository linkRepository;
    private final ReferralRepository referralRepository;
    private final PromotionRepository promotionRepository;
    private final MemberRepository memberRepository;
    private final ReferralService referralService;

    /**
     * Le lien a partager. {@code {code}} y est remplace par le code du lien ; par defaut, la page
     * d'inscription du portail, le code deja saisi.
     */
    @Value("${app.referral.share-url-template:${app.frontend-url:http://localhost:3000}/inscription?parrain={code}}")
    private String shareUrlTemplate;

    // Programmes ---------------------------------------------------------------------------------

    @Transactional
    public ProgramView create(ProgramRequest request, String actor) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (programRepository.findByCode(code).isPresent()) {
            throw new ConflictException("referral-program", "un programme porte déjà le code " + code);
        }
        ReferralProgram program = ReferralProgram.builder()
                .code(code)
                .status(ReferralProgramStatus.DRAFT)
                .createdBy(actor)
                .build();
        apply(program, request, true);
        return view(programRepository.save(program));
    }

    /**
     * Modifie un programme. Nom, description, dates, plafond et devise changent a tout moment ;
     * regle de qualification et promotions seulement en brouillon, car les parrainages en cours ont
     * ete promis avec elles.
     */
    @Transactional
    public ProgramView update(String code, ProgramRequest request) {
        ReferralProgram program = program(code);
        if (program.getStatus() == ReferralProgramStatus.ENDED) {
            throw new BadRequestException("Ce programme est clos, il ne se modifie plus.");
        }
        apply(program, request, program.getStatus() == ReferralProgramStatus.DRAFT);
        return view(programRepository.save(program));
    }

    /** Ouvre le programme (depuis le brouillon ou une pause). Il faut au moins une recompense. */
    @Transactional
    public ProgramView activate(String code) {
        ReferralProgram program = program(code);
        if (program.getStatus() == ReferralProgramStatus.ENDED) {
            throw new BadRequestException("Un programme clos ne se rouvre pas : créez-en un nouveau.");
        }
        if (program.getReferrerPromotion() == null && program.getRefereePromotion() == null) {
            throw new BadRequestException("Un programme sans récompense ni pour le parrain ni pour le filleul ne s'ouvre pas.");
        }
        if (program.getValidUntil() != null && program.getValidUntil().isBefore(Instant.now())) {
            throw new BadRequestException("La date de fin du programme est passée.");
        }
        program.setStatus(ReferralProgramStatus.ACTIVE);
        return view(programRepository.save(program));
    }

    /** Suspend les nouveaux parrainages ; ceux deja enregistres se qualifient toujours. */
    @Transactional
    public ProgramView pause(String code) {
        ReferralProgram program = program(code);
        if (program.getStatus() != ReferralProgramStatus.ACTIVE) {
            throw new BadRequestException("Seul un programme ouvert se met en pause.");
        }
        program.setStatus(ReferralProgramStatus.PAUSED);
        return view(programRepository.save(program));
    }

    /** Clot definitivement le programme : plus de nouveaux parrainages. */
    @Transactional
    public ProgramView end(String code) {
        ReferralProgram program = program(code);
        program.setStatus(ReferralProgramStatus.ENDED);
        if (program.getValidUntil() == null || program.getValidUntil().isAfter(Instant.now())) {
            program.setValidUntil(Instant.now());
        }
        return view(programRepository.save(program));
    }

    @Transactional(readOnly = true)
    public List<ProgramView> programs() {
        return programRepository.findAllByOrderByCreatedAtDesc().stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public ProgramView get(String code) {
        return view(program(code));
    }

    // Liens --------------------------------------------------------------------------------------

    /** Le lien d'un parrain, cree au besoin : un seul par parrain et par programme. */
    @Transactional
    public LinkView link(String programCode, String referrerType, String referrerCode) {
        ReferralProgram program = program(programCode);
        if (program.getStatus() == ReferralProgramStatus.ENDED) {
            throw new BadRequestException("Ce programme est clos.");
        }
        String type = referrerType.trim().toUpperCase(Locale.ROOT);
        String owner = referrerCode.trim();
        if (MEMBER.equals(type) && memberRepository.findByMemberIdAndDeletedFalse(owner).isEmpty()) {
            throw new ResourceNotFoundException("Membre introuvable : " + owner);
        }
        return linkView(referralService.linkFor(program.getCode(), type, owner));
    }

    /** Le lien du membre connecte ; seulement sur un programme ouvert. */
    @Transactional
    public LinkView myLink(Member member, String programCode) {
        ReferralProgram program = program(programCode);
        if (!program.openAt(Instant.now())) {
            throw new BadRequestException("Ce programme de parrainage n'est pas ouvert.");
        }
        return linkView(referralService.linkFor(program.getCode(), MEMBER, member.getMemberId()));
    }

    @Transactional(readOnly = true)
    public List<LinkView> myLinks(Member member) {
        return linkRepository.findAllForReferrer(MEMBER, member.getMemberId()).stream().map(this::linkView).toList();
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<LinkView> links(String programCode, int page, int size) {
        ReferralProgram program = program(programCode);
        Page<LinkView> links = linkRepository.findByProgram_IdOrderByCreatedAtDesc(program.getId(), pageRequest(page, size))
                .map(this::linkView);
        return new PaginatedResponse<>(links);
    }

    /** Coupe un lien (abus) ; les parrainages deja enregistres suivent leur cours. */
    @Transactional
    public LinkView setLinkActive(String linkCode, boolean active) {
        ReferralLink link = linkRepository.findByCode(linkCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Lien de parrainage introuvable : " + linkCode));
        link.setActive(active);
        return linkView(linkRepository.save(link));
    }

    // Parrainages --------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PaginatedResponse<ReferralView> referrals(String programCode, ReferralStatus status, String referrerCode, int page, int size) {
        Long programId = StringUtils.hasText(programCode) ? program(programCode).getId() : null;
        String referrer = StringUtils.hasText(referrerCode) ? referrerCode.trim() : null;
        return new PaginatedResponse<>(referralRepository.search(programId, status, referrer, pageRequest(page, size))
                .map(this::referralView));
    }

    @Transactional(readOnly = true)
    public ReferralView referral(String referralNumber) {
        return referralView(referralRepository.findByNumber(referralNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Parrainage " + referralNumber + " introuvable")));
    }

    /** Les parrainages du membre connecte, vus par le parrain. */
    @Transactional(readOnly = true)
    public List<MyReferralView> myReferrals(Member member) {
        return referralRepository.findAllForReferrer(MEMBER, member.getMemberId()).stream()
                .map(referral -> new MyReferralView(referral.getReferralNumber(), referral.getProgram().getName(),
                        friend(referral), referral.getStatus(), referral.getRegisteredAt(), referral.getQualifiedAt(),
                        Boolean.TRUE.equals(referral.getReferrerRewardGranted()), referral.getReferrerRewardAmount(),
                        referral.getCurrency()))
                .toList();
    }

    /** Les programmes ouverts, avec le lien du membre s'il en a deja un. */
    @Transactional(readOnly = true)
    public List<OpenProgramView> openPrograms(Member member) {
        return programRepository.findOpen(Instant.now()).stream().map(program -> {
            ReferralLink mine = member == null ? null
                    : linkRepository.findForReferrer(program.getId(), MEMBER, member.getMemberId()).orElse(null);
            return new OpenProgramView(program.getCode(), program.getName(), program.getDescription(), program.getValidUntil(),
                    ref(program.getReferrerPromotion()), ref(program.getRefereePromotion()),
                    program.getQualificationRule(), program.getQualificationDelayDays(), program.getMaxReferralsPerReferrer(),
                    mine == null ? null : mine.getCode(), mine == null ? null : url(mine.getCode()));
        }).toList();
    }

    // Page publique d'un lien --------------------------------------------------------------------

    /** Ce que voit la personne qui suit un lien, avant de s'inscrire. */
    @Transactional(readOnly = true)
    public PublicLinkView publicLink(String code) {
        ReferralLink link = StringUtils.hasText(code) ? linkRepository.findByCode(code.trim()).orElse(null) : null;
        if (link == null) {
            return new PublicLinkView(code, false, "Ce code de parrainage n'existe pas.", null, null, null, null);
        }
        ReferralProgram program = link.getProgram();
        String reason = !Boolean.TRUE.equals(link.getActive()) ? "Ce lien de parrainage n'est plus actif."
                : !program.openAt(Instant.now()) ? "Ce programme de parrainage n'est pas ouvert."
                : null;
        return new PublicLinkView(link.getCode(), reason == null, reason, program.getName(), program.getDescription(),
                ref(program.getRefereePromotion()), referrerFirstName(link));
    }

    /**
     * Verifie un code presente a l'inscription, avant toute ecriture : un code faux ou ferme se
     * corrige, il ne doit pas laisser un compte a moitie cree.
     */
    @Transactional(readOnly = true)
    public void requireUsableCode(String code) {
        PublicLinkView link = publicLink(code);
        if (!link.valid()) {
            throw new BadRequestException("Code de parrainage refusé : " + link.reason());
        }
    }

    // Aides --------------------------------------------------------------------------------------

    private void apply(ReferralProgram program, ProgramRequest request, boolean rulesEditable) {
        program.setName(request.name().trim());
        program.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        if (request.validFrom() != null && request.validUntil() != null && request.validUntil().isBefore(request.validFrom())) {
            throw new BadRequestException("La fin du programme précède son début.");
        }
        program.setValidFrom(request.validFrom());
        program.setValidUntil(request.validUntil());
        if (request.maxReferralsPerReferrer() != null && request.maxReferralsPerReferrer() < 1) {
            throw new BadRequestException("Le plafond par parrain est d'au moins un parrainage.");
        }
        program.setMaxReferralsPerReferrer(request.maxReferralsPerReferrer());
        program.setCurrency(StringUtils.hasText(request.currency()) ? request.currency().trim().toUpperCase(Locale.ROOT) : "XAF");

        boolean rulesChanged = !sameCode(program.getReferrerPromotion(), request.referrerPromotionCode())
                || !sameCode(program.getRefereePromotion(), request.refereePromotionCode())
                || (request.qualificationRule() != null && request.qualificationRule() != program.getQualificationRule())
                || (request.qualificationDelayDays() != null && !request.qualificationDelayDays().equals(program.getQualificationDelayDays()));
        if (!rulesEditable) {
            if (rulesChanged) {
                throw new BadRequestException("Règle et récompenses ne changent plus une fois le programme ouvert : "
                        + "les parrainages en cours ont été promis avec elles.");
            }
            return;
        }
        program.setReferrerPromotion(promotion(request.referrerPromotionCode()));
        program.setRefereePromotion(promotion(request.refereePromotionCode()));
        if (program.getReferrerPromotion() == null && program.getRefereePromotion() == null) {
            // La base le refuse aussi (ck_referral_program_rewards) : un programme qui ne recompense personne n'en est pas un
            throw new BadRequestException("Indiquez la récompense du parrain, celle du filleul, ou les deux "
                    + "(referrerPromotionCode, refereePromotionCode).");
        }
        ReferralQualificationRule rule = request.qualificationRule() == null ? ReferralQualificationRule.FIRST_PAYMENT : request.qualificationRule();
        if (rule == ReferralQualificationRule.TENURE_REACHED
                && (request.qualificationDelayDays() == null || request.qualificationDelayDays() < 1)) {
            throw new BadRequestException("Une qualification à l'ancienneté demande un délai en jours.");
        }
        program.setQualificationRule(rule);
        program.setQualificationDelayDays(rule == ReferralQualificationRule.TENURE_REACHED ? request.qualificationDelayDays() : null);
    }

    private static boolean sameCode(Promotion promotion, String code) {
        String current = promotion == null ? null : promotion.getCode();
        String wanted = StringUtils.hasText(code) ? code.trim() : null;
        return current == null ? wanted == null : current.equalsIgnoreCase(wanted == null ? "" : wanted);
    }

    private Promotion promotion(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        return promotionRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Promotion introuvable : " + code));
    }

    private ReferralProgram program(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Le code du programme est requis.");
        }
        return programRepository.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Programme de parrainage introuvable : " + code));
    }

    private ProgramView view(ReferralProgram program) {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ReferralStatus status : ReferralStatus.values()) {
            byStatus.put(status.name(), 0L);
        }
        if (program.getId() != null) {
            for (Object[] row : referralRepository.countByStatus(program.getId())) {
                byStatus.put(((ReferralStatus) row[0]).name(), ((Number) row[1]).longValue());
            }
        }
        long links = program.getId() == null ? 0 : linkRepository.countByProgram_Id(program.getId());
        return new ProgramView(program.getCode(), program.getName(), program.getDescription(), program.getStatus(),
                program.openAt(Instant.now()), program.getValidFrom(), program.getValidUntil(),
                ref(program.getReferrerPromotion()), ref(program.getRefereePromotion()),
                program.getQualificationRule(), program.getQualificationDelayDays(), program.getMaxReferralsPerReferrer(),
                program.getCurrency(), links, byStatus, program.getCreatedAt(), program.getCreatedBy(), program.getUpdatedAt());
    }

    private LinkView linkView(ReferralLink link) {
        ReferralProgram program = link.getProgram();
        return new LinkView(link.getCode(), url(link.getCode()), program.getCode(), program.getName(),
                program.openAt(Instant.now()), link.getReferrerType(), link.getReferrerCode(),
                link.getClickCount() == null ? 0 : link.getClickCount(), link.getSignupCount() == null ? 0 : link.getSignupCount(),
                Boolean.TRUE.equals(link.getActive()), link.getCreatedAt());
    }

    private ReferralView referralView(Referral referral) {
        return new ReferralView(referral.getReferralNumber(), referral.getProgram().getCode(), referral.getProgram().getName(),
                referral.getLink() == null ? null : referral.getLink().getCode(),
                referral.getReferrerType(), referral.getReferrerCode(), referral.getRefereeType(), referral.getRefereeCode(),
                referral.getStatus(), referral.getRegisteredAt(), referral.getQualifiedAt(), referral.getRejectedAt(),
                referral.getRejectionReason(),
                Boolean.TRUE.equals(referral.getReferrerRewardGranted()), referral.getReferrerRewardAmount(), referral.getReferrerRewardedAt(),
                Boolean.TRUE.equals(referral.getRefereeRewardGranted()), referral.getRefereeRewardAmount(), referral.getRefereeRewardedAt(),
                referral.getCurrency());
    }

    private static PromotionRef ref(Promotion promotion) {
        return promotion == null ? null : new PromotionRef(promotion.getCode(), promotion.getName(), promotion.getDescription());
    }

    String url(String code) {
        return shareUrlTemplate.replace("{code}", code);
    }

    /** « Awa N. » : assez pour que le parrain reconnaisse son ami, pas plus. */
    private String friend(Referral referral) {
        if (!MEMBER.equalsIgnoreCase(referral.getRefereeType())) {
            return null;
        }
        return memberRepository.findByMemberIdAndDeletedFalse(referral.getRefereeCode())
                .map(member -> shortName(member.getFirstname(), member.getLastname()))
                .orElse(null);
    }

    private String referrerFirstName(ReferralLink link) {
        if (!MEMBER.equalsIgnoreCase(link.getReferrerType())) {
            return null;
        }
        return memberRepository.findByMemberIdAndDeletedFalse(link.getReferrerCode()).map(Member::getFirstname).orElse(null);
    }

    static String shortName(String firstname, String lastname) {
        String first = StringUtils.hasText(firstname) ? firstname.trim() : "";
        String initial = StringUtils.hasText(lastname) ? " " + lastname.trim().substring(0, 1).toUpperCase(Locale.ROOT) + "." : "";
        String name = (first + initial).trim();
        return name.isEmpty() ? null : name;
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, MAX_PAGE_SIZE)));
    }
}
