/* IOTDEVICEAPI
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package org.disit.iotdeviceapi;

import org.disit.iotdeviceapi.builders.Builder;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import javax.activation.MimeType;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.disit.iotdeviceapi.parser.ParserConst;
import org.disit.iotdeviceapi.datatypes.Data;
import org.disit.iotdeviceapi.repos.Repos;
import org.disit.iotdeviceapi.loaders.Loader;
import org.disit.iotdeviceapi.providers.Provider;
import org.disit.iotdeviceapi.utils.Const;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.disit.iotdeviceapi.parser.Parser;
import org.disit.iotdeviceapi.dataquality.Validator;
import org.disit.iotdeviceapi.utils.IotDeviceApiException;
import org.disit.iotdeviceapi.logging.XLogger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
@WebServlet(name = "Enable", urlPatterns = {"/enable"})
public class Enable extends HttpServlet {

    HashMap<String, Loader> mLoaders; 
    int status;
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Warning", "Should send your requests via HTTP POST.");
           
    }
    
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        processRequest(request, response);
        
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        XLogger xlogger = null;
        Parser cfgParser;
        setStatus(Const.OK);
        
        try {

            // Parsing config file & initializing Xlogger
            
            String cfgFile = getServletConfig().getInitParameter(ParserConst.WEBXML_INIPAR_CFGFILE);           
            cfgParser = new Parser(cfgFile, request);
            if(Const.ERROR == cfgParser.getStatus()) {
                throw new IotDeviceApiException("Error while initializing parser. No Xlog available for this request. See preceeding errors for further details.");
            }
                    
            cfgParser.parseLogCfg();
            if(Const.ERROR == cfgParser.getStatus()) {
                throw new Exception("Error while retrieving Xlog configuration. No Xlog available for this request. See preceeding errors for further details.");
            }
            
            xlogger = new XLogger(Enable.class.getName(),cfgParser.getLoggingFolder());
            if(Const.ERROR == xlogger.getStatus()) {
                throw new IotDeviceApiException("Error while initializing Xlog. No Xlog available for this request. See preceeding errors for further details.");
            }
            
            xlogger.setConfig(cfgParser.getLoggingLevels());  
            if(Const.ERROR == xlogger.getStatus()) {
                throw new IotDeviceApiException("Error while configuring Xlog. No Xlog available for this request. See preceeding errors for further details.");
            }
            
            cfgParser.setXlogger(xlogger);
            
            cfgParser.parse();
            
            if(Const.ERROR == cfgParser.getStatus()) {
                throw new IotDeviceApiException("Error while parsing configuration.");
            }

            // Retrieving parsing results
            
            HashMap<String, Repos> repos = cfgParser.getRepos();
            HashMap<String, Class<?>> datatypes = cfgParser.getDatatypes();
            HashMap<String,Provider> providers = cfgParser.getProviders();
            HashMap<String,Constructor> builders = cfgParser.getBuilders();
            HashMap<String, Loader> loaders = cfgParser.getLoaders();
            HashMap<String, ArrayList<Validator>> validations = cfgParser.getValidations();
            this.mLoaders = loaders; 
            
            xlogger.log(Enable.class.getName(), Level.INFO, "request", repos.get(ParserConst.REQUEST).getParameter(ParserConst.REQUEST_RAW));
            if(Const.ERROR == xlogger.getStatus()) {
                throw new IotDeviceApiException("Error while logging request payload.");
            }

            // Building data
            
            HashMap<String, Data> data = new HashMap<>();
            NodeList dataNodes = cfgParser.getDataNodes();
            
            for(int n = 0; n < dataNodes.getLength(); n++) {
                
                Element cfgNode = (Element)dataNodes.item(n);
                String builderID = cfgNode.getAttribute(ParserConst.CFG_AT_DATA_BUILDER);
                Constructor builderConstructor = builders.get(builderID);
                Builder builder = (Builder)builderConstructor.newInstance(cfgNode, repos, datatypes, providers, data, xlogger);
                Data builtData = builder.build();
                if(Const.ERROR == builder.getStatus()) {
                    throw new IotDeviceApiException(MessageFormat.format("Error while building data: {0}.",new Object[]{cfgNode.getAttribute(ParserConst.CFG_AT_DATA_ID)}));
                }
                
                if(validations != null) {
                    ArrayList<Validator> builtDataValidations = validations.get(builtData.getId());
                    if(builtDataValidations != null) {
                        for(Validator validation: builtDataValidations) {
                            builtData = validation.clean(builtData, data);
                            if(Const.ERROR == validation.getStatus()) {
                                throw new IotDeviceApiException(MessageFormat.format("Validation failed for data: {0}.",new Object[]{builtData.getId()}));
                            }
                        }
                    }
                }
                
                data.put(builtData.getId(), builtData);
                
                xlogger.log(Enable.class.getName(), Level.FINE, "successfull data built", MessageFormat.format("Successfull build of data: {0}.",new Object[]{builtData.getId()}));
                
                if(cfgNode.hasAttribute(ParserConst.CFG_AT_DATA_TRIGGER)) {
                    if (null == data.get(cfgNode.getAttribute(ParserConst.CFG_AT_DATA_TRIGGER)).getValue() || 
                            0 == data.get(cfgNode.getAttribute(ParserConst.CFG_AT_DATA_TRIGGER)).getValue().length) {
                        builtData.setTriggered(false);
                        xlogger.log(Enable.class.getName(), Level.FINE, "untriggered data", MessageFormat.format("Data found to be untriggered: {0}. It is expected not to be produced in output, nor to be persisted by loaders. It could be employed for cleaning purposes in full updates.",new Object[]{builtData.getId()}));
                    }
                }
                                
                if(cfgNode.hasAttribute(ParserConst.CFG_AT_OUTPUT_DATA) && builtData.isTriggered()) {
                    try {
                        Data outputCfgData = data.get(cfgNode.getAttribute(ParserConst.CFG_AT_OUTPUT_DATA));
                        JSONObject outputCfg = (JSONObject)JSONValue.parse(outputCfgData.getValue()[0].toString());
                        builtData.setOutput(outputCfg);
                    }
                    catch(Exception outputException) {
                        JSONObject outputCfg = new JSONObject();
                        outputCfg.put(ParserConst.CFG_AT_OUTPUT_TYPE, "text/plain;charset=UTF-8");
                        outputCfg.put(ParserConst.CFG_AT_OUTPUT_GLUE, "");
                        outputCfg.put(ParserConst.CFG_AT_OUTPUT_TRAIL, "");
                        outputCfg.put(ParserConst.CFG_AT_OUTPUT_TAIL, "");
                        builtData.setOutput(outputCfg);
                        xlogger.log(Enable.class.getName(), Level.WARNING, "Unable to load output configuration. Default configuration will be used.",outputException);
                    }                    
                }
                
                if(cfgNode.hasAttribute(ParserConst.CFG_AT_DATA_LOAD) && !ParserConst.CFG_RLDRID_VOLATILE.equals(cfgNode.getAttribute(ParserConst.CFG_AT_DATA_LOAD))) {
                    loaders.get(cfgNode.getAttribute(ParserConst.CFG_AT_DATA_LOAD)).load(builtData);
                    if(Const.ERROR == loaders.get(cfgNode.getAttribute(ParserConst.CFG_AT_DATA_LOAD)).getStatus()) {
                        throw new IotDeviceApiException(MessageFormat.format("Error while loading data: {0}.",new Object[]{builtData.getId()}));
                    }
                    else {
                        xlogger.log(Enable.class.getName(), Level.FINE, "successfull data load", MessageFormat.format("Data loaded successfully: {0}", new Object[]{builtData.getId()}));
                    }
                }
                
            }
            
            // Disconnecting loaders
            
            for(String loaderID: loaders.keySet()) {
                if(mLoaders.get(loaderID).isConnected()) {
                    loaders.get(loaderID).disconnect(getStatus());
                }
                if(Const.ERROR == loaders.get(loaderID).getStatus()) {
                    throw new IotDeviceApiException(MessageFormat.format("Error while disconnecting loader: {0}.", new Object[]{loaderID}));
                }
                else {
                    xlogger.log(Enable.class.getName(), Level.FINE, "loader disconnected", MessageFormat.format("Loader {0} disconnected.", new Object[]{loaderID}));
                }
            }
            
            // Outputting response
            
            try (PrintWriter out = response.getWriter()) {
                response.setHeader("Access-Control-Allow-Origin", "*");
                response.setContentType("text/plain;charset=UTF-8");
                String responseString = "DONE";
                for(String id:data.keySet()) {
                    if(data.get(id).getOutput() != null) {
                        String newResponseString = new String();
                        if(null != data.get(id).getValue()) {
                            for(Object obj: data.get(id).getValue()) {
                                if(null != obj) {
                                    String glue = "";
                                    try { 
                                        glue = data.get(id).getOutput().get(ParserConst.CFG_AT_OUTPUT_GLUE).toString(); 
                                    } catch(Exception glueExc) { 
                                        xlogger.log(ListStaticAttr.class.getName(), Level.WARNING, "Invalid or missing glue for output data. Empty string will be used.", data.get(id).getOutput());
                                    }
                                    newResponseString = newResponseString+glue+obj.toString();
                                    try { 
                                        response.setContentType(new MimeType(data.get(id).getOutput().get(ParserConst.CFG_AT_OUTPUT_TYPE).toString()).toString()); 
                                    } catch(Exception e) {
                                        xlogger.log(ListStaticAttr.class.getName(), Level.WARNING, "Invalid or missing Content-type for output data. text/plain will be used.", data.get(id).getOutput());
                                    }
                                }
                            }
                        }
                        if(!newResponseString.isEmpty()) {
                            String trail = "";
                            try { 
                                trail = data.get(id).getOutput().get(ParserConst.CFG_AT_OUTPUT_TRAIL).toString(); 
                            } catch(Exception trailExc) { 
                                xlogger.log(ListStaticAttr.class.getName(), Level.WARNING, "Invalid or missing trail for output data. Empty string will be used.", data.get(id).getOutput());
                            }
                            String tail = "";
                            try { 
                                tail = data.get(id).getOutput().get(ParserConst.CFG_AT_OUTPUT_TAIL).toString(); 
                            } catch(Exception tailExc) { 
                                xlogger.log(ListStaticAttr.class.getName(), Level.WARNING, "Invalid or missing tail for output data. Empty string will be used.", data.get(id).getOutput());
                            }
                            String glue = "";
                            try { 
                                glue = data.get(id).getOutput().get(ParserConst.CFG_AT_OUTPUT_GLUE).toString(); 
                            } catch(Exception glueExc) { 
                                xlogger.log(ListStaticAttr.class.getName(), Level.WARNING, "Invalid or missing glue for output data. Empty string will be used.", data.get(id).getOutput());
                            }
                            responseString = trail.concat(newResponseString.substring(glue.length())).concat(tail);                        
                        }
                    }
                }
                out.print(responseString);
                xlogger.log(Enable.class.getName(), Level.INFO, "response", responseString);
            }
            catch(Exception e) {
                setStatus(Const.ERROR);
            }
            if(Const.ERROR == this.status) {
                throw new IotDeviceApiException("Error while producing output.");
            }
            
        }
        catch (Exception e) { 

            setStatus(Const.ERROR);
            
            if(mLoaders != null) {
                mLoaders.keySet().forEach((loaderID) -> {
                    if(mLoaders.get(loaderID) != null && 
                            mLoaders.get(loaderID).isConnected()) {
                        mLoaders.get(loaderID).disconnect(getStatus());
                    }
                });
            }
            
            if(xlogger!=null) xlogger.log(Enable.class.getName(), Level.SEVERE, "insert error", e);

            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Warning", e.getMessage());
            
        }
 
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "This servlet builds and persists data based on a XML configuration.";
    }
    
    private void setStatus(int status) {
        this.status = status;
    }
    
    public int getStatus() {
        return status;
    }

}
