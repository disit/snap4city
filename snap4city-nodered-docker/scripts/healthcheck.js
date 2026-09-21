'use strict';
const http = require('http');
const root = new URL(process.env.PUBLIC_URL).pathname.replace(/\/$/, '');
const req = http.get({ host: '127.0.0.1', port: 1880, path: root + '/auth/login', timeout: 4000 }, res => {
  res.resume();
  process.exitCode = res.statusCode === 200 ? 0 : 1;
});
req.on('timeout', () => req.destroy());
req.on('error', () => { process.exitCode = 1; });
