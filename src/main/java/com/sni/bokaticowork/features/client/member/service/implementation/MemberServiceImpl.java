package com.sni.bokaticowork.features.client.member.service.implementation;

import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.utils.format.Normalization;
import com.sni.bokaticowork.features.client.customer.dto.request.CustomerRequest;
import com.sni.bokaticowork.features.client.customer.enums.CustomerStatus;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.customer.service.interfaces.CustomerService;
import com.sni.bokaticowork.features.client.member.dto.request.CreateMemberRequest;
import com.sni.bokaticowork.features.client.member.dto.request.UpdateMemberRequest;
import com.sni.bokaticowork.features.client.member.dto.request.UpdateStatusRequest;
import com.sni.bokaticowork.features.client.member.dto.response.MemberResponse;
import com.sni.bokaticowork.features.client.member.dto.response.MemberSummaryResponse;
import com.sni.bokaticowork.features.client.member.enums.MemberStatus;
import com.sni.bokaticowork.features.client.member.mapper.interfaces.MemberMapper;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.model.MemberProfile;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberProfileRepository;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import com.sni.bokaticowork.features.client.member.repository.specification.MemberSpecification;
import com.sni.bokaticowork.features.client.member.repository.specification.criteria.MemberSearchCriteria;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.password.GeneratorOfPassword;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.validation.ValidationUtils;
import com.sni.bokaticowork.security.admin.user.dto.request.UserRequest;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import com.sni.bokaticowork.security.service.passwordResetService.interfaces.PasswordResetService;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@Transactional
@Slf4j
@Service
public class MemberServiceImpl  implements MemberService {

    private static final int GENERATED_PASSWORD_LENGTH = 8;

    private final MemberRepository memberRepo;
    private final MemberMapper memberMapper;
    private final CustomerService  customerService;
    private final CustomerRepository customerRepo;
    private final UserService userService;
    private final UserRepository userRepo;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final KycAutomationService kycAutomationService;
    private final MemberProfileRepository memberProfileRepository;
    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine emailTemplateEngine;
    private final PasswordResetService passwordResetService;
    private final com.sni.bokaticowork.core.utils.phone.PhoneNumberService phoneNumberService;
    private final com.sni.bokaticowork.security.admin.role.service.interfaces.RoleUserService roleUserService;
    private final com.sni.bokaticowork.features.payment.compliance.service.ComplianceSignals complianceSignals;

    @Value("${app.verify-base-url:}")
    private String publicBaseUrl;

    @Value("${app.portal.kyc-grace-period-days:7}")
    private int defaultKycGracePeriodDays;

