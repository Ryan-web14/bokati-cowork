// Lit la description OpenAPI de l'application · des modules, leurs routes, leurs champs. Sans
// dependance au navigateur, pour etre testable sous Node.

const API_PREFIX = /^\/sni\/api\/v\d+/;
const METHODS = ['get', 'post', 'put', 'patch', 'delete'];
const ORDER = { get: 0, post: 1, put: 2, patch: 3, delete: 4 };

const LABELS = {
  'admin': 'Administration', 'analytics': 'Analytique', 'auth': 'Authentification', 'billable-items': 'Éléments facturables',
  'billing': 'Facturation', 'bookings': 'Réservations', 'businesses': 'Structures', 'cash-registers': 'Caisses',
  'checkin': 'Check-in', 'companies': 'Entreprises', 'contracts': 'Contrats', 'countries': 'Pays', 'crm': 'CRM',
  'currencies': 'Devises', 'customers': 'Clients', 'document-types': 'Types de documents', 'documents': 'Documents',
  'domiciliation': 'Domiciliation', 'event-registrations': 'Événements', 'inventory': 'Stock', 'kyc': 'KYC',
  'marketplace': 'Marketplace', 'members': 'Membres', 'notifications': 'Notifications', 'pass-plans': 'Pass plans',
  'passes': 'Passes', 'payment-links': 'Liens de paiement', 'payments': 'Paiements', 'promotions': 'Promotions',
  'reports': 'Rapports', 'reporting': 'Rapports', 'resources': 'Ressources', 'services': 'Services', 'subscriptions': 'Abonnements',
  'subscription-metrics': 'Indicateurs d\'abonnement', 'support': 'Support', 'tasks': 'Tâches', 'visitors': 'Visiteurs',
  'wallets': 'Portefeuilles', 'wallet-holds': 'Retenues de portefeuille',
};

const title = (key) => LABELS[key] || key.split('-').map((word, index) => (index ? word : word[0].toUpperCase() + word.slice(1))).join(' ');

/** Le bout de chemin qui designe le module · les routes du portail forment leur propre groupe. */
export function moduleOf(path) {
  const parts = path.replace(API_PREFIX, '').split('/').filter(Boolean);
  if (parts[0] === 'client' || parts[0] === 'portal') {
    const second = (parts[1] || 'accueil').replace(/^\{.*\}$/, 'accueil');
    return { key: `client/${second}`, label: title(second), group: 'Portail client' };
  }
  const first = parts[0] || 'racine';
  return { key: first, label: title(first), group: 'Administration' };
}

/** Suit les references et fusionne les heritages · un schema cyclique s'arrete a la profondeur donnee. */
export function resolve(spec, schema, depth = 0) {
  if (!schema || depth > 8) return {};
  if (schema.$ref) {
    const name = schema.$ref.split('/').pop();
    return resolve(spec, spec.components?.schemas?.[name], depth + 1);
  }
  if (schema.allOf) {
    return schema.allOf.reduce((merged, part) => {
      const resolved = resolve(spec, part, depth + 1);
      return {
        ...merged, ...resolved,
        properties: { ...merged.properties, ...resolved.properties },
        required: [...(merged.required || []), ...(resolved.required || [])],
      };
    }, {});
  }
  return schema;
}

function parameters(spec, operation) {
  const list = [];
  for (const parameter of operation.parameters || []) {
    if (parameter.in === 'header' || parameter.in === 'cookie') continue;
    const schema = resolve(spec, parameter.schema);
    // Un Pageable arrive d'un bloc · il se saisit champ par champ.
    if (parameter.in === 'query' && schema.properties?.page !== undefined && schema.properties?.size !== undefined) {
      list.push({ name: 'page', in: 'query', schema: { type: 'integer', default: 0 } });
      list.push({ name: 'size', in: 'query', schema: { type: 'integer', default: 20 } });
      list.push({ name: 'sort', in: 'query', schema: { type: 'string' } });
      continue;
    }
    list.push({ name: parameter.name, in: parameter.in, required: Boolean(parameter.required), schema });
  }
  return list;
}

