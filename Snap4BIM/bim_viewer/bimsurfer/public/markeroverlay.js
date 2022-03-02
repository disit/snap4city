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

import * as vec3 from "./bimsurfer-viewer/glmatrix/vec3.js";
import { PopupBuilder } from "./popup.js";

/**
 * This represents a marker/pin (node)
 */
class Node {
    constructor(overlay, imgElement) {
        this.overlay = overlay;
        this.imgElement = imgElement;
        this.serviceURI = null;
        this.canvasCoordinates = [];
        this.coordinates = vec3.create();
        this.popup = null
    }

    /**
     * Processes the location and updates it when it changes
     * @param {*} _coordinates The 3d world coordinates of the observed point
     */
    process(_coordinates) {
        let v = this.isVisible(this.canvasCoordinates);

        // try some animated icons
        //this.animateIconSize();

        if (!v) {
            this.imgElement.style.opacity = "0.5";
            this.visible = false;
        } else {
            this.imgElement.style.opacity = "1";
            this.visible = true;
        }

        this.doUpdate(_coordinates);
    }

    destroy() {
        this.overlay.nodes.splice(this.overlay.nodes.indexOf(this), 1);
        this.imgElement.parentElement.removeChild(this.imgElement);
    }
}


/**
 * Helper class used for updating the location of pins
 */
class OverlayNode extends Node {
    constructor(overlay, imgElement, viewer, serviceUri) {
        super(overlay, imgElement);
        this.viewer = viewer;
        this.camera = viewer.camera;
        this.serviceURI = serviceUri;

        // make popup dragabble
        this.dragPopup = true;

        /**
         * Simulates the visibility of 2d pins in the 3d world,
         *  decrement opacity when the marked object isn't visible
         * @fix function that calculates the distance of the visible object
         */
        this.elementVisibility = false;//true;

        /** @fix add the popup element to display the data */
        this.imgElement.addEventListener("click", () => {
            //console.log("you clicked image with id: ", this.imgElement.id);

            // split string and get reference object id
            //this.setFocusOnObjectId(this.imgElement.id.split('_')[1]);

            /**@fix add the popup content here */
            this.addPopup();
        });



        //let baseSrc = "../images/gisMapIcons/";
        let s = this.imgElement.src;

        // do nothing if we are orbiting
        let orbiting = this.camera.orbiting;

        /*
        this.imgElement.addEventListener('mouseover', () => {
            if (!orbiting) {

                let src = this.imgElement.src;
                let splitSrc = src.split("/");
                let fName = splitSrc[splitSrc.length - 1];
                src = this.imgElement.src;
                src = src.replace(fName, "");

                fName = "over/" + fName.replace(".png", "_over.png");
                this.imgElement.src = src + fName;
            }
        });
        */

        /*
        this.imgElement.addEventListener('mouseout', () => {
            if (!orbiting) {
                this.imgElement.src = s;
            }
        });
        */
    }

    /**
     * 
     * @param {*} popup object that rapresents the popup window 
     * that will be viewed when clicking the selected pin
     */
    addPopup() {
        /**@fix the logic that will fetch and display popup data */
        var popupId = "popup_" + this.imgElement.id;
        if (this.popup == null || this.popup == undefined) {

            this.popup = PopupBuilder.create(popupId, this.serviceURI);

            if (this.dragPopup)
                this.dragPopupElement(this.popup);

            this.popup.style.top = parseInt(this.imgElement.style.top.replace("px", "")) - 290 + "px"; // -290px
            this.popup.style.left = parseInt(this.imgElement.style.left.replace("px", "")) + 17 + "px"; // +17px

            //console.log("top: " + this.imgElement.style.top + ", left: " + this.imgElement.style.left);
        } else {
            var tmpPopup = document.getElementById(popupId);
            if (tmpPopup != null || tmpPopup != undefined) {
                tmpPopup.style.display = "";
            }
        }
    }



    dragPopupElement(elmnt) {
        elmnt.style.cursor = "move";

        var pos1 = 0, pos2 = 0, pos3 = 0, pos4 = 0;
        if (document.getElementById(elmnt.id + "header")) {
            document.getElementById(elmnt.id + "header").onmousedown = dragMouseDown;
        } else {
            elmnt.onmousedown = dragMouseDown;
        }

        function dragMouseDown(e) {
            e = e || window.event;
            e.preventDefault();
            pos3 = e.clientX;
            pos4 = e.clientY;
            document.onmouseup = closeDragElement;
            document.onmousemove = elementDrag;
        }

        function elementDrag(e) {
            e = e || window.event;
            e.preventDefault();
            pos1 = pos3 - e.clientX;
            pos2 = pos4 - e.clientY;
            pos3 = e.clientX;
            pos4 = e.clientY;
            elmnt.style.top = (elmnt.offsetTop - pos2) + "px";
            elmnt.style.left = (elmnt.offsetLeft - pos1) + "px";
        }

        function closeDragElement() {
            document.onmouseup = null;
            document.onmousemove = null;
        }
    }

    /**
     * Sets focus to the selected 3d object
     * @param {*} id The uniqueId of the object that we want to set focus
     */
    setFocusOnObjectId(id) {
        let elems = [parseInt(id)];
        //console.log("elems", elems);
        //this.viewer.viewFit(elems);
        this.viewer.setSelectionState(elems, true, false);
    }

