# CountryController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 7 route(s).

## Routes

<a id="r-get-sni-api-v1-countries"></a>
### GET /sni/api/v1/countries

- Accès : public, sans jeton
- Réponse : `Collection<CountryResponse>`
- Modèles : [CountryResponse](#m-CountryResponse)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CountryController.java`, méthode `getAllCountries`

<a id="r-post-sni-api-v1-countries"></a>
### POST /sni/api/v1/countries

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `COUNTRY_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CountryRequest`
- Réponse : `vide`
- Modèles : [CountryRequest](#m-CountryRequest)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CountryController.java`, méthode `createCountry`

<a id="r-get-sni-api-v1-countries-by-phone-code"></a>
### GET /sni/api/v1/countries/by-phone-code

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `CountryResponse`
- Modèles : [CountryResponse](#m-CountryResponse)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CountryController.java`, méthode `getCountryByPhoneCode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `phoneCode` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-countries-phone-codes"></a>
### GET /sni/api/v1/countries/phone-codes

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `Collection<CountryService.PhoneCodeEntry>`
- Modèles : [CountryService.PhoneCodeEntry](#m-CountryService-PhoneCodeEntry)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CountryController.java`, méthode `phoneCodes`

<a id="r-delete-sni-api-v1-countries-countrycode"></a>
### DELETE /sni/api/v1/countries/{countryCode}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CountryController.java`, méthode `deleteCountry`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `countryCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-countries-countrycode"></a>
### GET /sni/api/v1/countries/{countryCode}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `CountryResponse`
- Modèles : [CountryResponse](#m-CountryResponse)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CountryController.java`, méthode `getCountryByCode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `countryCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-countries-countrycode"></a>
### PUT /sni/api/v1/countries/{countryCode}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `COUNTRY_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `CountryRequest`
- Réponse : `CountryResponse`
- Modèles : [CountryResponse](#m-CountryResponse), [CountryRequest](#m-CountryRequest)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CountryController.java`, méthode `updateCountry`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `countryCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CountryResponse"></a>
#### CountryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `countryCode` | texte |  |  |
| `phoneCode` | texte |  |  |
| `defaultCurrency` | [CurrencyResponse](#m-CurrencyResponse) |  |  |
| `ohadaMember` | booléen |  |  |

<a id="m-CountryRequest"></a>
#### CountryRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `countryCode` | texte |  | longueur min `0`, longueur max `3` |
| `currencyCode` | texte |  |  |
| `phoneCode` | texte |  |  |
| `isOhadaMember` | booléen |  |  |

<a id="m-CountryService-PhoneCodeEntry"></a>
#### CountryService.PhoneCodeEntry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `countryCode` | texte |  |  |
| `phoneCode` | texte |  |  |

<a id="m-CurrencyResponse"></a>
#### CurrencyResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currencyCode` | texte |  |  |
| `currencyName` | texte |  |  |
