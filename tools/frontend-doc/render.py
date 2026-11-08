"""Transforme l'inventaire produit par RouteCatalog.java en documentation par module.

Usage : python tools/frontend-doc/render.py <routes.json> <dossier de la doc>

Ecrit, pour chaque module, routes.md (le sommaire), routes/<Controleur>.md (chaque route et
chaque modele qu'elle touche) et routes.json (la meme chose pour une machine), plus MODULES.md a
la racine. Les README.md, rediges a la main, ne sont jamais touches.
"""
import collections
import json
import os
import re
import shutil
import sys

GENERATED = '> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.\n'
REF = '#/components/schemas/'

ACCESS = {
    'public': 'public, sans jeton',
    'member-portal': 'membre connecté au portail (`ROLE_MEMBER` et accès portail activé)',
    'authenticated': 'tout compte connecté',
}


def module_of(package):
    parts = package.split('.')
    if parts[0] == 'features':
        return parts[1]
    if parts[0] == 'security':
        return 'securite'
    if parts[0] == 'core' and parts[1] == 'settings':
        return 'parametres'
    return 'socle'


class Names:
    """Un nom court par type, allonge seulement quand deux types partagent le meme."""

    def __init__(self, full_names):
        self.short = {}
        groups = collections.defaultdict(list)
        for name in dict.fromkeys(full_names):
            groups[self._base(name, 1)].append(name)
        for base, names in groups.items():
            if len(names) == 1:
                self.short[names[0]] = base
                continue
            depth = 2
            while len({self._base(n, depth) for n in names}) < len(names) and depth < 8:
                depth += 1
            for n in names:
                self.short[n] = self._base(n, depth)

    @staticmethod
    def _base(name, depth):
        parts = name.split('.')
        # Une classe imbriquee garde le nom de sa classe englobante : SupportDtos.TicketView.
        start = len(parts) - 1
        while start > 0 and parts[start - 1][:1].isupper():
            start -= 1
        start = max(0, start - (depth - 1))
        return '.'.join(parts[start:])

    def __call__(self, name):
        return self.short.get(name, name.split('.')[-1])


def strip_entity(type_name):
    match = re.fullmatch(r'ResponseEntity<(.*)>', type_name)
    type_name = match.group(1) if match else type_name
    return {'binary': 'fichier (octets)', '?': 'non typé, voir le code', 'void': 'vide'}.get(type_name, type_name)


def type_refs(type_name):
    return re.findall(r'com\.sni[\w.]+', type_name)


def shorten(type_name, names):
    return re.sub(r'com\.sni[\w.]+', lambda m: names(m.group(0)), type_name)


def schema_refs(schema):
    found = []

    def walk(node):
        if isinstance(node, dict):
            for key, value in node.items():
                if key == '$ref' and isinstance(value, str):
                    found.append(value[len(REF):])
                else:
                    walk(value)
        elif isinstance(node, list):
            for value in node:
                walk(value)
    walk(schema)
    return found


def closure(roots, schemas, enums):
    seen, todo = [], list(roots)
    while todo:
        name = todo.pop(0)
        if name in seen or (name not in schemas and name not in enums):
            continue
        seen.append(name)
        if name in schemas:
            todo.extend(schema_refs(schemas[name]))
    return seen


def anchor(short_name):
    return 'm-' + re.sub(r'[^A-Za-z0-9]+', '-', short_name)


def field_type(prop, names):
    if '$ref' in prop:
        short = names(prop['$ref'][len(REF):])
        return f'[{short}](#{anchor(short)})'
    if 'allOf' in prop and len(prop['allOf']) == 1:
        return field_type(prop['allOf'][0], names)
    kind = prop.get('type') or (prop.get('types') or [None])[0]
    if kind == 'array':
        return field_type(prop.get('items', {}), names) + '[]'
    if 'enum' in prop:
        return 'enum : ' + ', '.join(f'`{v}`' for v in prop['enum'])
    if kind == 'object' or (kind is None and 'additionalProperties' in prop):
        extra = prop.get('additionalProperties')
        if isinstance(extra, dict) and extra:
            return 'objet { clé : ' + field_type(extra, names) + ' }'
        return 'objet libre'
    fmt = prop.get('format')
    label = {
        ('integer', 'int64'): 'entier int64 ⚠',
        ('integer', 'int32'): 'entier',
        ('number', None): 'nombre décimal',
        ('number', 'double'): 'nombre décimal',
        ('number', 'float'): 'nombre décimal',
        ('string', 'date-time'): 'date-heure ISO 8601 UTC',
        ('string', 'local-date-time'): 'date-heure locale sans fuseau',
        ('string', 'date'): 'date AAAA-MM-JJ',
        ('string', 'uuid'): 'uuid',
        ('string', 'binary'): 'fichier',
        ('string', 'byte'): 'base64',
        ('boolean', None): 'booléen',
        ('string', None): 'texte',
        ('integer', None): 'entier',
    }.get((kind, fmt))
    return label or f'{kind or "?"}{" (" + fmt + ")" if fmt else ""}'


