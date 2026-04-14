package com.sni.bokaticowork.features.client.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UpdateMemberProfileRequest {

    private LocalDate birthDate;

    @Size(max = 120)
    private String jobTitle;

    @Size(max = 120)
    private String companyRole;

    @Size(max = 255)
    private String address;

    @Size(max = 120)
    private String city;

    @Size(max = 120)
    private String country;


    //TOdo see what to do for the photo url

}