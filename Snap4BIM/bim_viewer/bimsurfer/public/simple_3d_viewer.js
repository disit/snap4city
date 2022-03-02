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

import { BimServerClient } from "../bimsurfer-api/bimserverclient.js"
import { Credentials } from "./credentials.js"
import { BimSurfer } from "./bimsurfer-viewer/bimsurfer.js";
import { MarkerOverlay } from "./markeroverlay.js";
import { FetchAPI } from "./fetchapi.js";
import { Project } from "./project.js";
import { UpdateUriPathVars } from "./update_uri_path_vars.js";
import { Toolbar } from "./toolbar.js";
import { ProjectTreeModel } from "./bimsurfer-viewer/projecttreemodel.js"
import { TreeView } from "./bimsurfer-viewer/treeview.js"
import { bimserverURL, viewerConfig, iconServerLocation } from "./config.js" 



export class Simple3DViewer {


	start() {
		this.animationEnabled = false;

		this.surfer = window.bimSurfer = new BimSurfer(viewerConfig);
		this.api = new BimServerClient(bimserverURL);
		var o = this;
		this.visiblePins = new Set()

		this.api.init(() => {
			new Credentials(this.api).getCredentials().then(() => {
				let dataFromUri = UpdateUriPathVars.getPathVariables();
				if (dataFromUri !== null && dataFromUri !== undefined) {
					if (dataFromUri.hasOwnProperty("poid")) {
						let poid = dataFromUri.poid;
						if (poid != null && poid != undefined && poid != "") {
							o.api.call("ServiceInterface", "getProjectByPoid", { poid: poid }, function (project) {
								o.project = project;
							}).done(() => {
								o.loadModel(o.project);
							});
						} else {
							o.loadProjects();
						}
					} else {
						o.loadProjects();
					}
				}
			});
		});
	}

	/**
	 * Loads a tree of projects available in bimserver
	 */
	loadProjects() {
		document.getElementById("project-viewer").style.display = "none";
		document.getElementById("projects-tree-viewer").style.display = "block";

		var treeView = new TreeView(document.getElementById("projects"));

		this.projectTreeModel = new ProjectTreeModel(this.api, treeView);

		this.projectTreeModel.load((node) => {
			this.loadModel(node.project);
		});
	}

	setwindowDimensions() {
		if (document.getElementById("data-chart-visualizer").style.display == "none") {
			document.getElementById("l-overlay").style.height = window.innerHeight + "px";
			document.getElementById("tree-container").style.height = (window.innerHeight - (window.innerHeight * 0.4)) + "px";
		} else {
			document.getElementById("l-overlay").style.height = (window.innerHeight - 200) + "px";
			document.getElementById("tree-container").style.height = (window.innerHeight - 200 - ((window.innerHeight / 100) * 40)) + "px";
		}
	}

	loadModel(project) {
		document.getElementById("l-overlay").style.height = window.innerHeight  + "px";
		document.getElementById("tree-container").style.height = (window.innerHeight - (window.innerHeight * 0.4)) + "px";

		window.onresize = this.setwindowDimensions;

		document.getElementById("project-viewer").style.display = "block";
		document.getElementById("projects-tree-viewer").style.display = "none";
		this.animationEnabled = false;

		this.canvas = document.querySelector("canvas");
		this.surfer._api = this.api;
		let oid = project.oid;
		this.surfer.loadModel(project, this.canvas).then(() => {
			let urlPathVars = UpdateUriPathVars.getPathVariables();
			if (urlPathVars && urlPathVars.hasOwnProperty("poid")) {
				if (urlPathVars.poid === oid) {
					if (urlPathVars.camera && urlPathVars.hasOwnProperty("camera")) {
						this.surfer.setCamera(urlPathVars.camera);
					}
				}
			}

		});

		this.camera = this.surfer._bimServerViewer.viewer.camera;
		this.viewer = this.surfer._bimServerViewer.viewer;

		this.surfer._bimServerViewer.setProgressListener((percentage) => {
			var progress = document.getElementById("progress");
			progress.style.display = "block";
			progress.style.color = "green";
			progress.style.width = percentage + "%";
			if (percentage == 100) {
				progress.style.display = "none";
			}
		});

		this.showProjectDetails(project.name, project.oid);

		UpdateUriPathVars.setPoid(oid);
		let updateUriPathVars = new UpdateUriPathVars(project.oid, this.surfer);
		updateUriPathVars.updateUri();
		UpdateUriPathVars.getPathVariables();
		let visibilityState = UpdateUriPathVars.getPathVariables().visibilityState;

		// create a marker/pin overlay object
		this.markerOverlay = new MarkerOverlay(this.canvas, this.viewer);

		document.getElementById("l-overlay").style.visibility = "visible";

		// shows the project tree structure
		let proj = new Project(project, this.api, this.surfer);
		proj.show(() => {
			proj.loadModel();
		});

		let toolbar = new Toolbar(this.viewer, visibilityState, updateUriPathVars);
		toolbar.setVisibility(visibilityState);
	}

