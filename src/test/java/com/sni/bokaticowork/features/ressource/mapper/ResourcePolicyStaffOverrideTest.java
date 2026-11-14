package com.sni.bokaticowork.features.ressource.mapper;

import com.sni.bokaticowork.features.ressource.dto.request.CreateResourcePolicyRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourcePolicyRequest;
import com.sni.bokaticowork.features.ressource.mapper.interfaces.ResourcePolicyMapper;
import com.sni.bokaticowork.features.ressource.mapper.interfaces.ResourcePolicyMapperImpl;
import com.sni.bokaticowork.features.ressource.mapper.interfaces.ResourcePolicyMapperImpl_;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/** La derogation de l'equipe se lit et se regle par les routes des politiques ; vraie par defaut. */
class ResourcePolicyStaffOverrideTest {

    private final ResourcePolicyMapper mapper = new ResourcePolicyMapperImpl();

    ResourcePolicyStaffOverrideTest() {
        ReflectionTestUtils.setField(mapper, "delegate", new ResourcePolicyMapperImpl_());
    }

    private static CreateResourcePolicyRequest create(Boolean staffOverrideAllowed) {
        return CreateResourcePolicyRequest.builder().name("Stricte").minBookingDurationMinutes(60).maxBookingDurationMinutes(480)
                .minBookingNoticeMinutes(1440).staffOverrideAllowed(staffOverrideAllowed).build();
    }

    @Test
    void createdPolicyAllowsStaffUnlessToldOtherwise() {
        assertThat(mapper.toEntity(create(null)).getStaffOverrideAllowed()).isTrue();
        assertThat(mapper.toEntity(create(false)).getStaffOverrideAllowed()).isFalse();
    }

    @Test
    void updateChangesTheFlagOnlyWhenGivenAndResponseShowsIt() {
        ResourcePolicy policy = mapper.toEntity(create(false));
        mapper.updateEntity(policy, UpdateResourcePolicyRequest.builder().name("Stricte bis").build());
        assertThat(policy.getStaffOverrideAllowed()).isFalse();
        mapper.updateEntity(policy, UpdateResourcePolicyRequest.builder().staffOverrideAllowed(true).build());
        assertThat(policy.getStaffOverrideAllowed()).isTrue();
        assertThat(mapper.toResponse(policy).getStaffOverrideAllowed()).isTrue();
    }
}
