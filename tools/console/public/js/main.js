// La console · connexion, menu des modules, et aiguillage des ecrans.
import { V1, call, errorOf, login, logout, session } from './api.js';
import { moduleScreen } from './generic.js';
import { contractsScreen, hoursScreen, marketplaceAdminScreen, marketplaceClientScreen, renewalScreen, settingsScreen } from './screens.js';
import { buildModules } from './spec.js';
import { clear, h, notice } from './ui.js';

const SCREENS = [
  ['settings', 'Paramètres', settingsScreen],
  ['hours', 'Horaires des ressources', hoursScreen],
  ['marketplace', 'Marketplace · administration', marketplaceAdminScreen],
  ['shop', 'Marketplace · portail', marketplaceClientScreen],
  ['contracts', 'Contrats et versions', contractsScreen],
  ['renewal', 'Renouvellement d\'abonnement', renewalScreen],
];

const app = document.getElementById('app');
let spec = null;
let modules = [];
let specError = null;

function loginScreen(message) {
  const email = h('input', { type: 'email', autocomplete: 'username', required: true });
  const password = h('input', { type: 'password', autocomplete: 'current-password', required: true });
  const feedback = h('div', {}, message && notice('bad', message));
  const form = h('form', { onSubmit: async (event) => {
    event.preventDefault();
    clear(feedback);
    try {
      await login(email.value, password.value);
      await start();
    } catch (error) {
      feedback.append(notice('bad', error.message));
    }
  } },
  h('h1', {}, 'Console de test'),
  h('p', { cls: 'muted' }, 'Bokati Cowork, en local. Connectez-vous avec un compte de votre base de développement.'),
  feedback,
  h('label', { cls: 'field' }, h('span', {}, 'E-mail'), email),
  h('label', { cls: 'field' }, h('span', {}, 'Mot de passe'), password),
  h('button', { type: 'submit', style: 'width:100%' }, 'Se connecter'));
  clear(app).append(h('div', { cls: 'login' }, h('div', { cls: 'card' }, form)));
}

async function loadSpec() {
  const result = await call('GET', '/v3/api-docs');
  if (result.status === 401) return false;
  if (result.ok) {
    spec = result.data;
    modules = buildModules(spec);
    specError = null;
  } else {
    specError = errorOf(result);
  }
  return true;
}

function sidebar(active) {
  const list = h('div', { cls: 'side-scroll' });
  const link = (href, label, count) => h('a', { href, cls: href === active ? 'active' : '' }, h('span', {}, label), count !== undefined && h('span', { cls: 'count' }, count));
  const render = (needle = '') => {
    const match = (text) => text.toLowerCase().includes(needle);
    const screens = SCREENS.filter(([, label]) => match(label));
    const groups = [...new Set(modules.map((module) => module.group))];
    clear(list).append(
      screens.length > 0 && h('div', { cls: 'side-group' }, 'Écrans dédiés'),
      ...screens.map(([id, label]) => link(`#/s/${id}`, label)),
      ...groups.flatMap((group) => {
        const items = modules.filter((module) => module.group === group && (match(module.label) || match(module.key)));
        return items.length === 0 ? [] : [h('div', { cls: 'side-group' }, group),
          ...items.map((module) => link(`#/m/${encodeURIComponent(module.key)}`, module.label, module.operations.length))];
      }));
  };
  render();
  const search = h('input', { placeholder: 'Chercher un module', onInput: (event) => render(event.target.value.toLowerCase()) });
  return h('nav', { cls: 'side' },
    h('div', { cls: 'side-head' }, h('div', { cls: 'side-title' }, 'Console de test'),
      h('div', { cls: 'side-user' }, session.get()?.email || ''),
      h('div', { cls: 'side-user' }, (session.get()?.roles || []).join(', '))),
    h('div', { cls: 'side-search' }, search),
    list,
    h('div', { cls: 'side-foot' }, h('button', { type: 'button', cls: 'ghost small', onClick: async () => { await logout(); loginScreen(); } }, 'Se déconnecter')));
}

function home(root) {
  const routes = modules.reduce((total, module) => total + module.operations.length, 0);
  root.append(h('h1', {}, 'Console de test'),
    h('p', { cls: 'muted' }, 'Outil local : les actions agissent sur la base de développement.'));
  if (specError) {
    root.append(notice('bad', `Description des routes indisponible (${specError}). Les écrans dédiés restent utilisables.`));
  } else {
    root.append(h('div', { cls: 'card' }, h('p', {}, `${modules.length} modules et ${routes} routes détectés.`),
      h('p', { cls: 'muted' }, 'Commencez par les écrans dédiés, ou choisissez un module à gauche.')));
  }
}

async function render() {
  const route = location.hash || '#/';
  const main = h('main', { cls: 'main' });
  clear(app).append(h('div', { cls: 'layout' }, sidebar(route), main));
  try {
    if (route.startsWith('#/s/')) {
      const screen = SCREENS.find(([id]) => id === route.slice(4));
      if (screen) await screen[2](main);
    } else if (route.startsWith('#/m/')) {
      const module = modules.find((item) => item.key === decodeURIComponent(route.slice(4)));
      if (module) moduleScreen(spec, module, main);
      else main.append(notice('bad', 'Module introuvable.'));
    } else {
      home(main);
    }
  } catch (error) {
    main.append(notice('bad', error.message));
  }
}

async function start() {
  if (!session.get()?.accessToken) {
    loginScreen();
    return;
  }
  clear(app).append(h('div', { cls: 'login' }, h('p', { cls: 'muted' }, 'Lecture de la description des routes…')));
  try {
    if (!await loadSpec()) {
      session.clear();
      loginScreen('Session expirée, reconnectez-vous.');
      return;
    }
  } catch (error) {
    specError = error.message;
  }
  await render();
}

window.addEventListener('hashchange', () => { if (session.get()) render(); });
start();
