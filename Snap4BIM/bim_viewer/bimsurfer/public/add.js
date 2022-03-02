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
import { ProjectTreeModel } from "./bimsurfer-viewer/projecttreemodel.js"
import { TreeView } from "./bimsurfer-viewer/treeview.js"
import { Credentials } from "./credentials.js"
import { BimSurfer } from "./bimsurfer-viewer/bimsurfer.js";
import { MarkerOverlay } from "./markeroverlay.js";
import { FetchAPI } from "./fetchapi.js";
import { Project } from "./project.js";
import { UpdateUriPathVars } from "./update_uri_path_vars.js";
import { Toolbar } from "./toolbar.js";
import { bimserverURL, viewerConfig, iconServerLocation } from "./config.js"


export class _3DView {

	// initial configuration of bimsurfer
	start() {
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
			let rs = document.getElementById("r-overlay") || null;
			if (rs != null) {
				rs.style.height = window.innerHeight + "px";
			}
		} else {
			document.getElementById("l-overlay").style.height = (window.innerHeight - 200) + "px";
			document.getElementById("tree-container").style.height = (window.innerHeight - 200 - ((window.innerHeight / 100) * 40)) + "px";
			let rs = document.getElementById("r-overlay") || null;
			if (rs != null) {
				rs.style.height = (window.innerHeight - 200) + "px";
			}

		}
	}

	/**
	 * Loads the selected project
	 * @param {*} project project object returned form bimserver
	 */
	loadModel(project) {
		document.getElementById("l-overlay").style.height = window.innerHeight + "px";
		document.getElementById("r-overlay").style.height = window.innerHeight + "px";
		document.getElementById("tree-container").style.height = window.innerHeight - (window.innerHeight * 0.4) + "px";

		window.onresize = this.setwindowDimensions;


		document.getElementById("projects-tree-viewer").style.display = "none";
		document.getElementById("project-viewer").style.display = "block";

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

		this.showProjectDetails(project.name, project.oid);

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

		// create a markerOverlay object 
		this.markerOverlay = new MarkerOverlay(this.canvas, this.viewer);
		document.getElementById("l-overlay").style.visibility = "visible";

		this.setMarker = false;

		// handle the click events
		this.canvas.addEventListener("click", (event) => {
			let data = this.getDataForSelected3DObject(event);
			if (data !== undefined) {
				this.updatePinFormFields(data);
			}
		});

		let proj = new Project(project, this.api, this.surfer);
		proj.show(() => {
			proj.loadModel();
		});

		UpdateUriPathVars.setPoid(oid);
		let updateUriPathVars = new UpdateUriPathVars(project.oid, this.surfer);
		updateUriPathVars.updateUri();
		let visibilityState = UpdateUriPathVars.getPathVariables().visibilityState;

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


	enableMarkerPoint() {
		this.setMarker = true;
	}

	/**
	 * Used to get the properties of the selected 3d object
	 * @param {*} event event
	 * @returns the selected 3d object properties
	 */
	getDataForSelected3DObject(event) {
		if (this.setMarker) {

			// get coordinates from canvas click
			let rect = this.canvas.getBoundingClientRect();
			let x = event.clientX - rect.left;
			let y = event.clientY - rect.top;

			// get the coordinates in the 3D world
			let pickedObj = this.viewer.pick({ canvasPos: [x, y] });
			let _3dCoordinatesPicked;

			if (pickedObj.object !== null) {
				_3dCoordinatesPicked = pickedObj.coordinates;

				let selectedObj = {
					pin_reference_object_id: pickedObj.object.uniqueId,
					ref_object_type: pickedObj.object.type,
					x: _3dCoordinatesPicked[0],
					y: _3dCoordinatesPicked[1],
					z: _3dCoordinatesPicked[2]
				};

				this.setMarker = false;
				return selectedObj;
			}
		}
	}

	/**
	 * Updates the form (r-overlay) with properties of the selected pin object
	 * @param {*} pin object 
	 */
	updatePinFormFields(pin) {
		// get the pin ID from the db
		let refProjectNameIn = document.getElementById('ref-project');
		let pinDatabaseId = document.getElementById('pin-id');

		let selectedObjTitle = document.getElementById('pin-title');
		let selectedObjDescription = document.getElementById('pin-description');
		let selectedObjType = document.getElementById('nature');
		let selectedObjSubtype = document.getElementById('subnature');
		let selectedObjAction = document.getElementById('action');
		let selectedObjUri = document.getElementById('service-uri');
		let selectedObjectId = document.getElementById('pin-object-id');
		let selectedCoordX = document.getElementById('pin-x');
		let selectedCoordY = document.getElementById('pin-y');
		let selectedCoordZ = document.getElementById('pin-z');
		let selectedPinIcon = document.getElementById('pin-icon');

		if (pin.id !== null && pin.id !== undefined) {
			pinDatabaseId.value = pin.id;
		}
		if (pin.poid !== null && pin.poid !== undefined) {
			refProjectNameIn.value = pin.poid;
		}
		if (pin.icon !== null && pin.icon !== undefined && pin.icon !== "") {
			selectedPinIcon.value = pin.icon;
		}
		if (pin.pin_description !== null && pin.pin_description !== undefined) {
			selectedObjDescription.value = pin.pin_description;
		}
		if (pin.nature !== null && pin.nature !== undefined) {
			selectedObjType.value = pin.nature;
		}
		if (pin.subnature !== null && pin.subnature !== undefined) {
			selectedObjSubtype.value = pin.subnature;
		}
		if (pin.action !== null && pin.action !== undefined) {
			selectedObjAction.value = pin.action;
		}
		if (pin.service_uri !== null && pin.service_uri !== undefined) {
			selectedObjUri.value = pin.service_uri;
		}
		if (pin.pin_title !== null && pin.pin_title !== undefined && pin.pin_title !== "") {
			selectedObjTitle.value = pin.pin_title;
		}
		if (pin.pin_reference_object_id !== null && pin.pin_reference_object_id !== undefined && pin.pin_reference_object_id !== 0) {
			selectedObjectId.value = pin.pin_reference_object_id;
		}
		if (pin.x !== null && pin.x !== undefined && pin.x !== 0) {
			selectedCoordX.value = pin.x;
		}
		if (pin.y !== null && pin.y !== undefined && pin.y !== 0) {
			selectedCoordY.value = pin.y;
		}
		if (pin.z !== null && pin.z !== undefined && pin.z !== 0) {
			selectedCoordZ.value = pin.z;
		}
	}

	/**
	 * Shows the data (l-overlay) related to the project poid
	 * @param {*} projectName project name
	 * @param {*} poid project id
	 */
	showProjectDetails(projectName, poid) {
		// show title and poid of selected project
		document.getElementById('project-details')
			.innerHTML += `<h2>${projectName}, poid: ${poid}</h2>`;

		// show a button for adding new pins
		document.getElementById('add-new-pin')
			.addEventListener('click', () => {
				let pinFormDiv = document.getElementById('r-overlay');
				pinFormDiv.style.visibility = 'visible';
				document.getElementById('add-pin').reset();
			});

		this.pinFormWindow(poid, projectName);

		// show list of pins for this project
		this.showPinList(poid);
	}

	/**
	 * Setup the functionality of the form (r-overlay)
	 * @param {*} poid project id
	 * @param {*} projectName project name
	 */
	pinFormWindow(poid, projectName) {
		let pinFormDiv = document.getElementById('r-overlay');
		//pinFormDiv.style.visibility = 'visible';

		let submitFormButton = document.getElementById('submit-pin-form');
		let cancelFormButton = document.getElementById('cancel-pin-form');
		let addSelectedObject = document.getElementById('select-pin-form');
		let resetFieldsButton = document.getElementById('reset-fields-button');
		let serviceUriField = document.getElementById('service-uri');
		let refProjectNameIn = document.getElementById('ref-project');
		refProjectNameIn.value = poid;

		let submited = true;

		submitFormButton.addEventListener('click', (e) => {
			this.submitPinForm(e, poid, projectName, submited);
		});
		cancelFormButton.addEventListener('click', () => {
			pinFormDiv.style.visibility = 'hidden';
		});
		resetFieldsButton.addEventListener('click', () => {
			document.getElementById('add-pin').reset();
		});


		// Adds the data of the selected 3d object to the form 
		addSelectedObject.addEventListener('click', () => {
			this.enableMarkerPoint();
		});

		// fetch data based on the service uri
		serviceUriField.addEventListener('change', (e) => {

			if (e.target.value !== "") {
				FetchAPI.fetchDataFromServiceUriXHR(e.target.value, (data) => {
					let dataType = Object.keys(data)[0];
					let serviceType = data[dataType].features[0].properties.serviceType;
					if (serviceType !== "" && serviceType !== null && serviceType !== undefined) {
						let tmp = serviceType.split("_");
						document.getElementById('nature').value = tmp[0];
						document.getElementById('subnature').value = tmp[1];
						document.getElementById('pin-icon').value = serviceType;
					}
				});
			}
		});
	}

	/**
	 * Gets the data from form (r-ovelay) and returns them using a callback(pinData)
	 * @param {*} callback to handle the data of the pin
	 */
	getPinFormData(callback) {
		let pinData = {};

		pinData.poid = parseInt(document.getElementById('ref-project').value);
		pinData.id = document.getElementById('pin-id').value;

		pinData.pin_title = document.getElementById('pin-title').value;
		pinData.pin_description = document.getElementById('pin-description').value;
		pinData.pin_reference_object_id = document.getElementById('pin-object-id').value;
		pinData.x = document.getElementById('pin-x').value;
		pinData.y = document.getElementById('pin-y').value;
		pinData.z = document.getElementById('pin-z').value;
		pinData.nature = document.getElementById('nature').value;
		pinData.subnature = document.getElementById('subnature').value;
		pinData.action = document.getElementById('action').value;
		pinData.service_uri = document.getElementById('service-uri').value;
		pinData.icon = document.getElementById('pin-icon').value;

		callback(pinData);
	}

	/**
	 * Submit(POST/PUT) data inserted in the form r-overlay
	 * @param {*} e event
	 * @param {*} poid project id
	 * @param {*} projectName project name
	 * @param {*} submited True/False 
	 */
	submitPinForm(e, poid, projectName, submited) {
		e.preventDefault();

		if (submited) {
			submited = false;

			this.getPinFormData((data) => {
				let message = document.getElementById('query-result');

				data.poid = poid;
				data.project_name = projectName;

				// verify if we have the database id of the pin
				if (data.id !== null && data.id !== undefined && data.id !== "") {
					console.log("PUTTing");
					// PUT because we have the id of the pin from the database
					FetchAPI.updatePin(data, (response) => {
						if (response.message !== "") {
							message.classList.add('alert-success');
							message.innerText = response.message;
							message.style.display = "block";
							message.focus();

							setTimeout(() => {
								message.innerText = "";
								message.classList.remove('alert-success');
								message.style.display = "none";
								submited = true;

								// clean the table 
								document.getElementById('pin-table').remove();
								this.markerOverlay.removeMarkerIconAll();

								// remove all the pin icons
								document.querySelectorAll('.pin').forEach((p) => {
									p.remove();
								});
								// here update the table of pins
								this.showPinList(poid);
								document.getElementById('add-pin').reset();

							}, 2000);
						}
					});

				} else {
					// remove the id property so we can add new data
					// otherwise it gets confused with modification of a pin
					delete data.id;

					FetchAPI.addPin(data, (response) => {
						if (response.hasOwnProperty('message')) {
							if (response.message !== "") {
								message.classList.add('alert-danger');
								message.innerText = response.message;
								message.style.display = "block";
								message.focus();
								setTimeout(() => {
									message.innerText = "";
									message.classList.remove('alert-danger');
									message.style.display = "none";
									submited = true;
								}, 2000);
							} else if (response.message === "" && response.affectedRows) {
								message.classList.add('alert-success');
								message.innerText = 'Pin saved!';
								message.style.display = "block";
								message.focus();
								setTimeout(() => {
									message.innerText = "";
									document.getElementById('add-pin').reset();
									message.classList.remove('alert-success')
									message.style.display = "none";
									submited = true;
									document.getElementById('add-pin').reset();
									this.showPinList(poid);
								}, 2000);
							}
						}
					});
				}
			});
		}
	}

	/**
	 * Shows a table with pins related to the project poid and 
	 * also shows the pin icons on the dom/canvas
	 * @param {*} poid project id
	 */
	showPinList(poid) {
		let projectPinsContainer = document.getElementById('project-pins');

		FetchAPI.fetchPinByPoid(poid, (data) => {
			// create a table template for listing the pins
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

						// check if the pin exists already in the dom, otherwise remove it
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
							} else if (dataPathVars.pins == null) {
								document.querySelectorAll(".pin").forEach(pinElement => {
									pinElement.style.display = "none";
									this.updatePinVisibilityArray(pinElement.attributes.service_uri.value, "none");
								});
							}
						}

						output += `
							<tr id='${pin.id}' object_id='${pin.pin_reference_object_id}' service_uri='${pin.service_uri}' class='pin-row-element'>
								<td class='pin-item'>
									<span class="align-middle">${pin.pin_title}</span>
								</td>
								<td class="text-end">
									<div class="btn-group-sm" role="group">
										<button id="modify-pin" class="modify-pin-btn btn btn-outline-primary btn-sm ">Modify</button>
										<button id="hide-pin" class="hide-pin-btn btn btn-outline-primary btn-sm ">Hide</button>
										<button id="remove-pin" class="remove-pin-btn btn btn-outline-danger btn-sm">Remove</button>
									</div>
								</td>
							</tr>
						`;
					});
				}
			}
			output += `</table>`
			projectPinsContainer.innerHTML = output;

			let copyGeneratedUrl = "<button id='coppy-generated-url-btn' type='button' class='l-overlay-control-btn btn btn-sm btn-outline-secondary'>" +
				"<img src='./img/link.png' title='Copy URL' class='loverlay-control-btn-img'></img>" +
				"<span>Copy URL</span>" +
				"</button>";
			let urlInputField = "<input id='url-input-field' class='form-control form-control-sm' readonly></input>";
			let sidebarInputSelector = "<div class='form-check justify-content-end mt-1'>" +
				"<input class='form-check-input' type='checkbox' value='' id='sidebar-checkbox'>" +
				"<label class='form-check-label' for='sidebar-checkbox' id='sidebar-checkbox-label'>" +
				"sidebar=on (default)" +
				"</label>" +
				"</div>";
			let showAllbtn = "<button id='show-all-btn' class='l-overlay-control-btn btn btn-sm btn-outline-secondary'>" +
				"<img src='./img/show_all.png' title='Show All Pins' class='loverlay-control-btn-img'></img>" +
				"<span>Show all</span>" +
				"</button>";
			let hideAllbtn = "<button id='hide-all-btn' class='l-overlay-control-btn btn btn-sm btn-outline-secondary'>" +
				"<img src='./img/hide_all.png' title='Hide All Pins' class='loverlay-control-btn-img'></img>" +
				"<span>Hide all</span>" +
				"</button>";
			projectPinsContainer.innerHTML += "<div id='other-controls'>" + "<div id='all-visibility-control' class='d-flex justify-content-end'>" + showAllbtn + hideAllbtn + "</div>" +
					"<div id='url-view-controls' class='card p-1 mt-3 mb-3'>" + 
						"<div id='url-controls' class='input-group'>" +
						"<div class='input-group-prepend'>" + copyGeneratedUrl + "</div>" +
						urlInputField +
					"</div>" +
					sidebarInputSelector +
					"</div>" +
			"</div>";



			let checkbox = document.getElementById("sidebar-checkbox");
			checkbox.addEventListener("change", () => {
				let urlField = document.getElementById("url-input-field");
				let url = window.location.href.slice();
				url.replace("add.html", "view.html")
				.replace("&sidebar=on", "")
				.replace("&sidebar=off", "");
				if (checkbox.checked) {
					document.getElementById("sidebar-checkbox-label").innerText = "sidebar=off";
					urlField.value = url + "&sidebar=off";
					UpdateUriPathVars.setSidebarVisibility("off");
				} else {
					document.getElementById("sidebar-checkbox-label").innerText = "sidebar=on (default)";
					urlField.value = url + "&sidebar=on";
					UpdateUriPathVars.setSidebarVisibility("on");
				}
			});

			document.getElementById("coppy-generated-url-btn").addEventListener('click', () => {
				let url = window.location.href.slice();
				let inputField = document.getElementById("url-input-field");
				inputField.value = url.replace("add.html", "view.html");
				inputField.focus();
				inputField.select();
				document.execCommand("copy");
			});

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

			//initialize url input field
			let url = window.location.href.replace("add.html", "view.html");
			let inputField = document.getElementById("url-input-field");
			inputField.value = url

			// add event listeners for the modify/show(hide)/delete buttons
			let m = document.querySelectorAll('.modify-pin-btn');
			let d = document.querySelectorAll('.remove-pin-btn');
			let h = document.querySelectorAll('.hide-pin-btn');

			let tableRows = document.querySelectorAll(".pin-row-element");
			tableRows.forEach(rowElement => {
				let pinIconId = rowElement.id + "_" + rowElement.attributes.object_id.value;

				rowElement.addEventListener("mouseover", () => {
					document.getElementById(pinIconId).style.borderTop = "2px solid #457B9D";
				});

				rowElement.addEventListener("mouseout", () => {
					document.getElementById(pinIconId).style.removeProperty("border-top");
				});
			});

			for (let i = 0; i < m.length; i++) {
				// modify button
				m[i].addEventListener('click', () => {
					document.getElementById('r-overlay').style.visibility = 'visible';
					let itemId = parseInt(m[i].parentNode.parentNode.parentNode.id);
					this.updatePinFormFields(this.getReferenceObjId(itemId, this.pins));
				});

				let _id = parseInt(h[i].parentNode.parentNode.parentNode.id);
				let rObj = this.getReferenceObjId(_id, this.pins);
				let imgId = rObj.id + "_" + rObj.pin_reference_object_id;
				let img = document.getElementById(imgId);
				let target_suri = h[i].parentNode.parentNode.parentNode.attributes.service_uri.nodeValue;

				// set hide button initiall text value
				if (img.style.display === "none") {
					h[i].innerText = "Show";
					this.updatePinVisibilityArray(target_suri, "none")
				} else {
					h[i].innerText = "Hide";
					this.updatePinVisibilityArray(target_suri, "")
				}

				// hide button
				h[i].addEventListener('click', () => {
					if (imgId !== null && imgId !== undefined) {
						//let img = document.getElementById(imgId);
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

				// remove button
				d[i].addEventListener('click', () => {
					let itemId = parseInt(d[i].parentNode.parentNode.parentNode.id);
					if (confirm("Confirm to remove item")) {
						FetchAPI.removePin({ id: itemId }, (res) => {
							this.removeItemWithId(itemId);
						});
					}
				});
			}
		});
	}

	/**
	 * Removes a pin (using its database id) from the table on l-overlay and from the dom/canvas  
	 * @param {*} id from the db of the pin
	 */
	removeItemWithId(id) {
		// remove item from the table on the l-overlay
		let pinElTable = document.getElementById(id);
		pinElTable.remove();

		// remove icon from the canvas/dom
		let refObj = this.getReferenceObjId(id, this.pins);
		let imgId = refObj.id + "_" + refObj.pin_reference_object_id;
		if (imgId !== null && imgId !== undefined) {
			document.getElementById(imgId).remove();
			console.log("removeItemWithId(id)", imgId);
			this.markerOverlay.removeMarkerIcon(imgId);
		}
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

var _3dViewer = new _3DView();

_3dViewer.start();
