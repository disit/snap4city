import * as mat4 from "./glmatrix/mat4.js";
import * as vec4 from "./glmatrix/vec4.js";
import * as vec3 from "./glmatrix/vec3.js";
import * as vec2 from "./glmatrix/vec2.js";

export const DRAG_ORBIT = 0xfe01;
export const DRAG_PAN = 0xfe02;
export const DRAG_SECTION = 0xfe03;

/**
 Controls the camera with user input.
 */
export class CameraControl {

    constructor(viewer) {

        this.viewer = viewer;

        this.mousePanSensitivity = 1; // 0.5;
        this.mouseOrbitSensitivity = 0.5;
        this.canvasPickTolerance = 4;

        this.canvas = viewer.canvas;
        this.camera = viewer.camera;

        this.mousePos = vec2.create();
        this.mouseDownPos = vec2.create();
        this.over = false; // True when mouse over canvas
        this.lastX = 0; // Last canvas pos while dragging
        this.lastY = 0;

        this.keysDown = new Map();
        this.keyMapping = {
            "ArrowRight": "x_pos",
            "ArrowLeft": "x_neg",
            "ArrowUp": "y_neg",
            "ArrowDown": "y_pos",
            "PageUp": "z_neg",
            "PageDown": "z_pos",
            //"w": "y_neg",
            //"a": "x_neg",
            //"s": "y_pos",
            //"d": "x_pos",
            "q": "z_neg",
            "z": "z_pos"
        };

        this.axoKeyMapping = {
            "1": "z_pos",
            "2": "z_neg",
            "3": "x_pos",
            "4": "x_neg",
            "5": "y_pos",
            "6": "y_neg",
        }

        this.mouseDown = false;
        this.dragMode = DRAG_ORBIT;

        this._tmp_topleftfront_0 = vec3.create();
        this._tmp_topleftfront_1 = vec3.create();

        this.canvas.oncontextmenu = (e) => {
            e.preventDefault();
        };

        this.canvas.addEventListener("mousedown", this.canvasMouseDownHandler = (e) => {
            this.canvasMouseDown(e);
        });

        this.canvas.addEventListener("mouseup", this.canvasMouseUpHandler = (e) => {
            this.canvasMouseUp(e);
        });

        this.documentMouseUpHandler = (e) => {
            this.documentMouseUp(e);
        };
        document.addEventListener("mouseup", this.documentMouseUpHandler);

        this.canvasKeyUpHandler = (e) => {
            this.canvasKeyProcess(e, false);
        };
        this.canvas.addEventListener("keyup", this.canvasKeyUpHandler);

        this.canvasKeyDownHandler = (e) => {
            this.canvasKeyProcess(e, true);
        };
        this.canvas.addEventListener("keydown", this.canvasKeyDownHandler);

        this.canvas.addEventListener("mouseenter", this.canvasMouseEnterHandler = (e) => {
            this.over = true;
            e.preventDefault();
        });

        this.canvas.addEventListener("mouseleave", this.canvasMouseLeaveHandler = (e) => {
            this.over = false;
            e.preventDefault();
        });

        this.canvas.addEventListener("mousemove", this.canvasMouseMoveHandler = (e) => {
            this.canvasMouseMove(e);
        });

        this.canvas.addEventListener("wheel", this.canvasMouseWheelHandler = (e) => {
            this.canvasWheel(e);
        });

        this.canvas.addEventListener("touchstart", this.touchStartHandler = (e) => {
            this.canvasMouseDown(e);
        });

        this.canvas.addEventListener("touchend", this.touchEndHandler = (e) => {
            this.canvasMouseUp(e);
        });

        this.canvas.addEventListener("touchmove", this.touchMoveHandler = (e) => {
            this.canvasMouseMove(e);
        });

        window.setInterval(this.keyTick.bind(this), 10);
    }

