'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const vm = require('vm');
const path = require('path');
function fixture(ownerships, status = 200, clientSecret = 'secret&value') {
  const files = new Map([['/data/refresh_token-temp-alice', 'refresh&token']]);
  const requests = [];
  let refreshJob;
  function XHR() {
    this.open = (method, url) => { this.url = url; };
    this.setRequestHeader = () => {};
    this.send = body => {
      requests.push({ url: this.url, body });
      this.readyState = 4;
      this.status = this.url.includes('/token') ? 200 : status;
      this.responseText = JSON.stringify(this.url.includes('/token')
        ? { access_token: 'access', refresh_token: 'new-refresh' } : ownerships);
    };
  }
  class Issuer {
    constructor(config) { Object.assign(this, config); this.Client = class { constructor(c) { Object.assign(this, c); } }; }
  }
  const ctx = {
    module: { exports: {} }, process: { env: {} }, console: { log() {} },
    require(name) {
      if (name === 'fs') return {
        existsSync: p => files.has(p), readFileSync: p => files.get(p),
        writeFileSync: (p, v) => files.set(p, v)
      };
      if (name === 'node-schedule') return { scheduleJob(cron, job) { refreshJob = job; } };
      if (name === 'xmlhttprequest') return { XMLHttpRequest: XHR };
      if (name === './lib/openid-client') return { Issuer, Strategy: function () {} };
      throw new Error(name);
    }
  };
  vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../node_modules/snap4city-user-authentication/snap4city-user-authentication.js'), 'utf8'), ctx);
  ctx.module.exports.init('app', 'https://test/ownership', 'https://test/nodered/', '/data/refresh_token', 'https://test/realm', 'client', clientSecret, 'snap4city');
  return { auth: ctx.module.exports.auth(), files, requests, refreshJob };
}
test('Client pubblico usa PKCE e rinnova senza client_secret', async () => {
  const f = fixture([{ elementId: 'app', username: 'alice' }], 200, '');
  assert.equal(f.auth.strategy.options.client.token_endpoint_auth_method, 'none');
  assert.equal(f.auth.strategy.options.usePKCE, 'S256');
  assert.ok(await f.auth.users('alice'));
  f.refreshJob();
  for (const request of f.requests.filter(r => r.body)) {
    assert.doesNotMatch(request.body, /client_secret/);
    assert.match(request.body, /client_id=client/);
  }
});
test('Ownership autorizza il proprietario e conserva il refresh token', async () => {
  const f = fixture([{ elementId: 'app', username: 'alice' }]);
  assert.equal((await f.auth.users('alice')).permissions, '*');
  assert.equal(f.files.get('/data/refresh_token'), 'new-refresh');
  assert.match(f.requests[0].body, /secret%26value/);
  assert.match(f.requests[0].body, /refresh%26token/);
});
test('Ownership nega accesso a un utente senza la app richiesta', async () => {
  assert.equal(await fixture([{ elementId: 'other' }]).auth.users('alice'), null);
});
test('Ownership nega accesso su errore HTTP o risposta non valida', async () => {
  assert.equal(await fixture([{ elementId: 'app' }], 403).auth.users('alice'), null);
  assert.equal(await fixture({ error: 'failure' }).auth.users('alice'), null);
});
test('Un delegato non sovrascrive il token del proprietario', async () => {
  const f = fixture([{ elementId: 'app', username: 'owner' }]);
  assert.ok(await f.auth.users('alice'));
  assert.equal(f.files.has('/data/refresh_token'), false);
});
test('Username con traversal respinto prima di leggere file', async () => {
  const f = fixture([]);
  assert.equal(await f.auth.users('../alice'), null);
  assert.equal(f.requests.length, 0);
});