    @Transactional
    @Override
    public MemberResponse create(CreateMemberRequest request,Boolean createByadmin) {
        try {
            normalizeCreateRequest(request);
            validateCreateRequest(request, createByadmin);
            ensureNoConflictingRecords(request);

            Customer customer = resolveCustomerForMemberCreation(request);

            UserRequest userRequest = UserRequest.builder()
                    .email(request.getEmail())
                    .firstname(request.getFirstname())
                    .lastname(request.getLastname())
                    .build();

            Member member = memberMapper.toEntity(request);
            String generatedPassword = null;
            boolean createdByAdmin = Boolean.TRUE.equals(createByadmin);

            if(createdByAdmin){
                member.setCreateByAdmin(true);
                if (request.isGeneratePassword()) {
                    generatedPassword = GeneratorOfPassword.generatePassword(GENERATED_PASSWORD_LENGTH);
                    userRequest.setGeneratePassword(false);
                    userRequest.setPassword(generatedPassword);
                } else {
                    userRequest.setPassword(request.getPassword());
                    userRequest.setGeneratePassword(false);
                }
            } else {
                member.setCreateByAdmin(false);
                userRequest.setGeneratePassword(false);
                userRequest.setPassword(request.getPassword());
            }

            Users user = userService.createUser(userRequest);

            member.setCustomer(customer);
            if (customer != null) {
                customer.setMember(true);
                customerRepo.save(customer);
            }
            member.setUser(user);
            long memberSeq = CodeComposer.extractSeq(sequenceGenerator.next("MEMBER", LocalDate.now()));
            member.setMemberId(CodeComposer.simpleWithMonth("MBR", LocalDate.now(), memberSeq));
            member.setStatus(MemberStatus.PENDING);
            member.setPortalAccess(true);
            member.setDeleted(false);
            if (!StringUtils.hasText(member.getWhatsappPhone())) {
                member.setWhatsappPhone(member.getPhone());
            }
            memberRepo.save(member);
            createProfileIfMissing(member, request, customer);
            kycAutomationService.initializeMemberKyc(member.getMemberId());
            // Controle des listes a l'entree en relation · apres commit, jamais en travers.
            complianceSignals.memberRegistered(member);
            sendMemberCreatedEmailAfterCommit(member, createdByAdmin);
            if (createdByAdmin) {
                sendPasswordSetupLinkAfterCommit(user);
            }

            return memberMapper.toResponse(member);
        } catch (ResourceAlreadyExistException e){
            log.error("Member creation conflict for email {}", request.getEmail(), e);
            throw new BadRequestException(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Invalid member creation request for email {}", request.getEmail(), e);
            throw new BadRequestException(e.getMessage());
        } catch (BadRequestException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Database constraint violation while creating member for email {}", request.getEmail(), e);
            throw new BadRequestException(resolveIntegrityViolationMessage(request, e), e);
        } catch (Exception e){
            log.error("Error creating member", e);
            throw new RuntimeException("Error creating member", e);
        }

    }

    @Override
    public void update(String memberId, UpdateMemberRequest request) {

        Member member = memberRepo.findByMemberIdAndDeletedFalse(memberId)
                .orElseThrow(()-> new ResourceNotFoundException("Member not found"));

        normalizeUpdateRequest(request);
        validateUpdateRequest(member, request);
        String previousPhone = member.getPhone();
        member = memberMapper.updateEntity(member, request);
        syncUserEmail(member, request);
        syncUserName(member, request);
        memberRepo.save(member);
        if (StringUtils.hasText(request.getPhone()) && previousPhone != null && !previousPhone.equals(member.getPhone())) {
            // Un numero qui change est un moyen de connexion qui change · la conformite le sait.
            complianceSignals.memberPhoneChanged(member, previousPhone, member.getPhone(), "MEMBER_UPDATE");
        }
    }

    @Override
    public MemberResponse getByMemberId(String memberId) {

        return memberMapper.toResponse( memberRepo.findVisibleByMemberId(memberId)
                .orElseThrow(()-> new ResourceNotFoundException("Member not found")));
    }

    @Override
    public MemberResponse getByEmail(String email) {

        if(!ValidationUtils.validateEmail(email)){
            throw new BadRequestException("Invalid email");
        }

        return memberMapper.toResponse( memberRepo.findVisibleByEmail(email)
                .orElseThrow(()-> new ResourceNotFoundException("Member not found")));
    }

    @Override
    public Member getByEmailForService(String email) {

        return memberRepo.findByEmailIgnoreCaseAndDeletedFalse(email)
                .orElseThrow(()-> new ResourceNotFoundException("Member not found"));
    }

    @Override
    public Member getByMemberIdForService(String memberId) {

        return memberRepo.findByMemberIdAndDeletedFalse(memberId)
                .orElseThrow(()-> new ResourceNotFoundException("Member not found"));
    }

    @Override
    public Member getByMemberIdForService(Long id){

        return memberRepo.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Member not found"));
    }

    @Override
    public Member getByUserForService(Long userId) {

        return memberRepo.findVisibleByUserId(userId)
                .orElseThrow(()-> new ResourceNotFoundException("Member not found"));
    }

    @Override
    public void ChangeStatus(String memberId, UpdateStatusRequest status) {

        Member member = memberRepo.findByMemberIdAndDeletedFalse(memberId)
                .orElseThrow(()-> new ResourceNotFoundException("Member not found"));

        member.setStatus(MemberStatus.valueOf(status.status()));
        
        if(member.getStatus() == MemberStatus.ACTIVE){
            member.setPortalAccess(true);
            if (member.getPortalActivatedAt() == null) {
                java.time.Instant now = java.time.Instant.now();
                member.setPortalActivatedAt(now);
                member.setKycGracePeriodEndAt(now.plus(defaultKycGracePeriodDays, java.time.temporal.ChronoUnit.DAYS));
            }
            Customer cus = member.getCustomer();
            cus.setStatus(CustomerStatus.ACTIVE);
            openPortalAccount(member, "ADMIN_ACTIVATION");
        } else if (member.getStatus() == MemberStatus.INACTIVE || member.getStatus() == MemberStatus.SUSPENDED) {
            member.setPortalAccess(false);
            Customer cus = member.getCustomer();
            cus.setStatus(CustomerStatus.INACTIVE);
        }

        memberRepo.save(member);
        kycAutomationService.syncMemberKyc(member.getMemberId());
    }

    private static final int MAX_LIST_SIZE = 1000;

    @Override
    public List<MemberResponse> getAllMembers() {
        return memberRepo.findAllVisible(PageRequest.of(0, MAX_LIST_SIZE)).stream()
                .map(memberMapper::toResponse)
                .toList();
    }

    @Override
    public List<MemberSummaryResponse> getAllMembersSummary() {
        return memberRepo.findAllVisible(PageRequest.of(0, MAX_LIST_SIZE)).stream()
                .map(memberMapper::toSummary)
                .toList();
    }

    @Override
    public PaginatedResponse<MemberResponse> list(Pageable pageable) {

        Page<MemberResponse> pages = memberRepo.findAllVisible(unsorted(pageable)).map(memberMapper::toResponse);

        return new PaginatedResponse<>(pages);
    }

    @Override
    public PaginatedResponse<MemberSummaryResponse> listSummary(Pageable pageable) {
        Page<MemberSummaryResponse> pages = memberRepo.findAllVisible(unsorted(pageable)).map(memberMapper::toSummary);

        return new PaginatedResponse<MemberSummaryResponse>(pages);
    }

    @Override
    public PaginatedResponse<MemberSummaryResponse> listSummaryByCustomer(String customerId, Pageable pageable) {

        Customer customer = customerService.getCustomerForService(customerId);
        Page<MemberSummaryResponse> pages = memberRepo.findAllVisibleByCustomerId(customer.getCustomerId(), unsorted(pageable))
                .map(memberMapper::toSummary);

        return new PaginatedResponse<MemberSummaryResponse>(pages);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberSummaryResponse> basicSearch(String query) {
        if (!StringUtils.hasText(query)) {
            throw new BadRequestException("Search query is required");
        }

        return memberRepo.basicSearch(query.trim()).stream()
                .map(memberMapper::toSummary)
                .toList();
    }

    @Override
    public PaginatedResponse<MemberSummaryResponse> search(MemberSearchCriteria criteria, Pageable pageable) {

        List<String> fuzzyMatchedMemberIds = resolveFuzzyMatchedMemberIds(criteria);
        if (fuzzyMatchedMemberIds != null && fuzzyMatchedMemberIds.isEmpty()) {
            return new PaginatedResponse<>(new PageImpl<>(Collections.<MemberSummaryResponse>emptyList(), pageable, 0));
        }

        Specification<Member> spec = MemberSpecification.search(criteria, fuzzyMatchedMemberIds);

        Page<MemberSummaryResponse> pages = memberRepo.findAll(spec, pageable).map(memberMapper::toSummary);

        return new PaginatedResponse<MemberSummaryResponse>(pages);
    }

    @Override
    public void delete(String memberId) {
        // La recherche inclut les archives : chercher avec DeletedFalse faisait repondre
        // "Member not found" sur un membre deja archive, message trompeur puisque le membre
        // existe. L'archivage est donc idempotent, et un second appel ne renvoie plus d'erreur.
        Member member = memberRepo.findByMemberIdIncludingArchived(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (Boolean.TRUE.equals(member.getDeleted())) {
            return;
        }
        member.setStatus(MemberStatus.ARCHIVED);
        member.setDeleted(true);
        memberRepo.save(member);
        if (member.getCustomer() != null) {
            boolean hasOtherMembers = memberRepo.existsByCustomerAndDeletedFalseAndIdNot(member.getCustomer(), member.getId());
            if (!hasOtherMembers) {
                member.getCustomer().setMember(false);
                customerRepo.save(member.getCustomer());
            }
        }
        kycAutomationService.syncMemberKyc(member.getMemberId());
    }


    @Override
    public MemberResponse transferCustomer(String memberId, String newCustomerId) {
        if (!StringUtils.hasText(newCustomerId)) {
            throw new BadRequestException("Customer id is required");
        }

        Member member = getByMemberIdForService(memberId);
        Customer customer = customerService.getCustomerForService(newCustomerId.trim());

        member.setCustomer(customer);
        memberRepo.save(member);

        return memberMapper.toResponse(member);
    }

    /**
     * Ce qu'un membre ACTIVE doit avoir pour que l'espace client l'accepte.
     *
     * <p>Le statut du membre ne suffit pas : le jeton porte les roles de l'utilisateur, et l'espace
     * client exige ROLE_MEMBER. Un membre active par un administrateur sans passer par le code de
     * verification restait sans role, et l'espace client lui repondait 403 a la premiere page.
     * Activer, c'est donner le role, ouvrir le compte, et se porter garant de l'adresse.</p>
     */
    private void openPortalAccount(Member member, String actor) {
        if (member.getUser() == null) {
            return;
        }
        roleUserService.addRoleToUser(member.getUser().getId(), "MEMBER", actor);
        userService.activateUser(member.getUser().getEmail());
        userService.markEmailVerified(member.getUser().getEmail(), actor);
    }

    @Override
    public void enablePortalAccess(String memberId) {

        Member member = getByMemberIdForService(memberId);
        member.setPortalAccess(true);
        openPortalAccount(member, "ADMIN_ACTIVATION");
        if (member.getPortalActivatedAt() == null) {
            java.time.Instant now = java.time.Instant.now();
            member.setPortalActivatedAt(now);
            member.setKycGracePeriodEndAt(now.plus(defaultKycGracePeriodDays, java.time.temporal.ChronoUnit.DAYS));
        }

        if(member.getStatus() == MemberStatus.INACTIVE || member.getStatus() == MemberStatus.SUSPENDED){
            member.setStatus(MemberStatus.ACTIVE);
        }

        memberRepo.save(member);
        kycAutomationService.syncMemberKyc(member.getMemberId());
    }

    @Override
    public void setKycGracePeriodDays(String memberId, int gracePeriodDays) {
        Member member = getByMemberIdForService(memberId);
        java.time.Instant base = member.getPortalActivatedAt() != null
                ? member.getPortalActivatedAt()
                : java.time.Instant.now();
        member.setKycGracePeriodEndAt(base.plus(gracePeriodDays, java.time.temporal.ChronoUnit.DAYS));
        memberRepo.save(member);
    }

    @Override
    public void disablePortalAccess(String memberId) {

        Member member = getByMemberIdForService(memberId);
        member.setPortalAccess(false);
        member.setStatus(MemberStatus.INACTIVE);
        memberRepo.save(member);
        kycAutomationService.syncMemberKyc(member.getMemberId());
    }

    private void createProfileIfMissing(Member member, CreateMemberRequest request, Customer customer) {
        if (member == null || member.getId() == null || memberProfileRepository.existsByMember_Id(member.getId())) {
            return;
        }
        try {
            MemberProfile.MemberProfileBuilder builder = MemberProfile.builder().member(member);
            if (request != null) {
                if (request.getBirthDate() != null) {
                    builder.birthDate(request.getBirthDate());
                }
                if (StringUtils.hasText(request.getGender())) {
                    builder.gender(request.getGender().trim());
                }
                if (StringUtils.hasText(request.getPreferredCommunicationChannel())) {
                    builder.preferredCommunicationChannel(request.getPreferredCommunicationChannel().trim());
                }
            }
            if (customer != null && customer.getAddress() != null) {
                Address addr = customer.getAddress();
                builder.address(formatStreet(addr));
                builder.city(addr.getCity());
                if (addr.getCountry() != null) {
                    builder.country(addr.getCountry().getName());
                }
            }
            memberProfileRepository.save(builder.build());
        } catch (Exception ex) {
            log.warn("Could not create profile for member {}: {}", member.getMemberId(), ex.getMessage());
        }
    }

    private String formatStreet(Address addr) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(addr.getStreetNumber())) {
            sb.append(addr.getStreetNumber()).append(" ");
        }
        if (StringUtils.hasText(addr.getStreetName())) {
            sb.append(addr.getStreetName());
        }
        if (sb.isEmpty() && StringUtils.hasText(addr.getDistrict())) {
            sb.append(addr.getDistrict());
        }
        return sb.isEmpty() ? null : sb.toString().trim();
    }
//TODO revoir cette methode
    private void sendMemberCreatedEmailAfterCommit(Member member, boolean passwordSetupPending) {
        if (member == null || !StringUtils.hasText(member.getEmail())) {
            return;
        }
        Runnable task = () -> {
            try {
                Context welcomeCtx = new Context();
                welcomeCtx.setVariable("name", member.getDisplayName().trim());
                welcomeCtx.setVariable("memberId", member.getMemberId());
                welcomeCtx.setVariable("email", member.getEmail());
                welcomeCtx.setVariable("portalAccess", Boolean.TRUE.equals(member.getPortalAccess()));
                welcomeCtx.setVariable("passwordSetupPending", passwordSetupPending);
                welcomeCtx.setVariable("plan", null);
                welcomeCtx.setVariable("startDate", null);
                String welcomeHtml = emailTemplateEngine.process("form/welcome-member-email", welcomeCtx);
                emailSender.sendHtmlEmail(member.getEmail(),
                        "Bienvenue dans votre espace membre · Elle A Osé", welcomeHtml);
            } catch (MessagingException ex) {
                log.warn("Unable to send member creation email to {}", member.getEmail(), ex);
            }
        };
        runAfterCommit(task);
    }

    private void sendPasswordSetupLinkAfterCommit(Users user) {
        Runnable task = () -> {
            try {
                passwordResetService.generatePasswordResetToken(user);
            } catch (Exception ex) {
                log.warn("Unable to send password setup link to {}", user.getEmail(), ex);
            }
        };
        runAfterCommit(task);
    }

    private String logoUrl() {
        if (!StringUtils.hasText(publicBaseUrl)) {
            return "/images/logo.png";
        }
        return publicBaseUrl.replaceAll("/+$", "") + "/images/logo.png";
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
    }


    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void normalizeCreateRequest(CreateMemberRequest request) {
        request.setExistingCustomerId(normalizeWhitespace(request.getExistingCustomerId()));
        request.setCustomerType(normalizeCustomerType(request.getCustomerType()));
        request.setFirstname(normalizeWhitespace(Normalization.normalizeFirstname(request.getFirstname())));
        request.setLastname(normalizeWhitespace(Normalization.normalizeLastname(request.getLastname())));
        request.setCompanyName(normalizeWhitespace(request.getCompanyName()));
        request.setEmail(normalizeEmail(request.getEmail()));
        request.setBillingEmail(normalizeOptionalEmail(request.getBillingEmail()));
        request.setPhone(normalizePhone(request.getPhone(), request.getPhoneCountry()));

        if (StringUtils.hasText(request.getWhatsappPhone())) {
            request.setWhatsappPhone(normalizePhone(request.getWhatsappPhone(), request.getPhoneCountry()));
        }
    }

    private void ensureNoConflictingRecords(CreateMemberRequest request) {
        if (Boolean.TRUE.equals(memberRepo.existsByEmailIgnoreCaseAndDeletedFalse(request.getEmail()))) {
            throw new ResourceAlreadyExistException("A member with this email already exists");
        }

        if (Boolean.TRUE.equals(memberRepo.existsByPhoneAndDeletedFalse(request.getPhone()))) {
            throw new ResourceAlreadyExistException("A member with this phone number already exists");
        }

        if (!StringUtils.hasText(request.getExistingCustomerId())
                && Boolean.TRUE.equals(customerRepo.existsByEmailIgnoreCase(request.getEmail()))) {
            throw new ResourceAlreadyExistException("A customer with this email already exists. Use existingCustomerId to attach this member to the existing customer");
        }

        if (userRepo.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ResourceAlreadyExistException("A user with this email already exists");
        }
    }

    private String resolveIntegrityViolationMessage(CreateMemberRequest request, DataIntegrityViolationException exception) {
        String knownConflict = resolveKnownIntegrityConflictMessage(request);
        String databaseMessage = resolveDatabaseErrorMessage(exception);

        if (StringUtils.hasText(databaseMessage)) {
            return "Database constraint violation: " + databaseMessage;
        }

        if (StringUtils.hasText(knownConflict)) {
            return knownConflict;
        }
        return "Member creation failed because one or more values must be unique";
    }

    private String resolveKnownIntegrityConflictMessage(CreateMemberRequest request) {
        if (Boolean.TRUE.equals(memberRepo.existsByEmailIgnoreCaseAndDeletedFalse(request.getEmail()))) {
            return "A member with this email already exists";
        }
        if (Boolean.TRUE.equals(memberRepo.existsByPhoneAndDeletedFalse(request.getPhone()))) {
            return "A member with this phone number already exists";
        }
        if (!StringUtils.hasText(request.getExistingCustomerId())
                && Boolean.TRUE.equals(customerRepo.existsByEmailIgnoreCase(request.getEmail()))) {
            return "A customer with this email already exists. Use existingCustomerId to attach this member to the existing customer";
        }
        if (userRepo.existsByEmailIgnoreCase(request.getEmail())) {
            return "A user with this email already exists";
        }
        return null;
    }

    private String resolveDatabaseErrorMessage(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        if (cause == null || !StringUtils.hasText(cause.getMessage())) {
            cause = exception.getCause();
        }
        if (cause == null || !StringUtils.hasText(cause.getMessage())) {
            return exception.getMessage();
        }
        return cause.getMessage().replaceAll("\\s+", " ").trim();
    }

    private String normalizeWhitespace(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private String normalizeOptionalEmail(String value) {
        return StringUtils.hasText(value) ? normalizeEmail(value) : null;
    }

    /**
     * Forme internationale · le pays complete l'indicatif s'il manque.
     *
     * <p>Un numero illisible passe ici tel quel, nettoye : c'est la validation qui le refusera,
     * avec le message qui va avec, plutot qu'une exception au milieu d'une normalisation.</p>
     */
    private String normalizePhone(String value, String country) {
        if (!StringUtils.hasText(value) || !ValidationUtils.validatePhoneNumber(value)) {
            return value == null ? null : value.replaceAll("\\s+", "");
        }
        return phoneNumberService.normalize(value, country);
    }

    private void normalizeUpdateRequest(UpdateMemberRequest request) {
        if (request == null) {
            return;
        }

        request.setFirstname(normalizeWhitespace(request.getFirstname()));
        request.setLastname(normalizeWhitespace(request.getLastname()));
        request.setEmail(normalizeOptionalEmail(request.getEmail()));
        request.setPhone(normalizePhone(request.getPhone(), request.getPhoneCountry()));
        request.setWhatsappPhone(normalizePhone(request.getWhatsappPhone(), request.getPhoneCountry()));
    }

    private void validateUpdateRequest(Member member, UpdateMemberRequest request) {
        if (request == null) {
            throw new BadRequestException("Member update request is required");
        }

        if (StringUtils.hasText(request.getEmail())) {
            if (!ValidationUtils.validateEmail(request.getEmail())) {
                throw new BadRequestException("A valid email is required");
            }
            if (!member.getEmail().equalsIgnoreCase(request.getEmail())
                    && (Boolean.TRUE.equals(memberRepo.existsByEmailIgnoreCaseAndDeletedFalse(request.getEmail()))
                    || userRepo.existsByEmailIgnoreCase(request.getEmail()))) {
                throw new BadRequestException("A member with this email already exists");
            }
        }

        if (StringUtils.hasText(request.getPhone())) {
            if (!ValidationUtils.validatePhoneNumber(request.getPhone())) {
                throw new BadRequestException("A valid phone number is required");
            }
            if (!request.getPhone().equals(member.getPhone())
                    && Boolean.TRUE.equals(memberRepo.existsByPhoneAndDeletedFalse(request.getPhone()))) {
                throw new BadRequestException("A member with this phone number already exists");
            }
        }

        if (StringUtils.hasText(request.getWhatsappPhone()) && !ValidationUtils.validatePhoneNumber(request.getWhatsappPhone())) {
            throw new BadRequestException("A valid whatsapp phone number is required");
        }
    }

    private void syncUserEmail(Member member, UpdateMemberRequest request) {
        if (!StringUtils.hasText(request.getEmail())) {
            return;
        }
        Users user = member.getUser();
        if (user != null && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            user.setEmail(request.getEmail());
            userRepo.save(user);
        }
    }

    private void syncUserName(Member member, UpdateMemberRequest request) {
        Users user = member.getUser();
        if (user == null) return;
        boolean changed = false;
        if (StringUtils.hasText(request.getFirstname()) && !request.getFirstname().equals(user.getFirstname())) {
            user.setFirstname(request.getFirstname());
            changed = true;
        }
        if (StringUtils.hasText(request.getLastname()) && !request.getLastname().equals(user.getLastname())) {
            user.setLastname(request.getLastname());
            changed = true;
        }
        if (changed) {
            userRepo.save(user);
        }
    }

    private void validateCreateRequest(CreateMemberRequest request, Boolean createByadmin) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            throw new BadRequestException("Member request is required");
        }

        CustomerType requestedCustomerType = resolveRequestedCustomerType(request);
        Customer existingCustomer = null;

        if (!StringUtils.hasText(request.getFirstname())) {
            errors.add("Firstname is required");
        }

        if (!StringUtils.hasText(request.getLastname())) {
            errors.add("Lastname is required");
        }

        if (!StringUtils.hasText(request.getEmail()) || !ValidationUtils.validateEmail(request.getEmail())) {
            errors.add("A valid email is required");
        }

        if (!StringUtils.hasText(request.getPhone()) || !ValidationUtils.validatePhoneNumber(request.getPhone())) {
            errors.add("A valid phone number is required");
        }

        if (StringUtils.hasText(request.getCustomerType()) && parseCustomerType(request.getCustomerType()) == null) {
            errors.add("Customer type is invalid");
        }

        if (!StringUtils.hasText(request.getExistingCustomerId())
                && requestedCustomerType == CustomerType.COMPANY
                && !StringUtils.hasText(request.getCompanyName())) {
            errors.add("Company name is required when customer type is COMPANY");
        }

        if (StringUtils.hasText(request.getBillingEmail()) && !ValidationUtils.validateEmail(request.getBillingEmail())) {
            errors.add("Billing email is invalid");
        }

        if (!Boolean.TRUE.equals(createByadmin) && !StringUtils.hasText(request.getPassword())) {
            errors.add("Password is required for portal registration");
        }

        if (Boolean.TRUE.equals(createByadmin) && !request.isGeneratePassword() && !StringUtils.hasText(request.getPassword())) {
            errors.add("Password is required when generatePassword is false");
        }

        if (StringUtils.hasText(request.getExistingCustomerId())) {
            existingCustomer = customerService.getCustomerForService(request.getExistingCustomerId());
        }

        if (requestedCustomerType == CustomerType.COMPANY && !StringUtils.hasText(request.getExistingCustomerId())) {
            errors.add("Existing customer id is required when customer type is COMPANY");
        }

        if (requestedCustomerType == CustomerType.COMPANY
                && existingCustomer != null
                && existingCustomer.getType() != CustomerType.COMPANY) {
            errors.add("Existing customer must be of type COMPANY when customer type is COMPANY");
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join(", ", errors));
        }
    }

    private Customer resolveCustomerForMemberCreation(CreateMemberRequest request) {
        if (StringUtils.hasText(request.getExistingCustomerId())) {
            return customerService.getCustomerForService(request.getExistingCustomerId());
        }

        CustomerType requestedCustomerType = resolveRequestedCustomerType(request);

        CustomerRequest customerRequest = CustomerRequest.builder()
                .type(requestedCustomerType.name())
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .companyName(request.getCompanyName())
                .email(request.getEmail())
                .billingEmail(request.getBillingEmail())
                .address(request.getAddress())
                .phone(request.getPhone())
                .phoneCountry(request.getPhoneCountry())
                .whatsappPhone(request.getWhatsappPhone())
                .build();

        return customerService.createCustomerForMember(customerRequest);
    }

    private CustomerType resolveRequestedCustomerType(CreateMemberRequest request) {
        if (StringUtils.hasText(request.getCustomerType())) {
            return parseCustomerType(request.getCustomerType());
        }
        return CustomerType.PERSON;
    }

    private CustomerType parseCustomerType(String value) {
        try {
            return CustomerType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String normalizeCustomerType(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private List<String> resolveFuzzyMatchedMemberIds(MemberSearchCriteria criteria) {
        if (criteria == null || !StringUtils.hasText(criteria.getQuery())) {
            return null;
        }

        return memberRepo.fuzzySearchMemberIds(criteria.getQuery().trim());
    }

    private Pageable unsorted(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

}
