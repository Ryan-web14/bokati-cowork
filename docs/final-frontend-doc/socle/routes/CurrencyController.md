# CurrencyController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 12 route(s).

## Routes

<a id="r-get-sni-api-v1-currencies"></a>
### GET /sni/api/v1/currencies

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `List<CurrencyResponse>`
- Modèles : [CurrencyResponse](#m-CurrencyResponse)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CurrencyController.java`, méthode `getAllCurrencies`

<a id="r-post-sni-api-v1-currencies"></a>
### POST /sni/api/v1/currencies

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `CURRENCY_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CurrencyRequest`
- Réponse : `CurrencyResponse`
- Modèles : [CurrencyResponse](#m-CurrencyResponse), [CurrencyRequest](#m-CurrencyRequest)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CurrencyController.java`, méthode `createCurrency`

<a id="r-get-sni-api-v1-currencies-all"></a>
### GET /sni/api/v1/currencies/all

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `List<CurrencyResponse>`
- Modèles : [CurrencyResponse](#m-CurrencyResponse)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CurrencyController.java`, méthode `getAllCurrenciesLegacy`

<a id="r-delete-sni-api-v1-currencies-currencycode"></a>
### DELETE /sni/api/v1/currencies/{currencyCode}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CurrencyController.java`, méthode `deleteCurrency`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `currencyCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-currencies-currencycode"></a>
### GET /sni/api/v1/currencies/{currencyCode}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `CurrencyResponse`
- Modèles : [CurrencyResponse](#m-CurrencyResponse)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CurrencyController.java`, méthode `getCurrencyByCode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `currencyCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-currencies-currencycode"></a>
### PUT /sni/api/v1/currencies/{currencyCode}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `CURRENCY_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `CurrencyRequest`
- Réponse : `CurrencyResponse`
- Modèles : [CurrencyResponse](#m-CurrencyResponse), [CurrencyRequest](#m-CurrencyRequest)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CurrencyController.java`, méthode `updateCurrency`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `currencyCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-currency"></a>
### GET /sni/api/v1/currency

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `List<CurrencyResponse>`
- Modèles : [CurrencyResponse](#m-CurrencyResponse)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CurrencyController.java`, méthode `getAllCurrencies`

<a id="r-post-sni-api-v1-currency"></a>
### POST /sni/api/v1/currency

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `CURRENCY_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CurrencyRequest`
- Réponse : `CurrencyResponse`
- Modèles : [CurrencyResponse](#m-CurrencyResponse), [CurrencyRequest](#m-CurrencyRequest)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CurrencyController.java`, méthode `createCurrency`

<a id="r-get-sni-api-v1-currency-all"></a>
### GET /sni/api/v1/currency/all

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `List<CurrencyResponse>`
- Modèles : [CurrencyResponse](#m-CurrencyResponse)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CurrencyController.java`, méthode `getAllCurrenciesLegacy`

<a id="r-delete-sni-api-v1-currency-currencycode"></a>
### DELETE /sni/api/v1/currency/{currencyCode}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CurrencyController.java`, méthode `deleteCurrency`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `currencyCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-currency-currencycode"></a>
### GET /sni/api/v1/currency/{currencyCode}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `CurrencyResponse`
- Modèles : [CurrencyResponse](#m-CurrencyResponse)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CurrencyController.java`, méthode `getCurrencyByCode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `currencyCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-currency-currencycode"></a>
### PUT /sni/api/v1/currency/{currencyCode}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `CURRENCY_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `CurrencyRequest`
- Réponse : `CurrencyResponse`
- Modèles : [CurrencyResponse](#m-CurrencyResponse), [CurrencyRequest](#m-CurrencyRequest)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/CurrencyController.java`, méthode `updateCurrency`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `currencyCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CurrencyResponse"></a>
#### CurrencyResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currencyCode` | texte |  |  |
| `currencyName` | texte |  |  |

<a id="m-CurrencyRequest"></a>
#### CurrencyRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currencyCode` | texte |  | longueur min `0`, longueur max `3` |
| `currencyName` | texte |  |  |
