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
package org.disit.iotdeviceapi.dataquality.basic;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import org.disit.iotdeviceapi.dataquality.Validator;
import org.disit.iotdeviceapi.datatypes.Data;
import org.disit.iotdeviceapi.utils.IotDeviceApiException;
import org.disit.iotdeviceapi.logging.XLogger;
import org.disit.iotdeviceapi.utils.Const;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class BasicValidator extends Validator {

    public BasicValidator(String id, Element config, XLogger xlogger) {
        super(id, config, xlogger);
    }

    @Override
    public Data clean(Data currentData, HashMap<String, Data> builtData) {
        
        try {
            
            // And/Or
            String andOr = null;
            if(config.hasAttribute(BasicValidatorConst.CFG_AT_ANDOR)) {
                andOr =  config.getAttribute(BasicValidatorConst.CFG_AT_ANDOR);
            }
            if(config.hasAttribute(BasicValidatorConst.CFG_AT_ANDOR) && (!(andOr.equals(BasicValidatorConst.CFG_VL_AND) || andOr.equals(BasicValidatorConst.CFG_VL_OR)))) {
                String exMsg = MessageFormat.format("Invalid validation group logic operation: {0}. Expected AND or OR. Validating data: \"{1}\". ValidatOR: \"{2}\". ", 
                    new Object[]{andOr, currentData.getId(), this.id});
                throw new IotDeviceApiException(exMsg);
            }
            
            // Possible group level
            String groupLevel = null;
            if(config.hasAttribute(BasicValidatorConst.CFG_AT_GRP_LEVEL)) {
                groupLevel = config.getAttribute(BasicValidatorConst.CFG_AT_GRP_LEVEL);
            }
            
            // Possible group id
            String groupId = null;
            if(config.hasAttribute(BasicValidatorConst.CFG_AT_GRP_ID)) {
                groupId = config.getAttribute(BasicValidatorConst.CFG_AT_GRP_ID);
            }
            
            // Validation group cardinality
            
            int validationGroupCardinality = 0;
            int validationFailuresCounter = 0;
            
            // Retrieving cardinality
            
            int cardinality = 0;
            Object[] dataValues = currentData.getValue();
            if(dataValues != null) cardinality = dataValues.length;
            
            // Initializing valid value
            
            ArrayList<Object> validValues = new ArrayList<>(); 
            if(dataValues != null && dataValues.length > 0) {
                validValues = new ArrayList<>(Arrays.asList(dataValues));
            }
                
            // Checking against min cardinality constraints
            
            NodeList minCardinalityNodes = config.getElementsByTagName(BasicValidatorConst.CFG_EL_MIN_CARDINALITY);
            validationGroupCardinality += minCardinalityNodes.getLength();
            for(int i = 0; i < minCardinalityNodes.getLength(); i++) {
                Element minCardinalityElement = (Element)minCardinalityNodes.item(i);
                String validationID = minCardinalityElement.getAttribute(BasicValidatorConst.CFG_AT_MIN_CARDINALITY_ID);
                String validationLevel = minCardinalityElement.getAttribute(BasicValidatorConst.CFG_AT_MIN_CARDINALITY_LEVEL);
                boolean validLevel = true;
                try {
                    Level.parse(validationLevel);
                }
                catch(Exception el) {
                    validLevel = false;
                }
                if(!validLevel) {
                    String exMsg = MessageFormat.format("Invalid data quality check severity level: \"{0}\". Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                        new Object[]{validationLevel, currentData.getId(), this.id, validationID});
                    throw new IotDeviceApiException(exMsg);
                }
                if(minCardinalityElement.hasAttribute(BasicValidatorConst.CFG_AT_MIN_CARDINALITY_REF)) {
                    String validationRef = minCardinalityElement.getAttribute(BasicValidatorConst.CFG_AT_MIN_CARDINALITY_REF);
                    Data minValueVarData = builtData.get(validationRef);
                    if(minValueVarData == null) {
                        String exMsg = MessageFormat.format("Data quality check failed: min cardinality ref data \"{0}\" could not be found. Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                new Object[]{validationRef, currentData.getId(), this.id, validationID});
                        throw new IotDeviceApiException(exMsg);
                    }
                    Object[] minValueVarDataValues = minValueVarData.getValue();
                    for(Object minValueVarDataValue: minValueVarDataValues) {
                        boolean validLimit = true;
                        try {
                            Integer.parseInt(minValueVarDataValue.toString());
                        }
                        catch(Exception ie) {
                            validLimit = false;
                        }
                        if(!validLimit) {
                            String exMsg = MessageFormat.format("Invalid min cardinality constraint: \"{0}\". Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                new Object[]{minValueVarDataValue.toString(), currentData.getId(), this.id, validationID});
                            throw new IotDeviceApiException(exMsg);
                        }
                        if(cardinality < Integer.parseInt(minValueVarDataValue.toString())) {
                            if(andOr.equals(BasicValidatorConst.CFG_VL_OR)) {
                                validationFailuresCounter++;
                                String msg = MessageFormat.format("One of the data quality checks in an OR validation group failed: min cardinality constraint value \"{0}\" found in ref data \"{1}\" that is greater than the detected cardinality of data \"{2}\" that is equal to \"{3}\". Validating data: \"{4}\". ValidatOR: \"{5}\". ValidatION: \"{6}\".", 
                                    new Object[]{Integer.parseInt(minValueVarDataValue.toString()), validationRef, currentData.getId(), String.valueOf(cardinality), currentData.getId(), this.id, validationID});
                                xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                            }
                            else if(Level.SEVERE == Level.parse(validationLevel)) {
                                String exMsg = MessageFormat.format("Data quality check failed: min cardinality constraint value \"{0}\" found in ref data \"{1}\" that is greater than the detected cardinality of data \"{2}\" that is equal to \"{3}\". Validating data: \"{4}\". ValidatOR: \"{5}\". ValidatION: \"{6}\".", 
                                    new Object[]{Integer.parseInt(minValueVarDataValue.toString()), validationRef, currentData.getId(), String.valueOf(cardinality), currentData.getId(), this.id, validationID});
                                throw new IotDeviceApiException(exMsg);
                            }
                            else {
                                String msg = MessageFormat.format("Data quality check failed: min cardinality constraint value \"{0}\" found in ref data \"{1}\" that is greater than the detected cardinality of data \"{2}\" that is equal to \"{3}\". Validating data: \"{4}\". ValidatOR: \"{5}\". ValidatION: \"{6}\".", 
                                    new Object[]{Integer.parseInt(minValueVarDataValue.toString()), validationRef, currentData.getId(), String.valueOf(cardinality), currentData.getId(), this.id, validationID});
                                xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                            }
                        }
                    }
                }
                else {
                    String minCardinalityStr = minCardinalityElement.getTextContent();
                    boolean validLimit = true;
                    try {
                        Integer.parseInt(minCardinalityStr);
                    }
                    catch(Exception ie) {
                        validLimit = false;
                    }
                    if(!validLimit) {
                        String exMsg = MessageFormat.format("Invalid min cardinality constraint: \"{0}\". Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                            new Object[]{minCardinalityStr, currentData.getId(), this.id, validationID});
                        throw new IotDeviceApiException(exMsg);
                    }
                    int minCardinality = Integer.parseInt(minCardinalityStr);
                    if(cardinality < minCardinality) {
                        if(andOr.equals(BasicValidatorConst.CFG_VL_OR)) {
                            validationFailuresCounter++;
                            String msg = MessageFormat.format("One of the data quality checks in an OR validation group failed: min cardinality constraint value \"{0}\" greater than the detected cardinality of data \"{1}\" that is equal to \"{2}\". Validating data: \"{3}\". ValidatOR: \"{4}\". ValidatION: \"{5}\".", 
                                new Object[]{String.valueOf(minCardinality), currentData.getId(), String.valueOf(cardinality), currentData.getId(), this.id, validationID});
                            xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                        }
                        else if(Level.SEVERE == Level.parse(validationLevel)) {
                            String exMsg = MessageFormat.format("Data quality check failed: min cardinality constraint value \"{0}\" greater than the detected cardinality of data \"{1}\" that is equal to \"{2}\". Validating data: \"{3}\". ValidatOR: \"{4}\". ValidatION: \"{5}\".", 
                                new Object[]{String.valueOf(minCardinality), currentData.getId(), String.valueOf(cardinality), currentData.getId(), this.id, validationID});
                            throw new IotDeviceApiException(exMsg);
                        }
                        else {
                            String msg = MessageFormat.format("Data quality check failed: min cardinality constraint value \"{0}\" greater than the detected cardinality of data \"{1}\" that is equal to \"{2}\". Validating data: \"{3}\". ValidatOR: \"{4}\". ValidatION: \"{5}\".", 
                                new Object[]{String.valueOf(minCardinality), currentData.getId(), String.valueOf(cardinality), currentData.getId(), this.id, validationID});
                            xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                        }
                    }
                }
            }
            
            // Checking against max cardinality constraints
            
            NodeList maxCardinalityNodes = config.getElementsByTagName(BasicValidatorConst.CFG_EL_MAX_CARDINALITY);
            validationGroupCardinality += maxCardinalityNodes.getLength();
            for(int i = 0; i < maxCardinalityNodes.getLength(); i++) {
                Element maxCardinalityElement = (Element)maxCardinalityNodes.item(i);
                String validationID = maxCardinalityElement.getAttribute(BasicValidatorConst.CFG_AT_MAX_CARDINALITY_ID);
                String validationLevel = maxCardinalityElement.getAttribute(BasicValidatorConst.CFG_AT_MAX_CARDINALITY_LEVEL);
                boolean validLevel = true;
                try {
                    Level.parse(validationLevel);
                }
                catch(Exception el) {
                    validLevel = false;
                }
                if(!validLevel) {
                    String exMsg = MessageFormat.format("Invalid data quality check severity level: {0}. Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                        new Object[]{validationLevel, currentData.getId(), this.id, validationID});
                    throw new IotDeviceApiException(exMsg);
                }
                if(maxCardinalityElement.hasAttribute(BasicValidatorConst.CFG_AT_MAX_CARDINALITY_REF)) {
                    String validationRef = maxCardinalityElement.getAttribute(BasicValidatorConst.CFG_AT_MAX_CARDINALITY_REF);
                    Data maxValueVarData = builtData.get(validationRef);
                    if(maxValueVarData == null) {
                        String exMsg = MessageFormat.format("Data quality check failed: max cardinality ref data \"{0}\" could not be found. Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                new Object[]{validationRef, currentData.getId(), this.id, validationID});
                        throw new IotDeviceApiException(exMsg);
                    }
                    Object[] maxValueVarDataValues = maxValueVarData.getValue();
                    for(Object maxValueVarDataValue: maxValueVarDataValues) {
                        boolean validLimit = true;
                        try {
                            Integer.parseInt(maxValueVarDataValue.toString());
                        }
                        catch(Exception ie) {
                            validLimit = false;
                        }
                        if(!validLimit) {
                            String exMsg = MessageFormat.format("Invalid max cardinality constraint: \"{0}\". Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                new Object[]{maxValueVarDataValue.toString(), currentData.getId(), this.id, validationID});
                            throw new IotDeviceApiException(exMsg);
                        }
                        if(cardinality > Integer.parseInt(maxValueVarDataValue.toString())) {
                            if(andOr.equals(BasicValidatorConst.CFG_VL_OR)) {
                                validationFailuresCounter++;
                                String msg = MessageFormat.format("One of the data quality checks in an OR validation group failed: max cardinality constraint value \"{0}\" found in ref data \"{1}\" that is lower than the detected cardinality of data \"{2}\" that is equal to \"{3}\". Validating data: \"{4}\". ValidatOR: \"{5}\". ValidatION: \"{6}\".", 
                                    new Object[]{Integer.parseInt(maxValueVarDataValue.toString()), validationRef, currentData.getId(), String.valueOf(cardinality), currentData.getId(), this.id, validationID});
                                xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check", msg);
                            }
                            else if(Level.SEVERE == Level.parse(validationLevel)) {
                                String exMsg = MessageFormat.format("Data quality check failed: max cardinality constraint value \"{0}\" found in ref data \"{1}\" that is lower than the detected cardinality of data \"{2}\" that is equal to \"{3}\". Validating data: \"{4}\". ValidatOR: \"{5}\". ValidatION: \"{6}\".", 
                                    new Object[]{Integer.parseInt(maxValueVarDataValue.toString()), validationRef, currentData.getId(), String.valueOf(cardinality), currentData.getId(), this.id, validationID});
                                throw new IotDeviceApiException(exMsg);
                            }
                            else {
                                String msg = MessageFormat.format("Data quality check failed: max cardinality constraint value \"{0}\" found in ref data \"{1}\" that is lower than the detected cardinality of data \"{2}\" that is equal to \"{3}\". Validating data: \"{4}\". ValidatOR: \"{5}\". ValidatION: \"{6}\".", 
                                    new Object[]{Integer.parseInt(maxValueVarDataValue.toString()), validationRef, currentData.getId(), String.valueOf(cardinality), currentData.getId(), this.id, validationID});
                                xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check", msg);
                            }
                        }
                    }
                }
                else {
                    String maxCardinalityStr = maxCardinalityElement.getTextContent();
                    boolean validLimit = true;
                    try {
                        Integer.parseInt(maxCardinalityStr);
                    }
                    catch(Exception ie) {
                        validLimit = false;
                    }
                    if(!validLimit) {
                        String exMsg = MessageFormat.format("Invalid max cardinality constraint: \"{0}\". Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                            new Object[]{maxCardinalityStr, currentData.getId(), this.id, validationID});
                        throw new IotDeviceApiException(exMsg);
                    }
                    int maxCardinality = Integer.parseInt(maxCardinalityStr);
                    if(cardinality > maxCardinality) {
                        if(andOr.equals(BasicValidatorConst.CFG_VL_OR)) {
                            validationFailuresCounter++;
                            String msg = MessageFormat.format("One of the data quality checks in an OR validation group failed: max cardinality constraint value \"{0}\" lower than the detected cardinality of data \"{1}\" that is equal to \"{2}\". Validating data: \"{3}\". ValidatOR: \"{4}\". ValidatION: \"{5}\".", 
                                new Object[]{String.valueOf(maxCardinality), currentData.getId(), String.valueOf(cardinality), currentData.getId(), this.id, validationID});
                            xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                        }
                        else if(Level.SEVERE == Level.parse(validationLevel)) {
                            String exMsg = MessageFormat.format("Data quality check failed: max cardinality constraint value \"{0}\" lower than the detected cardinality of data \"{1}\" that is equal to \"{2}\". Validating data: \"{3}\". ValidatOR: \"{4}\". ValidatION: \"{5}\".", 
                                new Object[]{String.valueOf(maxCardinality), currentData.getId(), String.valueOf(cardinality), currentData.getId(), this.id, validationID});
                            throw new IotDeviceApiException(exMsg);
                        }
                        else {
                            String msg = MessageFormat.format("Data quality check failed: max cardinality constraint value \"{0}\" lower than the detected cardinality of data \"{1}\" that is equal to \"{2}\". Validating data: \"{3}\". ValidatOR: \"{4}\". ValidatION: \"{5}\".", 
                                new Object[]{String.valueOf(maxCardinality), currentData.getId(), String.valueOf(cardinality), currentData.getId(), this.id, validationID});
                            xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                        }
                    }
                }
            }
            
            // Checking match
            
            NodeList matchNodes = config.getElementsByTagName(BasicValidatorConst.CFG_EL_MATCH);
            validationGroupCardinality += matchNodes.getLength();
            for(int i = 0; i < matchNodes.getLength(); i++) {
                Element matchElement = (Element)matchNodes.item(i);
                String validationID = matchElement.getAttribute(BasicValidatorConst.CFG_AT_MATCH_ID);
                String op = matchElement.getAttribute(BasicValidatorConst.CFG_AT_MATCH_OPERAND);
                if(!(BasicValidatorConst.CFG_VAL_OPERAND_OR.equals(op) || BasicValidatorConst.CFG_VAL_OPERAND_AND.equals(op))) {
                    String exMsg = MessageFormat.format("Invalid op parameter: \"{0}\". Expected \"and\" or \"or\". Validating data id: \"{1}\". ValidatOR id: \"{2}\". ValidatION id: \"{3}\".", 
                        new Object[]{op, currentData.getId(), this.id, validationID});
                    throw new IotDeviceApiException(exMsg);
                }
                String validationLevel = matchElement.getAttribute(BasicValidatorConst.CFG_AT_MATCH_LEVEL);
                boolean validLevel = true;
                try {
                    Level.parse(validationLevel);
                }
                catch(Exception el) {
                    validLevel = false;
                }
                if(!validLevel) {
                    String exMsg = MessageFormat.format("Invalid data quality check severity level: {0}. Validating data id: \"{1}\". ValidatOR id: \"{2}\". ValidatION id: \"{3}\".", 
                        new Object[]{validationLevel, currentData.getId(), this.id, validationID});
                    throw new IotDeviceApiException(exMsg);
                }
                NodeList valuesNodeList = matchElement.getElementsByTagName(BasicValidatorConst.CFG_EL_MATCH_VALUE);

                for(Object dObj : currentData.getValue()) {
                    
                    boolean oneMatched = false;
                    boolean oneFailed = false;
                    
                    for(int j = 0; j < valuesNodeList.getLength(); j++) {
                        Element valueElement = (Element)valuesNodeList.item(j);
                        if(valueElement.hasAttribute(BasicValidatorConst.CFG_AT_MATCH_VALUE_REF)) {
                            String ref = valueElement.getAttribute(BasicValidatorConst.CFG_AT_MATCH_VALUE_REF);
                            Data matchValueData = builtData.get(ref);
                            if(matchValueData == null) {
                                String exMsg = MessageFormat.format("Data quality check failed: match ref data \"{0}\" could not be found. Validating data id: \"{1}\". ValidatOR id: \"{2}\". ValidatION id: \"{3}\".", 
                                        new Object[]{ref, currentData.getId(), this.id, validationID});
                                throw new IotDeviceApiException(exMsg);
                            }
                            boolean oneOk = false;
                            for(Object obj: matchValueData.getValue()) {
                                if(dObj.toString().matches(obj.toString())) {
                                    oneOk = true;
                                }
                            }
                            if(oneOk) {
                                oneMatched = true;
                            }
                            else {
                                oneFailed = true;
                            }
                        }
                        else {
                            String matchStr = valueElement.getTextContent();
                            if(dObj.toString().matches(matchStr)) {
                                oneMatched = true;
                            }
                            else {
                                oneFailed = true;
                            }
                        }
                    }
                    
                    if(!oneMatched) {
                        if(andOr.equals(BasicValidatorConst.CFG_VL_OR)) {
                            validationFailuresCounter++;
                            String msg = MessageFormat.format("One of the data quality checks in an OR validation group failed: no matches for value \"{0}\" of data \"{1}\" . ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                        }
                        else if(Level.SEVERE == Level.parse(validationLevel)) {
                            String exMsg = MessageFormat.format("Data quality check failed: no matches for value \"{0}\" of data \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            throw new IotDeviceApiException(exMsg);
                        }
                        else {
                            validValues.remove(dObj);
                            String msg = MessageFormat.format("Data quality check failed: no matches for value \"{0}\" of data \"{1}\" . ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                        }
                    }

                    if(BasicValidatorConst.CFG_VAL_OPERAND_AND.equals(op) && oneFailed) {
                        if(andOr.equals(BasicValidatorConst.CFG_VL_OR)) {
                            validationFailuresCounter++;
                            String msg = MessageFormat.format("One of the data quality checks in an OR validation group failed: value \"{0}\" of data \"{1}\" does not match one or more of the values listed in the constraint. ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                        }
                        else if(Level.SEVERE == Level.parse(validationLevel)) {
                            String exMsg = MessageFormat.format("Data quality check failed: value \"{0}\" of data \"{1}\" does not match one or more of the values listed in the constraint. ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            throw new IotDeviceApiException(exMsg);
                        }
                        else {
                            validValues.remove(dObj);
                            String msg = MessageFormat.format("Data quality check failed: value \"{0}\" of data \"{1}\" does not match one or more of the values listed in the constraint. ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                        }
                    }
                
                }

            }
            
            // Checking lower than
            
            NodeList lowerThanNodes = config.getElementsByTagName(BasicValidatorConst.CFG_EL_LOWER_THAN);
            validationGroupCardinality += lowerThanNodes.getLength();
            for(int i = 0; i < lowerThanNodes.getLength(); i++) {
                Element lowerThanElement = (Element)lowerThanNodes.item(i);
                String validationID = lowerThanElement.getAttribute(BasicValidatorConst.CFG_AT_LOWER_THAN_ID);
                String op = lowerThanElement.getAttribute(BasicValidatorConst.CFG_AT_LOWER_THAN_OPERAND);
                if(!(BasicValidatorConst.CFG_VAL_OPERAND_OR.equals(op) || BasicValidatorConst.CFG_VAL_OPERAND_AND.equals(op))) {
                    String exMsg = MessageFormat.format("Invalid op parameter: \"{0}\". Expected \"and\" or \"or\". Validating data id: \"{1}\". ValidatOR id: \"{2}\". ValidatION id: \"{3}\".", 
                        new Object[]{op, currentData.getId(), this.id, validationID});
                    throw new IotDeviceApiException(exMsg);
                }
                String eq = lowerThanElement.getAttribute(BasicValidatorConst.CFG_AT_LOWER_THAN_EQUALITY);
                if(!(BasicValidatorConst.CFG_VAL_EQUALITY_INCLUDE.equals(eq) || BasicValidatorConst.CFG_VAL_EQUALITY_INCLUDE.equals(eq))) {
                    String exMsg = MessageFormat.format("Invalid equality parameter: \"{0}\". Expected \"include\" or \"exclude\". Validating data id: \"{1}\". ValidatOR id: \"{2}\". ValidatION id: \"{3}\".", 
                        new Object[]{eq, currentData.getId(), this.id, validationID});
                    throw new IotDeviceApiException(exMsg);
                }
                String validationLevel = lowerThanElement.getAttribute(BasicValidatorConst.CFG_AT_LOWER_THAN_LEVEL);
                boolean validLevel = true;
                try {
                    Level.parse(validationLevel);
                }
                catch(Exception el) {
                    validLevel = false;
                }
                if(!validLevel) {
                    String exMsg = MessageFormat.format("Invalid data quality check severity level: {0}. Validating data id: \"{1}\". ValidatOR id: \"{2}\". ValidatION id: \"{3}\".", 
                        new Object[]{validationLevel, currentData.getId(), this.id, validationID});
                    throw new IotDeviceApiException(exMsg);
                }
                NodeList valuesNodeList = lowerThanElement.getElementsByTagName(BasicValidatorConst.CFG_EL_LOWER_THAN_VALUE);

                for(Object dObj : currentData.getValue()) {
                    
                    boolean oneMatched = false;
                    boolean oneFailed = false;
                    
                    for(int j = 0; j < valuesNodeList.getLength(); j++) {
                        Element valueElement = (Element)valuesNodeList.item(j);
                        if(valueElement.hasAttribute(BasicValidatorConst.CFG_AT_LOWER_THAN_VALUE_REF)) {
                            String ref = valueElement.getAttribute(BasicValidatorConst.CFG_AT_LOWER_THAN_VALUE_REF);
                            Data refData = builtData.get(ref);
                            if(refData == null) {
                                String exMsg = MessageFormat.format("Data quality check failed: match ref data \"{0}\" could not be found. Validating data id: \"{1}\". ValidatOR id: \"{2}\". ValidatION id: \"{3}\".", 
                                        new Object[]{ref, currentData.getId(), this.id, validationID});
                                throw new IotDeviceApiException(exMsg);
                            }
                            boolean oneOk = false;
                            for(Object obj: refData.getValue()) {
                                
                                BigDecimal bddObj = null;
                                boolean validDec = true;
                                try {
                                    bddObj= new BigDecimal(dObj.toString());
                                }
                                catch(Exception ebd) {
                                    validDec = false;
                                }
                                if(!validDec) {
                                    String exMsg = MessageFormat.format("Data quality check failed: non-numeric value \"{0}\" in data to be validated. Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                        new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                                    throw new IotDeviceApiException(exMsg);
                                }

                                BigDecimal bdobj = null;
                                validDec = true;
                                try {
                                    bdobj= new BigDecimal(obj.toString());
                                }
                                catch(Exception ebd) {
                                    validDec = false;
                                }
                                if(!validDec) {
                                    String exMsg = MessageFormat.format("Data quality check failed: non-numeric threshold value \"{0}\". Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                        new Object[]{obj.toString(), currentData.getId(), this.id, validationID});
                                    throw new IotDeviceApiException(exMsg);
                                }
                                if(bddObj != null && bdobj != null) {
                                    if(BasicValidatorConst.CFG_VAL_EQUALITY_EXCLUDE.equals(eq)) {
                                        if(bddObj.compareTo(bdobj) < 0 ) {
                                            oneOk = true;
                                        }
                                    }
                                    else {
                                        if(bddObj.compareTo(bdobj) <= 0 ) {
                                            oneOk = true;
                                        }
                                    }
                                }
                            }
                            if(oneOk) {
                                oneMatched = true;
                            }
                            else {
                                oneFailed = true;
                            }
                        }
                        else {
                            String refStr = valueElement.getTextContent();
                            
                            BigDecimal bddObj = null;
                            boolean validDec = true;
                            try {
                                bddObj= new BigDecimal(dObj.toString());
                            }
                            catch(Exception ebd) {
                                validDec = false;
                            }
                            if(!validDec) {
                                String exMsg = MessageFormat.format("Data quality check failed: non-numeric value \"{0}\" in data to be validated. Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                    new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                                throw new IotDeviceApiException(exMsg);
                            }

                            BigDecimal bdobj = null;
                            validDec = true;
                            try {
                                bdobj= new BigDecimal(refStr);
                            }
                            catch(Exception ebd) {
                                validDec = false;
                            }
                            if(!validDec) {
                                String exMsg = MessageFormat.format("Data quality check failed: non-numeric threshold value \"{0}\". Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                    new Object[]{refStr, currentData.getId(), this.id, validationID});
                                throw new IotDeviceApiException(exMsg);
                            }
                            if(bddObj != null && bdobj != null) {
                                if(BasicValidatorConst.CFG_VAL_EQUALITY_EXCLUDE.equals(eq)) {
                                    if(bddObj.compareTo(bdobj) < 0 ) {
                                        oneMatched = true;
                                    }
                                    else {
                                        oneFailed = true;
                                    }
                                }
                                else {
                                    if(bddObj.compareTo(bdobj) <= 0 ) {
                                        oneMatched = true;
                                    }
                                    else {
                                        oneFailed = true;
                                    }
                                }
                            }
                        }
                    }
                    
                    if(!oneMatched) {
                        if(andOr.equals(BasicValidatorConst.CFG_VL_OR)) {
                            validationFailuresCounter++;
                            String msg = MessageFormat.format("One of the data quality checks in an OR validation group failed: value \"{0}\" of data \"{1}\" greater than all threshold values listed in the constraint. ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                        }
                        else if(Level.SEVERE == Level.parse(validationLevel)) {
                            String exMsg = MessageFormat.format("Data quality check failed: value \"{0}\" of data \"{1}\" greater than all threshold values listed in the constraint. ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            throw new IotDeviceApiException(exMsg);
                        }
                        else {
                            validValues.remove(dObj);
                            String msg = MessageFormat.format("Data quality check failed: value \"{0}\" of data \"{1}\" greater than all threshold values listed in the constraint. ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                        }
                    }

                    if(BasicValidatorConst.CFG_VAL_OPERAND_AND.equals(op) && oneFailed) {
                        if(andOr.equals(BasicValidatorConst.CFG_VL_OR)) {
                            validationFailuresCounter++;
                            String msg = MessageFormat.format("One of the data quality checks in an OR validation group failed: value \"{0}\" of data \"{1}\" greater than one or more of the threshold values listed in the constraint. ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                        }
                        else if(Level.SEVERE == Level.parse(validationLevel)) {
                            String exMsg = MessageFormat.format("Data quality check failed: value \"{0}\" of data \"{1}\" greater than one or more of the threshold values listed in the constraint. ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            throw new IotDeviceApiException(exMsg);
                        }
                        else {
                            validValues.remove(dObj);
                            String msg = MessageFormat.format("Data quality check failed: value \"{0}\" of data \"{1}\" greater than one or more of the threshold values listed in the constraint. ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                        }
                    }
                
                }

            }
            
            // Checking greater than
            
            NodeList greaterThanNodes = config.getElementsByTagName(BasicValidatorConst.CFG_EL_GREATER_THAN);
            validationGroupCardinality += greaterThanNodes.getLength();
            for(int i = 0; i < greaterThanNodes.getLength(); i++) {
                Element greaterThanElement = (Element)greaterThanNodes.item(i);
                String validationID = greaterThanElement.getAttribute(BasicValidatorConst.CFG_AT_GREATER_THAN_ID);
                String op = greaterThanElement.getAttribute(BasicValidatorConst.CFG_AT_GREATER_THAN_OPERAND);
                if(!(BasicValidatorConst.CFG_VAL_OPERAND_OR.equals(op) || BasicValidatorConst.CFG_VAL_OPERAND_AND.equals(op))) {
                    String exMsg = MessageFormat.format("Invalid op parameter: \"{0}\". Expected \"and\" or \"or\". Validating data id: \"{1}\". ValidatOR id: \"{2}\". ValidatION id: \"{3}\".", 
                            new Object[]{op, currentData.getId(), this.id, validationID});
                    throw new IotDeviceApiException(exMsg);
                }
                String eq = greaterThanElement.getAttribute(BasicValidatorConst.CFG_AT_GREATER_THAN_EQUALITY);
                if(!(BasicValidatorConst.CFG_VAL_EQUALITY_INCLUDE.equals(eq) || BasicValidatorConst.CFG_VAL_EQUALITY_INCLUDE.equals(eq))) {
                    String exMsg = MessageFormat.format("Invalid equality parameter: \"{0}\". Expected \"include\" or \"exclude\". Validating data id: \"{1}\". ValidatOR id: \"{2}\". ValidatION id: \"{3}\".", 
                        new Object[]{eq, currentData.getId(), this.id, validationID});
                    throw new IotDeviceApiException(exMsg);
                }
                String validationLevel = greaterThanElement.getAttribute(BasicValidatorConst.CFG_AT_GREATER_THAN_LEVEL);
                boolean validLevel = true;
                try {
                    Level.parse(validationLevel);
                }
                catch(Exception el) {
                    validLevel = false;
                }
                if(!validLevel) {
                    String exMsg = MessageFormat.format("Invalid data quality check severity level: {0}. Validating data id: \"{1}\". ValidatOR id: \"{2}\". ValidatION id: \"{3}\".", 
                            new Object[]{validationLevel, currentData.getId(), this.id, validationID});
                    throw new IotDeviceApiException(exMsg);
                }
                NodeList valuesNodeList = greaterThanElement.getElementsByTagName(BasicValidatorConst.CFG_EL_GREATER_THAN_VALUE);

                for(Object dObj : currentData.getValue()) {

                    boolean oneMatched = false;
                    boolean oneFailed = false;

                    for(int j = 0; j < valuesNodeList.getLength(); j++) {
                        Element valueElement = (Element)valuesNodeList.item(j);
                        if(valueElement.hasAttribute(BasicValidatorConst.CFG_AT_GREATER_THAN_VALUE_REF)) {
                            String ref = valueElement.getAttribute(BasicValidatorConst.CFG_AT_GREATER_THAN_VALUE_REF);
                            Data refData = builtData.get(ref);
                            if(refData == null) {
                                String exMsg = MessageFormat.format("Data quality check failed: match ref data \"{0}\" could not be found. Validating data id: \"{1}\". ValidatOR id: \"{2}\". ValidatION id: \"{3}\".", 
                                        new Object[]{ref, currentData.getId(), this.id, validationID});
                                throw new IotDeviceApiException(exMsg);
                            }
                            boolean oneOk = false;
                            for(Object obj: refData.getValue()) {

                                BigDecimal bddObj = null;
                                boolean validDec = true;
                                try {
                                    bddObj= new BigDecimal(dObj.toString());
                                }
                                catch(Exception ebd) {
                                    validDec = false;
                                }
                                if(!validDec) {
                                    String exMsg = MessageFormat.format("Data quality check failed: non-numeric value \"{0}\" in data to be validated. Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                            new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                                    throw new IotDeviceApiException(exMsg);
                                }

                                BigDecimal bdobj = null;
                                validDec = true;
                                try {
                                    bdobj= new BigDecimal(obj.toString());
                                }
                                catch(Exception ebd) {
                                    validDec = false;
                                }
                                if(!validDec) {
                                    String exMsg = MessageFormat.format("Data quality check failed: non-numeric threshold value \"{0}\". Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                            new Object[]{obj.toString(), currentData.getId(), this.id, validationID});
                                    throw new IotDeviceApiException(exMsg);
                                }
                                if(bddObj != null && bdobj != null) {

                                    if(BasicValidatorConst.CFG_VAL_EQUALITY_EXCLUDE.equals(eq)) {
                                        if(bddObj.compareTo(bdobj) > 0 ) {
                                            oneOk = true;
                                        }
                                    }
                                    else {
                                        if(bddObj.compareTo(bdobj) >= 0 ) {
                                            oneOk = true;
                                        }                                                            
                                    }
                                }
                            }
                            if(oneOk) {
                                oneMatched = true;
                            }
                            else {
                                oneFailed = true;
                            }
                        }
                        else {
                            String refStr = valueElement.getTextContent();

                            BigDecimal bddObj = null;
                            boolean validDec = true;
                            try {
                                bddObj= new BigDecimal(dObj.toString());
                            }
                            catch(Exception ebd) {
                                validDec = false;
                            }
                            if(!validDec) {
                                String exMsg = MessageFormat.format("Data quality check failed: non-numeric value \"{0}\" in data to be validated. Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                        new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                                throw new IotDeviceApiException(exMsg);
                            }

                            BigDecimal bdobj = null;
                            validDec = true;
                            try {
                                bdobj= new BigDecimal(refStr);
                            }
                            catch(Exception ebd) {
                                validDec = false;
                            }
                            if(!validDec) {
                                String exMsg = MessageFormat.format("Data quality check failed: non-numeric threshold value \"{0}\". Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                        new Object[]{refStr, currentData.getId(), this.id, validationID});
                                throw new IotDeviceApiException(exMsg);
                            }
                            if(bddObj != null && bdobj != null) {

                                if(BasicValidatorConst.CFG_VAL_EQUALITY_EXCLUDE.equals(eq)) {
                                    if(bddObj.compareTo(bdobj) > 0 ) {
                                        oneMatched = true;
                                    }
                                    else {
                                        oneFailed = true;
                                    }
                                }
                                else {
                                    if(bddObj.compareTo(bdobj) >= 0 ) {
                                        oneMatched = true;
                                    }
                                    else {
                                        oneFailed = true;
                                    }                                                    
                                }
                            }
                        }
                    }

                    if(!oneMatched) {
                        if(andOr.equals(BasicValidatorConst.CFG_VL_OR)) {
                            validationFailuresCounter++;
                            String msg = MessageFormat.format("One of the data quality checks in an OR validation group failed: value \"{0}\" of data \"{1}\" lower than all threshold values listed in the constraint. ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                    new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                        }
                        else if(Level.SEVERE == Level.parse(validationLevel)) {
                            String exMsg = MessageFormat.format("Data quality check failed: value \"{0}\" of data \"{1}\" lower than all threshold values listed in the constraint. ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                    new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            throw new IotDeviceApiException(exMsg);
                        }
                        else {
                            validValues.remove(dObj);
                            String msg = MessageFormat.format("Data quality check failed: value \"{0}\" of data \"{1}\" lower than all threshold values listed in the constraint. ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                    new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                        }
                    }

                    if(BasicValidatorConst.CFG_VAL_OPERAND_AND.equals(op) && oneFailed) {
                        if(andOr.equals(BasicValidatorConst.CFG_VL_OR)) {
                            validationFailuresCounter++;
                            String msg = MessageFormat.format("One of the data quality checks in an OR validation group failed: value \"{0}\" of data \"{1}\" lower than one or more of the threshold values listed in the constraint. ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                    new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                        }
                        else if(Level.SEVERE == Level.parse(validationLevel)) {
                            String exMsg = MessageFormat.format("Data quality check failed: value \"{0}\" of data \"{1}\" lower than one or more of the threshold values listed in the constraint. ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                    new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            throw new IotDeviceApiException(exMsg);
                        }
                        else {
                            validValues.remove(dObj);
                            String msg = MessageFormat.format("Data quality check failed: value \"{0}\" of data \"{1}\" lower than one or more of the threshold values listed in the constraint. ValidatOR: \"{2}\". ValidatION: \"{3}\".", 
                                    new Object[]{dObj.toString(), currentData.getId(), this.id, validationID});
                            xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                        }
                    }

                }

            }
            
            // Checking same-cardinality
            NodeList sameCardinalityNodes = config.getElementsByTagName(BasicValidatorConst.CFG_EL_SAME_CARDINALITY);
            validationGroupCardinality += sameCardinalityNodes.getLength();
            for(int i = 0; i < sameCardinalityNodes.getLength(); i++) {
                Element sameCardinalityElement = (Element)sameCardinalityNodes.item(i);
                String validationID = sameCardinalityElement.getAttribute(BasicValidatorConst.CFG_AT_SAME_CARDINALITY_ID);
                String validationLevel = sameCardinalityElement.getAttribute(BasicValidatorConst.CFG_AT_SAME_CARDINALITY_LEVEL);
                boolean validLevel = true;
                try {
                    Level.parse(validationLevel);
                }
                catch(Exception el) {
                    validLevel = false;
                }
                if(!validLevel) {
                    String exMsg = MessageFormat.format("Invalid data quality check severity level: {0}. Validating data id: \"{1}\". ValidatOR id: \"{2}\". ValidatION id: \"{3}\".", 
                            new Object[]{validationLevel, currentData.getId(), this.id, validationID});
                    throw new IotDeviceApiException(exMsg);
                }
                String sameCardinalityRef = sameCardinalityElement.getAttribute(BasicValidatorConst.CFG_AT_SAME_CARDINALITY_REF);
                Data refData = builtData.get(sameCardinalityRef);
                Object[] refValue = refData.getValue();
                if(refValue == null) refValue = new Object[0];
                if(cardinality != refValue.length) {
                    if(andOr.equals(BasicValidatorConst.CFG_VL_OR)) {
                        validationFailuresCounter++;
                        String msg = MessageFormat.format("One of the data quality checks in an OR validation group failed: \"{0}\" has cardinality {1}, while \"{2}\" has cardinality {3}. ValidatOR: \"{4}\". ValidatION: \"{5}\".", 
                                new Object[]{currentData.getId(), cardinality, refData.getId(), refValue.length, this.id, validationID});
                        xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                    }
                    else if(Level.SEVERE == Level.parse(validationLevel)) {
                        String exMsg = MessageFormat.format("Data quality check failed: \"{0}\" has cardinality {1}, while \"{2}\" has cardinality {3}. ValidatOR: \"{4}\". ValidatION: \"{5}\".", 
                                new Object[]{currentData.getId(), cardinality, refData.getId(), refValue.length, this.id, validationID});
                        throw new IotDeviceApiException(exMsg);
                    }
                    else {
                        String msg = MessageFormat.format("Data quality check failed: \"{0}\" has cardinality {1}, while \"{2}\" has cardinality {3}. ValidatOR: \"{4}\". ValidatION: \"{5}\".", 
                                new Object[]{currentData.getId(), cardinality, refData.getId(), refValue.length, this.id, validationID});
                        xlogger.log(BasicValidator.class.getName(), Level.parse(validationLevel), "data quality check failed", msg);
                    }
                }
                
            }
            
            // Check for validation group
            if(BasicValidatorConst.CFG_VL_OR.equals(andOr) && validationFailuresCounter == validationGroupCardinality) {
                boolean validLevel = true;
                try {
                    Level.parse(groupLevel);
                }
                catch(Exception el) {
                    validLevel = false;
                }
                if(!validLevel) {
                    String exMsg = MessageFormat.format("Invalid data quality check group severity level: \"{0}\". Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION group: \"{3}\".", 
                        new Object[]{groupLevel, currentData.getId(), this.id, groupId});
                    throw new IotDeviceApiException(exMsg);
                }
                if(Level.SEVERE == Level.parse(groupLevel)) {
                    String exMsg = MessageFormat.format("Data quality check failed: ALL conditions in OR validation group failed. Data ID: {0}. ValidatOR: \"{1}\". Group ID: \"{2}\".", 
                        new Object[]{currentData.getId(), this.id, groupId});
                    throw new IotDeviceApiException(exMsg);
                }
                else {
                    String msg = MessageFormat.format("Data quality check failed: ALL conditions in OR validation group failed. Data ID: {0}. ValidatOR: \"{1}\". Group ID: \"{2}\".", 
                        new Object[]{currentData.getId(), this.id, groupId});
                    xlogger.log(BasicValidator.class.getName(), Level.parse(groupLevel), "data quality check failed", msg);
                }

            }
            if(BasicValidatorConst.CFG_VL_AND.equals(andOr) && validationFailuresCounter > 0) {
                boolean validLevel = true;
                try {
                    Level.parse(groupLevel);
                }
                catch(Exception el) {
                    validLevel = false;
                }
                if(!validLevel) {
                    String exMsg = MessageFormat.format("Invalid data quality check group severity level: \"{0}\". Validating data: \"{1}\". ValidatOR: \"{2}\". ValidatION group: \"{3}\".", 
                        new Object[]{groupLevel, currentData.getId(), this.id, groupId});
                    throw new IotDeviceApiException(exMsg);
                }
                if(Level.SEVERE == Level.parse(groupLevel)) {
                    String exMsg = MessageFormat.format("Data quality check failed: AT LEAST ONE of the conditions in AND validation group failed. Data ID: {0}. ValidatOR: \"{1}\". Group ID: \"{2}\".", 
                        new Object[]{currentData.getId(), this.id, groupId});
                    throw new IotDeviceApiException(exMsg);
                }
                else {
                    String msg = MessageFormat.format("Data quality check failed: AT LEAST ONE of the conditions in AND validation group failed. Data ID: {0}. ValidatOR: \"{1}\". Group ID: \"{2}\".", 
                        new Object[]{currentData.getId(), this.id, groupId});
                    xlogger.log(BasicValidator.class.getName(), Level.parse(groupLevel), "data quality check failed", msg);
                }

            }

            // Clean
            
            currentData.setValue(validValues.toArray());
            
            // Return cleaned
            
            return currentData;
            
        }
        catch(Exception e) {
            xlogger.log(BasicValidator.class.getName(), Level.SEVERE, "data quality check failed", e);
            setStatus(Const.ERROR);
            return currentData;
        }
    }
    
}
