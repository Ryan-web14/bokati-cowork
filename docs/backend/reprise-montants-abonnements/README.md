# Reprise des montants des anciens abonnements

Migrations : `db/migration/V281__reprise_montants_abonnements.sql` (dev) et
`db/migration-prod/V277__reprise_montants_abonnements.sql` (prod), au contenu identique.

## Le probleme

Avant le correctif du 2026-11-08 (commit `8f552e9`), la creation d'un abonnement enregistrait dans
`subscription.subtotal_amount`, `tax_amount` et `total_amount` le total de la premiere facture :
prix de la periode, frais d'installation et caution. Le renouvellement relit ces montants : chaque
renouvellement refacturait donc les frais et la caution.

Le correctif ne change que les abonnements crees ensuite. Cette migration reprend les anciens.

## Ce que fait la migration

Un abonnement est repris si ses montants sont encore ceux de la creation :

- son total egale la somme de ses elements facturables de souscription
  (`SUBSCRIPTION_RECURRING`, `SUBSCRIPTION_SETUP_FEE`, `DEPOSIT`, a 0,01 pres) ;
- il a au moins des frais d'installation ou une caution.

Un abonnement dont le plan a change depuis porte deja le prix de la periode seul : il n'est pas touche.

Nouveaux montants :

- total : l'element `SUBSCRIPTION_RECURRING`, c'est-a-dire le prix de la periode taxe comprise,
  remises appliquees ;
- sous-total : l'ancien sous-total au prorata de ce prix (la caution n'est pas taxee, les frais
  d'installation le sont au meme taux) ;
- taxe : total moins sous-total.

Les passes n'ont pas besoin de reprise : leur renouvellement recalcule le prix de la periode depuis le plan.

## Trace et retour arriere

La table `subscription_amount_reprise` garde, par abonnement repris, les anciens et les nouveaux
montants, les frais et la caution retires, et les renouvellements deja factures au mauvais montant
(`overbilled_renewals`, `overbilled_amount`). Les factures deja emises ne sont pas modifiees : un
trop-facture se regle par avoir, au cas par cas.

Lister les clients a rembourser :

```sql
SELECT subscription_number, overbilled_renewals, overbilled_amount
FROM subscription_amount_reprise
WHERE overbilled_renewals > 0;
```

Revenir en arriere pour un abonnement :

```sql
UPDATE subscription s
SET subtotal_amount = r.old_subtotal_amount, tax_amount = r.old_tax_amount, total_amount = r.old_total_amount
FROM subscription_amount_reprise r
WHERE r.subscription_id = s.id AND r.subscription_number = 'SUB-...';
```

## Essai sur la base de dev (2026-11-08)

Execute dans une transaction annulee : 12 abonnements repris sur 19, aucun renouvellement deja
trop facture. Exemple : total 300 000 ramene a 200 000 (caution de 100 000 retiree) ; total
160 000 ramene a 145 000 (frais d'installation de 15 000 retires).
