// Les ecrans dedies · ce qui n'a pas encore d'interface dans l'administration.
import { V1, call, download, errorOf, must } from './api.js';
import { rowsOf } from './spec.js';
import { action, clear, field, h, keyValues, notice, status, table } from './ui.js';

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const DAY_LABELS = { MONDAY: 'Lundi', TUESDAY: 'Mardi', WEDNESDAY: 'Mercredi', THURSDAY: 'Jeudi', FRIDAY: 'Vendredi', SATURDAY: 'Samedi', SUNDAY: 'Dimanche' };
const key = (value) => encodeURIComponent(value);
const amount = (value, currency = 'XAF') => (value === null || value === undefined ? '' : `${Number(value).toLocaleString('fr-FR')} ${currency}`);

function tabs(root, entries) {
  const body = h('div', {});
  const buttons = entries.map(([label, render]) => h('button', { type: 'button', onClick: () => show(label, render) }, label));
  const show = async (label, render) => {
    buttons.forEach((button) => button.classList.toggle('active', button.textContent === label));
    clear(body);
    try { await render(body); } catch (error) { body.append(notice('bad', error.message)); }
  };
  root.append(h('div', { cls: 'tabs' }, buttons), body);
  show(entries[0][0], entries[0][1]);
}

// ---- Parametres ------------------------------------------------------------------------------

function settingInput(setting) {
  if (setting.type === 'BOOLEAN') {
    return h('select', {}, ['true', 'false'].map((value) => h('option', { value, selected: value === setting.value }, value === 'true' ? 'oui' : 'non')));
  }
  if (setting.type === 'ENUM') {
    return h('select', {}, (setting.options || []).map((value) => h('option', { value, selected: value === setting.value }, value)));
  }
  const type = setting.type === 'INTEGER' || setting.type === 'DECIMAL' ? 'number' : 'text';
  return h('input', { type, step: 'any', value: setting.value ?? '', style: 'width:100%' });
}

function settingRow(setting, reload) {
  const secret = setting.type === 'SECRET';
  const input = secret ? null : settingInput(setting);
  const history = h('div', {});
  const save = async () => { await must('PUT', `${V1}/admin/settings/${key(setting.key)}`, { body: { value: input.value } }); await reload(); };
  const reset = async () => { await must('DELETE', `${V1}/admin/settings/${key(setting.key)}`); await reload(); };
  const showHistory = async () => {
    const changes = await must('GET', `${V1}/admin/settings/${key(setting.key)}/history`);
    clear(history).append(h('div', { style: 'margin-top:6px' }, table(changes, { columns: ['changedAt', 'oldValue', 'newValue', 'reason'], empty: 'Aucun changement.' })));
  };
  return h('div', { cls: 'card', style: 'padding:10px 12px;margin-bottom:6px' },
    h('div', { cls: 'row', style: 'align-items:center' },
      h('div', { cls: 'grow' }, h('strong', {}, setting.label), ' ',
        h('span', { cls: `badge ${setting.effect === 'LIVE' ? 'ok' : ''}` }, setting.effect === 'LIVE' ? 'immédiat' : 'au redémarrage'),
        setting.modified && h('span', { cls: 'badge info', style: 'margin-left:4px' }, 'modifié'),
        setting.restartPending && h('span', { cls: 'badge warn', style: 'margin-left:4px' }, 'attend le redémarrage'),
        h('div', { cls: 'hint mono' }, setting.key)),
      secret
        ? h('span', { cls: `badge ${setting.configured ? 'ok' : 'warn'}` }, setting.configured ? 'secret renseigné' : 'secret absent')
        : h('div', { style: 'width:260px' }, input),
      !secret && action('Enregistrer', save, { cls: 'small' }),
      !secret && setting.modified && action('Rétablir', reset, { cls: 'ghost small' }),
      !secret && action('Historique', showHistory, { cls: 'ghost small' })),
    !secret && h('div', { cls: 'hint' }, `Par défaut : ${setting.defaultValue || 'vide'}`),
    history);
}

