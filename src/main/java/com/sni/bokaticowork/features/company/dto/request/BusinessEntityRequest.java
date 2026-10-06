package com.sni.bokaticowork.features.company.dto.request;

import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BusinessEntityRequest {

    @NotBlank(message = "the name of the business is required")
    private String name;

    @NotBlank(message = "the legal form of the business is required")
    private String legalForm;

    /**
     * Facultatif · toutes les entites n en ont pas encore un au moment de leur creation.
     *
     * <p>Quand il est fourni, il doit respecter le format congolais verifie par
     * {@code ValidationUtils.validateNiu}. Absent, il ne bloque plus l enregistrement.</p>
     */
    private String niuNumber;

    @NotBlank(message = "Rccm number is required")
    private String rccmNumber;

    private String taxId;

    private String activity;

    @Valid
    private AddressRequest address;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @Email(message = "Email is not valid")
    private String email;

    @NotBlank(message = "Base currency code is required")
    @Size(max = 3, message = "Base currency code must be 3 digits")
    private String baseCurrencyCode;
}
