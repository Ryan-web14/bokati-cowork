package com.sni.bokaticowork.core.baseClasses.repository;


import com.sni.bokaticowork.core.baseClasses.model.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    List<Currency> findCurrencyByCurrencyName(String currencyName);
    Optional<Currency> findByCurrencyCode(String currencyCode);

    @Modifying
    @Query(value = "DELETE FROM currency WHERE currency_code = :currencyCode", nativeQuery = true)
    void deleteByCurrencyCode(@Param("currencyCode") String currencyCode);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM currency WHERE currency_code = :currencyCode)")
    boolean existsByCurrencyCode(@Param("currencyCode") String currencyCode);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM currency WHERE  regexp_match(currency_name, :currencyName) IS NOT NULL)")
    boolean existsByCurrencyName(String currencyName);

    boolean existsByCurrencyCodeAndIdNot(String currencyCode, Long id);

    boolean existsByCurrencyNameIgnoreCaseAndIdNot(String currencyName, Long id);

    List<Currency> findAllByOrderByCurrencyNameAsc();

}
