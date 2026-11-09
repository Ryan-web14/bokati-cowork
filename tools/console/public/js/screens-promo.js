// Ecrans dedies · lots de coupons, parrainage (administration et portail), explorateur de la GED.
import { V1, call, download, errorOf, must } from './api.js';
import { action, clear, field, h, keyValues, notice, table } from './ui.js';

const key = (value) => encodeURIComponent(value);
const amount = (value, currency = 'XAF') => (value === null || value === undefined ? '' : `${Number(value).toLocaleString('fr-FR')} ${currency}`);
const input = (attributes = {}) => h('input', { style: 'width:100%', ...attributes });
const valueOf = (control) => (control.value === '' ? undefined : control.value);

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

/** Un pied de pagination simple · {@code load(page)} recharge. */
function pager(payload, page, load) {
  const total = payload?.pageable?.totalPages || 1;
  return h('div', { cls: 'row', style: 'margin-top:8px;align-items:center' },
    h('button', { type: 'button', cls: 'ghost small', disabled: page <= 0, onClick: () => load(page - 1) }, 'Précédent'),
    h('span', { cls: 'hint' }, `Page ${page + 1} sur ${total} · ${payload?.pageable?.totalElements ?? 0} élément(s)`),
    h('button', { type: 'button', cls: 'ghost small', disabled: page + 1 >= total, onClick: () => load(page + 1) }, 'Suivant'));
}

/** Enregistre une reponse en fichier · un texte (CSV, TXT) arrive en texte, pas en blob. */
function save(result, name) {
  if (!result.ok) throw new Error(errorOf(result));
  download(result.blob ? result : { ...result, blob: new Blob([result.text ?? ''], { type: result.type || 'text/plain' }) }, name);
}

/** Une adresse absolue de l'API devient un chemin, pour passer par le serveur de la console. */
const pathOf = (url) => { if (!/^https?:/.test(url || '')) return url; const parsed = new URL(url); return parsed.pathname + parsed.search; };

const rows = (payload) => (Array.isArray(payload) ? payload : payload?.data || payload?.content || []);

// ---- Lots de coupons -----------------------------------------------------------------------------

async function batchDetail(body, batchCode, back) {
  const detail = h('div', {});
  const codes = h('div', {});
  const statusFilter = h('select', {}, ['', 'ACTIVE', 'USED', 'EXPIRED', 'REVOKED'].map((value) => h('option', { value }, value || 'Tous les statuts')));
  const reason = input({ placeholder: 'Motif de révocation du lot' });
  const loadDetail = async () => {
    const batch = await must('GET', `${V1}/coupons/batches/${key(batchCode)}`);
    clear(detail).append(h('div', { cls: 'card' },
      h('h2', {}, `${batch.name} · ${batch.batchCode}`),
      h('p', {}, Object.entries(batch.countsByStatus || {}).map(([state, count]) => h('span', { cls: 'badge', style: 'margin-right:6px' }, `${state} ${count}`))),
      keyValues(batch)));
  };
  const loadCodes = async (page = 0) => {
    const payload = await must('GET', `${V1}/coupons/batches/${key(batchCode)}/coupons`, { query: { status: valueOf(statusFilter), page, size: 50 } });
    clear(codes).append(table(rows(payload), {
      columns: ['code', 'status', 'assignedToCode', 'redemptionCount', 'maxRedemptions', 'reservedCount', 'validUntil', 'revokedReason'],
      empty: 'Aucun code.',
      actions: (coupon) => coupon.status === 'ACTIVE' && action('Révoquer', async () => {
        const why = prompt(`Motif de révocation de ${coupon.code}`);
        if (!why) return;
        await must('POST', `${V1}/coupons/${key(coupon.code)}/revoke`, { query: { reason: why } });
        await loadCodes(page); await loadDetail();
      }, { cls: 'ghost small' }),
    }), pager(payload, page, loadCodes));
  };
  clear(body).append(
    h('button', { type: 'button', cls: 'ghost small', onClick: back }, '← Tous les lots'),
    detail,
    h('div', { cls: 'card row', style: 'align-items:flex-end' },
      field('Statut', statusFilter), action('Filtrer', () => loadCodes(0)),
      action('Exporter en CSV', async () => {
        save(await call('GET', `${V1}/coupons/batches/${key(batchCode)}/export`), `coupons-${batchCode}.csv`);
      }, { cls: 'ghost' }),
      h('div', { cls: 'grow' }, field('Révoquer tout le lot', reason)),
      action('Révoquer le lot', async () => {
        if (!reason.value) throw new Error('Indiquez un motif.');
        const outcome = await must('POST', `${V1}/coupons/batches/${key(batchCode)}/revoke`, { query: { reason: reason.value } });
        detail.prepend(notice('ok', `${outcome.revoked} code(s) révoqué(s) · ${outcome.alreadyRevoked} déjà révoqué(s), ${outcome.exhausted} épuisé(s), ${outcome.expired} expiré(s)`));
        await loadCodes(0); await loadDetail();
      }, { cls: 'ghost' })),
    codes);
  await loadDetail();
  await loadCodes(0);
}

