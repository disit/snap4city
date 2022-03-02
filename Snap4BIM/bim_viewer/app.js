/* Snap4BIM.
   Copyright (C) 2022 DISIT Lab http://www.disit.org - University of Florence

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.

   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */

const express = require("express");
const bodyParser = require("body-parser");
const router = require('./server/routes/routes');
const config = require("./config");

const app = express();
const PORT = config.port;

app.use('/', express.static(__dirname + '/bimsurfer'));
//app.use('/images', express.static(__dirname + '/server/images'));

app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());

// register routes
app.use(config.api.baseApi, router);

app.listen(PORT)
console.log(`Server started on port: ${PORT}` );
