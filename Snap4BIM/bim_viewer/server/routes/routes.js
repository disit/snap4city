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

const express = require('express');
const db = require("../db/mysql");
const router = express.Router();
const https = require('https');
const config = require("../../config");


/**
 * @returns pins based on the projec id (poid)
 */
router.get(config.api.projectPinsByPoid, (req, res) => {
    db.getPinsByPoid(req.params.poid, (err, result) => {
        if (err)
            res.status(400).json({ message: err.hasOwnProperty("message") ? err.message : "An error occurred" })
        else if (result == null)
            res.status(400).json({ message: "No content available" });
        else
            res.status(200).json(result);
    });
});

/**
 * @returns a pin based on the project id (poid) and pin title
 */
router.get(config.api.projectPinsByPoidAndTitle, (req, res) => {
    db.getPinByPoidAndTitle(req.params.poid, req.params.pinTitle, (err, result) => {
        if (err)
            res.status(400).json({ message: err.hasOwnProperty("message") ? err.message : "An error occurred" })
        else if (result == null)
            res.status(400).json({ message: "No content available!" });
        else
            res.status(200).json(result);
    });
});

/**
 * @returns pin properties using pin id (PK)
 */
router.get(config.api.projectPinById, (req, res) => {
    db.getPinById(req.params.id, (err, result) => {
        if (err)
            res.status(400).json({ message: err.hasOwnProperty("message") ? err.message : "An error occurred" })

        else if (result == null)
            res.status(400).json({ message: "No content available!" });
        else
            res.status(200).json(result);
    });
});

/** Insert new pin data
 * @returns the query result
 */
router.post(config.api.projectPinOperations, (req, res) => {
    db.setPin(req.body, (err, result) => {
        if (err) {
            res.status(400).json({ message: err.hasOwnProperty("message") ? err.message : "An error occurred" })
        } else if (result == null) {
            res.status(400).json({ message: "Error! Pin data fields missing." });
        } else
            res.status(200).json(result);
    });
});

/** Modifies pin data
 * @return query result
 */
router.put(config.api.projectPinOperations, (req, res) => {
    let id = req.body.id;
    delete req.body.id;

    db.updatePin(id, req.body, (err, result) => {
        if (err) {
            res.status(400).json({ message: err.hasOwnProperty("message") ? err.message : "An error occurred" })
        } else if (result == null) {
            let msg = "Updated pin with id: " + id;
            res.status(200).json({ message: msg });
        } else
            res.status(200).json(result);
    });
});

/** Deletes a pin from db
 * @returns query result
 */
router.delete(config.api.projectPinOperations, (req, res) => {
    console.log("DELETE method, pin:", req.body);
    db.deletePin(req.body.id, (err, result) => {
        if (err) {
            res.status(400).json({ message: err.hasOwnProperty("message") ? err.message : "An error occurred" })
        } else if (result == null) {
            let msg = "Deleted pin with id: " + req.body.id;
            res.status(400).json({ message: msg });
        } else
            res.status(200).json(result);
    });
});



/**
 * @fix this is used to fetch data from snap4city using serviceURI
 * Not sure if its the right solution
 * */
router.post(config.api.getServiceURIData, (req, res) => {
    let uri = req.body.serviceuri;
    
    let data = '';
    https.get(uri, (resp) => {
        resp.on('data', (chunk) => {
            data += chunk;
        });

        resp.on('end', () => {
            res.status(200).send(data);
        });

    }).on("error", (err) => {
        console.log("Error: " + err.message);
        res.status(400).json({"messge":"An error occured!"});
    });
});


module.exports = router;