export async function couponsScreen(root) {
  root.append(h('h1', {}, 'Lots de coupons'),
    h('p', { cls: 'muted' }, 'Générer un lot de codes pour une promotion, le suivre, exporter ses codes, le révoquer.'));
  const body = h('div', {});
  root.append(body);

  const list = async () => {
    const promotion = input({ placeholder: 'PROMO-...' });
    const listing = h('div', {});
    const form = {
      promotionCode: input({ placeholder: 'Code de la promotion' }), name: input({ placeholder: 'Partenariat rentrée' }),
      couponKind: h('select', {}, ['SINGLE_USE', 'MULTI_USE', 'UNIQUE_PER_SUBSCRIBER'].map((value) => h('option', { value }, value))),
      quantity: input({ type: 'number', value: '10' }), codePrefix: input({ placeholder: 'RENTREE' }),
      validUntil: input({ type: 'datetime-local' }),
    };
    const load = async (page = 0) => {
      const payload = await must('GET', `${V1}/coupons/batches`, { query: { promotionCode: valueOf(promotion), page, size: 20 } });
      clear(listing).append(table(rows(payload), {
        columns: ['batchCode', 'name', 'promotionCode', 'couponKind', 'totalCoupons', 'totalRedemptions', 'validUntil', 'createdAt'],
        empty: 'Aucun lot.',
        actions: (batch) => h('button', { type: 'button', cls: 'small', onClick: () => batchDetail(body, batch.batchCode, list) }, 'Ouvrir'),
      }), pager(payload, page, load));
    };
    clear(body).append(
      h('div', { cls: 'card' }, h('h2', {}, 'Générer un lot'),
        h('div', { cls: 'row' }, field('Promotion', form.promotionCode, { required: true }), field('Nom', form.name, { required: true }),
          field('Type', form.couponKind, { required: true }), field('Quantité', form.quantity), field('Préfixe', form.codePrefix),
          field('Valide jusqu\'au', form.validUntil)),
        action('Générer', async () => {
          const result = await must('POST', `${V1}/coupons/batches`, { body: {
            promotionCode: form.promotionCode.value, name: form.name.value, couponKind: form.couponKind.value,
            quantity: Number(form.quantity.value || 1), codePrefix: valueOf(form.codePrefix),
            validUntil: form.validUntil.value ? new Date(form.validUntil.value).toISOString() : undefined,
          } });
          await batchDetail(body, result.batchCode, list);
        })),
      h('div', { cls: 'card row', style: 'align-items:flex-end' }, field('Promotion', promotion), action('Rechercher', () => load(0))),
      listing);
    await load(0);
  };
  await list();
}

// ---- Parrainage, administration ------------------------------------------------------------------

