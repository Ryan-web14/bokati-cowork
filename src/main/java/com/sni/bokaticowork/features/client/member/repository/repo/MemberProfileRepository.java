package com.sni.bokaticowork.features.client.member.repository.repo;

import com.sni.bokaticowork.features.client.member.model.MemberProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

    Optional<MemberProfile> findByMember_MemberId(String memberId);
    boolean existsByMember_Id(Long memberId);

}
