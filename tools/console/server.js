// Console de test locale · sert les pages et relaie les appels vers l'API, ce qui evite tout souci de
// CORS. Aucune dependance : `node tools/console/server.js`.
//
//   CONSOLE_PORT   port de la console (5199)
//   CONSOLE_API    adresse de l'API (http://localhost:8080)

const http = require('node:http');
const https = require('node:https');
const fs = require('node:fs');
const path = require('node:path');

const PORT = Number(process.env.CONSOLE_PORT || 5199);
const API = new URL(process.env.CONSOLE_API || 'http://localhost:8080');
const ROOT = path.join(__dirname, 'public');
const TYPES = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8' };

function proxy(req, res) {
  const client = API.protocol === 'https:' ? https : http;
  const headers = { ...req.headers, host: API.host };
  delete headers.origin;
  delete headers.referer;
  const upstream = client.request({ hostname: API.hostname, port: API.port, path: req.url, method: req.method, headers }, (reply) => {
    res.writeHead(reply.statusCode, reply.headers);
    reply.pipe(res);
  });
  upstream.on('error', (error) => {
    res.writeHead(502, { 'content-type': 'application/json' });
    res.end(JSON.stringify({ message: `API injoignable sur ${API.origin} : ${error.message}` }));
  });
  req.pipe(upstream);
}

function serve(req, res) {
  const pathname = decodeURIComponent(new URL(req.url, 'http://console').pathname);
  const file = path.normalize(path.join(ROOT, pathname === '/' ? 'index.html' : pathname));
  if (!file.startsWith(ROOT) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) {
    res.writeHead(404);
    res.end('Introuvable');
    return;
  }
  res.writeHead(200, { 'content-type': TYPES[path.extname(file)] || 'application/octet-stream', 'cache-control': 'no-store' });
  fs.createReadStream(file).pipe(res);
}

function handler(req, res) {
  if (req.url.startsWith('/sni/') || req.url.startsWith('/v3/')) {
    proxy(req, res);
  } else {
    serve(req, res);
  }
}

if (require.main === module) {
  http.createServer(handler).listen(PORT, () => {
    console.log(`Console de test : http://localhost:${PORT}  ->  API ${API.origin}`);
  });
}

module.exports = { handler };
