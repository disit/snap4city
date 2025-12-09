/*
 *  Copyright (C) 2020 DISIT Lab http://www.disit.org - University of Florence
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
class OnOffSwitch {
	constructor( id , onText , offText ){
	  	var onoff = $(OnOffSwitch.template);
	    var inner = onoff.find(".onoffswitch-inner");
	    inner.attr('data-text-on' , onText );
	    inner.attr('data-text-off' , offText );
	     
	    var input = onoff.find('input.onoffswitch-checkbox');
	    input.attr( "id" , id );
	    
	    var label = onoff.find('label.onoffswitch-label');
	    label.attr( "for" , id );
	    
	    input.change( function(){
	    	onoff.toggleClass("on");
	    	onoff.toggleClass("off");
	    });
	    
	    this.onoff = onoff;
	    this.input = input;
  }
}

OnOffSwitch.template = 
	`<div class="onoffswitch on">
	    <input type="checkbox" name="onoffswitch" class="onoffswitch-checkbox" tabindex="0" checked>
	    <label class="onoffswitch-label">
	        <span class="onoffswitch-inner"></span>
	        <span class="onoffswitch-switch"></span>
	    </label>
	</div`;