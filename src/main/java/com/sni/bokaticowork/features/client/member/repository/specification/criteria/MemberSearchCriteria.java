package com.sni.bokaticowork.features.client.member.repository.specification.criteria;

import com.sni.bokaticowork.features.client.member.enums.MemberStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberSearchCriteria {

    private String query;

    private String customerId;

    private String memberId;

    private String code;

    private String firstname;

    private String lastname;

    private String email;

    private String phone;

    private MemberStatus status;

    private Boolean portalAccess;
}