	updatePinVisibilityArray(pinServiceURI, status) {
		if (this.visiblePins == null || this.visiblePins == undefined)
			this.visiblePins = new Set()

		if (status === "none") {
			if (this.visiblePins.size > 0) {
				if (this.visiblePins.has(pinServiceURI)) {
					this.visiblePins.delete(pinServiceURI)
				}
				UpdateUriPathVars.setPinPathVars(this.visiblePins)
			}
		} else {
			this.visiblePins.add(pinServiceURI)
			UpdateUriPathVars.setPinPathVars(this.visiblePins)
		}
	}

	showProjectDetails(projectName, poid) {
		let projectDetailsContainer = document.getElementById('project-details');
		projectDetailsContainer.innerHTML += `<h2>${projectName}, poid: ${poid}</h2>`;
		this.showPinList(poid);
	}

	showPinList(poid) {
		let projectPinsContainer = document.getElementById('project-pins');
		FetchAPI.fetchPinByPoid(poid, (data) => {
			let output = `
				<table id='pin-table' class="table table-hover table-condensed table-sm">
  				<tr>
    				<th>Pins</th>
					<th></th>
  				</tr>
			`;
			if (data) {
				if (data.hasOwnProperty('message')) {
					output += `
						<tr>
							<td>No pins for this project!</td>
						</tr>
					`;
					console.log(data.message);
				} else if (data.length !== undefined) {
					this.pins = data; // dont want to fetch each time the data

					data.forEach(pin => {
						let iconLocation = iconServerLocation + pin.icon + ".png";
						if (pin.icon === "") {
							iconLocation = "./img/pin.png"
						}

						// check if the pin exists already in the dom, else remove it
						let pinIconId = pin.id + "_" + pin.pin_reference_object_id
						let tempPin = document.getElementById(pinIconId);
						if (tempPin) {
							tempPin.remove();
						}

						// create the pin
						this.markerOverlay._newMarkerIcon(pin.id, pin.pin_reference_object_id, pin.pin_title, pin.x, pin.y, pin.z, iconLocation, pin.service_uri);

						// if pin is in the path vars show it otherwise hide it
						let dataPathVars = UpdateUriPathVars.getPathVariables()
						if (dataPathVars.hasOwnProperty("pins")) {
							if (dataPathVars.pins.length > 0) {
								if (!dataPathVars.pins.includes(pin.service_uri)) {
									document.getElementById(pinIconId).style.display = "none"
								} else {
									document.getElementById(pinIconId).style.display = "block"
								}
							}
						}

						output += `
							<tr id='${pin.id}' object_id='${pin.pin_reference_object_id}'  service_uri='${pin.service_uri}' class='pin-row-element'>
								<td class='pin-item'>
									<span class="align-middle">${pin.pin_title}</span>
								</td>
								<td class="text-end">
									<div class="btn-group-sm" role="group">
										<button id="hide-pin" class="hide-pin-btn btn btn-outline-primary btn-sm ">Hide</button>
									</div>
								</td>
							</tr>
						`;
					});
				}
			}
			output += `</table>`
			projectPinsContainer.innerHTML = output;

			let showAllbtn = "<button id='show-all-btn' class='l-overlay-control-btn btn btn-sm btn-outline-secondary'>" + 
				"<img src='./img/show_all.png' title='Show All Pins' class='loverlay-control-btn-img'></img>" +
				"<span>Show all</span>" + 
				"</button>";
			let hideAllbtn = "<button id='hide-all-btn' class='l-overlay-control-btn btn btn-sm btn-outline-secondary'>" + 
				"<img src='./img/hide_all.png' title='Hide All Pins' class='loverlay-control-btn-img'></img>" + 
				"<span>Hide all</span>" + 
				"</button>";
			projectPinsContainer.innerHTML += "<div id='other-controls'>" + 
				"<div id='all-visibility-control' class='text-center'>" + showAllbtn + hideAllbtn + "</div>" +
				"</div>";

			document.getElementById("show-all-btn").addEventListener('click', () => {
				document.querySelectorAll(".pin").forEach(pinElement => {
					pinElement.style.display = "block";
					this.updatePinVisibilityArray(pinElement.attributes.service_uri.value, "");
				});
				document.querySelectorAll('.hide-pin-btn').forEach(btn => {
					btn.innerText = "Hide";
				});
			});

			document.getElementById("hide-all-btn").addEventListener('click', () => {
				document.querySelectorAll(".pin").forEach(pinElement => {
					pinElement.style.display = "none";
					this.updatePinVisibilityArray(pinElement.attributes.service_uri.value, "none");
				});
				document.querySelectorAll('.hide-pin-btn').forEach(btn => {
					btn.innerText = "Show";
				});
			});

			if (data.length === undefined) {
				document.getElementById("show-all-btn").setAttribute("disabled", true);
				document.getElementById("hide-all-btn").setAttribute("disabled", true);
			}

			let tableRows = document.querySelectorAll(".pin-row-element");
			tableRows.forEach( rowElement => {
				let pinIconId = rowElement.id + "_" + rowElement.attributes.object_id.value;
				
				rowElement.addEventListener("mouseover", () => {
					document.getElementById(pinIconId).style.borderTop = "2px solid #457B9D";
				});

				rowElement.addEventListener("mouseout", () => {
					document.getElementById(pinIconId).style.removeProperty("border-top");
				});
			});

			let h = document.querySelectorAll('.hide-pin-btn');
			for (let i = 0; i < h.length; i++) {
				let _id = parseInt(h[i].parentNode.parentNode.parentNode.id);
				let rObj = this.getReferenceObjId(_id, this.pins);
				let imgId = rObj.id + "_" + rObj.pin_reference_object_id;
				let img = document.getElementById(imgId);
				let target_suri = h[i].parentNode.parentNode.parentNode.attributes.service_uri.nodeValue;


				if (img.style.display === "none") {
					h[i].innerText = "Show";
					this.updatePinVisibilityArray(target_suri, "none")
				} else {
					h[i].innerText = "Hide";
					this.updatePinVisibilityArray(target_suri, "")
				}

				h[i].addEventListener('click', () => {
					if (imgId !== null && imgId !== undefined) {
						let img = document.getElementById(imgId);
						if (img.style.display === "block" || img.style.display === "") {
							img.style.display = "none"
							h[i].innerText = "Show";
							this.updatePinVisibilityArray(target_suri, "none")
						} else {
							img.style.display = "block";
							h[i].innerText = "Hide";
							this.updatePinVisibilityArray(target_suri, "")
						}
					}
				});
			}
		});
	}

	/**
	 * @param {*} id Id of the pin from db
	 * @param {*} pinsArray The array of pins
	 * @returns Returns the id of the 3D object that refers the pin otherwise null/undefined
	 */
	getReferenceObjId(id, pinsArray) {
		for (let i = 0; i < pinsArray.length; i++) {
			if (id === pinsArray[i].id) {
				return pinsArray[i];
			}
		}
	}
}

var simple3dViewer = new Simple3DViewer();

simple3dViewer.start();