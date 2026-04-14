package com.sni.bokaticowork.features.client.member.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "member_profile")
public class MemberProfile {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id",foreignKey = @ForeignKey(name = "fk_member"),unique = true, nullable = false)
    private Member member;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "company_role")
    private String companyRole;

    @Column(name = "birth_date")
    private LocalDate birthDate;

//    @Column(name = "gender")
//    private String gender;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "country")
    private String country;

}
