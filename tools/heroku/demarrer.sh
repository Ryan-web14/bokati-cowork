#!/bin/sh
# Lancement du dyno web (Procfile). Options de la JVM : tools/heroku/jvm.options, communes avec
# l'entrainement du cache (preparer-demarrage.sh) ; un cache n'est valable qu'avec les memes options.
#
# - jar extrait au build (target/extracted) : classes lues dans des jars ordinaires, plus rapide que
#   le jar imbrique ;
# - cache AOT (target/app.aot) : classes deja chargees et liees, entraine au build ;
# - a defaut (build sans le profil heroku), le jar complet, comme avant.
cd "$(dirname "$0")/../.."
jar=target/extracted/bokati-cowork-0.0.1-SNAPSHOT.jar
cache=
if [ -f "$jar" ]; then
  if [ -f target/app.aot ]; then
    cache=-XX:AOTCache=target/app.aot
    echo "Demarrage : jar extrait, cache AOT"
  else
    echo "Demarrage : jar extrait, sans cache AOT"
  fi
else
  jar=target/bokati-cowork-0.0.1-SNAPSHOT.jar
  echo "Demarrage : jar complet"
fi
export MALLOC_ARENA_MAX=2
exec java @tools/heroku/jvm.options $cache -Dserver.port="${PORT:-8080}" -jar "$jar"
