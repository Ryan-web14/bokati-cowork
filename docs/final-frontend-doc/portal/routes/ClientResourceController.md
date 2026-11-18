# ClientResourceController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-client-resources"></a>
### GET /sni/api/v1/client/resources

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `PaginatedResponse<ClientResourceSummaryResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ClientResourceSummaryResponse](#m-ClientResourceSummaryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/resource/controller/ClientResourceController.java`, méthode `listResources`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `type` | requête | `String` |  |  |
| `group` | requête | `String` |  |  |
| `q` | requête | `String` |  |  |
| `capacity` | requête | `Integer` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=displayOrder,ASC |

<a id="r-get-sni-api-v1-client-resources-code"></a>
### GET /sni/api/v1/client/resources/{code}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientResourceDetailResponse`
- Modèles : [ClientResourceDetailResponse](#m-ClientResourceDetailResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/resource/controller/ClientResourceController.java`, méthode `getResource`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-resources-code-calendar"></a>
### GET /sni/api/v1/client/resources/{code}/calendar

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `PublicResourceCalendarResponse`
- Modèles : [PublicResourceCalendarResponse](#m-PublicResourceCalendarResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/resource/controller/ClientResourceController.java`, méthode `getCalendar`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` | oui |  |
| `to` | requête | `LocalDate` | oui |  |

<a id="r-get-sni-api-v1-client-resources-code-quote"></a>
### GET /sni/api/v1/client/resources/{code}/quote

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ResourcePriceQuoteResponse`
- Modèles : [ResourcePriceQuoteResponse](#m-ResourcePriceQuoteResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/resource/controller/ClientResourceController.java`, méthode `getQuote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `bookingUnit` | requête | `String` |  |  |
| `startedAt` | requête | `LocalDateTime` | oui |  |
| `endedAt` | requête | `LocalDateTime` | oui |  |
| `quantity` | requête | `Integer` |  | 1 |

<a id="r-get-sni-api-v1-client-resources-code-slots"></a>
### GET /sni/api/v1/client/resources/{code}/slots

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ResourceAvailabilityWindowResponse>`
- Modèles : [ResourceAvailabilityWindowResponse](#m-ResourceAvailabilityWindowResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/resource/controller/ClientResourceController.java`, méthode `getAvailableSlots`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `startedAt` | requête | `LocalDateTime` | oui |  |
| `endedAt` | requête | `LocalDateTime` | oui |  |
| `durationMinutes` | requête | `Integer` |  | 30 |
| `quantity` | requête | `Integer` |  | 1 |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ClientResourceSummaryResponse"></a>
#### ClientResourceSummaryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `typeName` | texte |  |  |
| `capacity` | entier |  |  |
| `zone` | texte |  |  |
| `locationLabel` | texte |  |  |
| `displayOrder` | entier |  |  |
| `amenityNames` | texte[] |  |  |
| `baseHourlyPrice` | entier |  |  |

<a id="m-ClientResourceDetailResponse"></a>
#### ClientResourceDetailResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `typeName` | texte |  |  |
| `capacity` | entier |  |  |
| `zone` | texte |  |  |
| `locationLabel` | texte |  |  |
| `displayOrder` | entier |  |  |
| `minBookingDurationMinutes` | entier |  |  |
| `maxBookingDurationMinutes` | entier |  |  |
| `minBookingNoticeMinutes` | entier |  |  |
| `cancellationNoticeMinutes` | entier |  |  |
| `allowCancellation` | booléen |  |  |
| `amenities` | [AmenityResponse](#m-AmenityResponse)[] |  |  |
| `pricingRules` | [ResourcePricingRuleResponse](#m-ResourcePricingRuleResponse)[] |  |  |
| `bookingOnRequest` | booléen |  |  |

<a id="m-PublicResourceCalendarResponse"></a>
#### PublicResourceCalendarResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `fromDate` | date AAAA-MM-JJ |  |  |
| `toDate` | date AAAA-MM-JJ |  |  |
| `days` | [PublicResourceCalendarResponse.Day](#m-PublicResourceCalendarResponse-Day)[] |  |  |

<a id="m-ResourcePriceQuoteResponse"></a>
#### ResourcePriceQuoteResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `bookingUnit` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `basePrice` | entier |  |  |
| `finalPrice` | entier |  |  |
| `appliedRuleId` | code identifiant (texte) |  |  |
| `appliedRuleLabel` | texte |  |  |
| `adjustmentType` | texte |  |  |
| `adjustmentValue` | entier |  |  |
| `units` | nombre décimal |  |  |
| `quantity` | entier |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `lines` | [PriceResult.Line](#m-PriceResult-Line)[] |  |  |
| `alternatives` | [PriceResult.Alternative](#m-PriceResult-Alternative)[] |  |  |

<a id="m-ResourceAvailabilityWindowResponse"></a>
#### ResourceAvailabilityWindowResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `durationMinutes` | entier |  |  |
| `remainingCapacity` | entier |  |  |
| `slotCount` | entier |  |  |

<a id="m-AmenityResponse"></a>
#### AmenityResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `active` | booléen |  |  |
| `quantity` | entier |  |  |
| `optional` | booléen |  |  |
| `extraPrice` | entier |  |  |

<a id="m-ResourcePricingRuleResponse"></a>
#### ResourcePricingRuleResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `resourceCode` | texte |  |  |
| `bookingUnit` | texte |  |  |
| `price` | entier |  |  |
| `label` | texte |  |  |
| `dayOfWeek` | entier |  |  |
| `startsAt` | [LocalTime](#m-LocalTime) |  |  |
| `endsAt` | [LocalTime](#m-LocalTime) |  |  |
| `adjustmentType` | texte |  |  |
| `adjustmentValue` | entier |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `lastMinuteMinutes` | entier |  |  |
| `priority` | entier |  |  |
| `daysOfWeek` | texte |  |  |
| `customerSegment` | texte |  |  |
| `planCode` | texte |  |  |
| `minParticipants` | entier |  |  |
| `maxParticipants` | entier |  |  |
| `minDurationMinutes` | entier |  |  |
| `maxDurationMinutes` | entier |  |  |
| `minLeadMinutes` | entier |  |  |
| `channel` | texte |  |  |
| `holidayMode` | texte |  |  |
| `audience` | [AudienceRef](#m-AudienceRef)[] |  |  |
| `stackable` | booléen |  |  |
| `perParticipant` | booléen |  |  |
| `active` | booléen |  |  |

<a id="m-PublicResourceCalendarResponse-Day"></a>
#### PublicResourceCalendarResponse.Day

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `totalSlots` | entier |  |  |
| `availableSlots` | entier |  |  |
| `totalCapacity` | entier |  |  |
| `remainingCapacity` | entier |  |  |
| `status` | texte |  |  |
| `windows` | [PublicResourceCalendarResponse.Window](#m-PublicResourceCalendarResponse-Window)[] |  |  |
| `dayLabel` | texte |  |  |

<a id="m-PriceResult-Line"></a>
#### PriceResult.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `from` | texte |  |  |
| `to` | texte |  |  |
| `units` | nombre décimal |  |  |
| `unitPrice` | nombre décimal |  |  |
| `baseRuleId` | code identifiant (texte) |  |  |
| `baseRuleLabel` | texte |  |  |
| `adjustments` | [PriceResult.Adjustment](#m-PriceResult-Adjustment)[] |  |  |
| `amount` | nombre décimal |  |  |

<a id="m-PriceResult-Alternative"></a>
#### PriceResult.Alternative

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `unit` | enum : `HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH` |  |  |
| `amount` | nombre décimal |  |  |
| `chosen` | booléen |  |  |

<a id="m-LocalTime"></a>
#### LocalTime

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `hour` | entier |  |  |
| `minute` | entier |  |  |
| `second` | entier |  |  |
| `nano` | entier |  |  |

<a id="m-AudienceRef"></a>
#### AudienceRef

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte |  |  |
| `code` | texte |  |  |

<a id="m-PublicResourceCalendarResponse-Window"></a>
#### PublicResourceCalendarResponse.Window

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `slotDurationMinutes` | entier |  |  |
| `totalCapacity` | entier |  |  |
| `remainingCapacity` | entier |  |  |
| `available` | booléen |  |  |
| `active` | booléen |  |  |
| `subscribersOnly` | booléen |  |  |

<a id="m-PriceResult-Adjustment"></a>
#### PriceResult.Adjustment

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ruleId` | code identifiant (texte) |  |  |
| `label` | texte |  |  |
| `type` | texte |  |  |
| `value` | entier |  |  |
