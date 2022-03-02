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

export function EventRegistry(){
	var o = this;
	o.registry = [];
	
	this.register = function(fn) {
		var skip = false;
		o.registry.forEach(function(existing){
			if (existing == fn) {
				skip = true;
			}
		});
		if (!skip) {
			o.registry.push(fn);
		}
	};
	
	this.unregister = function(fn) {
		var len = o.registry.length;
		while (len--) {
			if (o.registry[len] == fn) {
				o.registry.splice(len, 1);
			}
		}
	};
	
	this.size = function(){
		return o.registry.length;
	};
	
	this.trigger = function(callback){
		o.registry.forEach(callback);
	};
	
	this.clear = function(){
		o.registry = [];
	};
}