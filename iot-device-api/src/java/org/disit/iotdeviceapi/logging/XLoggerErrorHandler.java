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

import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class XLoggerErrorHandler implements ErrorHandler {
    
    private final PrintWriter out;

        XLoggerErrorHandler(PrintWriter out) {
            this.out = out;
        }

        private String getParseExceptionInfo(SAXParseException spe) {
            String systemId = spe.getSystemId();
            if (systemId == null) {
                systemId = "null";
            }

            String info = "URI=" + systemId + " Line=" + spe.getLineNumber() +
                          ": " + spe.getMessage();
            return info;
        }

        @Override
        public void warning(SAXParseException spe) throws SAXException {
            Logger.getLogger(XLoggerErrorHandler.class.getName()).log(Level.WARNING, null, getParseExceptionInfo(spe));
        }

        @Override
        public void error(SAXParseException spe) throws SAXException {
            Logger.getLogger(XLoggerErrorHandler.class.getName()).log(Level.SEVERE, null, getParseExceptionInfo(spe));
            throw new SAXException("Error: " + getParseExceptionInfo(spe));
        }

        @Override
        public void fatalError(SAXParseException spe) throws SAXException {
            Logger.getLogger(XLoggerErrorHandler.class.getName()).log(Level.SEVERE, null, getParseExceptionInfo(spe));
            throw new SAXException("Fatal Error: " + getParseExceptionInfo(spe));
        }
        
}
