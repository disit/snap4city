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

export class Toolbar {

    constructor(viewer, state, updatePathVars) {
        this.viewer = viewer;
        //this.visibilityState = state;
        this.setVisibility(state)
        this.updatePathVars = updatePathVars;

        this.leftSidebar = document.getElementById("l-overlay") || null;
        this.rightSidebar = document.getElementById("r-overlay") || null;
        this.timetrendWindow = document.getElementById("data-chart-visualizer") || null;

        this.container = document.createElement("div");
        this.container.id = "toolbar-container";

        this.mainContainerToolbar = document.createElement("div");
        this.mainContainerToolbar.classList.add("toolbar");
        this.mainContainerToolbar.id = "toolbar";

        this.container.appendChild(this.mainContainerToolbar);
        //document.body.appendChild(this.container);
        document.getElementById("main-data-window").appendChild(this.container);

        this.buildToolbar();
        this.onLoadCheckActiveBtnStates();
    }

    onLoadCheckActiveBtnStates() {
        if (this.leftSidebar.style.display == "none") {
            this.toggleSidebarBtn.children[0].classList.remove("active");
        } else {
            this.toggleSidebarBtn.children[0].classList.add("active");
        }

        if (this.timetrendWindow.style.display == "none") {
            this.timetrendVisibilityBtn.children[0].classList.remove("active");
        } else {
            this.timetrendVisibilityBtn.children[0].classList.add("active");
        }

    }

    getVisibilityState() {
        return {
            sidebarVisibilityState: this.sidebarVisibilityState,
            timetrendVisibilityState: this.timetrendVisibilityState
        }
    }

    buildToolbar() {

        this.visibilityControllers();
        this.cameraControllers();
        this.createHelpPage();

    }

    visibilityControllers() {
        let div = document.createElement("div");
        div.classList.add("visibility-buttons");
        this.mainContainerToolbar.appendChild(div);

        this.toggleSidebarBtn = this.createButton(
            "toggleSidebarBtn",
            div,
            "Hide/Show Sidebar",
            "./img/sidebar.png",
            ["toolbar-btn", "visibility-btn", "btn", "btn-sm", "btn-outline-primary"],
            "Toggle visibility of sidebars",
            (s) => {
                let sidebars = document.querySelectorAll(".overlay");
                sidebars.forEach(sidebar => {
                    let currentVisibility = sidebar.style.display;
                    if (currentVisibility != "none") {
                        sidebar.style.display = "none";
                        let tmpState = new Map();
                        tmpState.sidebar = "off";
                        this.updatePathVars.setVisibilityState(tmpState)
                        this.toggleSidebarBtn.children[0].classList.remove("active");
                    } else {
                        sidebar.style.display = "block";
                        let tmpState = new Map();
                        tmpState.sidebar = "on";
                        this.updatePathVars.setVisibilityState(tmpState)
                        this.toggleSidebarBtn.children[0].classList.add("active");
                    }
                });
            });



        // timetrend-chart
        if (this.timetrendWindow != null) {
            this.timetrendVisibilityBtn = this.createButton(
                "timetrendVisibilityBtn",
                div,
                "Hide/Show TimeTrend",
                "./img/chart.png",
                ["toolbar-btn", "visibility-btn", "btn", "btn-sm", "btn-outline-primary"],
                "Toggle visibility of timetrend chart",
                (s) => {
                    let currentVisibility = this.timetrendWindow.style.display;
                    if (currentVisibility != "none") {
                        let tmpState = new Map();
                        tmpState.timetrend = "off";
                        this.updatePathVars.setVisibilityState(tmpState)
                        this.timetrendWindow.style.display = "none";
                        this.timetrendVisibilityBtn.children[0].classList.remove("active");

                        if (this.leftSidebar != null) {
                            this.leftSidebar.style.height = window.innerHeight + "px";
                        }
                        // right sidebar initially doesnt exist so we check each time
                        let rs = document.getElementById("r-overlay") || null;
                        if (rs != null) {
                            rs.style.height = window.innerHeight + "px";
                        }
                    } else {
                        let tmpState = new Map();
                        tmpState.timetrend = "on";
                        this.updatePathVars.setVisibilityState(tmpState)
                        this.timetrendWindow.style.display = "block";
                        this.timetrendVisibilityBtn.children[0].classList.add("active");

                        if (this.leftSidebar != null) {
                            this.leftSidebar.style.height = (window.innerHeight - 200) + "px";
                        }
                        let rs = document.getElementById("r-overlay") || null;
                        if (rs != null) {
                            rs.style.height = window.innerHeight - 200 + "px";
                        }
                    }
                });
        }
    }

