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

// update the following urls hostnames and protocols
export const bimserverURL = "http://localhost/bimserver/";
const serverApi = 'http://localhost/api/';

export const projectEndpoint = serverApi + "project/";
export const pinEndpoint = serverApi + "pin/";
export const serviceuriEndpoint = serverApi + "serviceuri/";

// CORS security ??
export const serviceUriBase = "http://localhost/superservicemap/api/v1/?serviceUri=";
export const serviceUriFormat = "&format=json&fullCount=false";
export const superserviceMapUrl = "http://localhost/superservicemap/api/v1/?serviceUri=";
export const iconServerLocation = "http://localhost/ServiceMap/img/mapicons/";

export const viewerConfig = {
    quantizeNormals: true,
    quantizeVertices: true,
    quantizeColors: true,
    gpuReuse: true,
    useSmallIndicesIfPossible: true,
    defaultLayerEnabled: true,
    triangleThresholdDefaultLayer: 10000000,
    tilingLayerEnabled: false,
    maxOctreeDepth: 3,
    loaderSettings: {
        quantizeNormals: true,
        octEncodeNormals: false,
        quantizeVertices: true,
        quantizeColors: true,
        useObjectColors: false,
        prepareBuffers: true,
        generateLineRenders: true,
        tilingLayerReuse: false,
    },
    realtimeSettings: {
        drawTileBorders: true,
        drawLineRenders: false,
        orderIndependentTransparency: false
    }

};
