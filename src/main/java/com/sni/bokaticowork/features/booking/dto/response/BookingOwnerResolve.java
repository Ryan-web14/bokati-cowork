package com.sni.bokaticowork.features.booking.dto.response;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.model.BusinessEntity;

public record BookingOwnerResolve(Member member, Customer customer, BusinessEntity businessEntity)  {
}
