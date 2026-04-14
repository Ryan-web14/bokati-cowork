package com.sni.bokaticowork.features.client.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MemberProfileResponse {
    private String memberId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String whatsappPhone;
    private LocalDate birthDate;
    private String jobTitle;
    private String companyRole;
    private String address;
    private String city;
    private String country;
}