function programForm(program, done) {
  const f = {
    code: input({ value: program?.code || '', disabled: Boolean(program) }), name: input({ value: program?.name || '' }),
    description: input({ value: program?.description || '' }),
    referrerPromotionCode: input({ value: program?.referrerPromotion?.code || '', placeholder: 'Récompense du parrain' }),
    refereePromotionCode: input({ value: program?.refereePromotion?.code || '', placeholder: 'Récompense du filleul' }),
    qualificationRule: h('select', {}, ['FIRST_PAYMENT', 'SIGNUP', 'TENURE_REACHED'].map((value) => h('option', { value, selected: value === program?.qualificationRule }, value))),
    qualificationDelayDays: input({ type: 'number', value: program?.qualificationDelayDays ?? '' }),
    maxReferralsPerReferrer: input({ type: 'number', value: program?.maxReferralsPerReferrer ?? '' }),
  };
  const payload = () => ({
    code: f.code.value, name: f.name.value, description: valueOf(f.description),
    referrerPromotionCode: valueOf(f.referrerPromotionCode), refereePromotionCode: valueOf(f.refereePromotionCode),
    qualificationRule: f.qualificationRule.value,
    qualificationDelayDays: f.qualificationDelayDays.value ? Number(f.qualificationDelayDays.value) : undefined,
    maxReferralsPerReferrer: f.maxReferralsPerReferrer.value ? Number(f.maxReferralsPerReferrer.value) : undefined,
  });
  return h('div', { cls: 'card' }, h('h2', {}, program ? `Modifier ${program.code}` : 'Nouveau programme'),
    h('div', { cls: 'row' }, field('Code', f.code, { required: true }), field('Nom', f.name, { required: true }), field('Description', f.description)),
    h('div', { cls: 'row' }, field('Promotion du parrain', f.referrerPromotionCode), field('Promotion du filleul', f.refereePromotionCode),
      field('Qualification', f.qualificationRule), field('Délai (jours)', f.qualificationDelayDays), field('Plafond par parrain', f.maxReferralsPerReferrer)),
    h('p', { cls: 'hint' }, 'Au moins une récompense. Règle et récompenses ne changent plus une fois le programme ouvert.'),
    action(program ? 'Enregistrer' : 'Créer', async () => {
      const saved = program
        ? await must('PUT', `${V1}/referrals/programs/${key(program.code)}`, { body: payload() })
        : await must('POST', `${V1}/referrals/programs`, { body: payload() });
      await done(saved);
    }));
}

async function programDetail(body, code, back) {
  const program = await must('GET', `${V1}/referrals/programs/${key(code)}`);
  const links = h('div', {});
  const referrerType = h('select', {}, ['MEMBER', 'CUSTOMER'].map((value) => h('option', { value }, value)));
  const referrerCode = input({ placeholder: 'MBR-...' });
  const lifecycle = [
    ['DRAFT', 'activate', 'Ouvrir'], ['PAUSED', 'activate', 'Rouvrir'], ['ACTIVE', 'pause', 'Mettre en pause'],
  ].filter(([state]) => state === program.status);
  const loadLinks = async (page = 0) => {
    const payload = await must('GET', `${V1}/referrals/programs/${key(code)}/links`, { query: { page, size: 20 } });
    clear(links).append(table(rows(payload), {
      columns: ['code', 'referrerType', 'referrerCode', 'clickCount', 'signupCount', 'active', 'url'],
      empty: 'Aucun lien.',
      actions: (link) => action(link.active ? 'Couper' : 'Rouvrir', async () => {
        await must('POST', `${V1}/referrals/links/${key(link.code)}/${link.active ? 'deactivate' : 'activate'}`);
        await loadLinks(page);
      }, { cls: 'ghost small' }),
    }), pager(payload, page, loadLinks));
  };
  const reopen = () => programDetail(body, code, back);
  clear(body).append(
    h('button', { type: 'button', cls: 'ghost small', onClick: back }, '← Tous les programmes'),
    h('div', { cls: 'card' }, h('h2', {}, `${program.name} · ${program.code}`),
      h('p', {}, Object.entries(program.referralsByStatus || {}).map(([state, count]) => h('span', { cls: 'badge', style: 'margin-right:6px' }, `${state} ${count}`))),
      keyValues({ ...program, referrerPromotion: program.referrerPromotion?.code, refereePromotion: program.refereePromotion?.code }),
      h('div', { cls: 'row' },
        lifecycle.map(([, verb, label]) => action(label, async () => { await must('POST', `${V1}/referrals/programs/${key(code)}/${verb}`); await reopen(); })),
        program.status !== 'ENDED' && action('Clore', async () => {
          if (!confirm('Clore définitivement ce programme ?')) return;
          await must('POST', `${V1}/referrals/programs/${key(code)}/end`); await reopen();
        }, { cls: 'ghost' }))),
    program.status !== 'ENDED' ? programForm(program, reopen) : '',
    h('div', { cls: 'card row', style: 'align-items:flex-end' }, h('strong', {}, 'Créer le lien d\'un parrain'),
      field('Type', referrerType), field('Code', referrerCode),
      action('Créer le lien', async () => {
        const link = await must('POST', `${V1}/referrals/links`, { body: { programCode: code, referrerType: referrerType.value, referrerCode: referrerCode.value } });
        links.prepend(notice('ok', `Lien ${link.code} : ${link.url}`));
        await loadLinks(0);
      })),
    links);
  await loadLinks(0);
}

