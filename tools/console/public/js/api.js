// Les appels a l'API · le jeton, son rafraichissement, et des erreurs lisibles.
import { parseJson } from './spec.js';

export const V1 = '/sni/api/v1';
const KEY = 'bokati-console-session';

export const session = {
  get() {
    try { return JSON.parse(sessionStorage.getItem(KEY)); } catch { return null; }
  },
  set(value) { sessionStorage.setItem(KEY, JSON.stringify(value)); },
  clear() { sessionStorage.removeItem(KEY); },
};

async function send(method, path, { query, body, form } = {}) {
  const url = new URL(path, location.origin);
  Object.entries(query || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') url.searchParams.set(key, value);
  });
  const headers = {};
  const token = session.get()?.accessToken;
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  return fetch(url, { method, headers, body: form || (body === undefined ? undefined : JSON.stringify(body)) });
}

async function refresh() {
  const refreshToken = session.get()?.refreshToken;
  if (!refreshToken) return false;
  const response = await fetch(`${V1}/auth/refresh`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken }),
  });
  if (!response.ok) return false;
  session.set({ ...session.get(), ...(await response.json()) });
  return true;
}

/** Rend {ok, status, data, text, blob, type} · ne jette jamais pour une reponse de l'API. */
export async function call(method, path, options = {}) {
  let response = await send(method, path, options);
  if (response.status === 401 && await refresh()) {
    response = await send(method, path, options);
  }
  const type = response.headers.get('content-type') || '';
  const result = { ok: response.ok, status: response.status, type };
  if (type.includes('json')) {
    result.text = await response.text();
    try { result.data = parseJson(result.text); } catch { result.data = result.text; }
  } else if (type.startsWith('text/')) {
    result.text = await response.text();
    result.data = result.text;
  } else {
    result.blob = await response.blob();
    result.filename = /filename="?([^";]+)/.exec(response.headers.get('content-disposition') || '')?.[1];
  }
  return result;
}

/** Comme {@link call}, mais une erreur devient une exception au message lisible. */
export async function must(method, path, options) {
  const result = await call(method, path, options);
  if (!result.ok) throw new Error(errorOf(result));
  return result.data;
}

export function errorOf(result) {
  const data = result.data;
  const detail = data?.message || data?.error || data?.detail || (typeof data === 'string' ? data : '');
  const required = data?.requiredPermission ? ` (permission exigée : ${data.requiredPermission})` : '';
  return `Erreur ${result.status}${detail ? ` : ${detail}` : ''}${required}`;
}

export async function login(email, password) {
  const response = await fetch(`${V1}/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password }),
  });
  const text = await response.text();
  let data = null;
  try { data = parseJson(text); } catch { /* reponse non JSON */ }
  if (!response.ok) throw new Error(data?.message || `Connexion refusée (${response.status})`);
  session.set({ ...data, email });
  return data;
}

export async function logout() {
  await send('POST', `${V1}/auth/logout`).catch(() => {});
  session.clear();
}

export function download(result, fallback = 'fichier') {
  const link = document.createElement('a');
  link.href = URL.createObjectURL(result.blob);
  link.download = result.filename || fallback;
  link.click();
  URL.revokeObjectURL(link.href);
}
