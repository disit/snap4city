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

import { BimServerApiPromise } from "../bimsurfer-api/bimserverapipromise.js";
import { EventRegistry } from "./eventregistry.js";
import { ProjectTree } from "./ptree.js";


/**
 * 
 * @param {*} project object from bimserver
 * @param {*} api  object from bimserver javascrip api
 * @param {*} surfer BIMsurfer object
 */
export function Project(project, api, surfer) {
	var othis = this;
	othis.loaded = {}; // poid -> roid
	othis.models = {};
	othis.types = {};
	othis.project = project;
	othis.api = api;
	othis.surfer = surfer;


	othis.selectListeners = new EventRegistry();
	othis.unselectListeners = new EventRegistry();
	othis.objectVisibilityListeners = new EventRegistry();
	othis.modelLoadedListeners = new EventRegistry();
	othis.modelAddedListeners = new EventRegistry();
	othis.modelUnloadedListeners = new EventRegistry();


	this.show = function (push) {
		othis.updateProject(push);
	};

	// reloads the project
	this.reloadProject = function (poid, push) {
		if (poid == null) {
			poid = othis.project.oid;
		}
		othis.api.call("ServiceInterface", "getProjectByPoid", { poid: poid }, function (project) {
			othis.project = project;
			othis.updateProject(push)
		});
	};

	this.close = function () {
		othis.menuItems.forEach(function (menuItem) {
			menuItem.remove();
		});
	};

	this.selected = function (origin, groupId, object) {
		othis.surfer.setSelectionState({
			ids: [object.oid],
			selected: true,
			clear: true
		});

		othis.surfer.viewFit({ ids: [object.oid] })
		// push the object.oid to the list of selected elements
		//othis.treeElementSelected.push(object.oid);

		othis.selectListeners.trigger(function (selectListener) {
			selectListener(origin, groupId, object);
		});
	};

	this.unselected = function (groupId, id) {
		othis.unselectListeners.trigger(function (unselectListener) {
			unselectListener(groupId, id);
		});
	};

	this.setObjectVisibility = function (groupId, ids, mode) {
		if (!Array.isArray(ids)) {
			ids = [ids];
		}

		var objectArray = [];
		var oldModeArray = [];

		ids.forEach(function (id) {
			othis.models[groupId].get(id, function (object) {
				if (object != null) {
					var oldMode = object.trans.mode;
					if (oldMode != mode) {
						object.trans.mode = mode;
						objectArray.push(object);
						oldModeArray.push(oldMode);
					}
				}
			});
		});

		// get the project element ids
		// extract the elements to be hidden
		// show the elements from Array.from(viewer.viewObjects.keys())
		this.changeVisibilityOfObjects(objectArray);
	};

	this.changeVisibilityOfObjects = function (objectArray) {
		let visibleElements = [];
		let transparentElements = [];
		let hiddenElements = [];

		othis.surfer.reset({ visibility: true, colors: true });

		for (let i = 0; i < objectArray.length; i++) {
			if (objectArray[i] != null && objectArray[i].oid != null) {
				let object = objectArray[i];
				if (object.trans.mode == 0) {
					visibleElements.push(object.oid);
				}
				if (object.trans.mode == 1) {
					transparentElements.push(object.oid);
				}
				if (object.trans.mode == 2) {
					hiddenElements.push(object.oid)
				}
			}
		}

		othis.surfer.setVisibility({ ids: visibleElements, visible: true });
		othis.surfer.setVisibility({ ids: hiddenElements, visible: false });
		othis.surfer.setColor({
			ids: transparentElements,
			color: {
				r: 150,
				g: 100,
				b: 100,
				a: 0.8
			}
		});

	}

	this.resize = function () {};

	this.unloadRevision = function (project) {
		if (othis.loaded[project.oid] != null) {
			othis.modelUnloadedListeners.trigger(function (modelUnloadedListener) {
				modelUnloadedListener(project.oid, othis.loaded[project.oid]);
			});
		}
	};

	this.changeRevision = function (project, roid) {
		if (othis.loaded[project.oid] != null) {
			othis.modelUnloadedListeners.trigger(function (modelUnloadedListener) {
				modelUnloadedListener(project.oid, othis.loaded[project.oid]);
			});
		}
		othis.loaded[project.oid] = roid;

		if (project.nrSubProjects == 0 && roid != -1) {
			// TODO possibly cache
			othis.api.getModel(project.oid, roid, project.schema, false, function (model) {
				othis.models[roid] = model;
				othis.modelAddedListeners.trigger(function (listener) {
					listener(project, roid, [], {});
				});
			}, project.name);
		} else {
			othis.modelAddedListeners.trigger(function (modelAddedListener) {
				modelAddedListener(project, -1, [], {});
			});
		}
	};

	this.loadModel = function () {
		othis.projects.forEach(function (project) {
			var ids = [];
			var roid = project.lastRevisionId;
			var model = othis.models[roid];

			othis.preloadModel(project, roid).done(function () {
				// here we have the preloaded model
			});
		});
	};

	this.preloadModel = function (project, roid) {
		//var countingPromise = new CountingPromise();
		var promise = new BimServerApiPromise();
		var model = othis.models[roid];
		if (model == null) {
			console.log("no model", othis.models);
		} else {
			if (model.isPreloaded) {
				promise.fire();
				return promise;
			} else {
				if (project.schema == "ifc2x3tc1") {
					var preLoadQuery = {
						defines: {
							Representation: {
								type: "IfcProduct",
								fields: ["Representation", "geometry"]
							},
							ContainsElementsDefine: {
								type: "IfcSpatialStructureElement",
								field: "ContainsElements",
								include: {
									type: "IfcRelContainedInSpatialStructure",
									field: "RelatedElements",
									includes: [
										"IsDecomposedByDefine",
										"ContainsElementsDefine",
										"Representation"
									]
								}
							},
							IsDecomposedByDefine: {
								type: "IfcObjectDefinition",
								field: "IsDecomposedBy",
								include: {
									type: "IfcRelDecomposes",
									field: "RelatedObjects",
									includes: [
										"IsDecomposedByDefine",
										"ContainsElementsDefine",
										"Representation"
									]
								}
							}
						},
						queries: [
							{
								type: "IfcProject",
								includes: [
									"IsDecomposedByDefine",
									"ContainsElementsDefine"
								]
							},
							{
								type: {
									name: "IfcRepresentation",
									includeAllSubTypes: true
								}
							},
							{
								type: {
									name: "IfcProductRepresentation",
									includeAllSubTypes: true
								}
							},
							{
								type: "IfcPresentationLayerWithStyle"
							},
							{
								type: {
									name: "IfcProduct",
									includeAllSubTypes: true
								}
							},
							{
								type: "IfcProductDefinitionShape"
							},
							{
								type: "IfcPresentationLayerAssignment"
							},
							{
								type: "IfcRelAssociatesClassification",
								includes: [
									{
										type: "IfcRelAssociatesClassification",
										field: "RelatedObjects"
									},
									{
										type: "IfcRelAssociatesClassification",
										field: "RelatingClassification"
									}
								]
							},
							{
								type: "IfcSIUnit"
							},
							{
								type: "IfcPresentationLayerAssignment"
							}
						]
					};
				} else if (project.schema == "ifc4") {
					var preLoadQuery = {
						defines: {
							Representation: {
								type: "IfcProduct",
								fields: ["Representation", "geometry"]
							},
							ContainsElementsDefine: {
								type: "IfcSpatialStructureElement",
								field: "ContainsElements",
								include: {
									type: "IfcRelContainedInSpatialStructure",
									field: "RelatedElements",
									includes: [
										"IsDecomposedByDefine",
										"ContainsElementsDefine",
										"Representation"
									]
								}
							},
							IsDecomposedByDefine: {
								type: "IfcObjectDefinition",
								field: "IsDecomposedBy",
								include: {
									type: "IfcRelAggregates",
									field: "RelatedObjects",
									includes: [
										"IsDecomposedByDefine",
										"ContainsElementsDefine",
										"Representation"
									]
								}
							}
						},
						queries: [
							{
								type: "IfcProject",
								includes: [
									"IsDecomposedByDefine",
									"ContainsElementsDefine"
								]
							},
							{
								type: {
									name: "IfcRepresentation",
									includeAllSubTypes: true
								}
							},
							{
								type: {
									name: "IfcProductRepresentation",
									includeAllSubTypes: true
								}
							},
							{
								type: "IfcPresentationLayerWithStyle"
							},
							{
								type: {
									name: "IfcProduct",
									includeAllSubTypes: true
								},
							},
							{
								type: "IfcProductDefinitionShape"
							},
							{
								type: "IfcPresentationLayerAssignment"
							},
							{
								type: "IfcRelAssociatesClassification",
								includes: [
									{
										type: "IfcRelAssociatesClassification",
										field: "RelatedObjects"
									},
									{
										type: "IfcRelAssociatesClassification",
										field: "RelatingClassification"
									}
								]
							},
							{
								type: "IfcSIUnit"
							},
							{
								type: "IfcPresentationLayerAssignment"
							}
						]
					};
				}
				model.query(preLoadQuery, function (loaded) {
				}).done(function () {
					console.timeEnd("preloadModel " + roid);
					//Global.notifier.setInfo("Loading model data...", -1);
					setTimeout(function () {
						model.isPreloaded = true;
						othis.modelLoadedListeners.trigger(function (modelLoadedListener) {
							modelLoadedListener(project, roid);
						});
						//Global.notifier.setSuccess("Model data successfully loaded");
						promise.fire();
					}, 0);
				});
			}
		}
		return promise;
	};

	this.updateProject = function (callback) {
		othis.projects = [];
		othis.api.call("ServiceInterface", "getAllRelatedProjects", { poid: othis.project.oid }, function (list) {
			list.forEach(function (smallProject) {
				if (smallProject.state == "ACTIVE") {
					othis.loaded[smallProject.oid] = smallProject.lastRevisionId;
					othis.projects.push(smallProject);
				}
				if (smallProject.lastRevisionId != -1 && smallProject.nrSubProjects == 0) {
					othis.api.getModel(smallProject.oid, smallProject.lastRevisionId, smallProject.schema, false, function (model) {
						othis.models[smallProject.lastRevisionId] = model;

						// load types
						othis.loadTypes(smallProject, smallProject.lastRevisionId, (typesArray) => {
							let __selected;//= undefinend;
							let projecttree = new ProjectTree($(".projectTreeWrapper"), othis, {}, __selected, othis.api);
							projecttree.load();
							
							projecttree.addRevision(othis.project, othis.project.lastRevisionId, othis.typesArray);
						});

						callback();

					}, smallProject.name);
				}
			});

			othis.selectListeners.clear();
			othis.unselectListeners.clear();
			othis.objectVisibilityListeners.clear();
			othis.modelLoadedListeners.clear();
			othis.modelUnloadedListeners.clear();

		});
	};

	this.loadDetails = function (project) {
		if (othis.rightPanel != null) {
			//null
		} else {
			if (project.lastRevisionId != -1) {
				othis.api.call("ServiceInterface", "getRevision", { roid: project.lastRevisionId }, function (data) {
				});
			} else {
				//null
			}
		}
	};

	// Loads all the types of the ifc structure that this projects contains
	this.loadTypes = function (project, roid, callback) {
		if (roid != -1 && project.nrSubProjects == 0 || project.subProjects.length == 0) {
			var m = othis.models[roid];
			var t = 0;
			m.getAllOfType("IfcProduct", true, function (object) {
				t++;
				if (object.isA("IfcProduct")) {
					var type = object.getType();
					if (othis.types[type] == null) {
						othis.types[type] = { visible: 0, transparent: 0, total: 0 };
					}
					if (object.trans.mode == 0) {
						othis.types[type].visible = othis.types[type].visible + 1;
					} else if (object.trans.mode == 1) {
						othis.types[type].transparent = othis.types[type].transparent + 1;
					}
					othis.types[type].total = othis.types[type].total + 1;
				}
			}).done(() => {
				othis.typesArray = Object.getOwnPropertyNames(othis.types);
				callback(othis.typesArray);
			});
		}
	}
}