    /**
     * @private
     */
    getCanvasPosFromEvent(event, canvasPos) {
        if (!event) {
            event = window.event;
            canvasPos[0] = event.x;
            canvasPos[1] = event.y;
        } else {
            let pageX = null, pageY = null;
            if (window.TouchEvent && event instanceof TouchEvent) {
                if (event.touches.length == 0) {
                    return;
                }
                if (event.touches.length == 2) {
                    let coords = Array.from(event.touches).map(t => vec2.fromValues(t.pageX, t.pageY));
                    this.pinchDistance = vec2.length(vec2.sub(vec2.create(), ...coords));
                    let avg = vec2.add(vec2.create(), ...coords);
                    vec2.scale(avg, avg, 0.5);
                    pageX = avg[0];
                    pageY = avg[1];
                } else {
                    pageX = event.touches[0].pageX;
                    pageY = event.touches[0].pageY;
                }
            } else {
                pageX = event.pageX;
                pageY = event.pageY;
            }
            const rect = event.target.getBoundingClientRect();
            const totalOffsetLeft = rect.left;
            const totalOffsetTop = rect.top;
            canvasPos[0] = pageX - totalOffsetLeft;
            canvasPos[1] = pageY - totalOffsetTop;
        }
        return canvasPos;
    }

    /**
     * @private
     */
    getZoomRate() {
        var modelBounds = this.viewer.modelBounds;
        if (modelBounds) {
            var xsize = modelBounds[3] - modelBounds[0];
            var ysize = modelBounds[4] - modelBounds[1];
            var zsize = modelBounds[5] - modelBounds[2];
            var max = (xsize > ysize ? xsize : ysize);
            max = (zsize > max ? zsize : max);
            return max / 20;
        } else {
            return 1;
        }
    }

    /**
     * @private
     */
    canvasMouseDown(e) {
        this.lastPan = +new Date();

        this.getCanvasPosFromEvent(e, this.mousePos);

        this.lastX = this.mousePos[0];
        this.lastY = this.mousePos[1];

        this.mouseDown = true;
        this.mouseDownTime = e.timeStamp;
        this.mouseDownPos.set(this.mousePos);

        let handleSection = () => {
            this.mouseDownTime = 0;
            if (this.viewer.enableSectionPlane({ canvasPos: [this.lastX, this.lastY] })) {
                this.dragMode = DRAG_SECTION;
            } else if (!this.viewer.sectionPlaneIsDisabled) {
                this.viewer.disableSectionPlane();
                this.dragMode = DRAG_ORBIT;
            }
            this.viewer.removeSectionPlaneWidget();
        }

        let handleOrbit = () => {
            this.dragMode = e.shiftKey ? DRAG_PAN : DRAG_ORBIT;
            let picked = this.viewer.pick({ canvasPos: [this.lastX, this.lastY], select: false });
            if (picked && picked.coordinates && picked.object) {
                this.viewer.camera.center = picked.coordinates;
            } else {
                // Check if we can 'see' the previous center. If not, pick
                // a new point.
                let center_vp = vec3.transformMat4(vec3.create(), this.viewer.camera.center, this.viewer.camera.viewProjMatrix);

                let isv = true;
                for (let i = 0; i < 3; ++i) {
                    if (center_vp[i] < -1. || center_vp[i] > 1.) {
                        isv = false;
                        break;
                    }
                }

                if (!isv) {
                    let [x, y] = this.mousePos;
                    vec3.set(center_vp, x / this.viewer.width * 2 - 1, - y / this.viewer.height * 2 + 1, 1.);
                    vec3.transformMat4(center_vp, center_vp, this.camera.viewProjMatrixInverted);
                    vec3.subtract(center_vp, center_vp, this.camera.eye);
                    vec3.normalize(center_vp, center_vp);
                    vec3.scale(center_vp, center_vp, this.getZoomRate() * 10.);
                    vec3.add(center_vp, center_vp, this.camera.eye);
                    console.log("new center", center_vp);
                    this.viewer.camera.center = center_vp;
                }
            }
        };

        let handlePan = () => {
            this.dragMode = DRAG_PAN;
        }

        if (window.TouchEvent && e instanceof TouchEvent) {
            if (e.touches.length == 1) {
                handleOrbit();
            } else if (e.touches.length == 2) {
                this.lastPinchDistance = this.pinchDistance;
                handlePan();
            } else if (e.touches.length == 3) {
                handleSection();
            }
        } else {
            if (e.which == 1 && e.ctrlKey) {
                handleSection();
            } else if (e.which == 1) {
                handleOrbit();
            } else if (e.which == 2) {
                handlePan();
            }
        }

        this.over = true;
        if (this.dragMode == DRAG_PAN || e.shiftKey) {
            e.preventDefault();
        }
    }

