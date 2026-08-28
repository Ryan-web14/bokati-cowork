package com.sni.bokaticowork.features.client.member.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.customer.service.interfaces.CustomerService;
import com.sni.bokaticowork.features.client.member.dto.request.CreateMemberRequest;
import com.sni.bokaticowork.features.client.member.dto.response.MemberResponse;
import com.sni.bokaticowork.features.client.member.dto.response.MemberSummaryResponse;
import com.sni.bokaticowork.features.client.member.enums.MemberStatus;
import com.sni.bokaticowork.features.client.member.mapper.interfaces.MemberMapper;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberProfileRepository;
import com.sni.bokaticowork.features.client.member.repository.specification.criteria.MemberSearchCriteria;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import com.sni.bokaticowork.security.admin.user.dto.request.UserRequest;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import com.sni.bokaticowork.security.service.passwordResetService.interfaces.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepo;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private CustomerService customerService;

    @Mock
    private CustomerRepository customerRepo;

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepo;

    @Mock
    private SequenceGeneratorFacade sequenceGenerator;

    @Mock
    private KycAutomationService kycAutomationService;

    @Mock
    private MemberProfileRepository memberProfileRepository;

    @Mock
    private DefaultEmailSender emailSender;

    @Mock
    private SpringTemplateEngine emailTemplateEngine;

    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private MemberServiceImpl memberService;

    @Test
    void shouldAttachMemberToExistingCustomerWithoutCreatingNewCustomer() {
        CreateMemberRequest request = CreateMemberRequest.builder()
                .existingCustomerId("CUS-0001")
                .firstname("Jane")
                .lastname("Doe")
                .email("jane@example.com")
                .phone("060000000")
                .password("Secret123")
                .generatePassword(false)
                .build();

        Customer customer = Customer.builder().customerId("CUS-0001").build();
        Member member = Member.builder().phone("060000000").build();
        Users user = Users.builder().id(99L).email("jane@example.com").build();
        MemberResponse response = MemberResponse.builder().memberId("MBR-202604-00000001").email("jane@example.com").build();

        when(memberRepo.existsByEmailIgnoreCaseAndDeletedFalse("jane@example.com")).thenReturn(false);
        when(memberRepo.existsByPhoneAndDeletedFalse("060000000")).thenReturn(false);
        when(userRepo.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(customerService.getCustomerForService("CUS-0001")).thenReturn(customer);
        when(memberMapper.toEntity(request)).thenReturn(member);
        when(userService.createUser(any(UserRequest.class))).thenReturn(user);
        when(sequenceGenerator.next("MEMBER", LocalDate.now())).thenReturn("MEM-0001");
        when(memberMapper.toResponse(member)).thenReturn(response);

        MemberResponse created = memberService.create(request, true);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepo).save(memberCaptor.capture());
        verify(customerService, never()).createCustomer(any());
        assertSame(customer, memberCaptor.getValue().getCustomer());
        assertEquals(MemberStatus.PENDING, memberCaptor.getValue().getStatus());
        String expectedMemberId = String.format(
                "MBR-%d%02d-00000001",
                LocalDate.now().getYear(),
                LocalDate.now().getMonthValue()
        );
        assertEquals(expectedMemberId, memberCaptor.getValue().getMemberId());
        assertEquals(created.getMemberId(), response.getMemberId());
    }

    @Test
    void shouldTransferMemberToAnotherCustomer() {
        Member member = Member.builder().memberId("MEM-0001").build();
        Customer customer = Customer.builder().customerId("CUS-0002").build();
        MemberResponse response = MemberResponse.builder().memberId("MEM-0001").customerId("CUS-0002").build();

        when(memberRepo.findByMemberIdAndDeletedFalse("MEM-0001")).thenReturn(Optional.of(member));
        when(customerService.getCustomerForService("CUS-0002")).thenReturn(customer);
        when(memberMapper.toResponse(member)).thenReturn(response);

        MemberResponse transferred = memberService.transferCustomer("MEM-0001", "CUS-0002");

        verify(memberRepo).save(member);
        assertSame(customer, member.getCustomer());
        assertEquals("CUS-0002", transferred.getCustomerId());
    }

    @Test
    void shouldArchiveAMemberThatIsStillActive() {
        Member member = Member.builder().memberId("MEM-0003").deleted(false).build();
        when(memberRepo.findByMemberIdIncludingArchived("MEM-0003")).thenReturn(Optional.of(member));

        memberService.delete("MEM-0003");

        verify(memberRepo).save(member);
        assertTrue(member.getDeleted());
    }

    @Test
    void shouldNotFailWhenArchivingAMemberAlreadyArchived() {
        // Regression : la recherche filtrait sur DeletedFalse, si bien qu'un second appel
        // repondait 404 "Member not found" sur un membre qui existe et qui est simplement
        // deja archive. L'operation est idempotente.
        Member archived = Member.builder().memberId("MEM-0004").deleted(true).build();
        when(memberRepo.findByMemberIdIncludingArchived("MEM-0004")).thenReturn(Optional.of(archived));

        memberService.delete("MEM-0004");

        verify(memberRepo, never()).save(any(Member.class));
    }

    @Test
    void shouldStillReportAnUnknownMemberCodeAsNotFound() {
        when(memberRepo.findByMemberIdIncludingArchived("MEM-9999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> memberService.delete("MEM-9999"));
    }

    @Test
    void shouldAllowMemberCreationWhenCustomerWithSameEmailAlreadyExists() {
        CreateMemberRequest request = CreateMemberRequest.builder()
                .firstname("Jane")
                .lastname("Doe")
                .email("jane@example.com")
                .phone("060000000")
                .password("Secret123")
                .generatePassword(false)
                .build();

        Customer customer = Customer.builder().customerId("CUS-0001").build();
        Member member = Member.builder().phone("060000000").build();
        Users user = Users.builder().id(99L).email("jane@example.com").build();
        MemberResponse response = MemberResponse.builder().memberId("MEM-0001").email("jane@example.com").build();

        when(memberRepo.existsByEmailIgnoreCaseAndDeletedFalse("jane@example.com")).thenReturn(false);
        when(memberRepo.existsByPhoneAndDeletedFalse("060000000")).thenReturn(false);
        when(userRepo.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(customerService.createCustomerForMember(any())).thenReturn(customer);
        when(memberMapper.toEntity(request)).thenReturn(member);
        when(userService.createUser(any(UserRequest.class))).thenReturn(user);
        when(sequenceGenerator.next("MEMBER", LocalDate.now())).thenReturn("MEM-0001");
        when(memberMapper.toResponse(member)).thenReturn(response);

        MemberResponse created = memberService.create(request, true);

        verify(customerService).createCustomerForMember(any());
        assertEquals("MEM-0001", created.getMemberId());
    }

    @Test
    void shouldSendPasswordResetLinkWhenAdminCreatesMember() {
        CreateMemberRequest request = CreateMemberRequest.builder()
                .firstname("Jane")
                .lastname("Doe")
                .email("jane@example.com")
                .phone("060000000")
                .password("Secret123")
                .generatePassword(false)
                .build();

        Customer customer = Customer.builder().customerId("CUS-0001").build();
        Member member = Member.builder()
                .firstname("Jane")
                .lastname("Doe")
                .email("jane@example.com")
                .phone("060000000")
                .build();
        Users user = Users.builder().id(99L).email("jane@example.com").build();
        MemberResponse response = MemberResponse.builder().memberId("MEM-0001").email("jane@example.com").build();

        when(memberRepo.existsByEmailIgnoreCaseAndDeletedFalse("jane@example.com")).thenReturn(false);
        when(memberRepo.existsByPhoneAndDeletedFalse("060000000")).thenReturn(false);
        when(userRepo.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(customerService.createCustomerForMember(any())).thenReturn(customer);
        when(memberMapper.toEntity(request)).thenReturn(member);
        when(userService.createUser(any(UserRequest.class))).thenReturn(user);
        when(sequenceGenerator.next("MEMBER", LocalDate.now())).thenReturn("MEM-0001");
        when(memberMapper.toResponse(member)).thenReturn(response);

        memberService.create(request, true);

        verify(passwordResetService).generatePasswordResetToken(user);
    }

    @Test
    void shouldRequireExistingCustomerIdForCompanyMemberCreation() {
        CreateMemberRequest request = CreateMemberRequest.builder()
                .customerType("COMPANY")
                .companyName("ACME")
                .firstname("Jane")
                .lastname("Doe")
                .email("jane@example.com")
                .phone("060000000")
                .password("Secret123")
                .generatePassword(false)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> memberService.create(request, true));

        assertEquals("Existing customer id is required when customer type is COMPANY", ex.getMessage());
    }

    @Test
    void shouldRejectNonCompanyExistingCustomerForCompanyMemberCreation() {
        CreateMemberRequest request = CreateMemberRequest.builder()
                .existingCustomerId("CUS-0001")
                .customerType("COMPANY")
                .firstname("Jane")
                .lastname("Doe")
                .email("jane@example.com")
                .phone("060000000")
                .password("Secret123")
                .generatePassword(false)
                .build();

        Customer customer = Customer.builder()
                .customerId("CUS-0001")
                .type(CustomerType.PERSON)
                .build();

        when(customerService.getCustomerForService("CUS-0001")).thenReturn(customer);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> memberService.create(request, true));

        assertEquals("Existing customer must be of type COMPANY when customer type is COMPANY", ex.getMessage());
    }

    @Test
    void shouldCombineFuzzyQueryWithSpecificationSearch() {
        MemberSearchCriteria criteria = new MemberSearchCriteria();
        criteria.setQuery("jhn do");
        criteria.setStatus(MemberStatus.ACTIVE);

        Pageable pageable = PageRequest.of(0, 20);
        Member member = Member.builder().memberId("MBR-0001").build();
        MemberSummaryResponse summary = MemberSummaryResponse.builder().memberId("MBR-0001").build();

        when(memberRepo.fuzzySearchMemberIds("jhn do")).thenReturn(List.of("MBR-0001"));
        when(memberRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(member), pageable, 1));
        when(memberMapper.toSummary(member)).thenReturn(summary);

        var results = memberService.search(criteria, pageable);

        verify(memberRepo).fuzzySearchMemberIds("jhn do");
        verify(memberRepo).findAll(any(Specification.class), eq(pageable));
        assertEquals(1, results.getData().size());
        assertEquals("MBR-0001", results.getData().get(0).getMemberId());
    }

    @Test
    void shouldReturnEmptyPageWhenFuzzyQueryFindsNothing() {
        MemberSearchCriteria criteria = new MemberSearchCriteria();
        criteria.setQuery("zzzz unknown");

        Pageable pageable = PageRequest.of(0, 20);

        when(memberRepo.fuzzySearchMemberIds("zzzz unknown")).thenReturn(List.of());

        var results = memberService.search(criteria, pageable);

        verify(memberRepo).fuzzySearchMemberIds("zzzz unknown");
        verify(memberRepo, never()).findAll(any(Specification.class), eq(pageable));
        assertTrue(results.getData().isEmpty());
        assertEquals(0L, results.getPageable().getTotalElements());
    }
}
