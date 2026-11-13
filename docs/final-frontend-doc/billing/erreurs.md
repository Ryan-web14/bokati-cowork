# billing : erreurs et codes stables (lot B10)

> Retour au [README du module](README.md). Forme commune des erreurs : [README racine](../README.md).

Les messages de la facturation sont en français accentué. Une erreur liée à une règle porte en plus un **code stable** dans le champ `reason` de la réponse : le frontend teste `reason`, jamais le texte du message.

```json
{ "errorCode": "BAD_REQUEST", "status": 400, "message": "Un acompte se demande sur un devis accepté", "reason": "QUOTE_NOT_ACCEPTED", "traceId": "..." }
```

| `reason` | Situation |
| --- | --- |
| `BILLING_DOCUMENT_NOT_EDITABLE` | Le statut ne permet plus de modifier le document |
| `BILLING_DOCUMENT_NO_LINES` | Dupliquer un document sans ligne |
| `BILLING_DOCUMENT_NUMBER_REQUIRED` | Numéro de document manquant |
| `QUOTE_CLOSED` | Options d'un devis converti ou annulé |
| `QUOTE_NOT_NEGOTIABLE` | Négociation impossible dans ce statut |
| `QUOTE_NOT_ACCEPTED` | Acompte demandé sur un devis non accepté |
| `QUOTE_NO_DEPOSIT_REQUESTED` | Marquer payé un acompte jamais demandé |
| `QUOTE_NOT_ACCEPTABLE` | Accepter un devis dans un statut qui ne le permet pas |
| `QUOTE_CONVERTED` | Refuser un devis converti |
| `QUOTE_SIGNATURE` | Signature sur un devis sorti du circuit ou révisé (`409`) |
| `NOT_A_QUOTE` | Action de devis sur un autre type |
| `NOT_A_DRAFT` | Émettre autre chose qu'un brouillon |
| `NOT_SENDABLE` | Envoyer un document annulé ou remboursé |
| `NOT_PAYABLE` | Encaisser autre chose qu'une facture, ou un document clos |
| `PAYMENT_AMOUNT`, `REVERSAL_AMOUNT` | Montant de paiement ou d'annulation non positif |
| `CREDIT_NOTE_AMOUNT` | Montant d'avoir invalide |
| `CREDIT_NOTE_NOT_LINKED` | Avoir rattaché à aucune facture |
| `CREDIT_NOTE` | Avoir non validé ou déjà consommé (`409`) |
| `INVOICE_CHANGE` | Facture payée entre la demande de modification et l'accord (`409`) |
| `PAYMENT_SCHEDULE` | Échéancier actif déjà présent (`409`) |
| `CUSTOMER_TYPE_REQUIRED`, `CUSTOMER_CODE_REQUIRED`, `CUSTOMER_NAME_REQUIRED`, `CUSTOMER_REQUIRED` | Client incomplet |
| `CUSTOMER_NO_EMAIL` | Client sans adresse électronique |
| `NEGATIVE_TOTAL`, `NEGATIVE_RATE` | Total ou taux négatif |
| `LINE_QUANTITY`, `LINE_PRICE`, `LINE_DISCOUNT` | Ligne invalide |
| `RECOVERABLE_CLOSED`, `RECOVERABLE_OTHER_DOCUMENT` | Objet à récupérer déjà clos ou d'un autre document |

Les refus qui portent un montant ou une liste (stock manquant, plafond d'encours, remise hors limites) sont annoncés à l'avance par la simulation (`alerts[].code`, voir [saisie et simulation](saisie-et-simulation.md)) : afficher l'alerte pendant la saisie plutôt que d'attendre le refus.

## Routes dépréciées

Les routes `/sni/api/v1/billing/quotations/...` répondent encore jusqu'au 13/05/2027 avec les en-têtes `Deprecation: true`, `Sunset` et `Link` vers la route `/billing/quotes/...` qui les remplace. N'utiliser que `/billing/quotes/...`.

## Compléments (lots C0 à C16)

| Code | Où | Sens |
| --- | --- | --- |
| `REFERENCE_INCONNUE` | Article, réclamation | Valeur absente de la liste (mode strict) ou motif inconnu |
| `REFERENCE_INACTIVE` | Article | Entrée désactivée (mode strict) |
| `REFERENCE_UTILISEE` | Listes | Suppression d'une entrée encore utilisée |
| `REFERENCE_EXISTANTE`, `REFERENCE_INVALIDE`, `FUSION_IMPOSSIBLE` | Listes | Code déjà pris, libellé vide, fusion impossible |
| `TAXE_INVALIDE`, `TAXE_EXISTANTE` | Règles de taxe | Règle incohérente ou déjà existante |
| `DEROGATION_EN_ATTENTE` (409) | Émission, révision de devis | Remise hors limites : demande d'approbation partie ou en attente |
| `DEROGATION_MOTIF_REQUIS`, `DEROGATION_REFUSEE` | Idem | Motif manquant ; dérogation refusée |
| `LIVRAISON_*`, `RIEN_A_LIVRER` | Bons de livraison | Voir [livraisons](livraisons.md) |
| `RETENUE_*` | Retenues | Voir [retenues](retenues.md) |
| `CAUTION_*` | Cautions | Voir [cautions](cautions.md) |
| `TARIF_INVALIDE` | Tarifs clients | Prix et taux ensemble, dates incohérentes |
| `AFFECTATION_*` | Paiements en attente | Voir [paiements en attente](paiements-en-attente.md) |
| `RECLAMATION_*` | Réclamations | Voir [réclamations](reclamations.md) |
| `PROMESSE_*`, `RECOUVREMENT_IMPOSSIBLE` | Recouvrement | Voir [recouvrement](recouvrement.md) |
| `LANCEMENT_*`, `RIEN_A_FACTURER` | Facturation groupée | Voir [facturation groupée](facturation-groupee.md) |
| `MODELE_INACTIF`, `COMPOSANT_INVALIDE` | Devis types | Voir [devis types](devis-types.md) |
| `MOTIF_REQUIS` | Plusieurs | Motif obligatoire manquant |