    /**
     * This function tells if the marked point is visible or not
     * @note For now its functionality is disabled, because compute heavy
     * @param position Is an array type like [x, y] that represents the canvas position
     */
    isVisible(position) {
        // 1 step -- calculate the distance from eye to the first object at a given position in the canvas
        // 2 step -- calculate the distance from eye to the given coordinate of the marked object
        // 3 step -- if the distance in (1 step) is smaller then the distance in (2 step) then opacity=0.5
        //            otherwise if the diference is small (near zero) then use opacity=1.0

        // 1 step
        let orbiting = this.camera.orbiting;

        if (!orbiting && this.elementVisibility) {

            let eye = this.camera.eye;
            let firstObj = this.viewer.pick({ canvasPos: position, select: false });

            if (firstObj.object !== null && firstObj.object !== undefined) {
                let distanceFromFirstObj = vec3.distance(eye, firstObj.coordinates);

                // 2 step
                let distanceFromMarkedObj = vec3.distance(eye, this.coordinates);

                // 3 step
                if (distanceFromFirstObj < distanceFromMarkedObj) {
                    return false
                }
            }
        }
        return true;
    }


    animateIconSize() {
        let eye = this.camera.eye;
        let distanceFromMarkedObj = vec3.distance(eye, this.coordinates);

        console.log("distance from eye:", distanceFromMarkedObj);
    }

    /**
     * Updates the position of the pin (2d) based on the translation of that point in the 3d view
     * @param {*} pointCoordinates Coordinates in the 3D world
     */
    doUpdate(pointCoordinates) {
        this.coordinates = pointCoordinates;
        let [x, y] = this.overlay.transformPoint(pointCoordinates);

        // save the updated canvas coordinates
        this.canvasCoordinates.push(x);
        this.canvasCoordinates.push(y);

        this.imgElement.style.top = (y - 40) + "px";
        this.imgElement.style.left = (x - 17) + "px";

        if (this.popup != null || this.popup != undefined) {
            this.popup.style.top = (y - 330) + "px";
            this.popup.style.left = (x - 0) + "px";
        }
    }
}

//////////////////////////////////////////////////////////////////////////////////
/**
 *
 * @export
 * @class MarkerOverlay
 * A class that contains the all the pins/markers
 * and also manages their position based on the 3d object that
 * we are observing
 */
export class MarkerOverlay {
    constructor(domNode, viewer) {
        this.track = domNode;
        this.viewer = viewer;
        this.camera = viewer.camera;
        this.nodes = [];

        // used for popups
        this.popups = []

        this._constructorImageIcon();
    }

    /**
     * Creates the main div element that contains the markers/pins
     */
    _constructorImageIcon() {
        this.markerDivElement = document.createElement("div");
        this.markerDivElement.id = "markers";
        document.body.appendChild(this.markerDivElement);

        this.resize();

        this.camera.listeners.push(this.update.bind(this));
        window.addEventListener("resize", this.resize.bind(this), false);
        window.addEventListener("resize", () => {
            this.update();
        });
    }

    /**
     * Creates a new marker and adds it to the node list
     * @param {*} _id The uniqueId of the selected element on the 3d world
     * @param {*} _pinTitle Pint title
     * @param {*} _x x coordinate
     * @param {*} _y y coordinate
     * @param {*} _z z coordinate
     * @param {*} _image_source Image source (pin icon)
     */
    _newMarkerIcon(_id, _pinId, _pinTitle, _x, _y, _z, _image_source, serviceUri) {
        this.imgElement = document.createElement("img");
        this.imgElement.id = _id + "_" + _pinId;
        this.imgElement.classList.add('pin');
        this.imgElement.alt = _pinTitle;
        this.imgElement.src = _image_source;//"./pin.png";
        this.imgElement.style.position = "absolute";
        this.imgElement.setAttribute("service_uri", serviceUri);

        this.serviceURI = serviceUri;

        this.markerDivElement.appendChild(this.imgElement);

        let coordinates = vec3.clone([_x, _y, _z]);

        let tmp = new OverlayNode(this, this.imgElement, this.viewer, this.serviceURI);
        tmp.coordinates = coordinates;

        this.nodes.push(tmp);
    }

    /** 
    * Remove the pin dom element 
    * @param {*} id of the image element 
    */
    removeMarkerIcon(id) {
        for (let i = 0; i < this.nodes.length; i++) {
            if (id === this.nodes[i].imgElement.id) {
                delete this.nodes.imElement;
                this.nodes[i] = null;
                this.nodes.splice(i, 1);
            }
        }
    }

    /**
     * Remove all the pins from the node list
     */
    removeMarkerIconAll() {
        for (let i = 0; i < this.nodes.length; i++) {
            this.nodes = this.nodes.splice(i, 1);
        }
        this.nodes = [];
    }

    /**
     * Transforms 3d coordiantes into the 2d that serve for positioning the markers into the canvas
     * @param {*} p coordinates of the 3d point 
     */
    transformPoint(p) {
        let tmp = vec3.create();
        vec3.transformMat4(tmp, p, this.camera.viewProjMatrix);
        return [+tmp[0] * this.w + this.w, -tmp[1] * this.h + this.h]
    }

    update() {
        this.nodes.forEach((n) => {
            n.process(n.coordinates);
        });
    }

    resize() {
        let markerStyle = this.markerDivElement.style;

        markerStyle.left = "0px";
        markerStyle.top = "0px";
        markerStyle.width = (this.w = this.track.clientWidth) + "px";
        markerStyle.height = (this.h = this.track.clientHeight) + "px";

        this.w /= 2.;
        this.h /= 2.;

        this.aspect = this.w / this.h;
    }
}