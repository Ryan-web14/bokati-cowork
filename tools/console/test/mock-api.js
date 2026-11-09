// Un faux serveur · pour regarder le rendu de la console sans base ni compte. `node tools/console/test/mock-api.js`
// lance une API factice sur 8099 et la console dessus sur 5198.
const http = require('node:http');

const settings = [
  { key: 'bokati.marketplace.payment-window-minutes', label: 'Délai de paiement d\'une commande (minutes)', domain: 'MARKETPLACE', type: 'INTEGER', effect: 'LIVE', value: '45', defaultValue: '30', modified: true, editable: true, configured: null, restartPending: false, options: [] },
  { key: 'bokati.marketplace.sync-delay-ms', label: 'Cadence de suivi des commandes (ms)', domain: 'MARKETPLACE', type: 'INTEGER', effect: 'RESTART', value: '120000', defaultValue: '60000', modified: true, editable: true, configured: null, restartPending: true, options: [] },
  { key: 'app.security.jwt.secret', label: 'Secret des jetons', domain: 'MARKETPLACE', type: 'SECRET', effect: 'RESTART', value: null, defaultValue: null, modified: false, editable: false, configured: true, restartPending: false, options: [] },
];
const spec = {
  paths: {
    '/sni/api/v1/members': { get: { summary: 'Lister les membres', parameters: [{ name: 'pageable', in: 'query', schema: { $ref: '#/components/schemas/Pageable' } }] } },
    '/sni/api/v1/marketplace/offers': { post: { summary: 'Créer', requestBody: { content: { 'application/json': { schema: { type: 'object', required: ['title'], properties: { title: { type: 'string' }, kind: { type: 'string', enum: ['PRODUCT', 'SERVICE'] }, price: { type: 'number' } } } } } } } },
    '/sni/api/v1/client/marketplace/cart': { get: {} },
  },
  components: { schemas: { Pageable: { type: 'object', properties: { page: { type: 'integer' }, size: { type: 'integer' } } } } },
};
const routes = {
  'POST /sni/api/v1/auth/login': { accessToken: 't', refreshToken: 'r', roles: ['ROLE_SUPER_ADMIN'] },
  'GET /v3/api-docs': spec,
  'GET /sni/api/v1/members': { data: [{ memberId: 'MBR-1', firstname: 'Awa', status: 'ACTIVE', id: 7315645027609924123 }, { memberId: 'MBR-2', firstname: 'Jean', status: 'SUSPENDED' }], pageable: { page: 0, totalPages: 1, totalElements: 2 } },
  'GET /sni/api/v1/admin/settings/domains': [{ domain: 'MARKETPLACE', label: 'Marketplace', adminEditable: true, settings: 3 }],
  'GET /sni/api/v1/admin/settings': settings,
  'GET /sni/api/v1/resources': { data: [{ code: 'RES-1', name: 'Salle Baobab' }] },
  'GET /sni/api/v1/resources/RES-1/opening-hours': { resourceCode: 'RES-1', custom: true, hours: [{ dayOfWeek: 'MONDAY', open: '09:00:00', close: '13:00:00' }, { dayOfWeek: 'TUESDAY', open: '10:00:00', close: '16:00:00' }] },
  'GET /sni/api/v1/marketplace/orders': [{ orderNumber: 'MKC-2026-000001', status: 'PAID', buyerMemberCode: 'MBR-1', totalAmount: 4000, invoiceNumber: 'FAC-1', invoiceBalanceDue: 0, stockPending: true }],
  'GET /sni/api/v1/contracts': { data: [{ contractCode: 'CTR-1', title: 'Contrat abonnement', status: 'AMENDED' }] },
  'GET /sni/api/v1/contracts/CTR-1': { contractCode: 'CTR-1', title: 'Contrat abonnement SUB-1', status: 'AMENDED', startDate: '2026-09-01', endDate: '2026-12-31', ownerCode: 'MBR-1', renewalType: 'AUTO_RENEW' },
  'GET /sni/api/v1/contracts/CTR-1/versions': [
    { version: 1, kind: 'ORIGINAL', description: 'Contrat abonnement SUB-1', endDate: '2026-09-30', at: '2026-09-01T08:00:00Z' },
    { version: 2, kind: 'AMENDMENT', amendmentCode: 'AVN-1', description: 'Renouvellement de l\'abonnement SUB-1', previousEndDate: '2026-09-30', endDate: '2026-11-30', at: '2026-11-07T08:00:00Z' },
    { version: 3, kind: 'EXTENSION', description: 'Reprise après une pause', previousEndDate: '2026-11-30', endDate: '2026-12-31', at: '2026-11-20T08:00:00Z' }],
};

