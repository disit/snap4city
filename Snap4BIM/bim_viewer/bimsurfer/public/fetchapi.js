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

import { projectEndpoint, pinEndpoint, serviceuriEndpoint, serviceUriBase, serviceUriFormat   } from "./config.js" 

export class FetchAPI {

    /**
     * @param {*} poid Project id
     * @param {*} callback Callback function to handle the query result
     * @return Returns a list of pins for the selected poid
     */
    static fetchPinByPoid(poid, callback) {
        const url = projectEndpoint + poid;
        fetch(url)
            .then(response => response.json())
            .then(data => {
                callback(data);
            });
    }

    /**
     * 
     * @param {*} id Id of the pin saved on the database
     * @param {*} callback Callback function to handle the result
     */
    static fetchPinById(id, callback) {
        const url = pinEndpoint + id;

        fetch(url)
            .then(response => response.json())
            .then(data => {
                callback(data);
            });;
    }

    /**
     * @param {*} pinData Pin related data {id: 343, poid: 123456, pin_title: 'my title', ....} 
     * @param {*} callback A callback function to handle the returned data callback(response)
     * @todo handle duplicate pins with the same data
     */
    static addPin(pinData, callback) {
        const url = pinEndpoint;

        fetch(url, {
            method: 'POST',
            mode: 'cors',
            cache: 'no-cache',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/json'
            },
            redirect: 'follow', 
            referrerPolicy: 'no-referrer',
            body: JSON.stringify(pinData)

        })
            .then(res => res.json())
            .then(data => {
                callback(data);
            });
    }

    /**
     * 
     * @param {*} pinData The updated data of the pin
     * @param {*} callback Callback function to handle the result
     */
    static updatePin(pinData, callback) {
        const url = pinEndpoint;

        fetch(url, {
            method: 'PUT',
            mode: 'cors',
            cache: 'no-cache',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/json'
            },
            redirect: 'follow',
            referrerPolicy: 'no-referrer',
            body: JSON.stringify(pinData)

        })
            .then(res => res.json())
            .then(data => {
                callback(data);
            });
    }

    /**
     * 
     * @param {*} pinData pin data
     * @param {*} callback callback to handle query result
     */
    static removePin(pinData, callback) {
        const url = pinEndpoint;

        fetch(url, {
            method: 'DELETE',
            mode: 'cors',
            cache: 'no-cache',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/json'
            },
            redirect: 'follow',
            referrerPolicy: 'no-referrer',
            body: JSON.stringify(pinData)

        })
            .then(res => res.json())
            .then(data => {
                callback(data);
            });
    }

    /**
     * 
     * @param {*} serviceUri from snap4city os some sensor
     * @param {*} callback to handle the query result
     */
    static fetchDataFromServiceUriXHR(_serviceUri, callback) {
        let dataURI = JSON.stringify({ serviceuri: (serviceUriBase + _serviceUri + serviceUriFormat) });

        //console.log("dataURI",dataURI)

        fetch(serviceuriEndpoint, 
            {
            method: 'POST',
            mode: 'cors',
            cache: 'no-cache',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/json'
            },
            redirect: 'follow',
            referrerPolicy: 'no-referrer',
            body: dataURI
        })
            .then(res => res.json())
            .then(data => {
                callback(data);
                console.log("Data from snap4city", data);
            });
    }
}