async function referralsList(body) {
  const program = input({ placeholder: 'Code du programme' });
  const state = h('select', {}, ['', 'PENDING', 'QUALIFIED', 'REWARDED', 'REJECTED'].map((value) => h('option', { value }, value || 'Tous')));
  const referrer = input({ placeholder: 'Code du parrain' });
  const list = h('div', {});
  const load = async (page = 0) => {
    const payload = await must('GET', `${V1}/referrals`, { query: { programCode: valueOf(program), status: valueOf(state), referrerCode: valueOf(referrer), page, size: 20 } });
    clear(list).append(table(rows(payload), {
      columns: ['referralNumber', 'programCode', 'referrerCode', 'refereeCode', 'status', 'registeredAt', 'referrerRewardAmount', 'refereeRewardAmount'],
      empty: 'Aucun parrainage.',
      actions: (referral) => referral.status !== 'REWARDED' && referral.status !== 'REJECTED' && action('Écarter', async () => {
        const why = prompt('Motif');
        if (!why) return;
        await must('POST', `${V1}/referrals/${key(referral.referralNumber)}/reject`, { query: { reason: why } });
        await load(page);
      }, { cls: 'ghost small' }),
    }), pager(payload, page, load));
  };
  body.append(h('div', { cls: 'card row', style: 'align-items:flex-end' }, field('Programme', program), field('Statut', state), field('Parrain', referrer), action('Rechercher', () => load(0))), list);
  await load(0);
}

export async function referralsAdminScreen(root) {
  root.append(h('h1', {}, 'Parrainage · administration'),
    h('p', { cls: 'muted' }, 'Programmes, liens des parrains et parrainages enregistrés.'));
  tabs(root, [
    ['Programmes', async (body) => {
      const back = () => { clear(body); render(); };
      const render = async () => {
        const programs = await must('GET', `${V1}/referrals/programs`);
        body.append(table(programs, {
          columns: ['code', 'name', 'status', 'open', 'qualificationRule', 'links', 'maxReferralsPerReferrer'],
          empty: 'Aucun programme. Il faut au moins une promotion de récompense pour en créer un.',
          actions: (program) => h('button', { type: 'button', cls: 'small', onClick: () => programDetail(body, program.code, back) }, 'Ouvrir'),
        }), programForm(null, (saved) => programDetail(body, saved.code, back)));
      };
      await render();
    }],
    ['Parrainages', referralsList],
    ['Page publique d\'un lien', async (body) => {
      const code = input({ placeholder: 'Code du lien' });
      const out = h('div', {});
      body.append(h('div', { cls: 'card row', style: 'align-items:flex-end' }, field('Code', code),
        action('Voir comme un visiteur', async () => clear(out).append(keyValues(await must('GET', `${V1}/public/referrals/${key(code.value)}`))))), out);
    }],
  ]);
}

// ---- Parrainage, portail du membre -----------------------------------------------------------------

