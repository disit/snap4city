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
package org.disit.iotdeviceapi.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.disit.iotdeviceapi.Insert;
import org.disit.iotdeviceapi.dataquality.Validator;
import org.disit.iotdeviceapi.repos.Repos;
import org.disit.iotdeviceapi.loaders.Loader;
import org.disit.iotdeviceapi.providers.Provider;
import org.disit.iotdeviceapi.utils.Const;
import org.disit.iotdeviceapi.utils.Formatting;
import org.disit.iotdeviceapi.logging.XLogger;
import org.disit.iotdeviceapi.utils.IotDeviceApiException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class Parser {
    
    private String cfgFilePath;
    private Element cfgRoot;
    private HashMap<String, Repos> repos;
    private HashMap<String, Class<?>> datatypes;
    private HashMap<String,Provider> providers;
    private HashMap<String,Constructor> builders;
    private HashMap<String, Loader> loaders;
    private NodeList dataNodes;
    private int status;
    private String loggingFolder;
    private HashMap<String, Level> loggingLevels;
    private XLogger xlogger;
    private HashMap<String, Constructor> validators;
    private HashMap<String, ArrayList<Validator>> validations;
    HttpServletRequest request;
    Repos requestRepos;
    
    public Parser() {
        
        this.cfgFilePath = null;
        this.cfgRoot = null;
        this.repos = null;
        this.datatypes = null;
        this.providers = null;
        this.builders = null;
        this.loaders = null;
        this.dataNodes = null;
        this.loggingFolder = null;
        this.loggingLevels = null;
        this.xlogger = null;
        this.status = Const.OK;
        this.validators = null;
        this.validations = null;
        this.request = null;
        this.requestRepos = null;
        
    }
    
    public Parser(String cfgFilePath, HttpServletRequest request) {
        
        this.status = Const.OK;
        this.cfgRoot = null;
        this.repos = null;
        this.datatypes = null;
        this.providers = null;
        this.builders = null;
        this.loaders = null;
        this.dataNodes = null;
        this.loggingFolder = null;
        this.loggingLevels = null;
        this.xlogger = null;
        this.validators = null;
        this.validations = null;
        this.requestRepos = null;
        
        this.request = request;
        parseRequest();
        
        if(Const.OK == this.status) {
            this.cfgFilePath = cfgFilePath;
            if(!this.cfgFilePath.startsWith("/")) this.cfgFilePath = System.getProperty("user.home")+(!System.getProperty("user.home").endsWith("/")?"/":"")+this.cfgFilePath;
            parseCfgRoot();
        }
        
    }
    
    public void parse() {
        if(Const.OK == getStatus()) parseRepos();
        if(Const.OK == getStatus()) parseDatatypes();
        if(Const.OK == getStatus()) parseProviders();
        if(Const.OK == getStatus()) parseBuilders();
        if(Const.OK == getStatus()) parseLoaders();
        if(Const.OK == getStatus()) parseValidators();
        if(Const.OK == getStatus()) parseValidations();
        if(Const.OK == getStatus()) parseDataNodes();
    }
        
    private void parseCfgRoot() {
        try {    
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder(); 
            class MyEntityResolver implements EntityResolver {
                String cfgFile;
                public MyEntityResolver(String cfgFile) {
                    super();
                    this.cfgFile = cfgFile;
                }

                @Override
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    String entityFilename = systemId.split("/")[systemId.split("/").length-1];     
                    String cfgFolder = this.cfgFile.substring(0, 1+this.cfgFile.lastIndexOf("/"));
                    try {                        
                        File entityFile = new File(cfgFolder+entityFilename);
                        if(entityFile.exists()) {
                            InputStream inputStream = new FileInputStream(entityFile);
                            Reader reader = new InputStreamReader(inputStream,"UTF-8");
                            return new InputSource(reader);
                        }
                        else {
                            Logger.getLogger(Parser.class.getName()).log(Level.WARNING, "File not found: \"{0}{1}\", using: \"/WEB-INF/{2}\"", new Object[]{cfgFolder, entityFilename, entityFilename});
                            return new InputSource(request.getServletContext().getResourceAsStream("/WEB-INF/"+entityFilename));
                        }
                    }
                    catch(Exception e) {
                        Logger.getLogger(Parser.class.getName()).log(Level.WARNING, "Error reading from: \"{0}{1}\", using: \"/WEB-INF/{2}\"", new Object[]{cfgFolder, entityFilename, entityFilename});
                        return new InputSource(request.getServletContext().getResourceAsStream("/WEB-INF/"+entityFilename));
                    }
                }
            }
            db.setEntityResolver(new MyEntityResolver(this.cfgFilePath));
            OutputStreamWriter errorWriter = new OutputStreamWriter(System.err, "UTF-8");
            db.setErrorHandler(new ParserErrorHandler(new PrintWriter(errorWriter, true)));
            File cfgFile = new File(this.cfgFilePath);
            Document cfg;
            if(cfgFile.exists()) {
                cfg = db.parse(cfgFile);
            }
            else {
                InputStream cfgStream = request.getServletContext().getResourceAsStream("/WEB-INF"+this.cfgFilePath.substring(this.cfgFilePath.lastIndexOf("/")));
                cfg = db.parse(new InputSource(cfgStream));                
            }
            this.cfgRoot = cfg.getDocumentElement();
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, e);
            this.cfgRoot = null;
        }
    }
    
    public void parseLogCfg() {
        try {
            this.loggingFolder = System.getProperty("user.home");
            this.loggingLevels = new HashMap<>();
            NodeList admin = this.cfgRoot.getElementsByTagName(ParserConst.CFG_EL_ADMIN);
            if(admin.getLength() == 1) {
                NodeList xlogs = ((Element)admin.item(0)).getElementsByTagName(ParserConst.CFG_EL_REQUESTS_LOGGING);
                if(xlogs.getLength() == 1) {
                    Element xlogsElement = (Element)xlogs.item(0);
                    if(xlogsElement.hasAttribute(ParserConst.CFG_AT_LOGGING_FOLDER)) {
                        this.loggingFolder = xlogsElement.getAttribute(ParserConst.CFG_AT_LOGGING_FOLDER);
                    }
                    NodeList xlogLevel = xlogsElement.getElementsByTagName(ParserConst.CFG_EL_XLOG_LEVEL);
                    for(int i = 0; i < xlogLevel.getLength(); i++) {
                        String xlogClass = ((Element)xlogLevel.item(i)).getAttribute(ParserConst.CFG_AT_XLOG_CLASS);
                        String xlogLevelStr = ((Element)xlogLevel.item(i)).getAttribute(ParserConst.CFG_AT_XLOG_LEVEL);
                        try {
                            this.loggingLevels.put(xlogClass, Level.parse(xlogLevelStr));
                        }
                        catch(IllegalArgumentException iae) {
                            this.loggingLevels.put(xlogClass, Level.OFF);
                            Logger.getLogger(Parser.class.getName()).log(Level.WARNING, MessageFormat.format("Illegal value \"{0}\" for logging level of {1}. Set to OFF.",new Object[]{xlogLevelStr, xlogClass}));
                        }
                    }
                }
            }
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, e);
            this.loggingFolder = null;
        }
    }
    
    private void parseRepos() {
        try {
            Element datasourcesRoot = (Element)this.cfgRoot.getElementsByTagName(ParserConst.CFG_EL_DATASOURCES).item(0);
            this.repos = new HashMap<>();
            this.repos.put(ParserConst.REQUEST, requestRepos);
            this.repos.put(ParserConst.VOLATILE_REPOS, new Repos(ParserConst.VOLATILE_REPOS));
            for(int i = 0; i < datasourcesRoot.getElementsByTagName(ParserConst.CFG_EL_DATASOURCE).getLength(); i++) {
                String datasourceID = ((Element)datasourcesRoot.getElementsByTagName(ParserConst.CFG_EL_DATASOURCE).item(i)).getAttribute(ParserConst.CFG_AT_DATASOURCE_ID);
                NodeList datasourceParameters = ((Element)datasourcesRoot.getElementsByTagName(ParserConst.CFG_EL_DATASOURCE).item(i)).getElementsByTagName(ParserConst.CFG_EL_DATASOURCE_PARAM);
                Repos datasource = new Repos(datasourceID);
                for(int j = 0; j < datasourceParameters.getLength(); j++) {
                    String key = ((Element)datasourceParameters.item(j)).getAttribute(ParserConst.CFG_AT_DATASOURCE_PARAM_KEY);
                    String value = ((Element)datasourceParameters.item(j)).getAttribute(ParserConst.CFG_AT_DATASOURCE_PARAM_VAL);
                    datasource.setParameter(key, value);
                }
                this.repos.put(datasourceID, datasource);
            }
            getXlogger().log(Parser.class.getName(), Level.CONFIG, "datasources config", datasourcesRoot);
        } 
        catch(Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(Parser.class.getName(), Level.SEVERE, "parse datasources error", e);
            this.repos = null;
        }
    }
    
    private void parseDatatypes() {
        try {
            this.datatypes = new HashMap<>();
            Element datatypesRoot = (Element)this.cfgRoot.getElementsByTagName(ParserConst.CFG_EL_DATATYPES).item(0);
            for(int i = 0; i < datatypesRoot.getElementsByTagName(ParserConst.CFG_EL_DATATYPE).getLength(); i++) {
                String datatypeID = ((Element)datatypesRoot.getElementsByTagName(ParserConst.CFG_EL_DATATYPE).item(i)).getAttribute(ParserConst.CFG_AT_DATATYPE_ID);
                String datatypeClassName = ((Element)datatypesRoot.getElementsByTagName(ParserConst.CFG_EL_DATATYPE).item(i)).getAttribute(ParserConst.CFG_AT_DATATYPE_CLASS);
                Class<?> builderClass = Class.forName(datatypeClassName);
                this.datatypes.put(datatypeID, builderClass); 
            }
            getXlogger().log(Parser.class.getName(), Level.CONFIG, "datatypes config", datatypesRoot);
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(Parser.class.getName(), Level.SEVERE, "parse datatypes error", e);
            this.datatypes = null;
        }
    }
    
    private void parseProviders() {
        try {
            Element providersRoot = (Element)this.cfgRoot.getElementsByTagName(ParserConst.CFG_EL_PROVIDERS).item(0);
            this.providers = new HashMap<>();
            for(int i = 0; i < providersRoot.getElementsByTagName(ParserConst.CFG_EL_PROVIDER).getLength(); i++) {
                String providerID = ((Element)providersRoot.getElementsByTagName(ParserConst.CFG_EL_PROVIDER).item(i)).getAttribute(ParserConst.CFG_AT_PROVIDER_ID);
                String providerClassName = ((Element)providersRoot.getElementsByTagName(ParserConst.CFG_EL_PROVIDER).item(i)).getAttribute(ParserConst.CFG_AT_PROVIDER_CLASS);
                String providerDatasource = ((Element)providersRoot.getElementsByTagName(ParserConst.CFG_EL_PROVIDER).item(i)).getAttribute(ParserConst.CFG_AT_PROVIDER_DS);
                Class<?> providerClass = Class.forName(providerClassName);
                Constructor<?> constructor = providerClass.getConstructor(Repos.class);
                Provider provider = (Provider)constructor.newInstance(this.repos.get(providerDatasource));
                provider.setXlogger(getXlogger());
                this.providers.put(providerID, provider);
                getXlogger().log(Parser.class.getName(), Level.CONFIG, "providers config", providersRoot);
            }
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(Parser.class.getName(), Level.SEVERE, "parse providers error", e);
            this.providers = null;
        }
    }
    
    private void parseBuilders() {
        try {
            Element buildersRoot = (Element)this.cfgRoot.getElementsByTagName(ParserConst.CFG_EL_BUILDERS).item(0);
            this.builders = new HashMap<>();
            for(int i = 0; i < buildersRoot.getElementsByTagName(ParserConst.CFG_EL_BUILDER).getLength(); i++) {
                String builderID = ((Element)buildersRoot.getElementsByTagName(ParserConst.CFG_EL_BUILDER).item(i)).getAttribute(ParserConst.CFG_AT_BUILDER_ID);
                String builderClassName = ((Element)buildersRoot.getElementsByTagName(ParserConst.CFG_EL_BUILDER).item(i)).getAttribute(ParserConst.CFG_AT_BUILDER_CLASS);
                Class<?> builderClass = Class.forName(builderClassName);
                Constructor<?> constructor = builderClass.getConstructor(Element.class, HashMap.class, HashMap.class, HashMap.class, HashMap.class, XLogger.class);
                this.builders.put(builderID, constructor);
            }
            getXlogger().log(Parser.class.getName(), Level.CONFIG, "builders config", buildersRoot);
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(Parser.class.getName(), Level.SEVERE, "parse builders error", e);
            this.builders = null;
        }
    }
    
    private void parseLoaders() {
        try {    
            this.loaders = new HashMap<>();
            Element loadersRoot = (Element)this.cfgRoot.getElementsByTagName(ParserConst.CFG_EL_LOADERS).item(0);
            NodeList loadersNodes = loadersRoot.getElementsByTagName(ParserConst.CFG_EL_LOADER);
            for(int i = 0; i < loadersNodes.getLength(); i++) {
                String loaderID = ((Element)loadersNodes.item(i)).getAttribute(ParserConst.CFG_AT_LOADER_ID);
                Class<?> loaderClass = Class.forName(((Element)loadersNodes.item(i)).getAttribute(ParserConst.CFG_AT_LOADER_CLASS));
                Constructor<?> constructor = loaderClass.getConstructor();
                Loader loader = (Loader)constructor.newInstance();
                loader.setXlogger(getXlogger());
                String loaderDatasource = ((Element)loadersNodes.item(i)).getAttribute(ParserConst.CFG_AT_LOADER_DS);
                loader.connect(this.repos.get(loaderDatasource));
                if(Const.ERROR == loader.getStatus()) throw new Exception ("Loader connection failed.");
                Formatting formatting = new Formatting();
                Element formattingsElement = (Element)((Element)loadersNodes.item(i)).getElementsByTagName(ParserConst.CFG_EL_FORMATTINGS).item(0);
                for(int j = 0; j < formattingsElement.getElementsByTagName(ParserConst.CFG_EL_FORMATTING).getLength(); j++) {
                    Element formattingElement = (Element)formattingsElement.getElementsByTagName(ParserConst.CFG_EL_FORMATTING).item(j);
                    String cfgType = formattingElement.getAttribute(ParserConst.CFG_AT_FORMATTING_REF);
                    String ldrType = formattingElement.getTextContent();
                    formatting.put(cfgType, ldrType);
                }
                loader.setFormatting(formatting);
                this.loaders.put(loaderID, loader);
            }
            getXlogger().log(Parser.class.getName(), Level.CONFIG, "loaders config", loadersRoot );
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(Parser.class.getName(), Level.SEVERE, "parse loaders error", e);
            this.loaders = null;
        }
    }
    
    private void parseValidators() {
        try {
            Element dataQualityRoot = (Element)this.cfgRoot.getElementsByTagName(ParserConst.CFG_EL_DATA_QUALITY).item(0);
            if(dataQualityRoot != null) {
                Element validatorsRoot = (Element)dataQualityRoot.getElementsByTagName(ParserConst.CFG_EL_VALIDATORS).item(0);
                if(validatorsRoot != null) {
                    NodeList validatorsNodes = validatorsRoot.getElementsByTagName(ParserConst.CFG_EL_VALIDATOR);
                    for(int i = 0; i < validatorsNodes.getLength(); i++) {
                        String id = ((Element)validatorsNodes.item(i)).getAttribute(ParserConst.CFG_AT_VALIDATOR_ID);
                        String validatorClassName = ((Element)validatorsNodes.item(i)).getAttribute(ParserConst.CFG_AT_VALIDATOR_CLASS);
                        Class<?> validatorClass = Class.forName(validatorClassName);
                        Constructor<?> constructor = validatorClass.getConstructor(String.class, Element.class, XLogger.class);
                        if(validators == null) validators = new HashMap<>();
                        validators.put(id, constructor);
                    }
                }
            }
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(Parser.class.getName(), Level.SEVERE, "parse validatORS error", e);
            this.validators = null;
        }
    }
    
    private void parseValidations() {
        try {
            Element dataQualityRoot = (Element)this.cfgRoot.getElementsByTagName(ParserConst.CFG_EL_DATA_QUALITY).item(0);
            if(dataQualityRoot == null) return;
            Element validationsRoot = (Element)dataQualityRoot.getElementsByTagName(ParserConst.CFG_EL_VALIDATIONS).item(0);
            if(validationsRoot == null) return;
            NodeList validateNodes = validationsRoot.getElementsByTagName(ParserConst.CFG_EL_VALIDATE);
            for(int i = 0; i < validateNodes.getLength(); i++) {
                Element validateElement = (Element)validateNodes.item(i);
                String var = validateElement.getAttribute(ParserConst.CFG_AT_VALIDATE_REF);
                NodeList pickValidatorNodes = validateElement.getElementsByTagName(ParserConst.CFG_EL_PICK_VALIDATOR);
                for(int j = 0; j < pickValidatorNodes.getLength(); j++) {
                    Element pickedValidator = (Element)pickValidatorNodes.item(j);
                    String pickedValidatorID = pickedValidator.getAttribute(ParserConst.CFG_AT_PICK_VALIDATOR_REF);
                    Constructor pickedValidatorConstructor = getValidators().get(pickedValidatorID);
                    Validator validatorInstance = (Validator)pickedValidatorConstructor.newInstance(pickedValidatorID, pickedValidator, getXlogger());
                    if(validations == null) validations = new HashMap<>();
                    ArrayList<Validator> varValidators = validations.get(var);
                    if(varValidators == null) varValidators = new ArrayList<>();
                    varValidators.add(validatorInstance);
                    validations.put(var, varValidators);
                }
            }
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(Parser.class.getName(), Level.SEVERE, "parse validatIONS error", e);
            this.validations = null;
        }
    }

    private void parseDataNodes() {
        try {
            Element etlRoot = (Element)this.cfgRoot.getElementsByTagName(ParserConst.CFG_EL_PROCESS).item(0);
            this.dataNodes = etlRoot.getElementsByTagName(ParserConst.CFG_EL_DATA);
            getXlogger().log(Parser.class.getName(), Level.CONFIG, "data nodes config", MessageFormat.format("{0} data nodes found", new Object[]{this.dataNodes.getLength()}));
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(Parser.class.getName(), Level.SEVERE, "parse data nodes error", e);
            this.dataNodes = null;
        }
    }
    
    private void parseRequest() {
            
        try {
        String requestPayload = new String();
        try {
            StringBuilder jb = new StringBuilder();
            String line;
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                jb.append(line);
            }
            requestPayload = jb.toString();
        }
        catch(Exception e) {
            Logger.getLogger(Insert.class.getName()).log(Level.SEVERE, null, e);
            setStatus(Const.ERROR);
        }

        if(Const.ERROR == this.status) {
            throw new IotDeviceApiException("Error while reading request body. No Xlogger available yet. See preceeding errors for further details.");
        }

        requestRepos = new Repos(ParserConst.REQUEST);
        requestRepos.setParameter(ParserConst.REQUEST_RAW, requestPayload);
        for(String key: request.getParameterMap().keySet()) {
            if(request.getParameterMap().get(key).length == 1) {
                requestRepos.setParameter(key, request.getParameterMap().get(key)[0]);
            }
            else {
                for( int i = 0; i < request.getParameterMap().get(key).length; i++) {
                    requestRepos.setParameter(key+"["+String.valueOf(i)+"]", request.getParameterMap().get(key)[i]);
                }
            }
        }
        for(String headerName: Collections.list(request.getHeaderNames())) {
            requestRepos.setParameter(headerName, request.getHeader(headerName));
        }
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
        }
        
    }    
    
    public String getCfgFilePath() {
        return cfgFilePath;
    }

    public void setCfgFilePath(String cfgFilePath) {
        this.cfgFilePath = cfgFilePath;
    }

    public Element getCfgRoot() {
        return cfgRoot;
    }

    public HashMap<String, Repos> getRepos() {
        return repos;
    }

    public HashMap<String, Class<?>> getDatatypes() {
        return datatypes;
    }

    public HashMap<String, Provider> getProviders() {
        return providers;
    }

    public HashMap<String, Constructor> getBuilders() {
        return builders;
    }

    public HashMap<String, Loader> getLoaders() {
        return loaders;
    }

    public NodeList getDataNodes() {
        return dataNodes;
    }

    public int getStatus() {
        return status;
    }

    private void setStatus(int status) {
        this.status = status;
    }

    public String getLoggingFolder() {
        return loggingFolder;
    }

    public HashMap<String, Level> getLoggingLevels() {
        return loggingLevels;
    }

    public XLogger getXlogger() {
        return xlogger;
    }

    public void setXlogger(XLogger xlogger) {
        this.xlogger = xlogger;
    }

    public HashMap<String, Constructor> getValidators() {
        return validators;
    }

    public HashMap<String, ArrayList<Validator>> getValidations() {
        return validations;
    }
    
}