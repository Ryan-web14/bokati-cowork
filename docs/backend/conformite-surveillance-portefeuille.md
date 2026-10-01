# Conformité et surveillance du portefeuille · lot D bis

Détecter n'est pas surveiller. Un signalement qui n'ouvre pas un dossier, qu'aucune personne
nommée ne doit traiter et dont la clôture n'est pas datée ne protège de rien. Ce module fait le
reste. Tout est sous `/sni/api/v1/wallets/compliance`, réservé à `ADMIN` et `SUPER_ADMIN`, et chaque
geste entre dans une piste d'audit chaînée.

## Les règles · des données, pas du code

`compliance_rule` porte les seuils ; le code ne porte que les détecteurs qui savent les lire.
Modifier un seuil (`PUT /rules/{ruleCode}`) exige un motif et laisse une trace avec l'état d'avant et
d'après.

| Règle | Détecteur | Ce qu'elle cherche | Action par défaut |
|---|---|---|---|
| `CR-VELOCITY-COUNT` | `VELOCITY_COUNT` | plus de N sorties sur la fenêtre | FLAG |
| `CR-VELOCITY-AMOUNT` | `VELOCITY_AMOUNT` | cumul des sorties au-delà du seuil | REQUIRE_REVIEW |
| `CR-STRUCTURING` | `STRUCTURING` | N transferts entre `ratio × seuil` et le seuil | REQUIRE_REVIEW |
| `CR-RAPID-IN-OUT` | `RAPID_IN_OUT` | entrée puis sortie d'au moins `ratio` dans la fenêtre | REQUIRE_REVIEW |
| `CR-FAN-OUT` / `CR-FAN-IN` | `FAN_OUT` / `FAN_IN` | trop de contreparties distinctes | FLAG / REQUIRE_REVIEW |
| `CR-CIRCULARITY` | `CIRCULARITY` | A → B → C → A sur la fenêtre | REQUIRE_REVIEW |
| `CR-DORMANT-AMOUNT` | `DORMANT_AMOUNT` | réveil avec une sortie sans rapport avec l'historique | REQUIRE_REVIEW |
| `CR-IDENTITY-LIMIT` | `IDENTITY_LIMIT` | tentatives répétées au-delà du plafond du niveau | FLAG |
| `CR-SHARED-DEVICE` | `SHARED_DEVICE` | un appareil pilote N portefeuilles | REQUIRE_REVIEW |

Actions : `FLAG` signale ; `REQUIRE_REVIEW` ouvre un dossier avec échéance ; `BLOCK_OPERATION`
refuse **avant** exécution (évalué à l'initiation d'un transfert) ; `FREEZE_WALLET` gèle et ouvre un
dossier critique. Les règles tournent après chaque transfert (les deux bouts) et chaque
rechargement, dans leur propre transaction · une règle qui plante n'annule rien.

`POST /wallets/{walletNumber}/evaluate` passe les règles à la demande, pour comprendre ce qu'elles
diraient.

## Le dossier

Un sujet n'a qu'un dossier ouvert à la fois · les signalements suivants s'y ajoutent, et la
priorité ne baisse jamais. L'échéance dépend de la priorité (`bokati.compliance.case.due-hours.*` :
24 h critique, 72 h haute, 7 j moyenne, 14 j basse) et se mesure : les dossiers hors délai remontent
au tableau de bord et dans le journal chaque matin.

```
GET  /cases?scope=open|mine|all
GET  /cases/{n}            /cases/{n}/notes
POST /cases/{n}/assign     {assignee}
POST /cases/{n}/notes      {note}
POST /cases/{n}/escalate   {escalatedTo, reason}
POST /cases/{n}/close      {decision: CLEARED|CONFIRMED|REPORTED, rationale, findings}
GET  /cases/{n}/export?activityDays=90      → JSON complet du dossier
```

La clôture exige un motif d'au moins dix caractères · la base le refuse aussi. Le verdict retourne
aux règles : un dossier écarté compte un faux positif pour chaque règle qui l'alimentait, un
dossier confirmé un succès. C'est le **taux de faux positifs par règle** du tableau de bord, la
mesure qui garde le module vivant.

## Identité et plafonds

Un plafond dépassé ne se refuse plus sèchement. Le refus nomme les pièces qui manquent au dossier
KYC et une demande part par courriel (`WALLET_KYC_DOCUMENTS_REQUESTED`). La simulation de transfert
renvoie la même liste dans `upgradePath`, sans rien envoyer. Un dossier approuvé a déjà son palier ·
la seule voie est la dérogation nominative.

Revérification périodique : les portefeuilles au-dessus de
`bokati.compliance.identity.exposure-threshold` reçoivent une échéance
(`bokati.compliance.identity.review-days`) ; échue, elle ouvre un dossier de priorité basse. Un
changement de numéro de téléphone lève un signalement et déclenche un recontrôle des listes.

## Listes · le contrôle et sa preuve

Chaque contrôle laisse une ligne `screening_check` que la base interdit de réécrire : quelle liste,
quelle version, quelle date, quel résultat · même `CLEAR`, surtout `CLEAR`. Un contrôle dont on ne
peut pas montrer qu'il a eu lieu n'a pas eu lieu.

- À l'inscription, après commit. Périodiquement (`bokati.compliance.screening.recheck-days`). Au
  changement de numéro. À la demande (`POST /screening/checks`).
- `POST /screening/lists` charge une version d'une liste (`SANCTION` ou `PEP`) ; la version
  précédente est désactivée, jamais supprimée.
- Rapprochement large : noms normalisés, mots dans n'importe quel ordre, alias ; une année de
  naissance qui contredit écarte. Un rapprochement ouvre un dossier ; un humain tranche
  (`POST /screening/checks/{n}/review`, motivé).

## Piste d'audit

`compliance_audit_entry` · une seule chaîne SHA-256 pour tout le module, écriture en transaction
propre (ce qu'un administrateur a fait reste écrit même si l'opération a ensuite échoué),
`retain_until` à dix ans, aucun UPDATE ni DELETE possible, **aucune purge écrite**.
`GET /audit/verify` reparcourt la chaîne ; `GET /audit/{subjectType}/{subjectCode}` la lit.

## Gel sur instruction

Distinct de la suspension commerciale. `POST /wallets/{n}/freeze` porte l'autorité, la référence de
l'instruction et le motif ; le compte passe `LOCKED`, un dossier critique s'ouvre. La levée
(`POST /freezes/{n}/lift`) porte sa propre référence et son motif. **La levée ordinaire d'une
suspension refuse un compte gelé sur instruction** · un administrateur ne dégèle pas sans le savoir
ce qu'une autorité a figé.

## Tableau de bord

`GET /dashboard` : dossiers ouverts, hors délai, par priorité et par ancienneté ; rapprochements de
listes non revus ; gels actifs ; règles avec touches, confirmations, écarts, taux de faux positifs et
touches sur trente jours.
