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

var express = require("express");
var util = require("util");
var path = require("path");
var fs = require("fs");
var clone = require("clone");

var defaultContext = {
    page: {
        title: "Node-RED",
        favicon: "favicon.ico",
        tabicon: {
            icon: "red/images/node-red-icon-black.svg",
            colour: "#8f0000"
        },
        version: require(path.join(__dirname,"../../package.json")).version
    },
    header: {
        title: "Node-RED",
        image: "red/images/node-red.svg"
    },
    asset: {
        red: "red/red.js",
        main: "red/main.js",
        vendorMonaco: ""
    }
};

var theme = null;
var themeContext = clone(defaultContext);
var themeSettings = null;

var activeTheme = null;
var activeThemeInitialised = false;

var runtimeAPI;
var themeApp;

function serveFile(app,baseUrl,file) {
    try {
        var stats = fs.statSync(file);
        var url = baseUrl+path.basename(file);
        //console.log(url,"->",file);
        app.get(url,function(req, res) {
            res.sendFile(file);
        });
        return "theme"+url;
    } catch(err) {
        //TODO: log filenotfound
        return null;
    }
}

function serveFilesFromTheme(themeValue, themeApp, directory, baseDirectory) {
    var result = [];
    if (themeValue) {
        var array = themeValue;
        if (!util.isArray(array)) {
            array = [array];
        }

        for (var i=0;i<array.length;i++) {
            let fullPath = array[i];
            if (baseDirectory) {
                fullPath = path.resolve(baseDirectory,array[i]);
                if (fullPath.indexOf(path.resolve(baseDirectory)) !== 0) {
                    continue;
                }
            }
            var url = serveFile(themeApp,directory,fullPath);
            if (url) {
                result.push(url);
            }
        }
    }
    return result
}

module.exports = {
    init: function(settings, _runtimeAPI) {
        runtimeAPI = _runtimeAPI;
        themeContext = clone(defaultContext);
        if (process.env.NODE_ENV == "development") {
            themeContext.asset.red = "red/red.js";
            themeContext.asset.main = "red/main.js";
        }
        themeSettings = null;
        theme = settings.editorTheme || {};
        themeContext.asset.vendorMonaco = ((theme.codeEditor || {}).lib === "monaco") ? "vendor/monaco/monaco-bootstrap.js" : "";
        activeTheme = theme.theme;
    },

    app: function() {
        var i;
        var url;
        themeSettings = {};

        themeApp = express();

        if (theme.page) {

            themeContext.page.css = serveFilesFromTheme(
                theme.page.css,
                themeApp,
                "/css/")
            themeContext.page.scripts = serveFilesFromTheme(
                theme.page.scripts,
                themeApp,
                "/scripts/")

            if (theme.page.favicon) {
                url = serveFile(themeApp,"/favicon/",theme.page.favicon)
                if (url) {
                    themeContext.page.favicon = url;
                }
            }

            if (theme.page.tabicon) {
                let icon = theme.page.tabicon.icon || theme.page.tabicon
                url = serveFile(themeApp,"/tabicon/", icon)
                if (url) {
                    themeContext.page.tabicon.icon = url;
                }
                if (theme.page.tabicon.colour) {
                    themeContext.page.tabicon.colour = theme.page.tabicon.colour
                }
            }

            themeContext.page.title = theme.page.title || themeContext.page.title;

            // Store the resolved urls to these resources so nodes (such as Debug)
            // can access them
            theme.page._ = {
                css: themeContext.page.css,
                scripts: themeContext.page.scripts,
                favicon: themeContext.page.favicon
            }
        }

        if (theme.header) {

            themeContext.header.title = theme.header.title || themeContext.header.title;

            if (theme.header.hasOwnProperty("url")) {
                themeContext.header.url = theme.header.url;
            }

            if (theme.header.hasOwnProperty("image")) {
                if (theme.header.image) {
                    url = serveFile(themeApp,"/header/",theme.header.image);
                    if (url) {
                        themeContext.header.image = url;
                    }
                } else {
                    themeContext.header.image = null;
                }
            }
        }

        if (theme.deployButton) {
            if (theme.deployButton.type == "simple") {
                themeSettings.deployButton = {
                    type: "simple"
                }
                if (theme.deployButton.label) {
                    themeSettings.deployButton.label = theme.deployButton.label;
                }
                if (theme.deployButton.icon) {
                    url = serveFile(themeApp,"/deploy/",theme.deployButton.icon);
                    if (url) {
                        themeSettings.deployButton.icon = url;
                    }
                }
            }
        }

        if (theme.hasOwnProperty("userMenu")) {
            themeSettings.userMenu = theme.userMenu;
        }

        if (theme.login) {
            if (theme.login.image) {
                url = serveFile(themeApp,"/login/",theme.login.image);
                if (url) {
                    themeContext.login = {
                        image: url
                    }
                }
            }
        }
        themeApp.get("/", async function(req,res) {
            const themePluginList = await runtimeAPI.plugins.getPluginsByType({type:"node-red-theme"});
            themeContext.themes = themePluginList.map(theme => theme.id);
            res.json(themeContext);
        })

        if (theme.hasOwnProperty("menu")) {
            themeSettings.menu = theme.menu;
        }

        if (theme.hasOwnProperty("palette")) {
            themeSettings.palette = theme.palette;
        }

        if (theme.hasOwnProperty("projects")) {
            themeSettings.projects = theme.projects;
        }

        if (theme.hasOwnProperty("keymap")) {
            themeSettings.keymap = theme.keymap;
        }

        if (theme.theme) {
            themeSettings.theme = theme.theme;
        }

        if (theme.hasOwnProperty("tours")) {
            themeSettings.tours = theme.tours;
        }

        return themeApp;
    },
    context: async function() {
        if (activeTheme && !activeThemeInitialised) {
            const themePlugin = await runtimeAPI.plugins.getPlugin({
                id:activeTheme
            });
            if (themePlugin) {
                if (themePlugin.css) {
                    const cssFiles = serveFilesFromTheme(
                        themePlugin.css,
                        themeApp,
                        "/css/",
                        themePlugin.path
                    );
                    themeContext.page.css = cssFiles.concat(themeContext.page.css || [])
                    theme.page = theme.page || {_:{}}
                    theme.page._.css = cssFiles.concat(theme.page._.css || [])
                }
                if (themePlugin.scripts) {
                    const scriptFiles = serveFilesFromTheme(
                        themePlugin.scripts,
                        themeApp,
                        "/scripts/",
                        themePlugin.path
                    )
                    themeContext.page.scripts = scriptFiles.concat(themeContext.page.scripts || [])
                    theme.page = theme.page || {_:{}}
                    theme.page._.scripts = scriptFiles.concat(theme.page._.scripts || [])
                }
                if(theme.codeEditor) {
                    theme.codeEditor.options = Object.assign({}, themePlugin.monacoOptions, theme.codeEditor.options);
                }
            }
            activeThemeInitialised = true;
        }
        return themeContext;
    },
    settings: function() {
        return themeSettings;
    },
    serveFile: function(baseUrl,file) {
        return serveFile(themeApp,baseUrl,file);
    }
}