def constraints(prop):
    notes = []
    for key, label in (('minLength', 'longueur min'), ('maxLength', 'longueur max'), ('minimum', 'min'),
                       ('maximum', 'max'), ('minItems', 'éléments min'), ('maxItems', 'éléments max'),
                       ('pattern', 'motif'), ('default', 'défaut'), ('example', 'exemple')):
        if key in prop:
            value = prop[key]
            notes.append(f'{label} `{value}`')
    if prop.get('readOnly'):
        notes.append('lecture seule')
    if prop.get('description'):
        notes.append(prop['description'].replace('\n', ' '))
    return ', '.join(notes)


def merged(schema, schemas):
    """Les proprietes d'un schema, heritage compris."""
    properties, required = {}, list(schema.get('required', []))
    for part in schema.get('allOf', []):
        target = schemas.get(part.get('$ref', '')[len(REF):]) if '$ref' in part else part
        if target:
            sub_properties, sub_required = merged(target, schemas)
            properties.update(sub_properties)
            required += sub_required
    properties.update(schema.get('properties', {}))
    return properties, required


def render_model(name, schemas, enums, names):
    short = names(name)
    lines = [f'<a id="{anchor(short)}"></a>', f'#### {short}', '']
    if name in enums:
        lines += ['Énumération : ' + ', '.join(f'`{v}`' for v in enums[name]), '']
        return lines
    schema = schemas[name]
    if 'enum' in schema:
        lines += ['Énumération : ' + ', '.join(f'`{v}`' for v in schema['enum']), '']
        return lines
    properties, required = merged(schema, schemas)
    if not properties:
        lines += ['Objet sans champ décrit.', '']
        return lines
    lines += ['| Champ | Type | Requis | Contraintes |', '| --- | --- | --- | --- |']
    for field, prop in properties.items():
        lines.append(f'| `{field}` | {field_type(prop, names)} | {"oui" if field in required else ""} | {constraints(prop)} |')
    lines.append('')
    return lines


def access_text(access):
    if access['kind'] == 'admin':
        text = f'back-office : rôle d\'administration et droit `{access["permission"]}`'
    else:
        text = ACCESS[access['kind']]
    if access.get('preAuthorize'):
        text += f' ; contrôle supplémentaire sur la méthode : `{access["preAuthorize"]}`'
    return text


def route_title(route):
    return f'{" | ".join(route["methods"])} {route["path"]}'


def route_anchor(route):
    return 'r-' + re.sub(r'[^A-Za-z0-9]+', '-', '-'.join(route['methods']) + route['path']).strip('-').lower()


def render_route(route, names):
    lines = [f'<a id="{route_anchor(route)}"></a>', f'### {route_title(route)}', '']
    lines.append(f'- Accès : {access_text(route["access"])}')
    if route.get('deprecated'):
        lines.append('- **Obsolète** : ne pas l\'utiliser dans un nouvel écran.')
    if route.get('consumes'):
        lines.append(f'- Envoi : `{", ".join(route["consumes"])}`')
    if route.get('produces'):
        lines.append(f'- Renvoie : `{", ".join(route["produces"])}`')
    idempotency = route.get('idempotency')
    if idempotency:
        need = 'obligatoire' if idempotency['required'] else 'conseillé'
        lines.append(f'- En-tête `{idempotency["header"]}` : {need} (opération `{idempotency["operation"]}`, voir le README racine)')
    body = route.get('body')
    if body:
        lines.append(f'- Corps JSON{" (obligatoire)" if body["required"] else " (facultatif)"} : `{shorten(body["type"], names)}`')
    lines.append(f'- Réponse : `{shorten(strip_entity(route["response"]), names)}`')
    cited = list(dict.fromkeys(names(t) for t in route_types(route)))
    if cited:
        lines.append('- Modèles : ' + ', '.join(f'[{c}](#{anchor(c)})' for c in cited))
    lines.append(f'- Code : `{route["source"]}`, méthode `{route["handler"]}`')
    if route['params']:
        lines += ['', '| Paramètre | Où | Type | Requis | Défaut |', '| --- | --- | --- | --- | --- |']
        for param in route['params']:
            where = {'path': 'chemin', 'query': 'requête', 'pageable': 'requête (pagination)',
                     'header': 'en-tête', 'multipart': 'partie multipart',
                     'query-object': 'requête (champs de l\'objet)'}[param['in']]
            required = 'oui' if param.get('required') else ''
            lines.append(f'| `{param["name"]}` | {where} | `{shorten(param.get("type", ""), names)}` | {required} | {param.get("default", "")} |')
    lines.append('')
    return lines