export async function settingsScreen(root) {
  root.append(h('h1', {}, 'Paramètres de la plateforme'),
    h('p', { cls: 'muted' }, 'Les domaines visibles dépendent de votre rôle.'));
  const domains = await must('GET', `${V1}/admin/settings/domains`);
  const list = h('div', {});
  const select = h('select', { onChange: () => load() }, domains.map((item) =>
    h('option', { value: item.domain }, `${item.label} (${item.settings})`)));
  const search = h('input', { placeholder: 'Filtrer par libellé ou clé', onInput: () => render() });
  let settings = [];
  const render = () => {
    const needle = search.value.toLowerCase();
    clear(list).append(...settings
      .filter((item) => !needle || item.label.toLowerCase().includes(needle) || item.key.toLowerCase().includes(needle))
      .map((item) => settingRow(item, load)));
  };
  const load = async () => {
    settings = await must('GET', `${V1}/admin/settings`, { query: { domain: select.value } });
    render();
  };
  root.append(h('div', { cls: 'card row' }, field('Domaine', select), h('div', { cls: 'grow' }, field('Recherche', search))), list);
  await load();
}

// ---- Horaires --------------------------------------------------------------------------------

export async function hoursScreen(root) {
  root.append(h('h1', {}, 'Horaires des ressources'),
    h('p', { cls: 'muted' }, 'Une ressource suit les horaires globaux, sauf si elle a les siens. Un jour sans plage est fermé.'));
  const code = h('input', { placeholder: 'Code de la ressource' });
  const body = h('div', {});
  const resources = await call('GET', `${V1}/resources`, { query: { size: 100 } });
  const known = rowsOf(resources.data) || [];
  const picker = known.length > 0 && h('select', { onChange: (event) => { code.value = event.target.value; load(); } },
    h('option', { value: '' }, 'Choisir une ressource'), known.map((item) => h('option', { value: item.code }, `${item.name || item.code} (${item.code})`)));

  const rangeRow = (range, rows) => {
    const row = h('div', { cls: 'row', style: 'margin-bottom:4px' },
      h('select', {}, DAYS.map((day) => h('option', { value: day, selected: day === range.dayOfWeek }, DAY_LABELS[day]))),
      h('input', { type: 'time', value: String(range.open || '09:00').slice(0, 5) }),
      h('input', { type: 'time', value: String(range.close || '17:00').slice(0, 5) }),
      h('button', { type: 'button', cls: 'ghost small', onClick: () => row.remove() }, 'Retirer'));
    rows.append(row);
  };
  const load = async () => {
    if (!code.value) return;
    const view = await must('GET', `${V1}/resources/${key(code.value)}/opening-hours`);
    const rows = h('div', {});
    view.hours.forEach((range) => rangeRow(range, rows));
    const collect = () => [...rows.children].map((row) => {
      const [day, open, close] = row.querySelectorAll('select, input');
      return { dayOfWeek: day.value, open: open.value, close: close.value };
    });
    clear(body).append(h('div', { cls: 'card' },
      h('p', {}, view.custom ? h('span', { cls: 'badge info' }, 'horaires propres') : h('span', { cls: 'badge' }, 'horaires globaux')),
      rows,
      h('div', { cls: 'row', style: 'margin-top:8px' },
        h('button', { type: 'button', cls: 'ghost', onClick: () => rangeRow({ dayOfWeek: 'MONDAY' }, rows) }, 'Ajouter une plage'),
        action('Enregistrer pour cette ressource', async () => {
          await must('PUT', `${V1}/resources/${key(code.value)}/opening-hours`, { body: collect() });
          await load();
        }),
        view.custom && action('Revenir aux horaires globaux', async () => {
          await must('DELETE', `${V1}/resources/${key(code.value)}/opening-hours`);
          await load();
        }, { cls: 'ghost' }))));
  };
  root.append(h('div', { cls: 'card row' }, picker && field('Ressource', picker), field('Code', code), action('Charger', load, { target: body })), body);
}

// ---- Marketplace, administration ---------------------------------------------------------------