    createButton(buttonId, divContainer, name, iconSrc, _classes, desc, callback) {
        var div = document.createElement("div");
        divContainer.appendChild(div);
        div.state = false;

        var img = document.createElement("img");
        img.src = iconSrc;
        img.title = desc;
        img.classList.add("toolbar-btn-img");


        var button = document.createElement("button");
        button.id = buttonId;
        button.classList.add(..._classes);
        button.appendChild(img)
        button.addEventListener("click", () => {
            callback(div.state)
            div.state = !div.state;
        });

        div.appendChild(button);

        return div;
    }

    cameraControllers() {
        this.animationEnabled = false;
        this.panEnabled = false;

        // check if we have the viewer obj
        if (this.viewer != null) {
            this.camera = this.viewer.camera;
            this.cameraControl = this.viewer.cameraControl;
        }

        // add camera constroll pan/pitch/rotate
        let div = document.createElement("div");
        this.mainContainerToolbar.appendChild(div);
        div.classList.add("camera-controllers");

        // add animation
        this.viewer.addAnimationListener((deltaTime) => {
            if (this.animationEnabled) {
                this.viewer.camera.orbitYaw(0.3);
            }
        });

        function resetView(viewer) {
            //viewer.resetCamera();
            viewer.resetColors();
            viewer.resetVisibility();
        }

        this.rotate3DObjectBtn = this.createButton(
            "rotate3DObjectBtn",
            div,
            "Rotate 3D object",
            "./img/360.png",
            ["toolbar-btn", "btn", "btn-outline-primary", "btn-sm"],
            "Rotate 3D object",
            (s) => {
                this.panEnabled = !this.panEnabled;

                if (this.panEnabled) {

                    this.rotate3DObjectBtn.children[0].classList.remove("active");
                    this.panBtn.children[0].classList.add("active");

                    document.querySelector("canvas").addEventListener("mousemove", (e) => {
                        this.viewer.cameraControl.canvasPanEnabled(this.panEnabled);
                    });
                } else {
                    this.panBtn.children[0].classList.remove("active");
                    this.rotate3DObjectBtn.children[0].classList.add("active");

                    document.querySelector("canvas").addEventListener("mousemove", (e) => {
                        this.viewer.cameraControl.canvasPanEnabled(this.panEnabled);
                    });
                }
            }
        );

        if (!this.panEnabled) {
            this.rotate3DObjectBtn.children[0].classList.add("active");
        }

        this.panBtn = this.createButton(
            "panBtn",
            div,
            "Move object",
            "./img/hand-pan.png",
            ["toolbar-btn", "btn", "btn-outline-primary", "btn-sm"],
            "Pan",
            (s) => {
                this.panEnabled = !this.panEnabled;

                if (this.panEnabled) {
                    this.panBtn.children[0].classList.add("active");
                    this.rotate3DObjectBtn.children[0].classList.remove("active");

                    document.querySelector("canvas").addEventListener("mousemove", (e) => {
                        this.viewer.cameraControl.canvasPanEnabled(this.panEnabled);
                    })
                } else {
                    this.rotate3DObjectBtn.children[0].classList.add("active");
                    this.panBtn.children[0].classList.remove("active");
                }
            }
        );

        this.rotateCameraBtn = this.createButton(
            "rotateCameraBtn",
            div,
            "Rotate camera",
            "./img/rotate-camera.png",
            ["toolbar-btn", "btn", "btn-outline-primary", "btn-sm"],
            "Rotate camera",
            (s) => {
                if (!this.animationEnabled) {
                    this.rotateCameraBtn.children[0].classList.add("active");
                } else {
                    this.rotateCameraBtn.children[0].classList.remove("active");
                }

                this.animationEnabled = !this.animationEnabled;
                this.viewer.navigationActive = this.animationEnabled;
            }
        );

        this.resetViewBtn = this.createButton(
            "resetViewBtn",
            div,
            "Reset view",
            "./img/recycle.png",
            ["toolbar-btn", "btn", "btn-outline-primary", "btn-sm"],
            "Reset color and visibility of elements",
            (s) => {
                resetView(this.viewer);
            }
        );



    }

