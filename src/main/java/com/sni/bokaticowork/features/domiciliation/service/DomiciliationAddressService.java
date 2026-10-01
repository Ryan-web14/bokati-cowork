package com.sni.bokaticowork.features.domiciliation.service;

import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.AddressService;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationAddress;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationAddressRepository;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Le registre des adresses attribuables.
 *
 * <p>Une adresse s'y declare une fois, avec ce qu'elle permet · qualite fiscale ou non, nombre de
 * domicilies · et le registre dit ensuite combien l'occupent. C'est ce registre qu'un controle
 * demandera : quelles adresses, et qui y est.</p>
 */
@Service
@RequiredArgsConstructor
public class DomiciliationAddressService {

    private static final List<DomiciliationContract.Status> ENDED =
            List.of(DomiciliationContract.Status.TERMINATED, DomiciliationContract.Status.EXPIRED);

    private final DomiciliationAddressRepository addressRepository;
    private final DomiciliationContractRepository contractRepository;
    private final AddressService addressService;
    private final SequenceGeneratorFacade sequenceGenerator;

    public record AddressEntry(DomiciliationAddress address, long occupants) {
    }

    @Transactional
    public DomiciliationAddress register(String label, AddressRequest address, boolean fiscalCapable, Integer maxOccupants, String notes) {
        if (!StringUtils.hasText(label) || address == null) {
            throw new BadRequestException("Une adresse du registre a un libellé et une adresse postale");
        }
        Address persisted = addressService.createAddress(address);
        return addressRepository.save(DomiciliationAddress.builder()
                .code(sequenceGenerator.next("domiciliation_address"))
                .label(label.trim())
                .address(persisted)
                .fiscalCapable(fiscalCapable)
                .maxOccupants(maxOccupants)
                .notes(StringUtils.hasText(notes) ? notes.trim() : null)
                .build());
    }

    @Transactional
    public DomiciliationAddress update(String code, String label, Boolean fiscalCapable, Integer maxOccupants, Boolean active, String notes) {
        DomiciliationAddress entry = get(code);
        if (StringUtils.hasText(label)) entry.setLabel(label.trim());
        if (fiscalCapable != null) entry.setFiscalCapable(fiscalCapable);
        if (maxOccupants != null) entry.setMaxOccupants(maxOccupants);
        if (active != null) entry.setActive(active);
        if (notes != null) entry.setNotes(StringUtils.hasText(notes) ? notes.trim() : null);
        return addressRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public DomiciliationAddress get(String code) {
        return addressRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Adresse de domiciliation introuvable"));
    }

    @Transactional(readOnly = true)
    public List<AddressEntry> registry() {
        return addressRepository.findAllByOrderByLabelAsc().stream()
                .map(address -> new AddressEntry(address, contractRepository.countByAssignedAddress_IdAndStatusNotIn(address.getId(), ENDED)))
                .toList();
    }
}