async function offersTab(body) {
  const list = h('div', {});
  const load = async () => {
    const offers = await must('GET', `${V1}/marketplace/offers`);
    clear(list).append(table(offers, {
      columns: ['offerCode', 'title', 'kind', 'price', 'ownPrice', 'available', 'published'],
      empty: 'Aucune offre.',
      actions: (offer) => action(offer.published ? 'Retirer' : 'Publier', async () => {
        await must('POST', `${V1}/marketplace/offers/${offer.id}/${offer.published ? 'unpublish' : 'publish'}`);
        await load();
      }, { cls: 'ghost small', target: list }),
    }));
  };
  const kind = h('select', {}, h('option', { value: 'PRODUCT' }, 'Article de stock'), h('option', { value: 'SERVICE' }, 'Prestation'));
  const inputs = Object.fromEntries(['title', 'inventoryItemCode', 'locationCode', 'serviceItemCode', 'price', 'maxPerOrder'].map((name) => [name, h('input', {})]));
  const create = async () => {
    const payload = { kind: kind.value };
    Object.entries(inputs).forEach(([name, input]) => {
      if (input.value) payload[name] = name === 'price' || name === 'maxPerOrder' ? Number(input.value) : input.value;
    });
    await must('POST', `${V1}/marketplace/offers`, { body: payload });
    Object.values(inputs).forEach((input) => { input.value = ''; });
    await load();
  };
  body.append(h('div', { cls: 'card' }, h('h2', {}, 'Nouvelle offre'),
    h('div', { cls: 'row' }, field('Type', kind), field('Titre', inputs.title), field('Code article', inputs.inventoryItemCode),
      field('Emplacement', inputs.locationCode), field('Code prestation', inputs.serviceItemCode), field('Prix propre', inputs.price),
      field('Maximum par commande', inputs.maxPerOrder), action('Créer', create))),
    h('div', { cls: 'card' }, h('h2', {}, 'Offres'), list));
  await load();
}

async function ordersTab(body) {
  const list = h('div', {});
  const filter = h('select', { onChange: () => load() }, h('option', { value: '' }, 'Tous les statuts'),
    ['PENDING_PAYMENT', 'PAID', 'FULFILLED', 'CANCELLED', 'EXPIRED'].map((value) => h('option', { value }, value)));
  const stock = h('input', { type: 'checkbox', onChange: () => load() });
  const load = async () => {
    const orders = await must('GET', `${V1}/marketplace/orders`, { query: stock.checked ? { stockPending: true } : { status: filter.value } });
    clear(list).append(table(orders, {
      columns: ['orderNumber', 'status', 'buyerMemberCode', 'billingCompanyCode', 'totalAmount', 'invoiceNumber', 'invoiceBalanceDue', 'stockPending', 'expiresAt'],
      empty: 'Aucune commande.',
      actions: (order) => [
        order.status === 'PAID' && action('Remettre', async () => {
          const collectedBy = prompt('Retirée par (facultatif)') || undefined;
          await must('POST', `${V1}/marketplace/orders/${key(order.orderNumber)}/fulfil`, { body: { collectedBy } });
          await load();
        }, { cls: 'small', target: list }),
        ['PENDING_PAYMENT', 'PAID', 'FULFILLED'].includes(order.status) && action('Annuler', async () => {
          const reason = prompt('Motif de l\'annulation');
          if (!reason) return;
          const body2 = { reason };
          if (order.status === 'FULFILLED') body2.returnToStock = confirm('Remettre les articles en stock ?');
          await must('POST', `${V1}/marketplace/orders/${key(order.orderNumber)}/cancel`, { body: body2 });
          await load();
        }, { cls: 'ghost small', target: list }),
      ],
    }));
  };
  body.append(h('div', { cls: 'card' },
    h('div', { cls: 'row' }, field('Statut', filter), h('label', {}, stock, ' Stock à corriger')),
    list));
  await load();
}

export async function marketplaceAdminScreen(root) {
  root.append(h('h1', {}, 'Marketplace · administration'));
  tabs(root, [['Commandes', ordersTab], ['Offres', offersTab]]);
}