Object.assign(routes, {
  'GET /sni/api/v1/coupons/batches': { data: [{ batchCode: 'CPB-2026-0001', name: 'Partenariat rentree', promotionCode: 'PROMO-RENTREE', couponKind: 'SINGLE_USE', totalCoupons: 3, totalRedemptions: 1, validUntil: '2026-12-31T23:00:00Z', createdAt: '2026-11-09T10:00:00Z' }], pageable: { page: 0, totalPages: 1, totalElements: 1 } },
  'GET /sni/api/v1/coupons/batches/CPB-2026-0001': { batchCode: 'CPB-2026-0001', name: 'Partenariat rentree', promotionCode: 'PROMO-RENTREE', promotionName: 'Rentree -20 %', couponKind: 'SINGLE_USE', totalCoupons: 3, countsByStatus: { ACTIVE: 1, USED: 1, EXPIRED: 0, REVOKED: 1 }, totalRedemptions: 1, totalReserved: 0 },
  'GET /sni/api/v1/coupons/batches/CPB-2026-0001/coupons': { data: [{ code: 'RENTREE-7K2P', status: 'ACTIVE', redemptionCount: 0, maxRedemptions: 1, reservedCount: 0 }, { code: 'RENTREE-Q8XZ', status: 'USED', redemptionCount: 1, maxRedemptions: 1 }, { code: 'RENTREE-M3TT', status: 'REVOKED', revokedReason: 'Fuite' }], pageable: { page: 0, totalPages: 1, totalElements: 3 } },
  'GET /sni/api/v1/referrals/programs': [{ code: 'AMI-2026', name: 'Parrainez un ami', status: 'ACTIVE', open: true, qualificationRule: 'FIRST_PAYMENT', links: 4, maxReferralsPerReferrer: 10 }],
  'GET /sni/api/v1/referrals': { data: [{ referralNumber: 'PAR-202611-000001', programCode: 'AMI-2026', referrerCode: 'MBR-1', refereeCode: 'MBR-9', status: 'PENDING', registeredAt: '2026-11-09T09:00:00Z' }], pageable: { page: 0, totalPages: 1, totalElements: 1 } },
  'GET /sni/api/v1/client/referrals/programs': [{ code: 'AMI-2026', name: 'Parrainez un ami', description: 'Invitez un ami au coworking.', referrerPromotion: { code: 'P1', name: '5 000 XAF sur votre portefeuille' }, refereePromotion: { code: 'P2', name: '-20 % sur le premier mois' }, qualificationRule: 'FIRST_PAYMENT', myLinkUrl: 'https://elleaose.com/inscription?parrain=K7P2QX' }],
  'GET /sni/api/v1/client/referrals/links': [{ code: 'K7P2QX', programName: 'Parrainez un ami', clickCount: 12, signupCount: 2, active: true, url: 'https://elleaose.com/inscription?parrain=K7P2QX' }],
  'GET /sni/api/v1/client/referrals': [{ referralNumber: 'PAR-202611-000001', programName: 'Parrainez un ami', friend: 'Jean M.', status: 'REWARDED', registeredAt: '2026-11-01T09:00:00Z', rewardGranted: true, rewardAmount: 5000, currency: 'XAF' }],
  'GET /sni/api/v1/ged/items': { folder: null, breadcrumb: [], access: { level: 'ADMIN' }, items: [{ kind: 'FOLDER', code: 'FLD-1', name: 'Direction', documentsCount: 12, restricted: true }, { kind: 'FILE', code: 'DOC-1', name: 'statuts.pdf', size: 220000, mimeType: 'application/pdf', updatedAt: '2026-11-05T10:00:00Z', lock: { lockedBy: 'alice@exemple.cg', mine: false } }], page: { totalElements: 2 } },
  'GET /sni/api/v1/ged/search': { query: 'bail', totalElements: 1, hits: [{ item: { name: 'scan-0042.pdf' }, location: 'Juridique / Baux', snippet: '...Contrat de ⟦bail⟧ commercial, ⟦Echeance⟧ du loyer...', rank: 1.2 }] },
  'GET /sni/api/v1/ged/folders/FLD-1/access': { folderCode: 'FLD-1', restricted: true, restrictedAt: '2026-11-09T10:00:00Z', restrictedBy: 'admin@local', systemFolder: false, inheritedFrom: [], myLevel: 'ADMIN', canManage: true,
    grants: [{ granteeType: 'USER', grantee: 'admin@local', level: 'ADMIN', grantedAt: '2026-11-09T10:00:00Z' }, { granteeType: 'ROLE', grantee: 'STAFF', granteeLabel: 'Personnel', level: 'DOWNLOAD', grantedBy: 'admin@local', grantedAt: '2026-11-09T10:05:00Z' }] },
  'GET /sni/api/v1/ged/text/status': { pending: 3, byStatus: { DONE: 40, UNSUPPORTED: 2 } },
  'GET /sni/api/v1/ged/trash': [{ batch: 'b1', trashedAt: '2026-11-08T10:00:00Z', folders: 1, documents: 3, items: [{ name: 'Archives 2019' }] }],
});

const api = http.createServer((req, res) => {
  const key = `${req.method} ${req.url.split('?')[0]}`;
  const found = routes[key];
  res.writeHead(found ? 200 : 404, { 'content-type': 'application/json' });
  // Un grand identifiant part en nombre, comme la vraie API.
  res.end(JSON.stringify(found ?? { message: `Route absente du faux serveur : ${key}` }).replace('7315645027609924000', '7315645027609924123'));
});

api.listen(8099, () => {
  process.env.CONSOLE_API = 'http://localhost:8099';
  const { handler } = require('../server.js');
  http.createServer((req, res) => {
    // Ouvre un ecran deja connecte, pour les captures.
    if (req.url.startsWith('/__open')) {
      const to = new URL(req.url, 'http://x').searchParams.get('to') || '/';
      res.writeHead(200, { 'content-type': 'text/html' });
      res.end(`<script>sessionStorage.setItem('bokati-console-session', JSON.stringify({accessToken:'t',refreshToken:'r',email:'admin@local',roles:Array.from({length:190},(_,i)=>'PERMISSION_'+i)}));location.replace('/#${to}')</script>`);
      return;
    }
    handler(req, res);
  }).listen(5198, () => console.log('Console factice : http://localhost:5198'));
});
