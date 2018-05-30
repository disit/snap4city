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
package org.disit.iotdeviceapi.datatypes;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class Quad extends DataType {
    
    private Data graph;
    private Data subject;
    private Data property;
    private Data filler;

    public Quad() {
    }

    public Data getGraph() {
        return graph;
    }

    public void setGraph(Data graph) {
        this.graph = graph;
    }

    public Data getSubject() {
        return subject;
    }

    public void setSubject(Data subject) {
        this.subject = subject;
    }

    public Data getProperty() {
        return property;
    }

    public void setProperty(Data property) {
        this.property = property;
    }

    public Data getFiller() {
        return filler;
    }

    public void setFiller(Data filler) {
        this.filler = filler;
    }

}