// ---- Marketplace, portail ------------------------------------------------------------------------

async function shopTab(body) {
  const cart = h('div', {});
  const loadCart = async () => {
    const order = await must('GET', `${V1}/client/marketplace/cart`);
    const company = h('input', { placeholder: 'Vide, NONE, ou code organisation' });
    clear(cart).append(h('h2', {}, 'Mon panier'),
      table(order.lines, {
        columns: ['title', 'quantity', 'unitPrice', 'amount', 'available'],
        empty: 'Panier vide.',
        actions: (line) => [
          action('+1', async () => { await must('PATCH', `${V1}/client/marketplace/cart/lines/${line.id}`, { body: { quantity: line.quantity + 1 } }); await loadCart(); }, { cls: 'ghost small', target: cart }),
          action('-1', async () => { await must('PATCH', `${V1}/client/marketplace/cart/lines/${line.id}`, { body: { quantity: line.quantity - 1 } }); await loadCart(); }, { cls: 'ghost small', target: cart }),
        ],
      }),
      order.lines.length > 0 && h('div', { cls: 'row', style: 'margin-top:10px' },
        h('strong', {}, `Total : ${amount(order.totalAmount, order.currency)}`), field('Payeur', company),
        action('Valider la commande', async () => {
          const placed = await must('POST', `${V1}/client/marketplace/cart/checkout`, { body: { billingCompanyCode: company.value || null } });
          clear(cart).append(notice('ok', `Commande ${placed.orderNumber} validée, facture ${placed.invoiceNumber}.`));
        }, { target: cart })));
  };
  const offers = await must('GET', `${V1}/client/marketplace/offers`);
  body.append(h('div', { cls: 'card' }, h('h2', {}, 'Vitrine'), table(offers, {
    columns: ['title', 'kind', 'price', 'currency', 'available', 'maxPerOrder'],
    empty: 'Aucune offre publiée.',
    actions: (offer) => action('Ajouter', async () => {
      await must('POST', `${V1}/client/marketplace/cart/lines`, { body: { offerId: offer.id, quantity: 1 } });
      await loadCart();
    }, { cls: 'small' }),
  })), h('div', { cls: 'card' }, cart));
  await loadCart();
}

async function myOrdersTab(body) {
  const load = async () => {
    const orders = await must('GET', `${V1}/client/marketplace/orders`);
    clear(body).append(h('div', { cls: 'card' }, table(orders, {
      columns: ['orderNumber', 'status', 'totalAmount', 'invoiceNumber', 'invoiceBalanceDue', 'expiresAt'],
      empty: 'Aucune commande.',
      actions: (order) => order.status === 'PENDING_PAYMENT' && action('Annuler', async () => {
        await must('POST', `${V1}/client/marketplace/orders/${key(order.orderNumber)}/cancel`, { body: {} });
        await load();
      }, { cls: 'ghost small' }),
    })));
  };
  await load();
}

export async function marketplaceClientScreen(root) {
  root.append(h('h1', {}, 'Marketplace · portail'),
    h('p', { cls: 'muted' }, 'À tester connecté avec un compte membre.'));
  tabs(root, [['Boutique', shopTab], ['Mes commandes', myOrdersTab]]);
}

// ---- Contrats ------------------------------------------------------------------------------------

const VERSION_KIND = { ORIGINAL: 'Contrat d\'origine', AMENDMENT: 'Avenant', EXTENSION: 'Extension de date' };