    /**
     * @private
     */
    canvasMouseUp(e) {
        this.camera.orbitting = false;
        this.viewer.overlay.update();
        this.getCanvasPosFromEvent(e, this.mousePos);

        let dt = e.timeStamp - this.mouseDownTime;
        this.mouseDown = false;

        const handleClick = () => {
            if (dt < 500. && this.closeEnoughCanvas(this.mouseDownPos, this.mousePos)) {
                var viewObject = this.viewer.pick({
                    canvasPos: this.mousePos,
                    select: true, // e.which == 3,
                    shiftKey: (e.which == 1 || e.which == 0) ? e.shiftKey : this.viewer.selectedElements.size > 0, // e.which == 0 on touch events
                    onlyAdd: e.which == 3 && this.viewer.selectedElements.size > 0
                });
                if (viewObject && viewObject.object) {
                    //console.log("Picked", viewObject.object);
                }
                this.viewer.drawScene();
            }
        }

        if ((window.TouchEvent && e instanceof TouchEvent && this.dragMode == DRAG_ORBIT) || (e instanceof MouseEvent && e.which == 1)) {
            handleClick();
        }

        e.preventDefault();
    }

    /**
     * @private
     */
    canvasWheel(e) {
        this.getCanvasPosFromEvent(e, this.mousePos);
        var delta = Math.max(-1, Math.min(1, -e.deltaY * 40));
        if (delta === 0) {
            return;
        }
        var d = delta / Math.abs(delta);
        var zoom = -d * this.getEyeLookDist() / 20.;
        this.camera.zoom(zoom, this.mousePos);
        e.preventDefault();
    }

    keyTick() {
        let f;
        if (this.keysDown.size) {
            f = this.getEyeLookDist() / 600;
        }
        let vec = [0., 0., 0.];
        this.keysDown.forEach((v, action) => {
            if (v) {
                let axis = action.charCodeAt(0) - 120; // gets values in [x:0, y:1, z:2]
                let direction = action.charAt(2) == 'p'; // is p:positive or n:negative increment
                vec[axis] += direction ? +f : -f;
            }
        });
        if (this.keysDown.size) {
            this.camera.pan(vec);
        }
    }

    moveToAxo(axo) {
        this._tmp_topleftfront_0[0] = this._tmp_topleftfront_1[0] = (this.viewer.modelBounds[0] + this.viewer.modelBounds[3]) / 2;
        this._tmp_topleftfront_0[1] = this._tmp_topleftfront_1[1] = (this.viewer.modelBounds[1] + this.viewer.modelBounds[4]) / 2;
        this._tmp_topleftfront_0[2] = this._tmp_topleftfront_1[2] = (this.viewer.modelBounds[2] + this.viewer.modelBounds[5]) / 2;

        let axis = axo.charCodeAt(0) - 120;
        let direction = axo.charAt(2) == 'p';
        this._tmp_topleftfront_0[axis] = this.viewer.modelBounds[axis + (direction ? 3 : 0)];

        this.camera.calcViewFit(null, null, this._tmp_topleftfront_0, this._tmp_topleftfront_1);

        this.camera.interpolateView(this._tmp_topleftfront_0, this._tmp_topleftfront_1);
    }

    canvasKeyProcess(e, state) {
        let axo = this.axoKeyMapping[e.key];
        if (axo && state == false) {
            this.moveToAxo(axo);
            return;
        }
        let action = this.keyMapping[e.key];
        if (action) {
            if (state) {
                this.keysDown.set(action, state);
            } else {
                this.keysDown.delete(action);
            }
        } else if (e.key == "Control") {
            if (state) {
                if (this.viewer.sectionPlaneIsDisabled) {
                    this.viewer.positionSectionPlaneWidget({ canvasPos: [this.lastX, this.lastY] });
                }
            } else {
                this.viewer.removeSectionPlaneWidget();
            }
        } else if (e.key == "Home") {
            this.camera.viewFit({ animate: true });
            this.viewer.dirty = 2;
        } else if (e.key == "Insert") {
            // Should show the model from the side
            this.camera.target = [0, 0, 0];
            this.camera.eye = [1, 0, 0];
            this.camera.viewFit({ aabb: this.camera.modelBounds, animate: true });
        }
    }

