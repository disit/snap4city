<%-- 
   IOTDEVICEAPI
   Copyright (C) 2017 DISIT Lab http://www.disit.org - University of Florence

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.

   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <title>IoT Device API</title>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
    </head>
    <body style="font-family:sans-serif; font-size:large;">
        <h1>IoT Device API</h1>
        <h2>Insert</h2>
        <p>Briefly, the IoT Device API:</p>
        <ol><li>accepts in input a JSON representation of a device and its broker;</li><li>transforms the JSON in a set of N-Quads;</li><li>loads the N-Quads to a graph database;</li><li>returns the device URI in response.</li></ol>
        <p>The mapping is based on a XML configuration document (<a style="text-decoration:none;" href="./insert-api-cfg.xml" title="IoT Device API insert config">see current version</a>).</p>
        <p>If the input JSON is incomplete, the N-Quads based on the missing data are skipped.</p>
        <p>The API accepts HTTP POST requests to:</p>
        <p>https://servicemap.disit.org/WebAppGrafo/api/v1/iot/insert</p>
        <p>Before inserting, all subjects that are going to be inserted are cleared.</p>
        <p>As a result, the API is suitable for full updates too.</p>
        <p>If the execution completes successfully, the API returns a single line plain text response that consists of the service URI of the inserted device. Otherwise, it returns an empty body and a Warning header where the problem is described.</p>
        <h2>Delete</h2>
        <p>The IoT Device API also provides a delete interface.</p>
        <p>For the purpose, it accepts HTTP POST requests to:</p>
        <p>https://servicemap.disit.org/WebAppGrafo/api/v1/iot/delete</p>
        <p>The request body is expected to be a JSON that represents the device that has to be deleted, the same way as if it had to be inserted.</p>
        <p>All properties of all subjects that appear in the generated N-Quads are deleted. The generation is configurable, the same way as it can be configured for the input primitive. The current configuration always preserves the brokers, as they should be shared among several devices.</p>
        <p>If the execution completes successfully, the API returns a single line plain text with the URI of the device that has been deleted. Otherwise, an empty body is returned, and a Warning header is added where the description of the problem can be found.</p>
        <p>The current configuration document for the delete primitive can be found <a style="text-decoration:none;" href="./delete-api-cfg.xml" title="IoT Device API delete config">here</a>.</p>      
        <h2>Disable (deprecated, not anymore available)</h2>
        <p>The IoT Device API also provides a primitive for disabling specific attributes of a device.</p>
        <p>For the purpose, it accepts HTTP POST requests to:</p>
        <p>https://servicemap.disit.org/WebAppGrafo/api/v1/iot/disable</p>
        <p>The request body is expected to be a JSON object with a single property, named <span style="font-style: italic;">uri</span>, where the URI of the attribute that has to be disabled is expected to be found. </p>
        <p>The property http://www.disit.org/km4city/schema#disabled of the specified attribute is set to true.</p>
        <p>If the execution completes successfully, the API returns a single line plain text with the URI of the attribute that has been disabled. Otherwise, an empty body is returned, and a Warning header is added where the description of the problem can be found.</p>
        <p>The current configuration document for this primitive can be found <a style="text-decoration:none;" href="./disable-api-cfg.xml" title="IoT Device API disable config">here</a>.</p>      
        <h2>Enable (deprecated, not anymore available)</h2>
        <p>The IoT Device API also provides a primitive for (re)enabling specific attributes of a device.</p>
        <p>For the purpose, it accepts HTTP POST requests to:</p>
        <p>https://servicemap.disit.org/WebAppGrafo/api/v1/iot/enable</p>
        <p>The request body is expected to be a JSON object with a single property, named <span style="font-style: italic;">uri</span>, where the URI of the attribute that has to be enabled is expected to be found. </p>
        <p>The property http://www.disit.org/km4city/schema#disabled of the indicated attribute is set to false.</p>
        <p>If the execution completes successfully, the API returns a single line plain text with the URI of the attribute that has been enabled. Otherwise, an empty body is returned, and a Warning header is added where the description of the problem can be found.</p>
        <p>The current configuration document for this primitive can be found <a style="text-decoration:none;" href="./enable-api-cfg.xml" title="IoT Device API enable config">here</a>.</p>      
        <h2>Make Public (deprecated, not anymore available)</h2>
        <p>The IoT Device API also provides a primitive for marking a device as a <span style="font-style: italic;">public</span> device.</p>
        <p>For the purpose, it accepts HTTP POST requests to:</p>
        <p>https://servicemap.disit.org/WebAppGrafo/api/v1/iot/make-public</p>
        <p>The request body is expected to be a JSON object with a single property, named <span style="font-style: italic;">uri</span>, where the Service URI of the device that has to be marked as <span style="font-style: italic;">public</span> is expected to be found. </p>
        <p>The property http://www.disit.org/km4city/schema#ownership of the indicated device is set to <span style="font-style: italic;">public</span>.</p>
        <p>If the execution completes successfully, the API returns a single line plain text with the Service URI of the device that has been made <span style="font-style: italic;">public</span>. Otherwise, an empty body is returned, and a Warning header is added where the description of the problem can be found.</p>
        <p>The current configuration document for this primitive can be found <a style="text-decoration:none;" href="./make-public-api-cfg.xml" title="IoT Device API make-public config">here</a>.</p>      
        <h2>Make Private (deprecated, not anymore available)</h2>
        <p>The IoT Device API also provides a primitive for marking a device as a <span style="font-style: italic;">private</span> device.</p>
        <p>For the purpose, it accepts HTTP POST requests to:</p>
        <p>https://servicemap.disit.org/WebAppGrafo/api/v1/iot/make-private</p>
        <p>The request body is expected to be a JSON object with a single property, named <span style="font-style: italic;">uri</span>, where the Service URI of the device that has to be marked as <span style="font-style: italic;">private</span> is expected to be found. </p>
        <p>The property http://www.disit.org/km4city/schema#ownership of the indicated device is set to <span style="font-style: italic;">private</span>.</p>
        <p>If the execution completes successfully, the API returns a single line plain text with the Service URI of the device that has been made <span style="font-style: italic;">private</span>. Otherwise, an empty body is returned, and a Warning header is added where the description of the problem can be found.</p>
        <p>The current configuration document for this primitive can be found <a style="text-decoration:none;" href="./make-private-api-cfg.xml" title="IoT Device API make-private config">here</a>.</p>       
        <h2>Other APIs</h2>
        <p>Some new functionalities have been recently added as a part of the IoT Device API that allow updating the GPS position of devices, and managing with the defitions of the static attributes (devices metadata). Please refer to the Swagger documentation (see below) to learn more. </p>
        <h2>How to configure</h2>
        <p>Some details about the API behaviour including expected shaping of inputs, the API internal structuring, and about how to configure the API behavior through the editing of the configuration documents, including their organization and semantic, can be found <a href="./doc.pdf" title="How to configure">here</a>.</p>
        <h2>Formal Specification</h2>
        <p>An OpenAPI v3.0 specification is available for the IoT Device API (download <a href="./openapi.json" title="OpenAPI v3.0 IoT Device API Specification, JSON format">JSON</a>, <a href="./openapi.yaml" title="OpenAPI v3.0 IoT Device API Specification, YAML format">YAML</a>).</p>
        <p>The online Swagger <a href="https://editor.swagger.io/" title="Swagger Editor">editor</a> and <a href="https://swagger.io/specification/" title="OpenAPI Specification v3.0">documentation</a> have been leveraged for producing the API specification.</p>
        <p>The <a href="https://swagger.io/swagger-ui/" title="Swagger UI">Swagger UI</a> have been leveraged for generating the <a href="swagger/index.html" title="IoT Device API Online Documentation">OpenAPI v3.0.1 IoT Device API online documentation</a> of the API starting from its specification.</p>
    </body>
</html>