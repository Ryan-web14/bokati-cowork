package com.sni.bokaticowork.core.settings.userSettings.repository;

import com.sni.bokaticowork.core.settings.userSettings.model.MemberSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberSettingRepository extends JpaRepository<MemberSettings, Long> {

    Optional<MemberSettings> findByMember_Id(Long memberId);
}
