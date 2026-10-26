-- L instant ou un jeton de rafraichissement a ete revoque.
--
-- Un jeton deja consomme qui revient est la signature d un vol · le voleur et le titulaire
-- detiennent la meme valeur, et le premier des deux a s en servir a fait tourner la chaine.
-- Sans date, on ne distinguait pas ce cas de deux onglets qui rafraichissent au meme moment.
-- Les lignes existantes restent sans date · elles ne declenchent jamais l alerte.
ALTER TABLE refresh_token ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP;
