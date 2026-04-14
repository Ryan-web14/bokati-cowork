package com.sni.bokaticowork.core.baseClasses.controller;


import com.sni.bokaticowork.core.baseClasses.dto.request.CountryRequest;
import com.sni.bokaticowork.core.baseClasses.dto.response.CountryResponse;
import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.CountryService;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/countries")
@RequiredArgsConstructor
public class CountryController {

        private final CountryService countryService;

        @Audited(module = "COUNTRY", action = "CREATE_COUNTRY")
        @Idempotent(operation = "COUNTRY_CREATE", required = false)
        @PostMapping
        public ResponseEntity<Void> createCountry(@Valid @RequestBody CountryRequest request){

            countryService.addCountry(request);

            return ResponseEntity.status(201).build();
        }

        @Audited(module = "COUNTRY", action = "UPDATE_COUNTRY")
        @Idempotent(operation = "COUNTRY_UPDATE", required = false)
        @PutMapping("/{countryCode}")
        public ResponseEntity<CountryResponse> updateCountry(@PathVariable String countryCode,
                                                             @Valid @RequestBody CountryRequest request) {
            return ResponseEntity.ok(countryService.updateCountry(countryCode, request));
        }

        @GetMapping("/{countryCode}")
        public ResponseEntity<CountryResponse> getCountryByCode(@PathVariable String countryCode) {
            return ResponseEntity.ok(countryService.getCountryByCode(countryCode));
        }

        @GetMapping("/by-phone-code")
        public ResponseEntity<CountryResponse> getCountryByPhoneCode(@RequestParam String phoneCode) {
            return ResponseEntity.ok(countryService.getCountryByPhoneCode(phoneCode));
        }

        @GetMapping({"", "/all"})
        public ResponseEntity<Collection<CountryResponse>> getAllCountries(){

            List<CountryResponse> countries = countryService.getAllCountries();

            return ResponseEntity.status(HttpStatus.OK).body(countries);
        }

        @Audited(module = "COUNTRY", action = "DELETE_COUNTRY")
        @DeleteMapping("/{countryCode}")
        public ResponseEntity<Void> deleteCountry(@PathVariable String countryCode) {
            countryService.softDelete(countryCode);
            return ResponseEntity.noContent().build();
        }

}
