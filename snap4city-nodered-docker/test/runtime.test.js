'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
test('Avvio reale, API protette e redirect Keycloak con callback corretta', { timeout: 60000 }, async t => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'snap4city-smoke-'));
  const settingsFile = path.join(dir, 'settings.js');
  // Node-RED 2.2.2 invokes npm.cmd directly, unsupported by modern Windows Node.
  // Disable only palette installation in this Windows smoke test, not in Docker.
  fs.writeFileSync(settingsFile, `module.exports = require(${JSON.stringify(path.resolve('config/settings.js'))});\n` +
    (process.platform === 'win32' ? 'module.exports.externalModules = {palette:{allowInstall:false}};\n' : ''));
  const child = spawn(process.env.RUNTIME_NODE || process.execPath, [
    'node_modules/node-red/red.js', '--userDir', dir, '--settings', settingsFile
  ], { env: { ...process.env,
    APP_ID: 'test-app', PUBLIC_URL: 'http://127.0.0.1:1880/nodered/test-app',
    KEYCLOAK_REALM_URL: 'https://sso.example.org/realms/test',
    KEYCLOAK_CLIENT_ID: 'nodered-edge', KEYCLOAK_CLIENT_TYPE: 'public', KEYCLOAK_CLIENT_SECRET: '',
    NODE_RED_CREDENTIAL_SECRET: 'test-only-credential-key',
    OWNERSHIP_ENDPOINT: 'https://ownership.example.org/list',
    PROCESS_LOADER_URL: 'https://resources.example.org/api'
  }, windowsHide: true });
  let output = '';
  child.stdout.on('data', d => { output += d; });
  child.stderr.on('data', d => { output += d; });
  t.after(() => { child.kill(); });
  await new Promise((resolve, reject) => {
    const started = Date.now();
    const timer = setInterval(() => {
      if (output.includes('Started flows')) { clearInterval(timer); resolve(); }
      else if (child.exitCode !== null || output.includes('Failed to start server') || Date.now() - started > 45000) {
        clearInterval(timer); reject(new Error(output));
      }
    }, 100);
  });
  const base = 'http://127.0.0.1:1880/nodered/test-app';
  assert.match(output, /Node-RED version: v2\.2\.2/);
  assert.equal((await fetch(base + '/flows')).status, 401);
  assert.equal((await fetch(base + '/authentication/refreshtoken')).status, 401);
  const login = await (await fetch(base + '/auth/login')).json();
  assert.equal(login.type, 'strategy');
  const response = await fetch(base + '/auth/strategy', { redirect: 'manual' });
  assert.equal(response.status, 302);
  const redirect = new URL(response.headers.get('location'));
  assert.equal(redirect.origin, 'https://sso.example.org');
  assert.equal(redirect.searchParams.get('redirect_uri'), base + '/auth/strategy/callback');
  assert.ok(redirect.searchParams.get('state'));
  assert.equal(redirect.searchParams.get('client_id'), 'nodered-edge');
  assert.equal(redirect.searchParams.get('code_challenge_method'), 'S256');
  assert.match(redirect.searchParams.get('code_challenge'), /^[A-Za-z0-9_-]{43}$/);
  assert.equal((await fetch(base + '/red/red.js')).status, 200);
});
