# Secrets · inventaire, variables d'environnement, et nettoyage de l'historique

Les secrets qui étaient écrits en dur dans les `application*.yml` et dans le code Java ont été
retirés. Ce document liste ce qu'il faut poser où, et ce qu'il faut rotationner.

**Le point à retenir avant tout le reste : retirer un secret du dépôt ne le rend pas secret.**
Tout ce qui figure ci-dessous avec la mention « compromis » est public pour quiconque a eu accès au
dépôt, et le restera après le nettoyage de l'historique. Seule la **rotation** les invalide. Le
nettoyage de l'historique sert à ne pas en exposer de nouveaux, pas à réparer ceux-là.

---

## 1. Secrets compromis · à rotationner

Ils étaient en clair dans des fichiers suivis par git, donc dans l'historique.

| Variable | Ce qu'elle protège | Où elle était | Conséquence d'une fuite |
|---|---|---|---|
| `APP_JWT_SECRET` | Signature de tous les jetons | `application-dev.yml` + `JWTService.java` | **Forger un jeton avec les rôles de son choix** sur toute instance qui ne surchargeait pas |
| `BILLING_FISCAL_SIGNING_KEY` | Signature fiscale SEFC des factures | `application.yml` | **Forger la signature d'un document**, celle que `/verify/doc/{n}?k=` contrôle |
| `MICROSOFT_GRAPH_CLIENT_SECRET` | Application Azure AD d'envoi de courriels | `application-dev.yml` | **Envoyer du courrier au nom du domaine**, et tout ce que l'application a comme droits sur le locataire |
| `TOKEN_HASH_SECRET` | Hachage des jetons stockés | `application-dev.yml` | Retrouver un jeton depuis son empreinte en base |
| `PAWAYPAY_API_KEY` | Compte marchand mobile money | `application-dev.yml` et `.env` | **Dépenser et encaisser** avec le compte marchand · déjà signalé, toujours pas révoquée |

Les identifiants du locataire Azure (`MICROSOFT_GRAPH_CLIENT_ID`, `MICROSOFT_GRAPH_TENANT_ID`) ne
sont pas des secrets, mais ils étaient là aussi et désignent la cible · ils sont désormais exigés
de l'environnement par cohérence.

### Ordre de rotation

1. **`PAWAYPAY_API_KEY`** · c'est de l'argent. Révoquer sur le tableau de bord PawaPay, créer la
   nouvelle, poser sur Heroku.
2. **`MICROSOFT_GRAPH_CLIENT_SECRET`** · régénérer dans le portail Azure (App registrations →
   Certificates & secrets). L'ancien continue de fonctionner jusqu'à sa suppression · poser le
   nouveau avant de supprimer l'ancien, sinon le courrier s'arrête.
3. **`BILLING_FISCAL_SIGNING_KEY`** · **à traiter avec précaution.** Les documents déjà signés
   l'ont été avec l'ancienne clé ; les rotationner rendrait leur vérification impossible. À
   arbitrer avec la contrainte fiscale · soit la clé reste et le risque est accepté, soit un
   mécanisme de clé versionnée est introduit (identifiant de clé stocké avec la signature).
   **C'est le seul point de cette liste qui demande une décision métier, pas technique.**
4. **`APP_JWT_SECRET`** · la rotation invalide tous les jetons en circulation, donc déconnecte tout
   le monde. À une heure creuse.
5. **`TOKEN_HASH_SECRET`** · la rotation invalide les jetons déjà hachés en base (réinitialisations
   de mot de passe, codes à usage unique en cours). Même heure creuse.

---

## 2. Ce qui doit être posé dans l'environnement Heroku

Les variables **sans valeur par défaut** : l'application refuse de démarrer sans elles. C'est
voulu · une instance mal configurée doit s'arrêter, pas démarrer avec un secret public.

```bash
heroku config:set --app <votre-app> \
  APP_JWT_SECRET='<nouveau, 64 caracteres aleatoires>' \
  TOKEN_HASH_SECRET='<nouveau, 32 caracteres aleatoires>' \
  BILLING_FISCAL_SIGNING_KEY='<voir §1.3 avant de changer>' \
  MICROSOFT_GRAPH_CLIENT_ID='<identifiant application Azure>' \
  MICROSOFT_GRAPH_TENANT_ID='<identifiant locataire Azure>' \
  MICROSOFT_GRAPH_CLIENT_SECRET='<nouveau secret Azure>' \
  PAWAYPAY_API_KEY='<nouvelle cle PawaPay>'
```

Déjà exigées avant ce lot, à vérifier présentes :

```bash
heroku config --app <votre-app> | grep -E 'APP_BASE_URL|JDBC_DATABASE|CLOUDAMQP_URL'
```

`APP_BASE_URL`, `JDBC_DATABASE_URL`, `JDBC_DATABASE_USERNAME`, `JDBC_DATABASE_PASSWORD`,
`CLOUDAMQP_URL`.

### À renseigner aussi · sans elles la fonction est inerte, mais l'application démarre

| Variable | Sans elle |
|---|---|
| `PAWAYPAY_CALLBACK_SECRET` | Chaque rappel de l'opérateur coûte un appel d'API de reconfirmation |
| `PAWAYPAY_ALERT_EMAIL` | Un dépôt mobile money sans réponse ne prévient **personne** |
| `CASH_DECLARATION_DESK_EMAIL` | Les espèces annoncées retombent sur l'adresse du support |
| `CHECKIN_SCANNER_KEY` | Le check-in par scanner est désactivé |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | Vide en prod · le stockage passe par `BUCKETEER_*` ou `AWS_*`, déjà en place |

