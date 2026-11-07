// `node --test tools/console/test/`
import assert from 'node:assert/strict';
import test from 'node:test';
import { buildModules, coerce, columnsOf, formFields, moduleOf, parseJson, resolve, rowsOf } from '../public/js/spec.js';

const spec = {
  paths: {
    '/sni/api/v1/marketplace/offers': {
      get: { parameters: [{ name: 'pageable', in: 'query', schema: { $ref: '#/components/schemas/Pageable' } }, { name: 'Authorization', in: 'header', schema: { type: 'string' } }] },
      post: { summary: 'Créer', requestBody: { required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/Offer' } } } } },
    },
    '/sni/api/v1/marketplace/offers/{id}': { delete: { parameters: [{ name: 'id', in: 'path', required: true, schema: { type: 'integer' } }] } },
    '/sni/api/v1/client/marketplace/cart': { get: {} },
    '/sni/api/v1/client/{id}': { get: {} },
  },
  components: {
    schemas: {
      Pageable: { type: 'object', properties: { page: { type: 'integer' }, size: { type: 'integer' }, sort: { type: 'array' } } },
      Base: { type: 'object', required: ['title'], properties: { title: { type: 'string' } } },
      Offer: {
        allOf: [{ $ref: '#/components/schemas/Base' }, {
          type: 'object',
          properties: {
            kind: { type: 'string', enum: ['PRODUCT', 'SERVICE'] }, price: { type: 'number' }, published: { type: 'boolean' },
            startsOn: { type: 'string', format: 'date' }, lines: { type: 'array', items: { type: 'object' } }, id: { type: 'integer', readOnly: true },
          },
        }],
      },
      Loop: { type: 'object', properties: { next: { $ref: '#/components/schemas/Loop' } } },
    },
  },
};

test('les modules se forment sur le chemin, le portail a part', () => {
  assert.deepEqual(moduleOf('/sni/api/v1/marketplace/offers'), { key: 'marketplace', label: 'Marketplace', group: 'Administration' });
  assert.deepEqual(moduleOf('/sni/api/v1/client/marketplace/cart'), { key: 'client/marketplace', label: 'Marketplace', group: 'Portail client' });
  assert.equal(moduleOf('/sni/api/v1/client/{id}').key, 'client/accueil');
});

test('les routes sont classees, lecture d abord', () => {
  const modules = buildModules(spec);
  const marketplace = modules.find((module) => module.key === 'marketplace');
  assert.deepEqual(marketplace.operations.map((operation) => operation.method), ['GET', 'POST', 'DELETE']);
});

test('un Pageable se decompose et les en-tetes ne sont pas demandes', () => {
  const list = buildModules(spec).find((module) => module.key === 'marketplace').operations[0];
  assert.deepEqual(list.params.map((param) => param.name), ['page', 'size', 'sort']);
});

test('les champs suivent le schema, heritage compris, sans les champs en lecture seule', () => {
  const create = buildModules(spec).find((module) => module.key === 'marketplace').operations[1];
  const fields = formFields(spec, create.body.schema);
  const kinds = Object.fromEntries(fields.map((item) => [item.name, item.kind]));
  assert.deepEqual(kinds, { title: 'text', kind: 'enum', price: 'number', published: 'boolean', startsOn: 'date', lines: 'json' });
  assert.equal(fields.find((item) => item.name === 'title').required, true);
});

test('un schema cyclique ne boucle pas', () => {
  assert.doesNotThrow(() => resolve(spec, { $ref: '#/components/schemas/Loop' }));
});

test('une valeur saisie prend son type, vide elle n est pas envoyee', () => {
  assert.equal(coerce('number', '12.5'), 12.5);
  assert.equal(coerce('boolean', 'false'), false);
  assert.equal(coerce('text', ''), undefined);
  assert.deepEqual(coerce('json', '[1]'), [1]);
});

test('un grand identifiant garde tous ses chiffres', () => {
  const parsed = parseJson('{"id":7315645027609924123,"total":12,"list":[7315645027609924124]}');
  assert.equal(parsed.id, '7315645027609924123');
  assert.equal(parsed.total, 12);
  assert.equal(parsed.list[0], '7315645027609924124');
});

test('les lignes se trouvent dans une liste ou une page', () => {
  assert.deepEqual(rowsOf([1]), [1]);
  assert.deepEqual(rowsOf({ data: [2] }), [2]);
  assert.deepEqual(rowsOf({ content: [3] }), [3]);
  assert.equal(rowsOf({ id: 1 }), null);
});

test('les colonnes utiles viennent en tete, les objets ne font pas de colonne', () => {
  assert.deepEqual(columnsOf([{ note: 'x', status: 'ACTIVE', title: 'T', orderNumber: 'N', lines: [] }]), ['orderNumber', 'title', 'status', 'note']);
});
