// L'ecran genere d'un module · la liste de ses routes, et pour chacune un formulaire et sa reponse.
import { call, download, errorOf } from './api.js';
import { coerce, formFields, kindOf, resolve } from './spec.js';
import { action, clear, field, h, notice, renderPayload } from './ui.js';

function control(kind, { options, value } = {}) {
  if (kind === 'enum') {
    return h('select', {}, h('option', { value: '' }, ''), options.map((option) => h('option', { value: option }, option)));
  }
  if (kind === 'boolean') return h('select', {}, h('option', { value: '' }, ''), h('option', { value: 'true' }, 'oui'), h('option', { value: 'false' }, 'non'));
  if (kind === 'json') return h('textarea', { placeholder: 'JSON', style: 'min-height:60px' });
  if (kind === 'file') return h('input', { type: 'file' });
  const type = { number: 'number', date: 'date', datetime: 'datetime-local' }[kind] || 'text';
  return h('input', { type, step: kind === 'number' ? 'any' : undefined, value });
}

function bodyForm(spec, operation) {
  const fields = formFields(spec, operation.body.schema);
  const inputs = fields.map((item) => ({ ...item, input: control(item.kind, item) }));
  const raw = h('textarea', { placeholder: '{ }' });
  const form = h('div', { cls: 'row' }, inputs.map((item) => h('div', { cls: 'grow' }, field(item.name, item.input, { required: item.required }))));
  // Sans champs decrits, ou pour les corps complexes, le JSON reste possible.
  let jsonMode = fields.length === 0 || operation.body.schema.type === 'array';
  const sync = () => {
    form.style.display = jsonMode ? 'none' : 'flex';
    raw.style.display = jsonMode ? 'block' : 'none';
  };
  sync();
  const toggle = fields.length > 0 && h('button', { type: 'button', cls: 'ghost small', onClick: () => {
    if (!jsonMode) raw.value = JSON.stringify(read(), null, 2);
    jsonMode = !jsonMode;
    sync();
  } }, 'Formulaire ou JSON');
  const read = () => {
    const body = {};
    for (const item of inputs) {
      const value = coerce(item.kind, item.input.value);
      if (value !== undefined) body[item.name] = value;
    }
    return body;
  };
  const readForm = () => {
    if (!operation.body.multipart) return null;
    const data = new FormData();
    for (const item of inputs) {
      if (item.kind === 'file') {
        if (item.input.files[0]) data.append(item.name, item.input.files[0]);
      } else if (item.input.value !== '') {
        data.append(item.name, item.input.value);
      }
    }
    return data;
  };
  return {
    element: h('div', {}, h('h2', {}, 'Corps de la requête'), toggle, form, raw),
    read: () => (jsonMode ? (raw.value.trim() ? JSON.parse(raw.value) : undefined) : read()),
    readForm,
  };
}

function operationPanel(spec, operation) {
  const inputs = operation.params.map((param) => {
    const schema = resolve(spec, param.schema);
    const kind = kindOf(schema);
    const input = control(kind, { options: schema.enum, value: schema.default });
    return { ...param, kind: kind === 'json' ? 'text' : kind, input };
  });
  const body = operation.body ? bodyForm(spec, operation) : null;
  const result = h('div', { style: 'margin-top:12px' });

  const run = async () => {
    let path = operation.path;
    const query = {};
    for (const param of inputs) {
      const value = param.input.value;
      if (param.in === 'path') {
        if (!value) throw new Error(`Renseignez ${param.name}.`);
        path = path.replace(`{${param.name}}`, encodeURIComponent(value));
      } else {
        query[param.name] = value;
      }
    }
    if (operation.method === 'DELETE' && !confirm(`Supprimer ?\n${path}`)) return;
    clear(result).append(h('p', { cls: 'muted' }, 'Envoi…'));
    const form = body?.readForm();
    const response = await call(operation.method, path, { query, body: form ? undefined : body?.read(), form: form || undefined });
    clear(result);
    if (!response.ok) result.append(notice('bad', errorOf(response)));
    else if (response.blob) {
      result.append(notice('ok', `Fichier reçu (${response.type}).`), action('Télécharger', async () => download(response)));
    } else {
      result.append(notice('ok', `Réponse ${response.status}`));
    }
    if (response.data !== undefined && response.data !== '') result.append(renderPayload(response.data));
  };

  const panel = h('div', { cls: 'card' },
    h('div', { style: 'margin-bottom:10px' }, h('span', { cls: `verb ${operation.method}` }, operation.method), ' ', h('span', { cls: 'op-path' }, operation.path)),
    operation.summary && h('p', { cls: 'muted' }, operation.summary),
    inputs.length > 0 && h('div', { cls: 'row' }, inputs.map((param) =>
      field(`${param.name}${param.in === 'path' ? ' (chemin)' : ''}`, param.input, { required: param.required }))),
    body?.element,
    h('div', { style: 'margin-top:8px' }, action(operation.method === 'GET' ? 'Lire' : 'Envoyer', run, { cls: operation.method === 'DELETE' ? 'danger' : '', target: result })),
    result);
  // Une liste se lit des l'ouverture · rien a saisir, et rien a craindre d'un GET.
  if (operation.method === 'GET' && !operation.path.includes('{')) {
    run().catch((error) => clear(result).append(notice('bad', error.message)));
  }
  return panel;
}

export function moduleScreen(spec, module, root) {
  const panel = h('div', {});
  const buttons = new Map();
  const open = (operation) => {
    buttons.forEach((button, id) => button.classList.toggle('active', id === operation.id));
    clear(panel).append(operationPanel(spec, operation));
  };
  const list = h('div', { cls: 'ops' }, module.operations.map((operation) => {
    const button = h('button', { type: 'button', cls: 'op', onClick: () => open(operation) },
      h('span', { cls: `verb ${operation.method}` }, operation.method),
      h('span', { cls: 'op-path' }, operation.path.replace(/^\/sni\/api\/v\d+/, '')),
      operation.summary && h('span', { cls: 'muted' }, operation.summary));
    buttons.set(operation.id, button);
    return button;
  }));
  root.append(
    h('h1', {}, module.label),
    h('p', { cls: 'muted' }, `${module.group} · ${module.operations.length} route(s)`),
    panel,
    h('div', { cls: 'card' }, list));
  const first = module.operations.find((operation) => operation.method === 'GET' && !operation.path.includes('{')) || module.operations[0];
  if (first) open(first);
}
