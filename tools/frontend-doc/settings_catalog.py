"""Catalogue des parametres pour le frontend : docs/final-frontend-doc/parametres/cles.md et cles.json.

Lu depuis src/main/resources/settings/platform-settings.tsv (une ligne par reglage) et
SettingDomain.java (libelle et ouverture a ADMIN de chaque domaine). A relancer apres tout ajout
de reglage : python tools/frontend-doc/settings_catalog.py
"""
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
TSV = os.path.join(ROOT, 'src/main/resources/settings/platform-settings.tsv')
ENUM = os.path.join(ROOT, 'src/main/java/com/sni/bokaticowork/core/settings/platform/SettingDomain.java')
OUT = os.path.join(ROOT, 'docs/final-frontend-doc/parametres')

domains = []
for name, label, editable in re.findall(r'^\s+([A-Z_]+)\("([^"]+)",\s*(true|false)\)', open(ENUM, encoding='utf-8').read(), re.M):
    domains.append({'domain': name, 'label': label, 'adminEditable': editable == 'true'})

settings = []
for line in open(TSV, encoding='utf-8'):
    line = line.rstrip('\n')
    if not line or line.startswith('#'):
        continue
    cells = line.split('\t')
    if len(cells) < 6:
        continue
    key, domain, kind, effect, default, label = cells[:6]
    settings.append({'key': key, 'domain': domain, 'type': kind, 'effect': effect, 'default': default, 'label': label})

known = {d['domain'] for d in domains}
unknown = sorted({s['domain'] for s in settings} - known)
if unknown:
    sys.exit('Domaines du catalogue absents de SettingDomain : ' + ', '.join(unknown))

by_domain = {d['domain']: [s for s in settings if s['domain'] == d['domain']] for d in domains}

with open(os.path.join(OUT, 'cles.json'), 'w', encoding='utf-8', newline='\n') as out:
    json.dump({'domains': [dict(d, count=len(by_domain[d['domain']])) for d in domains], 'settings': settings}, out, ensure_ascii=False, indent=1)
    out.write('\n')

lines = ['# Paramètres : domaines et clés', '',
         "> Généré depuis `settings/platform-settings.tsv` et `SettingDomain.java` par `python tools/frontend-doc/settings_catalog.py`. Ne pas modifier à la main. "
         "Même contenu pour un programme : [cles.json](cles.json). Fonctionnement : [README](README.md).", '',
         '`GET /sni/api/v1/admin/settings/domains` et `GET /sni/api/v1/admin/settings?domain=` rendent ces réglages avec leur valeur courante. '
         "Un domaine `adminEditable: false` n'est visible que de `SUPER_ADMIN` (liste vide pour `ADMIN`). "
         "Un paramètre précis : `GET /sni/api/v1/admin/settings/{clé}`.", '',
         '## Domaines', '', '| Domaine | Libellé | Ouvert à `ADMIN` | Réglages |', '| --- | --- | --- | --- |']
for d in domains:
    lines.append(f"| [`{d['domain']}`](#{d['domain'].lower()}) | {d['label']} | {'oui' if d['adminEditable'] else 'non'} | {len(by_domain[d['domain']])} |")
lines.append('')
types = ', '.join('`' + t + '`' for t in sorted({s['type'] for s in settings}))
effects = ', '.join('`' + e + '`' for e in sorted({s['effect'] for s in settings}))
lines.append(f"Types : {types}. Effets : {effects} (`LIVE` : pris en compte tout de suite ; sinon au redémarrage). "
             "Une valeur vide dans « Défaut » : aucune valeur par défaut.")
for d in domains:
    rows = by_domain[d['domain']]
    lines += ['', f"## {d['domain']}", '', f"{d['label']} · {'ouvert à ADMIN' if d['adminEditable'] else 'SUPER_ADMIN seul'} · {len(rows)} réglage(s)", '']
    if not rows:
        lines.append('Aucun réglage dans le catalogue (réglages fournis par un module, s\'il y en a, visibles par la route).')
        continue
    lines += ['| Clé | Type | Effet | Défaut | Libellé |', '| --- | --- | --- | --- | --- |']
    for s in rows:
        default = s['default'].replace('|', '\\|')
        label = s['label'].replace('|', '\\|')
        lines.append(f"| `{s['key']}` | {s['type']} | {s['effect']} | `{default}` | {label} |")
with open(os.path.join(OUT, 'cles.md'), 'w', encoding='utf-8', newline='\n') as out:
    out.write('\n'.join(lines) + '\n')
print(len(domains), 'domaines,', len(settings), 'reglages')
