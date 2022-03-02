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

const mysql = require('mysql');
const moment = require('moment');
const config = require("../../config");


// MySQl connection properties
var con_config = {
    host: config.database.host,
    user: config.database.user,
    password: config.database.password,
    database: config.database.database
};

var connection;

function handleDisconnect() {
    connection = mysql.createConnection(con_config);

    connection.connect(function (err) {
        if (err) {
            console.log('error when connecting to db:', err);
            setTimeout(handleDisconnect, 2000);
        }
    });

    connection.on('error', function (err) {
        console.log('db error', err);
        if (err.code) { //=== 'PROTOCOL_CONNECTION_LOST') {
            handleDisconnect();
        } else {
            throw err;
        }
    });
}

handleDisconnect();


// Check if the pin object has all the required not null props
function pinProps(pin) {
    if (pin.poid === null || pin.poid === undefined) {
        return false;
    }
    if (pin.project_name === null || pin.project_name === undefined || pin.project_name === "") {
        return false;
    }
    if (pin.pin_reference_object_id === null || pin.pin_reference_object_id === undefined) {
        return false;
    }
    if (pin.pin_title === null || pin.pin_title === undefined || pin.pin_title === "") {
        return false;
    }
    if (pin.x === null || pin.x === undefined || pin.x === 0) {
        return false;
    }
    if (pin.y === null || pin.y === undefined || pin.y === 0) {
        return false;
    }
    if (pin.z === null || pin.z === undefined || pin.z === 0) {
        return false;
    }
    return true;
}

module.exports = {
    /**
     * 
     * @param {*} poid Project id
     * @param {*} callback A callback function
     * @returns An array of data that represent the pins to display
     */
    getPinsByPoid: function (poid, callback) {
        if (poid === null || poid === undefined) {
            return callback({ message: "Some fields should be not null" }, null);
        } else {
            let sql = "SELECT * FROM pins WHERE poid= ?";

            if (connection.state == "disconnected")
                return callback({ message: "Can't connect to database." }, null);
            connection.query(sql, poid, (err, results) => {
                if (err)
                    return callback(err, null);
                else if (results.length)
                    return callback(null, results);
                else
                    return callback(err, null);
            });


        }
    },

    /**
     * 
     * @param {*} poid The project id
     * @param {*} title Pin title (unique value)
     * @param {*} callback Callback funtion to handle the result of the query
     * @returns Returns the query result or an error message if empty
     */
    getPinByPoidAndTitle: function (poid, title, callback) {
        if (title === null || title === undefined || title === "" && poid === null || poid === undefined || poid === "") {
            return callback({ message: "Pin title should be not null" }, null);
        } else {
            let sql = "SELECT * FROM pins WHERE poid = ? AND pin_title= ?";

            if (connection.state == "disconnected")
                return callback({ message: "Can't connect to database." }, null);
            connection.query(sql, [poid, title], (err, results) => {
                if (err)
                    return callback(err, null);
                else if (results.length)
                    return callback(null, results);
                else
                    return callback(err, null);
            });
        }
    },
    /**
     * 
     * @param {*} id Id of the pin (PK)
     * @param {*} callback Callback function to handle the result of the query
     * @returns Returns a pin object
     */
    getPinById: function (id, callback) {
        if (id === null || id === undefined || id === "") {
            return callback({ message: "ID should be not null" }, null);
        } else {
            let sql = "SELECT * FROM pins WHERE id= ?";

            if (connection.state == "disconnected")
                return callback({ message: "Can't connect to database." }, null);
            connection.query(sql, id, (err, results) => {
                if (err)
                    return callback(err, null);
                else if (results.length)
                    return callback(null, results);
                else
                    return callback(err, null);
            });
        }
    },

    /**
     * 
     * @param {*} poid The project id
     * @param {*} pin_id The pin id
     * @param {*} callback A callback function
     * @returns Returns the object data that refers to a pin in the 3d world
     */
    getPinByPoidAndPinId: function (poid, pin_id, callback) {
        if (pin_id === null || poid === null) {
            return callback({ message: "Some fields should be not null" }, null);
        } else {
            let sql = "SELECT * FROM pins WHERE poid=? AND pin_reference_object_id=?";

            if (connection.state == "disconnected")
                return callback({ message: "Can't connect to database." }, null);
            connection.query(sql, [poid, pin_id], (err, results) => {
                if (err)
                    return callback(err, null);
                else if (results.length)
                    return callback(null, results);
                else
                    return callback(err, null);
            });
        }
    },

    /**
     * 
     * @param {*} pin Pin data received as a JSON object
     * @param {*} callback Callback function to handle the query result
     * @returns Return a message from the query execution
     */
    setPin: function (pin, callback) {
        if (pin === null || pin === undefined) {
            console.log("Pin object is undefined or null");
            return callback({ message: "Pin object is undefined or null" }, null);
        } else if (pinProps(pin)) {
            let sql = "INSERT INTO pins SET ?";
            pin.date = moment().format('YYYY-MM-DD HH:mm:ss');

            if (connection.state == "disconnected")
                return callback({ message: "Can't connect to database." }, null);
            connection.query(sql, pin, (err, result) => {
                console.log(result);
                if (err)
                    return callback(err, null);
                else if (result !== null && result !== undefined)
                    return callback(null, result);
            });

        } else {
            return callback({ message: "Pin object has missing properties" }, null)
        }
    },

    /**
     * 
     * @param {*} pinId Pin id from the database
     * @param {*} callback Callback function to handle query result
     * @return Returns a message from the query execution
     */
    deletePin: function (pinId, callback) {
        if (pinId === null) {
            return callback({ message: "Pin id should be not null" }, null);
        } else {
            let sql = "DELETE FROM pins WHERE id=?";

            if (connection.state == "disconnected")
                return callback({ message: "Can't connect to database." }, null);
            connection.query(sql, pinId, (err, result) => {
                if (err)
                    return callback(err, null);
                else if (result.length)
                    return callback(null, result);
                else
                    return callback(err, null);
            });
        }
    },

    /**
     * 
     * @param {*} pin Pin data that will be updated on the database
     * @param {*} callback Callback function
     * @return Return a message from the query execution
     */
    updatePin: function (id, pin, callback) {
        if (pin === undefined || pin === null) {
            return callback({ message: "Some fields should be not null" }, null);
        } else {
            if (id !== null || id !== undefined) {

                let sql = "UPDATE pins SET ? WHERE id=?";
                pin.date = moment().format('YYYY-MM-DD');

                delete pin.id; // remove the property id from data

                if (connection.state == "disconnected")
                    return callback({ message: "Can't connect to database." }, null);
                connection.query(sql, [pin, id], (err, result) => {
                    console.log("error:",err);
                    if (err)
                        return callback(err, null);
                    else if (result.length)
                        return callback(null, result);
                    else
                        return callback(err, null);
                });
            } else {
                return callback({ message: "Pin id should be not null" }, null);
            }
        }
    }
}
