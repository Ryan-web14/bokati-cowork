# billing : changement de client sur une facture (lot B6)

> Retour au [README du module](README.md). Toutes les routes sont sous `/sni/api/v1/billing`.

`POST /invoices/{invoiceNumber}/change-customer` :

```json
{ "customerType": "CUSTOMER", "customerCode": "CUS-...", "reason": "Facturer la maison mère", "payments": "KEEP_AS_CREDIT" }
```

`payments` (décision D6) : `KEEP_AS_CREDIT` (défaut) ou `TRANSFER`.

## Deux cas, choisis par le serveur

**Sur place** (`mode = IN_PLACE`) : facture non certifiée, sans paiement ni écriture comptable. La même facture passe au nouveau client dans une nouvelle version : fiche reprise (nom, adresse, NIU, identifiants, exonération de TVA), recalcul, contrôle du blocage et du plafond d'encours du nouveau client.

**Réémission** (`mode = REISSUE`) : facture certifiée, comptabilisée ou payée. En une action :

1. une nouvelle facture identique au nouveau client (émise, et certifiée si l'originale l'était) ;
2. un avoir du reste dû au client d'origine, imputé sur sa facture ;
3. un avoir des règlements reçus :
   - `KEEP_AS_CREDIT` : gardé au client d'origine, à rembourser ou imputer plus tard ;
   - `TRANSFER` : reporté sur la nouvelle facture (le payeur réel reste tracé), écriture `411` de l'un vers l'autre ;
4. la facture d'origine est annulée, remplacée par la nouvelle.

Réponse `ChangeCustomerResponse` : `mode`, `originalInvoice`, `newInvoice`, `unpaidCreditNote`, `paidCreditNote`, `paymentsMode`, `transferredAmount`.

Écran conseillé : un assistant qui affiche, avant confirmation, ce qui va se passer (sur place, ou avoirs et nouvelle facture) et, si la facture a des paiements, le choix « Garder en avoir au client d'origine » ou « Reporter sur la nouvelle facture ».

## Comptabilité

Chaque avoir et la nouvelle facture produisent leurs écritures sur le `411` de chaque client ; le transfert passe par une pièce `411` d'origine vers `411` du nouveau client. La facture d'origine n'est pas contre-passée une seconde fois (ses avoirs le font).