export async function contractsScreen(root) {
  root.append(h('h1', {}, 'Contrats et versions'),
    h('p', { cls: 'muted' }, 'Le contrat, puis chaque avenant et chaque extension de date. La dernière version est en vigueur.'));
  const code = h('input', { placeholder: 'Code du contrat' });
  const body = h('div', {});
  const load = async () => {
    const [contract, versions] = await Promise.all([
      must('GET', `${V1}/contracts/${key(code.value)}`),
      must('GET', `${V1}/contracts/${key(code.value)}/versions`),
    ]);
    clear(body).append(
      h('div', { cls: 'card' }, h('h2', {}, contract.title || contract.contractCode), h('p', {}, status(contract.status)),
        keyValues({ contractCode: contract.contractCode, startDate: contract.startDate, endDate: contract.endDate, ownerCode: contract.ownerCode, renewalType: contract.renewalType })),
      h('div', { cls: 'card' }, h('h2', {}, `Versions (${versions.length})`), h('div', { cls: 'timeline' }, versions.map((version) =>
        h('div', { cls: 'step' },
          h('strong', {}, `Version ${version.version} · ${VERSION_KIND[version.kind] || version.kind}`),
          version.amendmentCode && h('span', { cls: 'mono', style: 'margin-left:6px' }, version.amendmentCode),
          h('div', {}, version.description),
          h('div', { cls: 'hint' }, version.previousEndDate
            ? `Fin : ${version.previousEndDate} → ${version.endDate}`
            : version.endDate ? `Fin : ${version.endDate}` : 'Sans changement de date'),
          version.at && h('div', { cls: 'hint' }, new Date(version.at).toLocaleString('fr-FR')))))));
  };
  const list = await call('GET', `${V1}/contracts`, { query: { size: 100, sort: 'createdAt,desc' } });
  const known = rowsOf(list.data) || [];
  const picker = known.length > 0 && h('select', { onChange: (event) => { code.value = event.target.value; load().catch((error) => clear(body).append(notice('bad', error.message))); } },
    h('option', { value: '' }, 'Choisir un contrat'), known.map((item) => h('option', { value: item.contractCode }, `${item.contractCode} · ${item.title || ''} · ${item.status}`)));
  root.append(h('div', { cls: 'card row' }, picker && field('Contrat', picker), field('Code', code), action('Charger', load, { target: body })), body);
}

// ---- Renouvellement ------------------------------------------------------------------------------

export async function renewalScreen(root) {
  root.append(h('h1', {}, 'Renouvellement d\'abonnement'),
    h('p', { cls: 'muted' }, 'La période, les droits et le contrat n\'avancent qu\'au paiement de la facture.'));
  const number = h('input', { placeholder: 'Numéro d\'abonnement' });
  const body = h('div', {});
  const load = async () => {
    const subscription = await must('GET', `${V1}/subscriptions/${key(number.value)}`);
    clear(body).append(h('div', { cls: 'card' },
      h('h2', {}, subscription.subscriptionNumber), h('p', {}, status(subscription.status)),
      keyValues({ currentPeriodStart: subscription.currentPeriodStart, currentPeriodEnd: subscription.currentPeriodEnd, nextBillingDate: subscription.nextBillingDate, autoRenew: subscription.autoRenew, contractCode: subscription.contractCode })),
    subscription.pendingRenewalInvoice
      ? h('div', { cls: 'card' }, notice('warn', `Renouvellement en attente du paiement de la facture ${subscription.pendingRenewalInvoice}.`),
        keyValues({ pendingRenewalStart: subscription.pendingRenewalStart, pendingRenewalEnd: subscription.pendingRenewalEnd }))
      : h('div', { cls: 'card' }, notice('ok', 'Aucun renouvellement en attente.')),
    h('div', { cls: 'card' }, action('Renouveler maintenant', async () => {
      await must('PATCH', `${V1}/subscriptions/${key(number.value)}/renew`);
      await load();
    })));
  };
  const list = await call('GET', `${V1}/subscriptions`, { query: { size: 100 } });
  const known = rowsOf(list.data) || [];
  const picker = known.length > 0 && h('select', { onChange: (event) => { number.value = event.target.value; load().catch((error) => clear(body).append(notice('bad', error.message))); } },
    h('option', { value: '' }, 'Choisir un abonnement'), known.map((item) => h('option', { value: item.subscriptionNumber }, `${item.subscriptionNumber} · ${item.status}`)));
  root.append(h('div', { cls: 'card row' }, picker && field('Abonnement', picker), field('Numéro', number), action('Charger', load, { target: body })), body);
}

export { errorOf, download };
