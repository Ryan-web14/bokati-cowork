#!/bin/sh
# Build Heroku (profil Maven heroku, phase package) : prepare un demarrage rapide du dyno.
#   preparer-demarrage.sh <dossier target> <java> <nom du jar>
#
# 1. Extrait le jar (java -Djarmode=tools extract) puis supprime le jar complet : le slug garde a peu
#    pres sa taille (limite Heroku 500 Mo).
# 2. Entraine le cache AOT de Java (-XX:AOTCacheOutput) : l'application demarre une fois, jusqu'a la
#    fin de la creation des beans (spring.context.exit=onRefresh), puis s'arrete ; les classes chargees
#    sont gardees dans target/app.aot et reprises a chaque demarrage du dyno.
#    L'entrainement ne touche a rien : environnement vide (aucune variable de l'application ni cle),
#    base et services a une adresse fermee (127.0.0.1:1), Flyway et validation du schema coupes,
#    taches planifiees coupees, secrets aleatoires jetes ensuite.
# Un entrainement en echec ne fait pas echouer le build : le dyno demarre alors sans cache.
set -eu
cible=$1
java=$2
nom=$3
racine=$(cd "$cible/.." && pwd)
jar="$cible/$nom"

echo "-----> Extraction du jar pour le demarrage"
rm -rf "$cible/extracted" "$cible/app.aot" "$cible/app.aotconf"
"$java" -Djarmode=tools -jar "$jar" extract --destination "$cible/extracted"
rm -f "$jar" "$jar.original"

alea() { head -c 48 /dev/urandom | base64 | tr -d '/+=\n'; }
ferme=127.0.0.1:1

echo "-----> Entrainement du cache de demarrage (AOT)"
cd "$racine"
if env -i PATH="$(dirname "$java"):/usr/bin:/bin" LANG=C.UTF-8 "$java" @tools/heroku/jvm.options \
    -XX:AOTCacheOutput=target/app.aot \
    -Dspring.profiles.active=prod \
    -Dspring.context.exit=onRefresh \
    -Dbokati.scheduling.enabled=false \
    -Dspring.flyway.enabled=false \
    -Dspring.jpa.hibernate.ddl-auto=none \
    -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
    -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
    -Dspring.datasource.hikari.initialization-fail-timeout=-1 \
    -Dspring.datasource.hikari.connection-timeout=250 \
    -DJDBC_DATABASE_URL=jdbc:postgresql://$ferme/entrainement \
    -DJDBC_DATABASE_USERNAME=entrainement \
    -DJDBC_DATABASE_PASSWORD=entrainement \
    -DCLOUDAMQP_URL=amqp://entrainement:entrainement@$ferme/entrainement \
    -DREDIS_URL=redis://$ferme \
    -DAPP_BASE_URL=https://entrainement.invalid \
    -DAPP_JWT_SECRET="$(alea)" \
    -DTOKEN_HASH_SECRET="$(alea)" \
    -DBILLING_FISCAL_SIGNING_KEY="$(alea)" \
    -DMICROSOFT_GRAPH_CLIENT_ID=entrainement \
    -DMICROSOFT_GRAPH_CLIENT_SECRET=entrainement \
    -DMICROSOFT_GRAPH_TENANT_ID=entrainement \
    -DMINIO_ENDPOINT=http://$ferme \
    -DMINIO_BUCKET=entrainement \
    -DMINIO_ACCESS_KEY=entrainement \
    -DMINIO_SECRET_KEY=entrainement \
    -jar target/extracted/"$nom" > target/aot-entrainement.log 2>&1; then
  code=0
else
  code=$?
fi
# Le cache est ecrit a la sortie de la JVM, meme apres un echec de demarrage : il reste valable, avec
# moins de classes. Seule son absence prive le dyno du cache.
if [ -f target/app.aot ]; then
  echo "       cache cree : $(du -h target/app.aot | cut -f1) (code de sortie $code)"
else
  echo "       pas de cache (code de sortie $code), le dyno demarrera sans ; fin du journal :"
  tail -n 40 target/aot-entrainement.log | cut -c1-400
fi
rm -f target/app.aotconf target/app.aot.config
# Classes compilees et sources generees : deja dans le jar extrait, inutiles au dyno (taille du slug).
rm -rf target/classes target/test-classes target/generated-sources target/generated-test-sources