    createHelpPage() {
        let div = document.createElement("div");
        this.mainContainerToolbar.appendChild(div);
        div.id = "help-button-container";
        //div.classList.add("help-button-container");

        let modalContainer = document.createElement("div");
        document.body.appendChild(modalContainer);
        modalContainer.id = "modal-container";
        modalContainer.style.display = "none";

        modalContainer.innerHTML = '<div id="modal-content">' +
            '<h3>Viewer commands</h3><br>' +
            '<span style="font-size:75%">' +
            '<b>[Mouse down]</b> and drag for yaw and pitch <br>' +
            '<b>[Mouse wheel]</b> to zoom <br>' +
            '<b>[Left] [Right] [Up] [Down]</b> arrows to move the object in the screen <br>' +
            '<b>[Ctrl] + [Mouse down]</b> section plane <br>' +
            '<b>[h]</b> hide selected <br>' +
            '<b>[Shift] + [h]</b> unhide all <br>' +
            '<b>[c]</b> set selected elem to random opaque color <br>' +
            '<b>[d]</b> set selected elem to random transparent color <br>' +
            '<b>[Shift] + [c]</b> reset all color overrides <br>' +
            '</span>' +
            '</div>'
            ;

        this.helpButton = this.createButton(
            "helpBtn",
            div,
            "Help",
            "./img/help.png",
            ["help-button", "btn", "btn-sm", "btn-outline-info"],
            "Command description",
            (s) => {
                let display = modalContainer.style.display;
                if (display == "none") {
                    modalContainer.style.display = "block";
                    this.helpButton.children[0].classList.add("active");
                } else {
                    modalContainer.style.display = "none"
                    this.helpButton.children[0].classList.remove("active");
                }
            });

    }

    updateVisibility(visibilityState) {
        this.visibilityState = visibilityState;
        this.updatePathVars.setVisibilityState()
    }

    // used for updating path urls
    setVisibility(statesDict) {

        function setState(_window, state) {
            _window.forEach(w => {
                if (state != null) {
                    if (state == "on") {
                        w.style.display = "block";
                    } else {
                        w.style.display = "none";
                    }
                }
            });
        }

        function getStateVisibility() {
            let state = {};

            if (document.getElementById("l-overlay").style.display == "none") {
                state.sidebar = "off"
            } else {
                state.sidebar = "on"
            }

            if (document.getElementById("data-chart-visualizer").style.display == "none") {
                state.timetrend = "off"
            } else {
                state.timetrend = "on"
            }
            return state
        }

        if (statesDict != null) {
            if (statesDict.hasOwnProperty("sidebar") && statesDict.sidebar != null && statesDict.sidebar != undefined) {
                //statesDict.sidebar = getStateVisibility().sidebar;
                setState(document.querySelectorAll(".overlay"), statesDict.sidebar);
            }
            if (statesDict.hasOwnProperty("timetrend") && statesDict.timetrend != null && statesDict.timetrend != undefined) {
                //statesDict.timetrend = getStateVisibility().timetrend;
                setState(document.querySelectorAll("#data-chart-visualizer"), statesDict.timetrend);
            }
        }
    }
}