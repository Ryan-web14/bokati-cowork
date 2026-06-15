package com.sni.bokaticowork.features.client.member.service.interfaces;

import com.sni.bokaticowork.features.client.member.dto.request.CreateMemberRequest;
import com.sni.bokaticowork.features.client.member.dto.request.UpdateMemberRequest;
import com.sni.bokaticowork.features.client.member.dto.request.UpdateStatusRequest;
import com.sni.bokaticowork.features.client.member.dto.response.MemberResponse;
import com.sni.bokaticowork.features.client.member.dto.response.MemberSummaryResponse;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.specification.criteria.MemberSearchCriteria;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberService {

    MemberResponse create(CreateMemberRequest request, Boolean createByAdmin);
    void update(String memberCode, UpdateMemberRequest request);
    MemberResponse getByMemberId(String memberId);
    MemberResponse getByEmail(String email);
    Member getByEmailForService(String email);
    Member getByMemberIdForService(String memberId);
    Member getByMemberIdForService(Long id);
    Member getByUserForService(Long userId);
    void ChangeStatus(String memberId, UpdateStatusRequest status);
    List<MemberResponse> getAllMembers();
    List<MemberSummaryResponse> getAllMembersSummary();
    PaginatedResponse<MemberResponse> list(Pageable pageable);
    PaginatedResponse<MemberSummaryResponse> listSummary(Pageable pageable);
    PaginatedResponse<MemberSummaryResponse> listSummaryByCustomer(String customerId, Pageable pageable);
    List<MemberSummaryResponse> basicSearch(String query);
    PaginatedResponse<MemberSummaryResponse> search(MemberSearchCriteria criteria, Pageable pageable);
    void delete(String memberId);
    MemberResponse transferCustomer (String memberId, String newCustomerId);
    void enablePortalAccess(String memberId);
    void disablePortalAccess(String memberId);
    void setKycGracePeriodDays(String memberId, int gracePeriodDays);

}
