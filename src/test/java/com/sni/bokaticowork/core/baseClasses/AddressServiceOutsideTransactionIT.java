package com.sni.bokaticowork.core.baseClasses;

import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import com.sni.bokaticowork.core.baseClasses.dto.response.AddressResponse;
import com.sni.bokaticowork.core.baseClasses.mapper.interfaces.AddressMapperImpl;
import com.sni.bokaticowork.core.baseClasses.mapper.interfaces.AddressMapperImpl_;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.baseClasses.model.Country;
import com.sni.bokaticowork.core.baseClasses.repository.AddressRepository;
import com.sni.bokaticowork.core.baseClasses.service.implementation.AddressServiceImpl;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le service d'adresses appele comme le fait le controleur, sans transaction autour.
 *
 * <p>Le pays d'une adresse est paresseux. Sans transaction dans le service, la lecture d'une
 * adresse tombait en LazyInitializationException (500, open-in-view coupe) et la mise a jour
 * echouait de meme, sans jamais etre ecrite.</p>
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({AddressServiceImpl.class, AddressMapperImpl.class, AddressMapperImpl_.class})
class AddressServiceOutsideTransactionIT extends PostgresIntegrationTestBase {

    @Autowired private AddressServiceImpl addressService;
    @Autowired private AddressRepository addressRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;
    private Long addressId;
    private String countryCode;
    private String countryName;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        Country country = tx.execute(status -> entityManager
                .createQuery("SELECT c FROM Country c ORDER BY c.id", Country.class)
                .setMaxResults(1)
                .getSingleResult());
        countryCode = country.getCountryCode();
        countryName = country.getName();
        addressId = tx.execute(status -> addressRepository.save(Address.builder()
                .streetNumber("12")
                .streetName("Rue des tests")
                .city("Brazzaville")
                .country(entityManager.getReference(Country.class, country.getId()))
                .build()).getId());
    }

    @AfterEach
    void cleanUp() {
        if (addressId != null) {
            tx.executeWithoutResult(status -> addressRepository.deleteById(addressId));
        }
    }

    @Test
    @DisplayName("Une adresse se lit hors transaction, avec le nom de son pays")
    void addressIsReadWithItsCountry() {
        AddressResponse response = addressService.getAddressById(addressId);

        assertThat(response.getCountry()).isEqualTo(countryName);
        assertThat(response.getFullAddress()).contains("Brazzaville").contains(countryName);
    }

    @Test
    @DisplayName("Une mise a jour hors transaction lit le pays et s'ecrit en base")
    void addressUpdateIsWritten() {
        addressService.updateAddress(addressId, AddressRequest.builder()
                .streetNumber("14")
                .streetName("Rue des tests")
                .city("Pointe-Noire")
                .countryCode(countryCode)
                .build());

        String city = tx.execute(status -> addressRepository.findById(addressId).orElseThrow().getCity());
        assertThat(city).isEqualTo("Pointe-Noire");
    }
}