### À vérifier impérativement

```bash
heroku config:get SPRING_PROFILES_ACTIVE --app <votre-app>   # doit valoir prod
heroku config:get AUTO_ADMIN_SECURITY_CONTEXT_ENABLED --app <votre-app>   # doit valoir false
```

Le profil est ce qui met `auto-admin` à `false` et le jeton d'accès à 15 minutes. S'il n'est pas
`prod`, l'application démarre avec les défauts de développement · voir `audit-securite.md` §0.

---

## 3. Ce qui est déjà en place en local

Le `.env` (non suivi par git) portait déjà la plupart des variables. Ont été ajoutées pour que le
développement continue de fonctionner après le retrait des valeurs par défaut :

- `CHECKIN_SCANNER_KEY` · une clé aléatoire a été générée
- `BOKATI_STOMP_CLIENT_LOGIN` / `CLIENT_PASSCODE` / `SYSTEM_LOGIN` / `SYSTEM_PASSCODE` · `guest`,
  qui est la valeur du courtier local

Vérifié : le contexte Spring démarre avec le seul `.env`, sans aucune valeur par défaut dans le
code. Aucune erreur de résolution de variable.

**Note sur le `.env`** : il contient toujours les anciens secrets compromis. Après rotation, il
faut y poser les nouveaux · et les anciens y resteront sans danger uniquement parce qu'ils auront
été révoqués.

Un `.env.example` sans valeurs serait utile pour qu'un nouveau poste sache quoi renseigner · il
n'existe pas aujourd'hui.

---

## 4. Nettoyage de l'historique git

### Ce que ça fait, et ce que ça ne fait pas

**Ça ne rend aucun secret secret.** Les valeurs listées en §1 ont été clonées, mises en cache par
GitHub, possiblement lues. Le nettoyage empêche un nouveau lecteur du dépôt de les trouver ; il ne
retire rien à qui les a déjà.

**Ce que ça coûte** : la réécriture change l'identifiant de chaque commit réécrit. Concrètement :

- tous les clones existants deviennent incompatibles · chacun doit re-cloner, pas `git pull` ;
- les branches `new-main` et `feature/payment-facturation` doivent être poussées en force ;
- toute référence à un commit (ticket, note, lien GitHub) pointe dans le vide ;
- GitHub conserve les anciens objets accessibles par leur empreinte un certain temps · il faut
  **demander au support GitHub de purger le cache**, sinon les anciens commits restent lisibles
  par URL directe.

### La séquence, si vous décidez de le faire

L'outil adapté est `git filter-repo` (le successeur de `filter-branch`, bien plus rapide et sûr).

```bash
# 1. Une sauvegarde complete, hors du repertoire de travail
git clone --mirror https://github.com/Ryan-web14/bokati-cowork.git ../bokati-backup.git

# 2. Un clone neuf pour la reecriture · filter-repo refuse un depot avec du travail en cours
git clone https://github.com/Ryan-web14/bokati-cowork.git ../bokati-clean
cd ../bokati-clean

# 3. Le fichier des remplacements · une ligne par secret, valeur litterale
cat > ../remplacements.txt <<'EOF'
Q7mP2xL9vB4nH6sT1yK8dF5wR3cZ0aEQ7mP2xL9vB4nH6sT1yK8dF5wR3cZ0aE==>SECRET_RETIRE
12f70020a5b8526752f2f245f2148ea881223390f356e0e8246ccde588042d8f==>SECRET_RETIRE
jpj8Q~j4pgjkI4DBLCIBN_r3GbCoI7IVEoemfaiE==>SECRET_RETIRE
K8sL2vQ9xM4pZ7tN1cR6aB3eY5uH0jD==>SECRET_RETIRE
EOF
# La cle PawaPay aussi · la recuperer depuis l historique, elle est longue :
#   git log -p -S 'PAWAYPAY_API_KEY' -- src/main/resources/application-dev.yml

# 4. La reecriture
git filter-repo --replace-text ../remplacements.txt

# 5. Verification · doit ne rien rendre
git log --all -p | grep -cE 'Q7mP2xL9|12f70020a5b8|jpj8Q~j4|K8sL2vQ9'

# 6. La poussee en force · irreversible pour les clones existants
git push --force --all
git push --force --tags
```

Puis, et c'est la partie qu'on oublie :

1. **Prévenir tout le monde** avant le point 6 · quiconque a un clone doit le jeter.
2. **Ouvrir un ticket au support GitHub** pour purger les objets détachés et le cache.
3. **Rotationner quand même** tout ce qui est en §1 · le nettoyage ne les protège pas.

### Mon avis

Faites la **rotation d'abord**, et le nettoyage ensuite ou jamais. La rotation est ce qui ferme le
risque ; le nettoyage est de l'hygiène. Si l'on doit choisir entre les deux faute de temps, c'est
la rotation qu'il faut faire · un secret rotationné dans l'historique n'est plus qu'une chaîne de
caractères sans pouvoir.

Je n'ai **rien réécrit ni poussé en force** · la séquence ci-dessus est à exécuter en connaissance
de cause, et elle casse tous les clones existants.

---

## 5. Pour que ça ne revienne pas

- Un **`.env.example`** listant les variables sans leurs valeurs.
- Un **contrôle au démarrage** qui refuse le profil `prod` avec une configuration non sûre · c'est
  le point 5 du lot 1 de `audit-securite.md`.
- Une **analyse de secrets** avant chaque poussée · `gitleaks` ou le scan de secrets GitHub, en
  hook `pre-commit` ou en intégration continue. C'est ce qui aurait évité toute cette liste.
