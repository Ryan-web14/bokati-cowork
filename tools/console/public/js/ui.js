// De quoi composer les pages sans bibliotheque.
import { columnsOf, rowsOf } from './spec.js';

export function h(tag, attributes = {}, ...children) {
  const element = document.createElement(tag);
  for (const [name, value] of Object.entries(attributes || {})) {
    if (value === undefined || value === null || value === false) continue;
    if (name.startsWith('on')) element.addEventListener(name.slice(2).toLowerCase(), value);
    else if (name === 'value') element.value = value;
    else if (name === 'checked' || name === 'disabled' || name === 'selected') element[name] = Boolean(value);
    else element.setAttribute(name === 'cls' ? 'class' : name, value);
  }
  children.flat(Infinity).forEach((child) => {
    if (child !== null && child !== undefined && child !== false) {
      element.append(child instanceof Node ? child : String(child));
    }
  });
  return element;
}

export const clear = (node) => { node.replaceChildren(); return node; };

export const notice = (kind, message) => h('div', { cls: `notice ${kind}` }, message);

/** Un bouton qui attend la fin de son action et affiche son erreur au lieu de la perdre. */
export function action(label, run, { cls = '', target = null } = {}) {
  const button = h('button', { type: 'button', cls, onClick: async () => {
    button.disabled = true;
    try {
      await run();
    } catch (error) {
      const place = target || button.parentElement;
      place.prepend(notice('bad', error.message));
    } finally {
      button.disabled = false;
    }
  } }, label);
  return button;
}

export function field(label, control, { required = false, hint = null } = {}) {
  return h('label', { cls: `field${required ? ' req' : ''}` }, h('span', {}, label), control, hint && h('span', { cls: 'hint' }, hint));
}

const STATUS_TONES = [
  [/^(ACTIVE|PAID|FULFILLED|APPROVED|CONFIRMED|COMPLETED|AMENDED|SIGNED|SUCCEEDED|PUBLISHED|VALIDATED)$/, 'ok'],
  [/^(PENDING|PENDING_.*|GRACE_PERIOD|PAST_DUE|DRAFT|CART|PARTIALLY_PAID|WAITING.*|UNDER_REVIEW|AWAITING.*)$/, 'warn'],
  [/^(CANCELLED|EXPIRED|SUSPENDED|REJECTED|FAILED|TERMINATED|OVERDUE)$/, 'bad'],
];

export function status(value) {
  const tone = STATUS_TONES.find(([pattern]) => pattern.test(String(value)))?.[1] || '';
  return h('span', { cls: `badge ${tone}` }, value);
}

export function cell(key, value) {
  if (value === null || value === undefined) return '';
  if (typeof value === 'boolean') return value ? 'oui' : 'non';
  if (/status$/i.test(key)) return status(value);
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value)) return new Date(value).toLocaleString('fr-FR');
  if (typeof value === 'object') return JSON.stringify(value);
  return value;
}

/** Un tableau · {@code actions} ajoute une derniere colonne de boutons. */
export function table(rows, { columns = null, actions = null, empty = 'Aucune ligne.' } = {}) {
  if (!rows.length) return h('p', { cls: 'muted' }, empty);
  const headers = columns || columnsOf(rows);
  return h('div', { cls: 'table-wrap' }, h('table', { cls: 'data' },
    h('thead', {}, h('tr', {}, headers.map((key) => h('th', {}, key)), actions && h('th', { cls: 'actions' }))),
    h('tbody', {}, rows.map((row) => h('tr', {},
      headers.map((key) => h('td', {}, cell(key, row?.[key]))),
      actions && h('td', { cls: 'actions' }, actions(row)))))));
}

export function keyValues(object) {
  return h('dl', { cls: 'kv' }, Object.entries(object)
    .filter(([, value]) => value === null || typeof value !== 'object')
    .flatMap(([key, value]) => [h('dt', {}, key), h('dd', {}, cell(key, value))]));
}

/** Une reponse d'API lisible · tableau, fiche, ou brut au choix. */
export function renderPayload(payload) {
  const raw = h('pre', { cls: 'json' }, typeof payload === 'string' ? payload : JSON.stringify(payload, null, 2));
  const rows = rowsOf(payload);
  let readable = null;
  if (rows) {
    const page = payload?.pageable;
    readable = h('div', {}, table(rows),
      page && h('p', { cls: 'hint', style: 'margin-top:6px' }, `Page ${page.page + 1} sur ${page.totalPages || 1} · ${page.totalElements} élément(s)`));
  } else if (payload && typeof payload === 'object') {
    readable = keyValues(payload);
  }
  if (!readable) return raw;
  raw.style.display = 'none';
  const toggle = h('button', { type: 'button', cls: 'ghost small', style: 'margin:8px 0', onClick: () => {
    const hidden = raw.style.display === 'none';
    raw.style.display = hidden ? 'block' : 'none';
    toggle.textContent = hidden ? 'Masquer le JSON' : 'Voir le JSON';
  } }, 'Voir le JSON');
  return h('div', {}, readable, toggle, raw);
}
