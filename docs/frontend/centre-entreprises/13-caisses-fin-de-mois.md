# Clôture de caisse en fin de mois et rapport de clôture

**Fin de mois.** Chaque nuit à 00:05 (heure de Brazzaville,
`bokati.payment.cash-session.month-end-cron`), le système clôture toute session encore ouverte
avant le premier du mois en cours, puis la renouvelle :

- caisse tenue à la main : la session est close au montant attendu, sans montant compté ni écart
  (`countedClosingAmount` et `varianceAmount` vides, `closedBy = SYSTEM`, motif dans
  `varianceReason`) ; une nouvelle session s'ouvre au nom du même caissier, avec ce montant en
  fonds de caisse (mouvement `OPENING_FLOAT` « Report de la session … ») ;
- caisse automatique (mobile money, portefeuille) : close et rouverte, sans report.

Le 1er au matin, ce sont les sessions du mois écoulé ; un passage manqué est rattrapé au suivant.

**Rapport de clôture.** À chaque clôture définitive, le rapport de clôture en PDF part par e-mail
aux comptes `SUPER_ADMIN`, `ADMIN` et `MANAGER` actifs et au responsable de la caisse
(`managerEmail`, à défaut le superviseur `bokati.payment.cash-session.supervisor-email`) :

| Clôture | Qui |
| --- | --- |
| Par le caissier, sans écart | dès la clôture |
| Par le caissier, avec écart | à la validation de l'écart (la demande de validation part au superviseur, comme avant) |
| Par le système, seuil d'une caisse automatique | à chaque seuil atteint |
| Par le système, fin de mois | à la clôture, avec la session ouverte à sa suite |

Désactivable par `bokati.payment.cash-session.closing-report.enabled=false`.
