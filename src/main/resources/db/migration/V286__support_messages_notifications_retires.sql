-- Support : retrait des notifications de la plateforme enregistrees comme messages du client.
--
-- Les courriels automatiques (nouveau message, delai depasse...) partent de la boite du support et
-- y revenaient quand elle en etait aussi destinataire ; le lecteur de la boite les ajoutait au
-- ticket comme des reponses du client. Il les ecarte desormais ; ceux deja enregistres sont retires
-- ici. On les reconnait a leur contenu : l'en-tete des courriels de la plateforme, et le pied des
-- notifications de ticket. Une reponse de client commence par ses propres mots, elle n'est pas visee.

CREATE TEMP TABLE support_notification_messages ON COMMIT DROP AS
SELECT m.id
FROM ticket_message m
WHERE m.sender_type = 'CLIENT'
  AND m.message LIKE 'Elle A Os%Service client%'
  AND m.message LIKE '%Ticket de r_f_rence%'
  AND m.message LIKE '%votre message sera ajout_ _ votre ticket%';

-- Une piece jointe rattachee a l'un d'eux reste au ticket, sans message
UPDATE ticket_attachment SET message_id = NULL
WHERE message_id IN (SELECT id FROM support_notification_messages);

DELETE FROM ticket_message WHERE id IN (SELECT id FROM support_notification_messages);
