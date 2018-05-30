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
package org.disit.iotdeviceapi.logging;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.disit.iotdeviceapi.utils.Const;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class XLogger {
    
    private final String filename;
    private final long timestamp;
    int status;
    private HashMap<String, Level> config;
    
    public XLogger (String api, String folder) {
        this.timestamp = System.currentTimeMillis() / 1000L;
        this.filename = System.getProperty("user.home")+folder+"/"+Long.toString(this.timestamp)+"_"+UUID.randomUUID().toString()+".xml";
        status = Const.OK;
        this.config = new HashMap<>();
    }
    
    public void log(String origin, Level level, String label, Object content) {
        try {		
            Level minLevel = Level.OFF;
            if(config.containsKey(origin)) minLevel = config.get(origin);
            if(minLevel.intValue() <= level.intValue()) {
                String messageStr;
                if(content instanceof Throwable) {
                    Logger.getLogger(origin).log(Level.SEVERE, null, (Throwable)content);
                    messageStr = ((Throwable) content).getMessage();
                    if(messageStr == null) messageStr = "null";
                }
                else if(content instanceof Element) {
                    messageStr = xmlElemToStr((Element)content);
                }
                else {
                    Logger.getLogger(origin).log(level, label.concat(": ").concat(content.toString()));
                    messageStr = content.toString();
                }
                File logFile = new File(this.filename);
                Document doc;
                Element rootElement;
                if(!logFile.exists()) {
                    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                    doc = docBuilder.newDocument();
                    rootElement = doc.createElement("request-log");
                    doc.appendChild(rootElement);
                }
                else {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder(); 
                    OutputStreamWriter errorWriter = new OutputStreamWriter(System.err, "UTF-8");
                    db.setErrorHandler(new XLoggerErrorHandler(new PrintWriter(errorWriter, true)));
                    doc = db.parse(new File(this.filename));
                    rootElement = doc.getDocumentElement();
                }
                Element logElement = doc.createElement("log");
                rootElement.appendChild(logElement);
                Element datetime = doc.createElement("datetime");
                Date date = new Date(this.timestamp*1000L); 
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); 
                datetime.setTextContent(sdf.format(date));
                logElement.appendChild(datetime);    
                Element levelElement = doc.createElement("level");
                levelElement.setTextContent(level.getName());
                logElement.appendChild(levelElement);
                Element whereElement = doc.createElement("where");
                whereElement.setTextContent(origin);
                logElement.appendChild(whereElement);
                Element labelElement = doc.createElement("label");
                labelElement.setTextContent(label);
                logElement.appendChild(labelElement);            
                Element messageElement = doc.createElement("message");
                messageElement.setTextContent(messageStr);
                logElement.appendChild(messageElement);
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new File(this.filename));
                transformer.transform(source, result);
            }
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            Logger.getLogger(XLogger.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public int getStatus() {
        return status;
    }

    private void setStatus(int status) {
        this.status = status;
    }

    public HashMap<String, Level> getConfig() {
        return config;
    }

    public void setConfig(HashMap<String, Level> config) {
        this.config = config;
    }
    
    private String xmlElemToStr(Element node) {
        /*DOMImplementationLS lsImpl = (DOMImplementationLS)node.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
        LSSerializer lsSerializer = lsImpl.createLSSerializer();
        NodeList childNodes = node.getChildNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < childNodes.getLength(); i++) {
           sb.append(lsSerializer.writeToString(childNodes.item(i)));
        }
        return sb.toString(); */
                String s = "";
        NodeList childs = node.getChildNodes();
        for( int i = 0;i<childs.getLength();i++ ){
            s+= serializeNode(childs.item(i));
        }
        return s;
    }

    private String serializeNode(Node node){
        String s = "";
        if( node.getNodeName().equals("#comment")) return new String(); 
        if( node.getNodeName().equals("#text") ) return node.getTextContent();
        s+= "<" + node.getNodeName()+" ";
        NamedNodeMap attributes = node.getAttributes();
        if( attributes!= null ){
            for( int i = 0;i<attributes.getLength();i++ ){
                s+=attributes.item(i).getNodeName()+"=\""+attributes.item(i).getNodeValue()+"\" ";
            }
        }
        NodeList childs = node.getChildNodes();
        if( childs == null || childs.getLength() == 0 ){
            s+= "/>";
            return s;
        }
        s+=">";
        for( int i = 0;i<childs.getLength();i++ )
            s+=serializeNode(childs.item(i));
        s+= "</"+node.getNodeName()+">";
        return s;
    }
    
}