export async function referralsClientScreen(root) {
  root.append(h('h1', {}, 'Parrainage · portail'),
    h('p', { cls: 'muted' }, 'Ce que voit un membre connecté : connectez-vous avec un compte membre.'));
  const programs = h('div', {});
  const mine = h('div', {});
  const load = async () => {
    const open = await must('GET', `${V1}/client/referrals/programs`);
    clear(programs).append(h('h2', {}, 'Programmes ouverts'), ...(open.length === 0 ? [h('p', { cls: 'muted' }, 'Aucun programme ouvert.')] : open.map((program) =>
      h('div', { cls: 'card' }, h('strong', {}, program.name), h('p', {}, program.description || ''),
        h('p', { cls: 'hint' }, `Vous : ${program.referrerPromotion?.name || 'rien'} · votre ami : ${program.refereePromotion?.name || 'rien'} · règle ${program.qualificationRule}`),
        program.myLinkUrl
          ? h('p', {}, h('span', { cls: 'mono' }, program.myLinkUrl), ' ', h('button', { type: 'button', cls: 'ghost small', onClick: () => navigator.clipboard?.writeText(program.myLinkUrl) }, 'Copier'))
          : action('Obtenir mon lien', async () => { await must('POST', `${V1}/client/referrals/links`, { body: { programCode: program.code } }); await load(); })))));
    const referrals = await must('GET', `${V1}/client/referrals`);
    const links = await must('GET', `${V1}/client/referrals/links`);
    clear(mine).append(h('h2', {}, 'Mes liens'), table(links, { columns: ['code', 'programName', 'clickCount', 'signupCount', 'active', 'url'], empty: 'Aucun lien.' }),
      h('h2', {}, 'Mes parrainages'), table(referrals.map((item) => ({ ...item, rewardAmount: amount(item.rewardAmount, item.currency || 'XAF') })),
        { columns: ['friend', 'programName', 'status', 'registeredAt', 'rewardGranted', 'rewardAmount'], empty: 'Aucun ami parrainé pour le moment.' }));
  };
  root.append(programs, mine);
  await load();
}

// ---- Explorateur de la GED --------------------------------------------------------------------------

const LEVELS = ['READ', 'DOWNLOAD', 'EDIT', 'DELETE', 'ADMIN'];

/** Le panneau d'acces d'un dossier : restreindre, rouvrir, donner et retirer des droits. */
async function accessPanel(target, folderCode, refresh) {
  const show = async () => {
    const view = await must('GET', `${V1}/ged/folders/${key(folderCode)}/access`);
    const type = h('select', {}, ['USER', 'ROLE'].map((value) => h('option', { value }, value === 'USER' ? 'Utilisateur (e-mail)' : 'Rôle (nom)')));
    const grantee = input({ placeholder: 'alice@exemple.cg ou STAFF' });
    const level = h('select', {}, LEVELS.map((value) => h('option', { value }, value)));
    clear(target).append(h('div', { cls: 'card' },
      h('h2', {}, `Accès · ${folderCode}`),
      h('p', {}, view.restricted ? h('span', { cls: 'badge warn' }, 'restreint') : h('span', { cls: 'badge' }, 'ouvert à tous'),
        ' Votre niveau : ', h('strong', {}, view.myLevel),
        view.inheritedFrom?.length ? ` · aussi restreint par : ${view.inheritedFrom.map((ref) => ref.name).join(' › ')}` : ''),
      view.systemFolder && h('p', { cls: 'hint' }, 'Dossier du plan de classement : il ne se restreint pas.'),
      view.restricted && table(view.grants, {
        columns: ['granteeType', 'grantee', 'granteeLabel', 'level', 'grantedBy', 'grantedAt'],
        empty: 'Aucun droit.',
        actions: (grant) => view.canManage && action('Retirer', async () => {
          await must('DELETE', `${V1}/ged/folders/${key(folderCode)}/grants`, { query: { granteeType: grant.granteeType, grantee: grant.grantee } });
          await show();
        }, { cls: 'ghost small' }),
      }),
      view.canManage && h('div', { cls: 'row', style: 'align-items:flex-end;margin-top:8px' },
        view.restricted && [field('Bénéficiaire', type), field('Qui', grantee), field('Niveau', level),
          action('Donner', async () => {
            await must('PUT', `${V1}/ged/folders/${key(folderCode)}/grants`, { body: { granteeType: type.value, grantee: grantee.value, level: level.value } });
            await show();
          })],
        !view.systemFolder && action(view.restricted ? 'Ouvrir à tous' : 'Restreindre', async () => {
          await must('POST', `${V1}/ged/folders/${key(folderCode)}/${view.restricted ? 'unrestrict' : 'restrict'}`);
          await show();
          await refresh();
        }, { cls: view.restricted ? 'ghost' : '' }))));
  };
  await show();
}