function bodyOf(spec, operation) {
  const content = operation.requestBody?.content;
  if (!content) return null;
  const media = content['application/json'] || content['multipart/form-data'] || Object.values(content)[0];
  return {
    required: Boolean(operation.requestBody.required),
    multipart: Boolean(content['multipart/form-data']) && !content['application/json'],
    schema: resolve(spec, media?.schema),
  };
}

export function buildModules(spec) {
  const modules = new Map();
  for (const [path, item] of Object.entries(spec.paths || {})) {
    for (const method of METHODS) {
      const operation = item[method];
      if (!operation) continue;
      const module = moduleOf(path);
      if (!modules.has(module.key)) modules.set(module.key, { ...module, operations: [] });
      modules.get(module.key).operations.push({
        id: `${method} ${path}`, method: method.toUpperCase(), path,
        summary: operation.summary || operation.description || '',
        params: parameters(spec, operation),
        body: bodyOf(spec, operation),
      });
    }
  }
  const sorted = [...modules.values()];
  sorted.forEach((module) => module.operations.sort((a, b) =>
    ORDER[a.method.toLowerCase()] - ORDER[b.method.toLowerCase()] || a.path.localeCompare(b.path)));
  return sorted.sort((a, b) => a.group.localeCompare(b.group) || a.label.localeCompare(b.label));
}

/** Les champs d'un formulaire · ce qui ne tient pas dans un champ simple se saisit en JSON. */
export function formFields(spec, schema) {
  const resolved = resolve(spec, schema);
  const required = new Set(resolved.required || []);
  return Object.entries(resolved.properties || {})
    .filter(([, field]) => !field.readOnly)
    .map(([name, field]) => {
      const detail = resolve(spec, field);
      return { name, required: required.has(name), kind: kindOf(detail), schema: detail, options: detail.enum || null };
    });
}

export function kindOf(schema) {
  if (schema.enum) return 'enum';
  if (schema.type === 'boolean') return 'boolean';
  if (schema.type === 'integer' || schema.type === 'number') return 'number';
  if (schema.type === 'string') {
    if (schema.format === 'date') return 'date';
    if (schema.format === 'date-time') return 'datetime';
    if (schema.format === 'binary') return 'file';
    return 'text';
  }
  return 'json';
}

/** Une valeur saisie, dans le type attendu · vide, elle n'est pas envoyee. */
export function coerce(kind, value) {
  if (value === '' || value === undefined || value === null) return undefined;
  switch (kind) {
    case 'number': return Number(value);
    case 'boolean': return value === true || value === 'true';
    case 'datetime': return new Date(value).toISOString();
    case 'json': return JSON.parse(value);
    default: return value;
  }
}

/**
 * Lit du JSON sans abimer les grands identifiants · JavaScript arrondit au-dela de 2^53, ils sont
 * donc gardes en texte.
 */
export function parseJson(text) {
  return JSON.parse(text.replace(/([:\[,]\s*)(-?\d{16,})(?=\s*[,\}\]])/g, '$1"$2"'));
}

/** Les lignes d'une reponse · une liste, ou une page sous {@code data} ou {@code content}. */
export function rowsOf(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.content)) return payload.content;
  return null;
}

/** Les colonnes d'un tableau · les valeurs simples, les plus utiles d'abord. */
export function columnsOf(rows, limit = 12) {
  const columns = [];
  for (const row of rows.slice(0, 50)) {
    if (row === null || typeof row !== 'object') continue;
    for (const [key, value] of Object.entries(row)) {
      if (!columns.includes(key) && (value === null || typeof value !== 'object')) columns.push(key);
    }
  }
  const rank = (key) => (/(^id$|code$|number$)/i.test(key) ? 0 : /name|title|label/i.test(key) ? 1 : /status/i.test(key) ? 2 : 3);
  return columns.map((key, index) => [key, index]).sort((a, b) => rank(a[0]) - rank(b[0]) || a[1] - b[1]).map(([key]) => key).slice(0, limit);
}