    /**
     * @private
     */
    closeEnoughCanvas(p, q) {
        return p[0] >= (q[0] - this.canvasPickTolerance) &&
            p[0] <= (q[0] + this.canvasPickTolerance) &&
            p[1] >= (q[1] - this.canvasPickTolerance) &&
            p[1] <= (q[1] + this.canvasPickTolerance);
    }

    /**
     * @private
     */
    canvasMouseMove(e) {
        if (!this.over) {
            return;
        }
        if (this.mouseDown || e.ctrlKey) {
            this.getCanvasPosFromEvent(e, this.mousePos);
            if (this.dragMode == DRAG_SECTION) {
                this.viewer.moveSectionPlane({ canvasPos: this.mousePos });
            } else if (e.ctrlKey) {
                this.viewer.positionSectionPlaneWidget({ canvasPos: this.mousePos });
            } else {
                var x = this.mousePos[0];
                var y = this.mousePos[1];
                var xDelta = (x - this.lastX);
                var yDelta = (y - this.lastY);
                this.lastX = x;
                this.lastY = y;
                if (this.dragMode == DRAG_ORBIT) {
                    let f = 0.5;
                    if (xDelta !== 0) {
                        this.camera.orbitYaw(-xDelta * this.mouseOrbitSensitivity * f);
                    }
                    if (yDelta !== 0) {
                        this.camera.orbitPitch(yDelta * this.mouseOrbitSensitivity * f);
                    }
                    this.camera.orbitting = true;
                } else if (this.dragMode == DRAG_PAN) {
                    // tfk: using the elapsed time didn't seem to make navigation smoother.

                    // let now = +new Date();
                    // let elapsed = now - this.lastPan;
                    // elapsed /= 20.;

                    let dist = this.getEyeLookDist();
                    if (window.TouchEvent && e instanceof TouchEvent && e.touches.length == 2) {
                        let factor = Math.pow(this.pinchDistance / this.lastPinchDistance, 0.5);
                        this.camera.zoom(dist - dist * factor, this.mousePos);
                        this.lastPinchDistance = this.pinchDistance;
                    }
                    var f = dist / 600;
                    // f *= elapsed;
                    this.camera.pan([xDelta * f, yDelta * f, 0.0]);
                    // this.lastPan = now;
                }
            }
        }
        e.preventDefault();
    }

    canvasPanEnabled(_enabled){
        if(_enabled){
            this.dragMode = DRAG_PAN; 
        } else {
            this.dragMode = DRAG_ORBIT;
        }
    }


    /**
     * @private
     */
    documentMouseUp(e) {
        this.mouseDown = false;
        // Potential end-of-pan
        if (this.dragMode == DRAG_PAN) {
            this.camera.updateLowVolumeListeners();
        }
        this.dragMode = DRAG_ORBIT;
    }

    getEyeLookDist() {
        let d = this.viewer.lastRecordedDepth;
        if (!this.mouseDown && !this.keysDown.size && ((+new Date()) - this.viewer.recordedDepthAt) > 500) {
            // Reread depth at mouse coordinates for sensitivity measures
            this.viewer.pick({ canvasPos: this.mousePos, select: false });
        }
        if (d === null) {
            return this.getZoomRate() * 20.;
        } else {
            // Always add a bit so that we can zoom past a window
            return d + this.getZoomRate();
        }
    }

    /**
     * @private
     */
    cleanup() {
        var canvas = this.canvas;
        document.removeEventListener("mouseup", this.documentMouseUpHandler);
        canvas.removeEventListener("mousedown", this.canvasMouseDownHandler);
        canvas.removeEventListener("mouseup", this.canvasMouseUpHandler);
        document.removeEventListener("mouseup", this.documentMouseUpHandler);
        canvas.removeEventListener("keyup", this.canvasKeyUpHandler);
        canvas.removeEventListener("keydown", this.canvastKeyDownHandler);
        canvas.removeEventListener("mouseenter", this.canvasMouseEnterHandler);
        canvas.removeEventListener("mouseleave", this.canvasMouseLeaveHandler);
        canvas.removeEventListener("mousemove", this.canvasMouseMoveHandler);
        canvas.removeEventListener("wheel", this.canvasMouseWheelHandler);
    }
}