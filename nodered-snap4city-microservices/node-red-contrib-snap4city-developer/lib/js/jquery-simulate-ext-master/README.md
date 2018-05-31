[![Project Status: Abandoned](https://img.shields.io/badge/project%20status-abandoned-red.svg)](https://github.com/j-ulrich/jquery-simulate-ext/issues/39)    
**NOTE:** jQuery Simulate Extended is not actively developed anymore. It may still work but chances are high that some features are broken in recent browsers. For more information, see [issue #39](https://github.com/j-ulrich/jquery-simulate-ext/issues/39).


jQuery Simulate Extended Plugin 1.3.0
=====================================

The jQuery Simulate Extended plugin (a.k.a. jquery-simulate-ext) provides methods for simulating complex
user interactions based on the [jQuery.simulate()](https://github.com/jquery/jquery-simulate) plugin.
The plugin provides simulation of:

* Drag & Drop
* Key Sequences
* Key Combinations

Additionally, the extended plugin includes documentation and fixes for the jQuery simulate plugin itself.

#### Table of Contents ####
- [Usage](#usage)
	- [Example](#example)
	- [Demos](#demos)
- [Documentation](#documentation)
- [Requirements](#requirements)
- [Compatibility](#compatibility)
	- [Quirk Detection](#quirk-detection)
- [Licensing](#licensing)


Usage
-----
To use the jquery-simulate-ext plugin, you need to include (in the given order):

1. `bililiteRange.js`
  [if you want to use `jquery.simulate.key-sequence.js` or `jquery.simulate.key-combo.js`]
1. `jquery-x.y.z.js`
1. `jquery.simulate.js`
1. `jquery.simulate.ext.js`
1. `jquery.simulate.drag-n-drop.js` [if you want to simulate drag & drop]
1. `jquery.simulate.key-sequence.js` [if you want to simulate key sequences]
1. `jquery.simulate.key-combo.js` [if you want to simulate key combos]

The simulations are executed by calling the `.simulate()` function on a jQuery object. The simulation
is then executed on all elements in the collection of the jQuery object (unless otherwise noted).

- Synopsis: `.simulate(type, options)`
- Parameters:
	* __type__ _{String}_: The type of the interaction to be simulated.
	* __options__ _{Object}_: An option object containing the options for the action to be simulated.

The types of simulated actions are:

- From the jquery-simulate plugin:
	- Mouse Events: `"mousemove"`, `"mousedown"`, `"mouseup"`, `"click"`, `dblclick"`,
		`"mouseover"`, `"mouseout"`, `"mouseenter"`, `"mouseleave"`, `"contextmenu"`
	- Key Events: `"keydown"`, `"keyup"`, `"keypress"`
	- `"focus"`
	- `"blur"`
- From the jquery-simulate-ext plugin:
	- Drag & Drop: `"drag-n-drop"`, `"drag"`, `"drop"`
	- `"key-sequence"`
	- `"key-combo"`

#### Example: ####
```javascript
$('input[name="testInput"]').simulate("key-sequence", {sequence: "asdf"});
```

#### Demos: ####
The [demos folder](https://github.com/j-ulrich/jquery-simulate-ext/tree/master/demo) contains a
demonstration of most of the features of the simulate extended plugins.

Live demos can be found at jsFiddle and JS Bin where you can also play around with the plugin:

- http://jsfiddle.net/Ignitor/Psjhf/embedded/result/ ([jsFiddle](http://jsfiddle.net/Ignitor/Psjhf/))
- http://jsbin.com/inalax/25/edit#live ([JS Bin](http://jsbin.com/inalax/25/edit))


Documentation
-------------
The options and events for the different interactions are described in the files in the [doc folder](https://github.com/j-ulrich/jquery-simulate-ext/tree/master/doc):
* [Mouse Events](https://github.com/j-ulrich/jquery-simulate-ext/tree/master/doc/simulate.md)
* [Key Events](https://github.com/j-ulrich/jquery-simulate-ext/tree/master/doc/simulate.md)
* [Focus/Blur](https://github.com/j-ulrich/jquery-simulate-ext/tree/master/doc/simulate.md)
* [Drag & Drop](https://github.com/j-ulrich/jquery-simulate-ext/tree/master/doc/drag-n-drop.md)
* [Key Sequence](https://github.com/j-ulrich/jquery-simulate-ext/tree/master/doc/key-sequence.md)
* [Key Combination](https://github.com/j-ulrich/jquery-simulate-ext/tree/master/doc/key-combo.md)

### Global Options: ###
Options recognized by all jquery-simulate-ext plugins:

* __eventProps__ _{Object}_: Defines custom properties which will be attached to the simulated events.   
	__Note:__ Trying to define default properties of events (e.g. `type`, `bubbles`, `altKey`, etc.) using this option
	might not work since those properties are typically read-only.   
	__Note:__ The `dispatchEvent()` function of all major browsers will remove custom properties from the event.
	Therefore, the [`jquery.simulate.js`](https://github.com/j-ulrich/jquery-simulate-ext/tree/master/libs/jquery.simulate.js)
	from the jquery-simulate-ext repository contains an option to use `jQuery.trigger()` instead of the
	native `dispatchEvent()`. This causes that the simulated event will only trigger event handlers attached using the same
	jQuery instance and not handlers attached using [`addEventListener()`](https://developer.mozilla.org/en-US/docs/Web/API/EventTarget.addEventListener)
	or using another versions of jQuery (see http://bugs.jquery.com/ticket/11047 for more information), but it's the only way
	to allow the custom properties to be used in the event handlers. To activate this option, define `jQueryTrigger: true`
	in the `eventProps` option object. For example:
	
	```javascript
	$('#mySimulateTarget').simulate('key-sequence', {
		sequence: "my simulated text",
		eventProps: {
			jQueryTrigger: true,
			simulatedEvent: true
		}
	});
	```
	__Tip:__ As the example shows, this allows to flag the simulated events, which allows to
	distinguish the simulated events from real events in the event handlers.
	See [issue #12](https://github.com/j-ulrich/jquery-simulate-ext/issues/12) for more information.


Requirements
------------
The plugin requires
* [jQuery 1.7.0+](http://jquery.com)
* [jQuery Simulate](https://github.com/jquery/jquery-ui/blob/master/tests/jquery.simulate.js)   
  __Note:__ With the [current version](https://github.com/jquery/jquery-ui/blob/485ca7192ac57d018b8ce4f03e7dec6e694a53b7/tests/jquery.simulate.js)
  of `jquery.simulate.js`, not all features of jquery-simulate-ext work correctly (see for example
  [Drag & Drop within iframes](https://github.com/j-ulrich/jquery-simulate-ext/tree/master/doc/drag-n-drop.md#iframes)).
  Therefore, the jquery-simulate-ext repository contains a fixed version of `jquery.simulate.js` at
  [`libs/jquery.simulate.js`](https://github.com/j-ulrich/jquery-simulate-ext/tree/master/libs/jquery.simulate.js).
* [bililiteRange](http://bililite.com/blog/2011/01/17/cross-browser-text-ranges-and-selections) for
	the key-sequence and key-combo plugins

Compatibility
------------
The plugins have been successfully tested with jQuery 1.7.2, 1.10.2, 2.1.0 and jQuery Simulate
[@485ca7192a](https://github.com/jquery/jquery-ui/blob/485ca7192ac57d018b8ce4f03e7dec6e694a53b7/tests/jquery.simulate.js),
[@25938de206](https://github.com/jquery/jquery-simulate/blob/25938de20622a6c127a7082bd751f6d2f88eabd4/jquery.simulate.js).
However, they should be compatible with other/future versions as well.

### Quirk Detection ###
There are some issues with bililiteRange and some browsers. To workaround these issues, jquery-simulate-ext
performs some quirk detections when the document is ready. Those quirk detections also contain temporary DOM manipulations.
If you don't want those DOM manipulations to take place, you can disable the quirk detection by setting the flag
`ext_disableQuirkDetection` in the `jQuery.simulate` object **after** `jquery.simulate.js` has been loaded but **before**
any jquery-simulate-ext plugin is loaded. For example:
```html
<!-- ... -->
<script type="text/javascript" src="../libs/jquery.simulate.js"></script>
<script type="text/javascript">$.simulate.ext_disableQuirkDetection = true;</script>
<script type="text/javascript" src="../src/jquery.simulate.ext.js"></script>
<script type="text/javascript" src="../src/jquery.simulate.key-sequence.js"></script>
<!-- ... -->
```
For more information, see [issue #9](https://github.com/j-ulrich/jquery-simulate-ext/issues/9).

Licensing
---------
Copyright &copy; 2014 Jochen Ulrich
https://github.com/j-ulrich/jquery-simulate-ext

Licensed under the [MIT license](http://opensource.org/licenses/MIT).

