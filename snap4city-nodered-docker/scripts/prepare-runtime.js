'use strict';
const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '..');
const modules = path.join(root, 'node_modules');
const runtimeRoot = process.env.NR_RUNTIME_DIR || root;
const runtimeModules = path.join(runtimeRoot, 'node_modules');
if (runtimeRoot !== root) {
  // Node-RED discovers palette modules below its runtime node_modules directory.
  // Link every direct ADV palette dependency installed in /opt/snap4city.
  const directDependencies = Object.keys(require(path.join(root, 'package.json')).dependencies);
  for (const name of directDependencies.filter(name =>
    name === 'node-red-dashboard' || name.startsWith('node-red-contrib-') || name.startsWith('node-red-node-'))) {
    const link = path.join(runtimeModules, name);
    if (!fs.existsSync(link)) fs.symlinkSync(path.join(modules, name), link, 'dir');
  }
}
if (require(path.join(runtimeModules, 'node-red/package.json')).version !== '2.2.2') {
  throw new Error('La base deve contenere Node-RED 2.2.2');
}
// Resolve the actual package location: npm may nest @node-red packages.
for (const name of fs.readdirSync(path.join(root, 'upstream/@node-red'))) {
  const target = path.dirname(require.resolve(`@node-red/${name}/package.json`, {
    paths: [path.join(runtimeModules, 'node-red')]
  }));
  fs.cpSync(path.join(root, 'upstream/@node-red', name), target, { recursive: true });
  if (name === 'runtime') {
    const settingsFile = path.join(target, 'lib/api/settings.js');
    const settings = fs.readFileSync(settingsFile, 'utf8').replace(
      'version: runtime.settings.version',
      'version: runtime.settings.version,\nprocessLoaderUrl: runtime.settings.processLoaderUrl');
    fs.writeFileSync(settingsFile, settings);
  }
}
const target = path.join(modules, 'snap4city-user-authentication');
fs.cpSync(path.join(root, 'upstream/snap4city-user-authentication'), target, { recursive: true });
let auth = fs.readFileSync(path.join(target, 'snap4city-user-authentication.js'), 'utf8');
// Keep the original Snap4City protocol; remove token dumps and unsafe filename input.
auth = auth.replace(/console\.log\([^\n]*\^\^Got [^\n]*\);/g, 'console.log("SSO token exchange completed");');
auth = auth.replace('const params_scope = \'openid username profile offline_access\';',
  'const params_scope = process.env.SSO_SCOPE || "openid profile offline_access";');
auth = auth.replace('console.log((new Date()).toString() + "^^Logged user: %j", userinfo);',
  'userinfo.username = userinfo.username || userinfo.preferred_username;\n' +
  'if (!userinfo.username || !/^[a-zA-Z0-9@._-]+$/.test(userinfo.username) || !tokenset.refresh_token) { return done(new Error("Invalid SSO username or missing refresh token")); }');
auth = auth.replaceAll('"&client_secret=" + _keycloak_clientsecret', '"&client_secret=" + encodeURIComponent(_keycloak_clientsecret)');
auth = auth.replaceAll('"&client_secret=" + client.client_secret', '"&client_secret=" + encodeURIComponent(client.client_secret)');
auth = auth.replaceAll('"client_id=" + _keycloak_clientid', '"client_id=" + encodeURIComponent(_keycloak_clientid)');
auth = auth.replaceAll('"client_id=" + client.client_id', '"client_id=" + encodeURIComponent(client.client_id)');
auth = auth.replaceAll('"&refresh_token=" + old_refresh_token', '"&refresh_token=" + encodeURIComponent(old_refresh_token)');
auth = auth.replace('ownerships = JSON.parse(xmlHttp.responseText);',
  'var ownerships = xmlHttp.status === 200 ? JSON.parse(xmlHttp.responseText) : [];\n' +
  'if (!Array.isArray(ownerships)) { ownerships = []; }');
auth = auth.replace('return new Promise(function (resolve) {',
  'return new Promise(function (resolve) {\n' +
  'if (typeof username !== "string" || !/^[a-zA-Z0-9@._-]+$/.test(username)) { resolve(null); return; }');
auth = auth.replace('client_secret: _keycloak_clientsecret',
  'client_secret: _keycloak_clientsecret,\n' +
  'token_endpoint_auth_method: _keycloak_clientsecret ? "client_secret_basic" : "none"');
auth = auth.replace('client: client,', 'client: client,\nusePKCE: "S256",');
auth = auth.replaceAll('"&client_secret=" + encodeURIComponent(_keycloak_clientsecret)',
  '(_keycloak_clientsecret ? "&client_secret=" + encodeURIComponent(_keycloak_clientsecret) : "")');
auth = auth.replaceAll('"&client_secret=" + encodeURIComponent(client.client_secret)',
  '(client.client_secret ? "&client_secret=" + encodeURIComponent(client.client_secret) : "")');
fs.writeFileSync(path.join(target, 'snap4city-user-authentication.js'), auth);
console.log('Snap4City editor and SSO installed.');
