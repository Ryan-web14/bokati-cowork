-- =====================================================================================
-- Lot 1 · Verrouillage et idempotence des modules monetaires (portefeuille + facturation)
--
-- Contexte : features/payment et features/billing etaient les deux seuls modules a muter
-- des montants sans aucun verrou (0 @Lock / 0 @Version), la ou inventory et ressource
-- utilisent PESSIMISTIC_WRITE depuis le depart. Trois classes de bug en decoulaient :
--
--   1. Perte de mise a jour · deux debits concurrents de 800 sur un solde de 1000
--      passaient tous les deux le controle de solde et laissaient 200 au lieu de
--      refuser le second (WalletLedgerService.debit, lecture-modification-ecriture).
--   2. Double credit · aucune contrainte d'unicite sur les ecritures de grand livre,
--      donc un callback PawaPay rejoue ou un double-clic sur une recharge creditait
--      deux fois (features/payment n'utilisait @Idempotent nulle part).
--   3. Double allocation · un paiement pouvait etre impute deux fois a la meme facture.
--
-- Cette migration pose les garde-fous en base ; le code applicatif pose le verrou.
-- =====================================================================================


-- -------------------------------------------------------------------------------------
-- 1. Verrou optimiste sur le compte portefeuille
--    Seconde ligne de defense derriere le SELECT ... FOR UPDATE pose par le code : tout
--    chemin d'ecriture qui oublierait le verrou pessimiste echouera au commit au lieu
--    d'ecraser silencieusement le solde d'une transaction concurrente.
-- -------------------------------------------------------------------------------------
ALTER TABLE wallet_account
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;


-- -------------------------------------------------------------------------------------
-- 2. Cle d'idempotence sur les ecritures de grand livre
--    Meme schema que email_delivery_log.dedup_key (V204) : colonne nullable + index
--    unique partiel. Une ecriture sans source identifiable (recharge admin sans
--    reference) garde une cle NULL et n'est donc pas dedupliquee - c'est volontaire,
--    on ne peut pas distinguer deux recharges manuelles legitimes du meme montant.
--    Les lignes existantes gardent NULL : la deduplication n'est pas retroactive.
-- -------------------------------------------------------------------------------------
ALTER TABLE wallet_ledger_entry
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(180);

CREATE UNIQUE INDEX IF NOT EXISTS ux_wallet_ledger_idempotency_key
    ON wallet_ledger_entry (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- Le releve de compte est trie par date decroissante sur un portefeuille donne
-- (WalletLedgerEntryRepository.findByWallet_IdOrderByCreatedAtDesc) sans index dedie.
CREATE INDEX IF NOT EXISTS idx_wallet_ledger_wallet_created
    ON wallet_ledger_entry (wallet_id, created_at DESC);


-- -------------------------------------------------------------------------------------
-- 3. Invariants de solde en base
--    NOT VALID : la contrainte s'applique a toute insertion ou mise a jour future sans
--    rejouer la validation sur l'historique. Un deploiement ne peut donc pas echouer a
--    cause d'une ligne heritee incoherente. La validation de l'historique est faite
--    separement par le worker de reconciliation (WalletReconciliationWorker), qui
--    signale les ecarts au lieu de bloquer la mise en production.
--
--    ledger_balance = available_balance + held_balance est l'invariant central :
--      credit       ledger+X  available+X
--      debit        ledger-X  available-X
--      placeHold              available-X  held+X
--      captureHold  ledger-X               held-X
--      releaseHold            available+X  held-X
-- -------------------------------------------------------------------------------------
ALTER TABLE wallet_account
    DROP CONSTRAINT IF EXISTS ck_wallet_account_non_negative;
ALTER TABLE wallet_account
    ADD CONSTRAINT ck_wallet_account_non_negative
    CHECK (available_balance >= 0 AND held_balance >= 0) NOT VALID;

ALTER TABLE wallet_account
    DROP CONSTRAINT IF EXISTS ck_wallet_account_balance_split;
ALTER TABLE wallet_account
    ADD CONSTRAINT ck_wallet_account_balance_split
    CHECK (ledger_balance = available_balance + held_balance) NOT VALID;

-- Un proprietaire ne peut avoir qu'un seul portefeuille par devise ; getOrCreateWallet
-- s'appuie sur findByOwnerAndCurrency sans que rien ne garantisse l'unicite, donc deux
-- appels concurrents pouvaient creer deux portefeuilles pour le meme couple.
CREATE UNIQUE INDEX IF NOT EXISTS ux_wallet_account_owner_currency
    ON wallet_account (owner_type, owner_code, currency);


-- -------------------------------------------------------------------------------------
-- 4. Unicite des allocations de paiement
--    allocateOne() cree une allocation par (transaction, facture). Un rejeu qui
--    passerait la garde de statut en creerait une seconde et gonflerait le montant
--    paye de la facture.
--
--    Un index unique ne peut pas etre pose NOT VALID. Si des doublons herites existent
--    (0 en dev, a verifier en prod), on emet un avertissement visible dans le log de
--    deploiement plutot que de faire echouer la migration sur des donnees monetaires.
-- -------------------------------------------------------------------------------------
DO $$
DECLARE
    duplicate_groups INTEGER;
BEGIN
    SELECT COUNT(*) INTO duplicate_groups
    FROM (
        SELECT payment_transaction_id, billing_document_number
        FROM payment_allocation
        GROUP BY 1, 2
        HAVING COUNT(*) > 1
    ) d;

    IF duplicate_groups > 0 THEN
        RAISE WARNING 'V206 : % groupe(s) d''allocations en double detecte(s) · index unique ux_payment_allocation_txn_document NON cree. Corriger les doublons puis creer l''index manuellement.', duplicate_groups;
    ELSE
        CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_allocation_txn_document
            ON payment_allocation (payment_transaction_id, billing_document_number);
    END IF;
END $$;
