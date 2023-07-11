/**
 * Copyright JS Foundation and other contributors, http://js.foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

module.exports = function(RED) {
    "use strict";
    const Ajv = require('ajv');
    const ajv = new Ajv({allErrors: true});
    ajv.addMetaSchema(require('ajv/lib/refs/json-schema-draft-06.json'));

    function JSONNode(n) {
        RED.nodes.createNode(this,n);
        this.indent = n.pretty ? 4 : 0;
        this.action = n.action||"";
        this.property = n.property||"payload";
        this.schema = null;
        this.compiledSchema = null;

        var node = this;

        this.on("input", function(msg,send,done) {
            var validate = false;
            if (msg.schema) {
                // If input schema is different, re-compile it
                if (JSON.stringify(this.schema) != JSON.stringify(msg.schema)) {
                    try {
                        this.compiledSchema = ajv.compile(msg.schema);
                        this.schema = msg.schema;
                    } catch(e) {
                        this.schema = null;
                        this.compiledSchema = null;
                        done(RED._("json.errors.schema-error-compile"));
                        return;
                    }
                }
                validate = true;
            }
            var value = RED.util.getMessageProperty(msg,node.property);
            if (value !== undefined) {
                if (typeof value === "string" || Buffer.isBuffer(value)) {
                    // if (Buffer.isBuffer(value) && node.action !== "obj") {
                    //     node.warn(RED._("json.errors.dropped")); done();
                    // }
                    // else
                    if (node.action === "" || node.action === "obj") {
                        try {
                            RED.util.setMessageProperty(msg,node.property,JSON.parse(value));
                            if (validate) {
                                if (this.compiledSchema(msg[node.property])) {
                                    delete msg.schema;
                                    send(msg);
                                    done();
                                } else {
                                    msg.schemaError = this.compiledSchema.errors;
                                    done(`${RED._("json.errors.schema-error")}: ${ajv.errorsText(this.compiledSchema.errors)}`);
                                }
                            } else  {
                                send(msg);
                                done();
                            }
                        }
                        catch(e) { done(e.message); }
                    } else {
                        // If node.action is str and value is str
                        if (validate) {
                            if (this.compiledSchema(JSON.parse(msg[node.property]))) {
                                delete msg.schema;
                                send(msg);
                                done();
                            } else {
                                msg.schemaError = this.compiledSchema.errors;
                                done(`${RED._("json.errors.schema-error")}: ${ajv.errorsText(this.compiledSchema.errors)}`);
                            }
                        } else {
                            send(msg);
                            done();
                        }
                    }
                }
                else if ((typeof value === "object") || (typeof value === "boolean") || (typeof value === "number")) {
                    if (node.action === "" || node.action === "str") {
                        if (!Buffer.isBuffer(value)) {
                            try {
                                if (validate) {
                                    if (this.compiledSchema(value)) {
                                        RED.util.setMessageProperty(msg,node.property,JSON.stringify(value,null,node.indent));
                                        delete msg.schema;
                                        send(msg);
                                        done();
                                    } else {
                                        msg.schemaError = this.compiledSchema.errors;
                                        done(`${RED._("json.errors.schema-error")}: ${ajv.errorsText(this.compiledSchema.errors)}`);
                                    }
                                } else {
                                    RED.util.setMessageProperty(msg,node.property,JSON.stringify(value,null,node.indent));
                                    send(msg);
                                    done();
                                }
                            }
                            catch(e) { done(RED._("json.errors.dropped-error")); }
                        }
                        else { node.warn(RED._("json.errors.dropped-object")); done(); }
                    } else {
                        // If node.action is obj and value is object
                        if (validate) {
                            if (this.compiledSchema(value)) {
                                delete msg.schema;
                                send(msg);
                                done();
                            } else {
                                msg.schemaError = this.compiledSchema.errors;
                                done(`${RED._("json.errors.schema-error")}: ${ajv.errorsText(this.compiledSchema.errors)}`);
                            }
                        } else {
                            send(msg);
                            done();
                        }
                    }
                }
                else { node.warn(RED._("json.errors.dropped")); done(); }
            }
            else { send(msg); done(); } // If no property - just pass it on.
        });
    }
    RED.nodes.registerType("json",JSONNode);
}