export async function gedScreen(root) {
  root.append(h('h1', {}, 'GED · explorateur'),
    h('p', { cls: 'muted' }, 'Parcourir les dossiers, déposer, numériser, chercher dans le contenu des fichiers, corbeille.'));
  tabs(root, [
    ['Parcourir', async (body) => {
      const view = h('div', {});
      const panel = h('div', {});
      const open = async (folderCode) => {
        const listing = await must('GET', `${V1}/ged/items`, { query: { folderCode, size: 100 } });
        const files = h('input', { type: 'file', multiple: true });
        const folderName = input({ placeholder: 'Nouveau dossier' });
        clear(view).append(
          h('p', {}, h('a', { href: '#', onClick: (event) => { event.preventDefault(); open(undefined); } }, 'Racine'),
            (listing.breadcrumb || []).map((crumb) => [' › ', h('a', { href: '#', onClick: (event) => { event.preventDefault(); open(crumb.code); } }, crumb.name)])),
          h('div', { cls: 'card row', style: 'align-items:flex-end' },
            field('Dossier', folderName), action('Créer', async () => { await must('POST', `${V1}/ged/folders`, { body: { name: folderName.value, parentCode: folderCode } }); await open(folderCode); }),
            field('Fichiers', files), action('Déposer', async () => {
              const failures = [];
              for (const file of files.files) {
                const form = new FormData();
                form.append('files', file);
                if (folderCode) form.append('folderCode', folderCode);
                const result = await call('POST', `${V1}/ged/files`, { form });
                if (!result.ok) failures.push(`${file.name} : ${errorOf(result)}`);
              }
              await open(folderCode);
              if (failures.length) view.prepend(notice('bad', failures.join(' · ')));
            })),
          listing.access ? h('p', { cls: 'hint' }, `Votre niveau ici : ${listing.access.level}`) : '',
          table((listing.items || []).map((item) => ({ ...item, locked: item.lock ? `${item.lock.lockedBy}${item.lock.mine ? ' (vous)' : ''}` : '' })), {
            columns: ['kind', 'name', 'updatedAt', 'size', 'mimeType', 'documentsCount', 'restricted', 'locked'],
            empty: 'Dossier vide.',
            actions: (item) => item.kind === 'FOLDER'
              ? h('span', {}, h('button', { type: 'button', cls: 'small', onClick: () => open(item.code) }, 'Ouvrir'),
                action('Accès', () => accessPanel(panel, item.code, () => open(folderCode)), { cls: 'ghost small' }))
              : h('span', {},
                item.downloadUrl && action('Télécharger', async () => save(await call('GET', pathOf(item.downloadUrl)), item.name), { cls: 'ghost small' }),
                action('Texte', async () => {
                  const text = await must('GET', `${V1}/ged/files/${key(item.code)}/text`);
                  view.append(h('div', { cls: 'card' }, h('h2', {}, `Texte de ${item.name}`), keyValues({ ...text, content: undefined }), h('pre', { cls: 'json' }, text.content || '(pas de texte)')));
                }, { cls: 'ghost small' }),
                action(item.lock ? 'Déverrouiller' : 'Verrouiller', async () => {
                  await must('POST', `${V1}/ged/files/${key(item.code)}/${item.lock ? 'unlock' : 'lock'}`);
                  await open(folderCode);
                }, { cls: 'ghost small' }),
                !item.protectedItem && action('Corbeille', async () => { await must('POST', `${V1}/ged/trash`, { body: { documentCodes: [item.code] } }); await open(folderCode); }, { cls: 'ghost small' })),
          }));
      };
      body.append(view, panel);
      await open(undefined);
    }],
    ['Rechercher', async (body) => {
      const q = input({ placeholder: 'echeance bail, "bail commercial", -brouillon' });
      const out = h('div', {});
      const highlight = (snippet) => h('span', {}, String(snippet || '').split(/(⟦[^⟧]*⟧)/).map((part) =>
        part.startsWith('⟦') ? h('mark', {}, part.slice(1, -1)) : part));
      body.append(h('div', { cls: 'card row', style: 'align-items:flex-end' }, h('div', { cls: 'grow' }, field('Recherche dans le contenu et le nom', q)),
        action('Chercher', async () => {
          const result = await must('GET', `${V1}/ged/search`, { query: { q: q.value } });
          clear(out).append(h('p', { cls: 'hint' }, `${result.totalElements} résultat(s)`), ...result.hits.map((hit) =>
            h('div', { cls: 'card' }, h('strong', {}, hit.item.name), ' ', h('span', { cls: 'hint' }, hit.location || 'Racine'),
              hit.snippet && h('p', {}, highlight(hit.snippet)))));
        })), out);
      const state = await call('GET', `${V1}/ged/text/status`);
      if (state.ok) body.append(h('p', { cls: 'hint' }, `Lecture des fichiers : ${state.data.pending} en attente · ${JSON.stringify(state.data.byStatus)}`),
        action('Lire un lot maintenant', async () => { const run = await must('POST', `${V1}/ged/text/run`); body.append(notice('ok', JSON.stringify(run))); }, { cls: 'ghost small' }));
    }],
    ['Numériser', async (body) => {
      const pages = h('input', { type: 'file', accept: 'image/*', multiple: true });
      const title = input({ placeholder: 'Courrier impots mars 2026' });
      const folderCode = input({ placeholder: 'Code du dossier (vide : racine)' });
      const out = h('div', {});
      body.append(h('div', { cls: 'card' }, h('p', { cls: 'hint' }, 'Les images, dans l\'ordre choisi, deviennent un seul PDF. 10 Mo au plus par envoi.'),
        h('div', { cls: 'row' }, field('Pages', pages), field('Titre', title), field('Dossier', folderCode)),
        action('Créer le PDF', async () => {
          const form = new FormData();
          [...pages.files].forEach((file) => form.append('pages', file));
          if (title.value) form.append('title', title.value);
          if (folderCode.value) form.append('folderCode', folderCode.value);
          const result = await call('POST', `${V1}/ged/scans`, { form });
          if (!result.ok) throw new Error(errorOf(result));
          clear(out).append(notice('ok', `Créé : ${result.data.name}`), keyValues(result.data));
        })), out);
    }],
    ['Corbeille', async (body) => {
      const load = async () => {
        const batches = await must('GET', `${V1}/ged/trash`);
        clear(body).append(
          h('p', { cls: 'hint' }, 'Les éléments sont détruits définitivement 30 jours après leur suppression.'),
          action('Vider toute la corbeille', async () => {
            const why = prompt('Destruction définitive et irréversible. Motif ?');
            if (!why) return;
            const result = await must('POST', `${V1}/ged/trash/empty`, { query: { olderThanDays: 0 }, body: { reason: why } });
            body.prepend(notice('ok', `${result.documents} fichier(s) détruit(s), ${result.heldSkipped} épargné(s) (gel), ${result.failed} échec(s)`));
            await load();
          }, { cls: 'ghost small' }),
          table(batches.map((batch) => ({ ...batch, items: (batch.items || []).map((item) => item.name).join(', ') })), {
            columns: ['trashedAt', 'folders', 'documents', 'items'],
            empty: 'La corbeille est vide.',
            actions: (batch) => h('span', {},
              action('Restaurer', async () => { await must('POST', `${V1}/ged/trash/${key(batch.batch)}/restore`); await load(); }, { cls: 'small' }),
              action('Détruire', async () => {
                const why = prompt('Destruction définitive et irréversible. Motif ?');
                if (!why) return;
                await must('POST', `${V1}/ged/trash/${key(batch.batch)}/purge`, { body: { reason: why } });
                await load();
              }, { cls: 'ghost small' })),
          }));
      };
      await load();
    }],
    ['Conservation', async (body) => {
      const holds = h('div', {});
      const due = h('div', {});
      const usage = h('div', {});
      const loadHolds = async () => {
        const list = await must('GET', `${V1}/ged/holds`, { query: { active: false } });
        clear(holds).append(h('h2', {}, 'Gels juridiques'), table(list, {
          columns: ['code', 'targetType', 'targetName', 'reason', 'reference', 'placedBy', 'placedAt', 'active', 'releasedAt'],
          empty: 'Aucun gel.',
          actions: (hold) => hold.active && action('Lever', async () => {
            const why = prompt('Motif de la levée');
            if (!why) return;
            await must('POST', `${V1}/ged/holds/${key(hold.code)}/release`, { body: { reason: why } });
            await loadHolds();
          }, { cls: 'ghost small' }),
        }));
        const type = h('select', {}, ['FOLDER', 'DOCUMENT'].map((value) => h('option', { value }, value === 'FOLDER' ? 'Dossier' : 'Fichier')));
        const code = input({ placeholder: 'FLD-... ou DOC-...' });
        const reason = input({ placeholder: 'Contentieux, contrôle fiscal...' });
        const reference = input({ placeholder: 'Référence (facultatif)' });
        holds.append(h('div', { cls: 'card row', style: 'align-items:flex-end' }, field('Cible', type), field('Code', code),
          h('div', { cls: 'grow' }, field('Motif', reason)), field('Référence', reference),
          action('Geler', async () => {
            await must('POST', `${V1}/ged/holds`, { body: { targetType: type.value, targetCode: code.value, reason: reason.value, reference: valueOf(reference) } });
            await loadHolds();
          })));
      };
      const loadDue = async () => {
        const list = await must('GET', `${V1}/ged/retention/due`);
        const rows = list.map((line) => ({ code: line.item.code, name: line.item.name, policyCode: line.policyCode, dueAt: line.dueAt, held: line.item.held, location: line.location }));
        const decide = (verb, label) => action(label, async () => {
          const codes = rows.filter((row) => !row.held).map((row) => row.code);
          if (codes.length === 0) return;
          const why = prompt(`${label} ${codes.length} fichier(s). Motif ?`);
          if (!why) return;
          await must('POST', `${V1}/ged/retention/${verb}`, { body: { documentCodes: codes, reason: why } });
          await loadDue();
        }, { cls: 'ghost small' });
        clear(due).append(h('h2', {}, 'À détruire'),
          h('div', { cls: 'row' }, action('Appliquer les règles maintenant', async () => {
            const run = await must('POST', `${V1}/ged/retention/run`);
            due.prepend(notice('ok', `${run.applied} fichier(s) traité(s)`));
            await loadDue();
          }, { cls: 'ghost small' }), rows.length > 0 ? decide('destroy', 'Détruire tout') : '', rows.length > 0 ? decide('keep', 'Garder tout') : ''),
          table(rows, { columns: ['name', 'policyCode', 'dueAt', 'held', 'location'], empty: 'Rien à détruire.' }));
      };
      const loadUsage = async () => {
        const folderCode = input({ placeholder: 'Code du dossier (vide : toute la GED)' });
        const quota = input({ type: 'number', placeholder: 'Quota en octets' });
        const out = h('div', {});
        clear(usage).append(h('h2', {}, 'Espace et quotas'),
          h('div', { cls: 'card row', style: 'align-items:flex-end' }, field('Dossier', folderCode),
            action('Voir', async () => clear(out).append(keyValues(await must('GET', `${V1}/ged/usage`, { query: { folderCode: valueOf(folderCode) } })))),
            field('Quota', quota),
            action('Fixer le quota', async () => {
              clear(out).append(keyValues(await must('PUT', `${V1}/ged/folders/${key(folderCode.value)}/quota`, { body: { quotaBytes: quota.value ? Number(quota.value) : null } })));
            }, { cls: 'ghost' })),
          out);
      };
      body.append(holds, due, usage);
      await loadHolds();
      await loadDue();
      await loadUsage();
    }],
  ]);
}
