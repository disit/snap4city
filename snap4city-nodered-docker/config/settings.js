'use strict';
const fs = require('fs');
process.umask(0o077);
function required(name) {
  const value = process.env[name];
  if (!value || value.includes('CHANGE_ME')) throw new Error(`Configurare ${name}`);
  return value;
}
function secret(name) {
  return process.env[name + '_FILE']
    ? fs.readFileSync(process.env[name + '_FILE'], 'utf8').trim()
    : required(name);
}
function url(name) {
  const value = required(name).replace(/\/$/, '');
  const parsed = new URL(value);
  if (!['https:', 'http:'].includes(parsed.protocol) || parsed.search || parsed.hash) {
    throw new Error(`URL non valido: ${name}`);
  }
  return value;
}
const appid = required('APP_ID');
if (!/^[a-zA-Z0-9_-]+$/.test(appid)) throw new Error('APP_ID non valido');
const publicUrl = url('PUBLIC_URL');
if (!new URL(publicUrl).pathname.endsWith('/' + appid)) {
  throw new Error('PUBLIC_URL deve terminare con /APP_ID');
}
const realm = url('KEYCLOAK_REALM_URL');
const clientid = required('KEYCLOAK_CLIENT_ID');
const clientType = process.env.KEYCLOAK_CLIENT_TYPE || 'confidential';
if (!['public', 'confidential'].includes(clientType)) throw new Error('KEYCLOAK_CLIENT_TYPE non valido');
const clientsecret = clientType === 'public' ? undefined : secret('KEYCLOAK_CLIENT_SECRET');
const ownership = url('OWNERSHIP_ENDPOINT') + '/';
const auth = require('snap4city-user-authentication');
auth.init(appid, ownership, publicUrl.slice(0, -appid.length), '/data/refresh_token',
  realm, clientid, clientsecret, new URL(realm).hostname);
module.exports = {
  APPID: appid,
  uiPort: 1880,
  uiHost: '0.0.0.0',
  flowFile: 'flows.json',
  httpRoot: new URL(publicUrl).pathname,
  credentialSecret: secret('NODE_RED_CREDENTIAL_SECRET'),
  adminAuth: auth.auth(),
  keycloakBaseUri: realm,
  keycloakClientid: clientid,
  keycloakClientsecret: clientsecret,
  processLoaderUrl: url('PROCESS_LOADER_URL'),
  dashboardManagerBaseUrl: process.env.DASHBOARD_MANAGER_URL,
  dashboardSmartCityUrl: process.env.DASHBOARD_MANAGER_URL,
  ownershipUrl: process.env.OWNERSHIP_BASE_URL,
  myPersonalDataUrl: process.env.PERSONAL_DATA_URL,
  ascapiUrl: process.env.SERVICEMAP_URL,
  iotDirectoryUrl: process.env.IOT_DIRECTORY_URL,
  wsServerUrl: process.env.WS_SERVER_URL,
  functionGlobalContext: {},
  logging: { console: { level: 'info', metrics: false, audit: false } }
};