def route_types(route):
    found = type_refs(route['response'])
    if route.get('body'):
        found += type_refs(route['body']['type'])
    for param in route['params']:
        found += type_refs(param.get('type', ''))
    return found


def main(catalog_path, out_dir):
    catalog = json.load(open(catalog_path, encoding='utf-8'))
    schemas, enums = catalog['schemas'], catalog['enums']
    names = Names(list(schemas) + list(enums) + [t for r in catalog['routes'] for t in route_types(r)])
    modules = collections.defaultdict(list)
    for route in catalog['routes']:
        modules[module_of(route['package'])].append(route)

    summary = ['# Modules', '', GENERATED,
               f'{len(catalog["routes"])} routes dans {catalog["controllers"]} contrôleurs.', '',
               '| Module | Routes | Back-office | Portail membre | Publiques | Fonctionnement |',
               '| --- | --- | --- | --- | --- | --- |']
    for module in sorted(modules, key=lambda m: -len(modules[m])):
        routes = sorted(modules[module], key=lambda r: (r['controller'], r['path'], r['methods'], r['handler'], r.get('consumes', [])))
        folder = os.path.join(out_dir, module)
        shutil.rmtree(os.path.join(folder, 'routes'), ignore_errors=True)
        os.makedirs(os.path.join(folder, 'routes'), exist_ok=True)
        by_controller = collections.OrderedDict()
        for route in routes:
            by_controller.setdefault(route['controller'], []).append(route)

        index = [f'# {module} : routes', '', GENERATED,
                 f'{len(routes)} routes. Le fonctionnement et les parcours sont dans [README.md](README.md).', '',
                 '| Verbe | Chemin | Accès | Détail |', '| --- | --- | --- | --- |']
        for controller, items in by_controller.items():
            for route in items:
                kind = route['access']['kind']
                who = route['access'].get('permission', '') if kind == 'admin' else {'public': 'public', 'member-portal': 'membre', 'authenticated': 'connecté'}[kind]
                index.append(f'| {" ".join(route["methods"])} | `{route["path"]}` | {who} | [{controller}](routes/{controller}.md#{route_anchor(route)}) |')
            lines = [f'# {controller}', '', GENERATED, f'Module [{module}](../README.md) · {len(items)} route(s).', '', '## Routes', '']
            for route in items:
                lines += render_route(route, names)
            models = closure([t for r in items for t in route_types(r)], schemas, enums)
            if models:
                lines += ['## Modèles', '', 'Les champs JSON de chaque type cité plus haut, et des types qu\'ils contiennent.', '']
                for model in models:
                    lines += render_model(model, schemas, enums, names)
            write(os.path.join(folder, 'routes', f'{controller}.md'), lines)
        write(os.path.join(folder, 'routes.md'), index + [''])

        used = closure([t for r in routes for t in route_types(r)], schemas, enums)
        machine = {
            'module': module,
            'routes': routes,
            'schemas': {names(n): schemas[n] for n in used if n in schemas},
            'enums': {names(n): enums[n] for n in used if n in enums},
            'names': {names(n): n for n in used},
        }
        with open(os.path.join(folder, 'routes.json'), 'w', encoding='utf-8', newline='\n') as handle:
            json.dump(machine, handle, ensure_ascii=False, indent=1)

        count = collections.Counter(r['access']['kind'] for r in routes)
        summary.append(f'| [{module}]({module}/routes.md) | {len(routes)} | {count["admin"]} | {count["member-portal"]} | '
                       f'{count["public"]} | [README]({module}/README.md) |')
    summary += ['', f'Rôles du back-office : {", ".join(f"`{r}`" for r in catalog["adminRealmRoles"])}.', '']
    write(os.path.join(out_dir, 'MODULES.md'), summary)
    print(f'{len(modules)} modules ecrits dans {out_dir}')


def write(path, lines):
    with open(path, 'w', encoding='utf-8', newline='\n') as handle:
        handle.write('\n'.join(lines).rstrip('\n') + '\n')


if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
