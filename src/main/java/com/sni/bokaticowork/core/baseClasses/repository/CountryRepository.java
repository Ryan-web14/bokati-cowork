package com.sni.bokaticowork.core.baseClasses.repository;


import com.sni.bokaticowork.core.baseClasses.model.Country;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {


    @Query(nativeQuery = true, value = "SELECT * FROM country  WHERE country_code = :countryCode")
    Optional<Country> findByCountryCode(@Param("countryCode") String countryCode);

    Optional<Country> findCountryByName(String name);

    Optional<Country> getCountryByCountryCode(@Min(value = 2) @Max(value = 3) String countryCode);

    void deleteByCountryCode(String countryCode);

    Optional<Country> findCountryByPhoneCode(@Param("phoneCode") String phoneCode);

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE country SET deleted = true WHERE country_code = :countryCode")
    void softDeleteByCountryCode(@Param("countryCode") String countryCode);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM country WHERE country_code = :countryCode)")
    boolean existsByCountryCode(@Param("countryCode") String countryCode);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM country WHERE  regexp_match(name, :countryName) IS NOT NULL)")
    boolean existsByCountryName(@Param("countryName") String countryName);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM country WHERE phone_code = :phoneCode)")
    boolean existByPhoneCode(@Param("phoneCode") String phoneCode);

    boolean existsByCountryCodeAndIdNot(String countryCode, Long id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByPhoneCodeAndIdNot(String phoneCode, Long id);

    List<Country> findAllByOrderByNameAsc();

}
