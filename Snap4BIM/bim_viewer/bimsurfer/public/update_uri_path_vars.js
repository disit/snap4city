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

export class UpdateUriPathVars {
    constructor(poid, surfer) {
        this.poid = poid;
        this.surfer = surfer;
        this.viewer = surfer._bimServerViewer.viewer;
        this.camera = surfer._bimServerViewer.viewer.camera;

        this.visibilityState = {}

        this.firstLoad = false;
    }

    setPoid(poid) {
        if (poid) {
            this.poid = poid;
        }
    }

    updateUri() {

        this.camera.addCamPosListener(() => {
            const url = new URL(window.location);

            if (this.firstLoad) {
                let camObject = this.surfer.getCamera();
                let eye = camObject.eye;

                // temporary solution for not updating path vars before first load
                if (eye[0] !== 0 && eye[1] !== 1 && eye[2] !== 0) {

                    url.searchParams.set('poid', '' + this.poid);
                    url.searchParams.set('eye', '[' + camObject.eye + ']');
                    url.searchParams.set('target', '[' + camObject.target + ']');
                    url.searchParams.set('up', '[' + camObject.up + ']');
                    url.searchParams.set('fovy', '' + camObject.fovy);
                    url.searchParams.set('type', '' + camObject.type);
                }
            }

            window.history.pushState({}, '', window.unescape(url));

            this.firstLoad = true;
        });

    }

    // add visibility path variables
    updateVisibilityParams() {
        const url = new URL(window.location);

        if (this.visibilityState.sidebar != null) {
            url.searchParams.set('sidebar', '' + this.visibilityState.sidebar);
        }

        if (this.visibilityState.timetrend != null) {
            url.searchParams.set('timetrend', '' + this.visibilityState.timetrend);
        }

        window.history.pushState({}, '', window.unescape(url));
    }

    static getPathVariables() {
        function getURLParameterByName(name, url = window.location.href) {
            name = name.replace(/[\[\]]/g, '\\$&');
            let regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
                results = regex.exec(url);
            if (!results) return null;
            if (!results[2]) return '';
            return decodeURIComponent(results[2].replace(/\+/g, ' '));
        }

        var returnValue = {}

        let poid = getURLParameterByName("poid");
        if (poid != null && poid != undefined && poid != "") {
            poid = parseInt(poid);
        }

        let camera = {};

        let eye = getURLParameterByName("eye");
        if (eye != null && eye != undefined && eye != "") {
            camera.eye = new Float32Array(3);

            let tmp = eye.split(",");
            camera.eye[0] = parseFloat(tmp[0].substr(1, tmp[0].length));
            camera.eye[1] = parseFloat(tmp[1]);
            camera.eye[2] = parseFloat(tmp[2].substr(0, tmp[2].length - 1));
        }

        let target = getURLParameterByName("target");
        if (target != null && target != undefined && target != "") {
            camera.target = new Float32Array(3);

            let tmp = target.split(",");
            camera.target[0] = parseFloat(tmp[0].substr(1, tmp[0].length));
            camera.target[1] = parseFloat(tmp[1]);
            camera.target[2] = parseFloat(tmp[2].substr(0, tmp[2].length - 1));
        }

        let up = getURLParameterByName("up");;
        if (up != null && up != undefined && up != "") {
            let tmp = up.split(",");
            let x, y, z;
            x = parseFloat(tmp[0].substr(1, tmp[0].length));
            y = parseFloat(tmp[1]);
            z = parseFloat(tmp[2].substr(0, tmp[2].length - 1));

            if (x !== 0 && y !== 0 && z !== 1) {
                camera.up = new Float32Array(3);
                camera.up[0] = x;
                camera.up[1] = y;
                camera.up[2] = z;
            }
        }

        let fovy = getURLParameterByName("fovy");
        if (fovy != null && fovy != undefined && fovy != "") {
            camera.fovy = parseInt(fovy);
        }

        let type = getURLParameterByName("type");
        if (type != null && type != undefined && type != "") {
            camera.type = type;
        }

        let sidebar = null;
        let timetrend = null;

        let sidebarVisibility = getURLParameterByName("sidebar");
        if (sidebarVisibility != null && sidebarVisibility != undefined && sidebarVisibility != "") {
            sidebar = sidebarVisibility;
        }

        let timetrendVisibility = getURLParameterByName("timetrend");
        if (timetrendVisibility != null && timetrendVisibility != undefined && timetrendVisibility != "") {
            timetrend = timetrendVisibility;
        }

        let pinsArray = []
        let pinsVisibility = new URL(window.location.href).searchParams.get("pins");
        //getURLParameterByName("pins");
        if (pinsVisibility != null && pinsVisibility != undefined && pinsVisibility != "") {
            const pinsTmp = pinsVisibility.replace("[", "").replace("]", "").split(",");

            pinsTmp.forEach(v => {
                if (v == "") {
                    let index = pinsTmp.indexOf(v)
                    if (index > -1) {
                        pinsTmp.splice(index, 1)
                    }
                }
            });

            if (pinsTmp.length == 0) {
                pinsArray = null
            } else {
                pinsArray = pinsTmp
            }
        }

        if (poid != null && poid != undefined && poid != 0) {
            returnValue.poid = poid
        }
        if (camera != null && camera != undefined) {
            returnValue.camera = camera
        }
        if (pinsArray != null && pinsArray != undefined && pinsArray.length > 0) {
            returnValue.pins = pinsArray
        }
        var visibilityState = {}
        if (sidebar != null && sidebar != undefined) {
            visibilityState.sidebar = sidebar
        }
        if (timetrend != null && timetrend != undefined) {
            visibilityState.timetrend = timetrend
        }

        if (Object.values(visibilityState).length > 0) {
            returnValue.visibilityState = visibilityState
        }

        return returnValue

    }

    /**
     * @param pinsArray {} an array containing pin ids
     */
    static setPinPathVars(serviceURISet) {
        var pinsArray = Array.from(serviceURISet)

        if (pinsArray.length == 0) {
            pinsArray = null
        }

        const url = new URL(window.location);
        url.searchParams.set('pins', pinsArray);
        window.history.pushState({}, '', window.unescape(url));
        
    }

    static setTimetrendVisibility(visible) {
        let btn = document.getElementById("timetrendVisibilityBtn");
        const url = new URL(window.location);
        url.searchParams.set('timetrend', visible);
        window.history.pushState({}, '', window.unescape(url));
    }

    static setSidebarVisibility(visible) {
        const url = new URL(window.location);
        url.searchParams.set('sidebar', visible);
        window.history.pushState({}, '', window.unescape(url));
    }

    static setPoid(poid) {
        const url = new URL(window.location);
        url.searchParams.set('poid', poid);
        window.history.pushState({}, '', window.unescape(url));
    }

    setVisibilityState(state) {
        this.visibilityState = state
        this.updateVisibilityParams();
    }
}