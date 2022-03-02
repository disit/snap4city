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
import { Tree, Node } from "./tree.js";

export function ProjectTree(containerDiv, parent, options, selected, api) {

	var othis = this;
	othis.api = api;
	othis.selected = selected;
	othis.selectedNode = null;

	// this loads the tree with the root node
	this.load = function () {
		othis.tree = new Tree($(".tree"));
		// add a function to the array of listeners
		othis.tree.selectionListeners.push(function (node) {
			//parent.selectedObject = othis.tree;
			parent.selectedObject = node.object;
			// add values to the property of the parent object, initially = null/undefined
		});
		var lastParent = -1;
		var nrSubprojects = 0;
	};

	// removes the "Ifc" from the string
	this.stripIfc = function (input) {
		if (input.startsWith("Ifc")) {
			return input.substring(3);
		} else {
			return input;
		}
	}

	// adds the project (lastREvision) root element
	this.addRevision = function (project, roid, types) {

		// parentNode is the root node element
		var parentNode = othis.tree.find(project.parentId);

		// this has id = -1
		if (parentNode == null) {
			parentNode = othis.tree.find(-1);
		}

		var node = Object.create(Node.prototype);
		node.init(project.oid, project.name, project);

		parentNode.add(node);
		//var dropdown = othis.createDropDown(node);

		node.addToDom(function (node) {
			return othis.createEye(node)
		});

		node.click(function () {
			node.select();
			othis.api.call("ServiceInterface", "getProjectByPoid", { poid: node.project.oid }, function (project) {
				parent.loadDetails(project);
			});
		});

		//var history = History.getState().data;
		if (othis.project != null) { //&& (history.poid == project.oid || othis.project.oid == project.oid)) {
			node.select();
		}

		node.setIcon("folder");
		node.type = "Project";
		node.project = project;
		node.groupId = roid;
		node.roid = roid;
		node.sort = true;
		node.poid = project.oid;
		node.hint("Project");

		if (options != null && options.hideDeleted == true && project.state == "DELETED") {
			node.hide();
		}
		if (project.nrSubProjects == 0 || (project.subProjects != null && project.subProjects.length == 0)) {
			if (roid != -1) {
				node.onLoad(function () {
					othis.loadProjectTree(project, roid, node, types);
				});
			}
		} else {
			node.doneLoading();
			node.open();
		}
	};

	this.setShowEyes = function (show) {
		containerDiv.find(".eye").each(function (index, eye) {
			if (!show) {
				$(eye).hide();
			} else {
				var node = $(eye).data("node");
				if (node.project != null) {
					if (node.project.lastRevisionId != -1) {
						$(eye).show();
					}
				}
			}
		});
	};

	this.loadRevision = function (project, roid, types) {
		var promise = new BimServerApiPromise();
		var node = othis.tree.find(project.oid);

		if (project.nrSubProjects == 0) {
			//			promise.chain(node.open());
		} else {
			node.doneLoading();
			node.open()////promise.chain(node.open());
		}
		if (options != null && options.hideDeleted == true && project.state == "DELETED") {
			node.hide();
		}
		return promise;
	};

	this.unloadRevision = function (poid, roid) {
		var node = othis.tree.find(poid);
		if (node != null) {
			node.remove();
		}
	};

	this.close = function () { };

	// this fucntion is for the dropdown menu in bimvie.ws
	this.toggleDropDown = function (node) {
		othis.selectedNode = node;

		var thisElement = $(this);

		Global.bimServerApi.call("ServiceInterface", "getAllRevisionsOfProject", { poid: othis.selectedNode.poid }, function (revisions) {
			dropdown.find(".revisions li").remove();
			if (revisions.length == 0) {
				dropdown.find(".btnDownload").hide();
				dropdown.find(".revisionDivider").hide();
				dropdown.find(".btnRegenerateGeometry").hide();
				$(".revisionsLi").hide();
			} else {
				dropdown.find(".btnDownload").show();
				dropdown.find(".revisionDivider").show();
				dropdown.find(".btnRegenerateGeometry").show();
				$(".revisionsLi").show();
				revisions.forEach(function (revision) {
					var li = $("<li><a>" + revision.id + " (" + revision.comment + ")</a></li>");
					li.find("a").attr("poid", othis.selectedNode.poid);
					li.find("a").attr("roid", revision.oid);
					li.find("a").data("project", othis.selectedNode.project);
					if (othis.selectedNode.roid == revision.oid) {
						li.find("a").css("font-weight", "bold");
					}
					li.find("a").click(function () {
						dropdown.hide();
						var a = $(this);
						parent.changeRevision(a.data("project"), a.attr("roid"));
					});
					dropdown.find(".revisions").append(li);
				});
			}
			dropdown.show();
		});
	};

	// this function is for the dropdown menu in bimvie.ws
	this.createDropDown = function (node) {
		var dropdown = $("<a>");
		dropdown.addClass("tree-dropdown");
		dropdown.attr("data-toggle", "dropdown");
		dropdown.attr("title", "New project");
		dropdown.attr("data-html", "<p>To <span class=\"underline\">checkin a revision</span> or <span class=\"underline\">add subprojects</span> click on the triangle <img src=\"../img/dropdown.png\"/> or use the project menu in the top-left corner</p>");
		dropdown.attr("data-target", "#dropdown");
		dropdown.click(function () {
			othis.toggleDropDown.call(this, node);
		});
		node.addButton(dropdown);
		return dropdown;
	};

	this.hide = function () {
		containerDiv.hide();
	};

	// this helps locating the element on the tree when click on 3d space
	this.show = function () {
		containerDiv.show();
		if (othis.selectedId != -1) {
			var node = othis.tree.find(othis.selectedId);
		}
	};

	this.select = function (origin, id) {
		var treeNode = othis.tree.find(id);
		if (treeNode != null) {
			treeNode.openRecursive();
			treeNode.select();

			if (origin != othis) {
				// Somehow scrollTo messes up the selection of the selected object in WebGL, probably some browser drawing issue
				setTimeout(function () {
					$(".sidespanWrapper").scrollTo(treeNode.li);
				}, 1);
			}
			return true;
		} else {
			return false;
		}
	};

	this._selected = function (origin, groupId, object) {
		if (othis.selected == object) {
			return;
		}
		othis.selected = object;
		othis.select(object.oid);
		var treeNode = othis.tree.find(object.oid);
		if (othis.select(origin, object.oid)) {
			//nothing here
		} else {
			// Maybe this object has not yet been loaded in the tree, but is available in the model
			var model = parent.models[groupId];
			model.get(object.oid, function (object) {
				// Now we will have to see to which parent structure this object belongs...
				if (object.getContainedInStructure != null) {
					object.getContainedInStructure(function (struc) {
						struc.getRelatingStructure(function (relatingStructure) {
							if (relatingStructure.getType() == "IfcBuildingStorey") {
								var buildingStructureNode = othis.tree.find(relatingStructure.oid);
								if (buildingStructureNode == null) {
									console.log("IfcBuildingStorey not found in tree");
								} else {
									othis.loadBuildingStorey(buildingStructureNode, relatingStructure, []).done(function () {
										if (!othis.select(origin, id)) {
											console.log("Still not found");
										}
									});
								}
							}
						});
					});
				}
			});
		}
	};

	this.unselected = function (groupId, id) {
		var treeNode = othis.tree.find(id);
		if (treeNode != null) {
			treeNode.unselect();
		}
	};

	// when object is visibile or not, this sets up the eye on the tree
	this.objectVisibilityChanged = function (objects, oldMode) {
		objects.forEach(function (object) {
			var node = othis.tree.find(object.oid);
			if (node != null) {
				var mode = object.trans.mode;
				if (node.eye != null) {
					node.eye.attr("mode", mode);
					if (mode == 0) {
						node.eye.removeClass("eyeclosed").removeClass("eyehalfopen").addClass("eyeopen");
					} else if (mode == 1) {
						node.eye.removeClass("eyeopen").removeClass("eyeclosed").addClass("eyehalfopen");
					} else if (mode == 2) {
						node.eye.removeClass("eyehalfopen").removeClass("eyeopen").addClass("eyeclosed");
					}
				}
			}
		});
	};

	this.treeItemClick = function (treeNode) {

		if (typeof treeNode.id === "number" && treeNode.type.startsWith("Ifc")) {
			var groupNode = treeNode.findFirstParentWithAttr("groupId");
			parent.selected(othis, groupNode.groupId, treeNode.object);
		}
	};

	this.toggleEye = function (node, mode) {
		//		setTimeout(function(){
		node.loadRecursively().done(function () {
			var projectNodes = node.listFilterByType("Project");
			if (projectNodes.length == 0) {
				othis.internalToggleEye(node, mode);
			} else {
				projectNodes.forEach(function (projectNode) {
					if (projectNode.roid != -1) {
						if (!parent.models[projectNode.roid].isPreloaded) {
							othis.loadRevision(projectNode.project, projectNode.roid, []).done(function () {
								othis.internalToggleEye(projectNode, mode);
							});
						} else {
							othis.internalToggleEye(projectNode, mode);
						}
					}
				});
			}
		});
		//		}, 500);
	};

	this.internalToggleEye = function (node, mode) {
		var nodeList = node.list();
		var groupNode = node.findFirstParentWithAttr("groupId");
		if (groupNode.groupId != -1) {

			/**@fix settings class */
			var hiddenTypes = { "IfcOpeningElement": true, "IfcSpace": true };
			var ifcOids = [];
			nodeList.forEach(function (subNode) {
				if (subNode.type.startsWith("Ifc") && typeof subNode.id === "number") {
					var explicit = node.id == subNode.id || (node.type != null && node.type == subNode.type);
					if (hiddenTypes[subNode.type] == null || explicit || mode != 0) {
						ifcOids.push(subNode.id);
					}
				}
			});

			// here calls the parent object to set the visibility of the selected element
			parent.setObjectVisibility(groupNode.groupId, ifcOids, mode);
			nodeList.forEach(function (n) {
				var explicit = node.id == n.id || (node.type != null && node.type == n.type);
				if (hiddenTypes[n.type] == null || explicit || mode != 0) {
					if (n.eye != null) {
						n.eye.attr("mode", mode);
						if (mode == 0) {
							n.eye.removeClass("eyeclosed").removeClass("eyehalfopen").addClass("eyeopen");
						} else if (mode == 1) {
							n.eye.removeClass("eyeopen").removeClass("eyeclosed").addClass("eyehalfopen");
						} else if (mode == 2) {
							n.eye.removeClass("eyehalfopen").removeClass("eyeopen").addClass("eyeclosed");
						}
					}
				}
			});
		}
	};

	this.createEye = function (node) {
		var eye = $("<div>");
		eye.addClass("eye");
		if (node.object.trans != null) {
			var mode = node.object.trans.mode;
		} else {
			var mode = 0;
		}
		if (mode == 0) {
			eye.addClass("eyeopen");
		} else if (mode == 1) {
			eye.addClass("eyehalfopen");
		} else if (mode == 2) {
			eye.addClass("eyeclosed");
		}
		eye.attr("mode", mode);
		eye.click(function () {
			var mode = parseInt(eye.attr("mode"));
			othis.toggleEye(node, (mode + 1) % 3);
		});
		//eye.toggle(parent.threeDAspectVisible.get());
		eye.data("node", node);
		node.eye = eye;
		return eye;
	};

	this.processRelatedElement = function (parentNode, relatedElement, createdTypes, types) {
		var typeNode = createdTypes[relatedElement.getType()];
		if (typeNode == null) {
			typeNode = Object.create(Node.prototype);
			typeNode.init(relatedElement.getType(), this.stripIfc(relatedElement.getType()), relatedElement);

			typeNode.setIcon("types");
			typeNode.type = relatedElement.getType();


			if (types[relatedElement.getType()] == null) {
				typeNode.addToDom(function (node) { return othis.createEye(node) });
			} else {
				typeNode.addToDom(function (node) { return othis.createEye(node) });
			}

			createdTypes[relatedElement.getType()] = typeNode;
			parentNode.add(typeNode);

		}
		othis.buildDecomposedTree(relatedElement, typeNode, types);
	};

	this.loadBuildingStorey = function (newNode, object, types) {
		var promise = new BimServerApiPromise();
		var createdTypes = {};
		object.getIsDecomposedBy(function (isDecomposedBy) {
			if (isDecomposedBy != null) {
				isDecomposedBy.getRelatedObjects(function (relatedObject) {
					othis.processRelatedElement(newNode, relatedObject, createdTypes, types);
				});
			}
		});
		object.getContainsElements(function (relReferencedInSpatialStructure) {
			relReferencedInSpatialStructure.getRelatedElements(function (relatedElement) {
				othis.processRelatedElement(newNode, relatedElement, createdTypes, types);
			}).done(function () {
				object.getIsDecomposedBy(function (isDecomposedBy) {
					if (isDecomposedBy != null) {
						isDecomposedBy.getRelatedObjects(function (relatedObject) {
							othis.processRelatedElement(newNode, relatedObject, createdTypes, types);
						});
					}
				});
			});
		}).done(function () {
			promise.fire();
		});
		return promise;
	};

	this.buildDecomposedTree = function (object, tree, types) {
		var name = null;
		if (object.getLongName != null) {
			if (object.getLongName() != null && object.getLongName() != "") {
				name = object.getLongName();
			}
		}
		if (name == null) {
			if (object.getName() != null && object.getName() != "") {
				name = object.getName();
			}
		}
		if (name == null) {
			name = "Unknown";
		}
		var newNode = Object.create(Node.prototype);
		newNode.init(object.oid, name, object);

		newNode.type = object.getType();
		if (object.getType() == "IfcProject" || object.getType() == "IfcSite" || object.getType() == "IfcBuilding" || object.getType() == "IfcBuildingStorey" || object.getType() == "IfcSpace") {
			newNode.setIcon("hierarchy");
		} else {
			newNode.setIcon("ball");
		}
		newNode.click(othis.treeItemClick);
		newNode.hint(object.getType() + " - " + object.getName());

		newNode.addToDom(function (node) { return othis.createEye(node) });

		tree.add(newNode);
		if (object.getType() == "IfcBuildingStorey") {
			newNode.onLoad(function () {
				var promise = new BimServerApiPromise();
				othis.loadBuildingStorey(newNode, object, types).done(function () {
					promise.fire();
				});
				return promise;
			});
		} else {
			newNode.onLoad(function () {
				var promise = new BimServerApiPromise();
				//				setTimeout(function(){
				object.getIsDecomposedBy(function (isDecomposedBy) {
					isDecomposedBy.getRelatedObjects(function (relatedObject) {
						othis.buildDecomposedTree(relatedObject, newNode, types);
					});
				}).done(function () {
					if (object.getContainsElements != null) {
						object.getContainsElements(function (containedElement) {
							containedElement.getRelatedElements(function (relatedElement) {
								othis.buildDecomposedTree(relatedElement, newNode, types);
							});
						});
					}
					promise.fire();
				});
				//				}, 50);
				return promise;
			});
			if ((object.object._rIsDecomposedBy == null || object.object._rIsDecomposedBy.length == 0) && (object.object._rContainsElements == null || object.object._rContainsElements.length == 0)) {
				newNode.setNoChildren();
			}
		}
	}

	//**@fix model is null */
	this.loadProjectTree = function (project, roid, node, types) {
		var promise = new BimServerApiPromise();
		var model = parent.models[roid];
		if (model != null) {
			parent.preloadModel(project, roid).done(function () {

				// model.getAllOfType(type, includeAllSubTypes, callback)
				model.getAllOfType("IfcProject", false, function (project) {
					othis.buildDecomposedTree(project, node, types);
				}).done(function () {
					promise.fire();
					node.li.append(node.ul);
					node.doneLoading();
				});
			});
		} else {
			promise.fire();
		}
		return promise;
	};

	this.gatherRoidList = function (node, list) {
		if (node.type == "Project") {
			if (node.roid != -1) {
				list.push(node.roid);
			}
			if (node.children) {
				node.children.forEach(function (child) {
					othis.gatherRoidList(child, list);
				});
			}
		}
	};

	this.addMenuItem = function (title, cl, click, popup) {
		var li = $("<li class=\"" + cl + "\"></li>");
		var a = $("<a>" + title + "</a>");
		a.click(function () {
			containerDiv.find(".dropdown").hide();
			click();
		});
		li.append(a);
		containerDiv.find(".treeDropDown").append(li);
		return li;
	};

	this.addMenuDivider = function (className) {
		var li = $("<li class=\"divider\"></li>");
		if (className != null) {
			li.addClass(className);
		}
	};
}
