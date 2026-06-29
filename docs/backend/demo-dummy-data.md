# Demo Dummy Data

Migration: `V35__seed_demo_dummy_data.sql`

Ces donnees servent aux tests manuels des endpoints en environnement local/dev. Elles sont idempotentes avec `WHERE NOT EXISTS`.

## Comptes

Mot de passe pour tous les comptes seedes: `password`

| Email | Role | Usage |
| --- | --- | --- |
| `admin.demo@sni-cg.com` | `ADMIN` | Back-office admin |
| `staff.demo@sni-cg.com` | `STAFF` | Back-office staff |
| `member.demo@sni-cg.com` | `MEMBER` | Portail member actif |
| `pending.member.demo@sni-cg.com` | `MEMBER` | Portail member KYC en cours |

## Customers Et Members

| Code | Type | Statut | Usage |
| --- | --- | --- | --- |
| `CUS-DEMO-PERSON-01` | `PERSON` | `ACTIVE` | Customer personne avec member actif |
| `CUS-DEMO-COMPANY-01` | `COMPANY` | `PENDING` | Customer entreprise en attente KYC |
| `MEM-DEMO-ACTIVE-01` | Member | `ACTIVE` | Test `/members/me/profile`, contrats, KYC approuve |
| `MEM-DEMO-PENDING-01` | Member | `PENDING` | Test correction/revue KYC |

## Business

| Code | Statut | Usage |
| --- | --- | --- |
| `BUS-DEMO-01` | `ACTIVE` | Contrats, documents business, company admin |

## Resources

| Code | Type | Usage |
| --- | --- | --- |
| `RES-DEMO-ROOM-01` | `ROOM` | Disponibilites, amenities, reservation |
| `RES-DEMO-DESK-01` | `DESK` | Listing/search resource |

Amenities:

| Code | Usage |
| --- | --- |
| `WIFI` | Amenity standard |
| `PROJECTOR` | Amenity salle |

Policy:

| Code | Details |
| --- | --- |
| `DEMO-POLICY-STANDARD` | Slots 30 minutes, min 30, max 240 |

## KYC Et Documents

| Code | Statut | Usage |
| --- | --- | --- |
| `KYC-DEMO-MEMBER-APPROVED` | `APPROVED` | KYC complet |
| `KYC-DEMO-MEMBER-PENDING` | `IN_PROGRESS` | KYC incomplet |
| `DOC-DEMO-MEMBER-ID-APPROVED` | `APPROVED` | Document KYC valide |
| `DOC-DEMO-MEMBER-ID-PENDING` | `PENDING_REVIEW` | Document a revoir |

Les fichiers physiques ne sont pas crees. Les documents seedes sont des metadonnees de test. Pour tester l'antivirus, MIME et stockage reel, utiliser l'endpoint multipart d'upload document.

## Contract

| Code | Statut | Usage |
| --- | --- | --- |
| `CTR-DEMO-ACTIVE-01` | `ACTIVE` | Listing, detail, workflow contract |

Parties:

| Party | Role |
| --- | --- |
| `MEM-DEMO-ACTIVE-01` | `SIGNATORY` |
| `BUS-DEMO-01` | `OPERATOR` |

## Notes Schema

Certaines colonnes presentes dans la migration ne sont pas exposees par les entites actuelles mais existent encore dans les anciennes migrations SQL et peuvent etre `NOT NULL`, par exemple `customer.fullname`, `member.address_id` et `document.type_code`. Elles sont renseignees uniquement pour respecter le schema SQL live historique.
