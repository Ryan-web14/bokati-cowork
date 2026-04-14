package com.sni.bokaticowork.features.contract.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.contract.enums.ContractPartyRole;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "contract_party")
public class ContractParty {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false, foreignKey = @ForeignKey(name = "fk_contract_party_contract"))
    private Contract contract;

    @Column(name = "party_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private DocumentOwnerType partyType;

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(name = "party_code", nullable = false, length = 120)
    private String partyCode;

    @Column(name = "display_name", nullable = false, length = 250)
    private String displayName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "role", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ContractPartyRole role;

    @Column(name = "sign_order")
    private Integer signOrder;

    @Column(name = "must_sign", nullable = false)
    private Boolean mustSign;
}
