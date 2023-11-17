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
package org.disit.iotdeviceapi.loaders.virtuoso;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import org.disit.iotdeviceapi.repos.Repos;
import org.disit.iotdeviceapi.datatypes.Data;
import org.disit.iotdeviceapi.datatypes.Quad;
import org.disit.iotdeviceapi.datatypes.UrlWrapper;
import org.disit.iotdeviceapi.loaders.Loader;
import org.disit.iotdeviceapi.utils.Const;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sparql.SPARQLRepository;
import virtuoso.sesame2.driver.VirtuosoRepository;

/**
 * 
 * @author Mirco Soderi @ DISIT DINFO UNIFI (mirco.soderi at unifi dot it)
 */
public class VirtuosoLoader extends Loader {

    static Repository mRepository;
    RepositoryConnection mRepositoryConnection;
    String deleteInsert;
    ArrayList<Data> pendingDataForInsert;
    ArrayList<Data> pendingDataForDelete;
    
    @Override
    public void connect(Repos datasource) {
            
        try {
            if(mRepository == null) {
              Repository repo;
              String virtuosoEndpoint = datasource.getParameter(VirtuosoLoaderConst.CFG_DS_PAR_VIRTENDPT);
              String virtuosoUser = datasource.getParameter(VirtuosoLoaderConst.CFG_DS_PAR_VIRTUSER);
              if(virtuosoUser == null) virtuosoUser = VirtuosoLoaderConst.DEFAULT_VIRTUSER;
              String virtuosoPwd = datasource.getParameter(VirtuosoLoaderConst.CFG_DS_PAR_VIRTPWD);
              if(virtuosoPwd == null) virtuosoPwd = VirtuosoLoaderConst.DEFAULT_VIRTPWD;
              String virtuosoTimeout = datasource.getParameter(VirtuosoLoaderConst.CFG_DS_PAR_VIRTIMEOUT);
              if(virtuosoTimeout == null) virtuosoTimeout = VirtuosoLoaderConst.DEFAULT_TIMEOUT;
              if(virtuosoEndpoint!=null && !virtuosoEndpoint.trim().isEmpty()) {
                VirtuosoRepository vrepo = new VirtuosoRepository(virtuosoEndpoint, virtuosoUser, virtuosoPwd);
                vrepo.setQueryTimeout(Integer.parseInt(virtuosoTimeout)); 
                repo = vrepo;
              }
              else {
                String sparqlEndpoint = datasource.getParameter(VirtuosoLoaderConst.CFG_DS_PAR_SPARQLENDPT);
                repo = new SPARQLRepository(sparqlEndpoint);
              }
              repo.initialize();
              mRepository = repo;
            }
            this.mRepositoryConnection = mRepository.getConnection();
            setConnected(true);
            this.deleteInsert = datasource.getParameter(VirtuosoLoaderConst.CFG_DS_PAR_DELINS);
            if(null != this.deleteInsert) {
                if(!(VirtuosoLoaderConst.CFG_DS_VAL_DELINS_OFF.equals(this.deleteInsert) || 
                        VirtuosoLoaderConst.CFG_DS_VAL_DELINS_SUBJ.equals(this.deleteInsert) ||
                        VirtuosoLoaderConst.CFG_DS_VAL_DELINS_PROP.equals(this.deleteInsert))) {
                    throw new Exception("Invalid argument: delete/insert");
                }
            }
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(VirtuosoLoader.class.getName(), Level.SEVERE, "connect error", e);
        }
    }

    @Override
    public void disconnect(int transactStatus) {
        try {
            if(Const.OK == transactStatus && null != this.pendingDataForInsert) this.deleteInsert(false);
            if(Const.OK == transactStatus && null != this.pendingDataForDelete) this.deleteInsert(true);
            this.mRepositoryConnection.close();
            setConnected(false);
        } 
        catch (Exception ex) {
            setStatus(Const.ERROR);
            getXlogger().log(VirtuosoLoader.class.getName(), Level.SEVERE, "disconnect error", ex);
        }
    }

    @Override
    public void load(Data data) {
        try {
            if(!VirtuosoLoaderConst.DATATYPE_ID_QUAD.equals(data.getType())) return;
            if(!data.isTriggered()) return;
            if(VirtuosoLoaderConst.CFG_DS_VAL_DELINS_OFF.equals(this.deleteInsert)) {
                for(Object quad: data.getValue()) {
                    Data graphs = ((Quad)quad).getGraph();
                    Data subjects = ((Quad)quad).getSubject();
                    Data properties = ((Quad)quad).getProperty();
                    Data fillers = ((Quad)quad).getFiller();
                    if(graphs.getValue() == null || subjects.getValue() == null || properties.getValue() == null || fillers.getValue() == null) continue;
                    int max = Collections.max(Arrays.asList(new Integer[]{graphs.getValue().length, subjects.getValue().length, properties.getValue().length, fillers.getValue().length}));
                    if( (graphs.getValue().length == 1 || graphs.getValue().length == max ) &&
                        (subjects.getValue().length == 1 || subjects.getValue().length == max ) &&
                        (properties.getValue().length == 1 || properties.getValue().length == max ) &&
                        (fillers.getValue().length == 1 || fillers.getValue().length == max )
                    ) { 
                        for(int i = 0; i < max; i++) {
                            String queryGraph = MessageFormat.format(getFormatting().get(graphs.getType()), new Object[]{ 
                                graphs.getValue().length > 1 ? graphs.getValue()[i].toString() : graphs.getValue()[0].toString()
                            });
                            String querySubject = MessageFormat.format(getFormatting().get(subjects.getType()), new Object[]{ 
                                subjects.getValue().length > 1 ? subjects.getValue()[i].toString() : subjects.getValue()[0].toString()
                            });
                            String queryProperty = MessageFormat.format(getFormatting().get(properties.getType()), new Object[]{ 
                                properties.getValue().length > 1 ? properties.getValue()[i].toString() : properties.getValue()[0].toString()
                            });
                            boolean isUri = new UrlWrapper().fromString(fillers.getValue().length > 1 ? fillers.getValue()[i].toString() : fillers.getValue()[0].toString()) != null;                                                                        
                            String queryFiller = MessageFormat.format(getFormatting().get(isUri ? "uri" : fillers.getType()), new Object[]{ 
                                fillers.getValue().length > 1 ? fillers.getValue()[i].toString() : fillers.getValue()[0].toString()
                            });
                            TupleQuery tq = this.mRepositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, 
                                    MessageFormat.format("INSERT '{' GRAPH {0} '{' {1} {2} {3} '}' '}' ", 
                                    new Object[]{queryGraph, querySubject, queryProperty, queryFiller}));
                            long ts = System.currentTimeMillis();
                            tq.evaluate();
                            getXlogger().log(VirtuosoLoader.class.getName(), Level.INFO, "executed query", MessageFormat.format("INSERT '{' GRAPH {0} '{' {1} {2} {3} '}' '}' ", 
                                new Object[]{queryGraph, querySubject, queryProperty, queryFiller}), System.currentTimeMillis() - ts);
                        }
                    }
                    else {
                        for(Object graph:graphs.getValue()!=null?graphs.getValue():new Object[]{}) {
                            for(Object subject:subjects.getValue()!=null?subjects.getValue():new Object[]{}) {
                                for(Object property:properties.getValue()!=null?properties.getValue():new Object[]{}) {
                                    for(Object filler:fillers.getValue()!=null?fillers.getValue():new Object[]{}) {
                                        if(graph != null && subject != null && property != null && filler != null) {
                                            String queryGraph = MessageFormat.format(getFormatting().get(graphs.getType()), new Object[]{graph.toString()});
                                            String querySubject = MessageFormat.format(getFormatting().get(subjects.getType()), new Object[]{subject.toString()});
                                            String queryProperty = MessageFormat.format(getFormatting().get(properties.getType()), new Object[]{property.toString()});
                                            boolean isUri = new UrlWrapper().fromString(filler.toString()) != null;                                                                                                                 
                                            String queryFiller = MessageFormat.format(getFormatting().get(isUri ? "uri" : fillers.getType()), new Object[]{filler.toString()});
                                            TupleQuery tq = this.mRepositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, 
                                            MessageFormat.format("INSERT '{' GRAPH {0} '{' {1} {2} {3} '}' '}' ", 
                                                    new Object[]{queryGraph, querySubject, queryProperty, queryFiller}));
                                            long ts = System.currentTimeMillis();
                                            tq.evaluate();
                                            getXlogger().log(VirtuosoLoader.class.getName(), Level.INFO, "executed query", MessageFormat.format("INSERT '{' GRAPH {0} '{' {1} {2} {3} '}' '}' ", 
                                                    new Object[]{queryGraph, querySubject, queryProperty, queryFiller}), System.currentTimeMillis() - ts);
                                        }
                                    }
                                } 
                            } 
                        }
                    }
                }
            }
            else {
                if(this.pendingDataForInsert == null) {
                    this.pendingDataForInsert = new ArrayList<>();
                }
                this.pendingDataForInsert.add(data);
            }
        } 
        catch (Exception ex) {
            setStatus(Const.ERROR);
            getXlogger().log(VirtuosoLoader.class.getName(), Level.SEVERE, "load error", ex);
        } 
    }
    
    @Override
    public void unload(Data data) { 
        try {
            if(!VirtuosoLoaderConst.DATATYPE_ID_QUAD.equals(data.getType())) return;
            if(!data.isTriggered()) return;
            if(VirtuosoLoaderConst.CFG_DS_VAL_DELINS_OFF.equals(this.deleteInsert)) {
                for(Object quad: data.getValue()) {
                    Data graphs = ((Quad)quad).getGraph();
                    Data subjects = ((Quad)quad).getSubject();
                    Data properties = ((Quad)quad).getProperty();
                    Data fillers = ((Quad)quad).getFiller();
                    if(graphs.getValue() == null || subjects.getValue() == null || properties.getValue() == null || fillers.getValue() == null) continue;
                    int max = Collections.max(Arrays.asList(new Integer[]{graphs.getValue().length, subjects.getValue().length, properties.getValue().length, fillers.getValue().length}));
                    if( (graphs.getValue().length == 1 || graphs.getValue().length == max ) &&
                        (subjects.getValue().length == 1 || subjects.getValue().length == max ) &&
                        (properties.getValue().length == 1 || properties.getValue().length == max ) &&
                        (fillers.getValue().length == 1 || fillers.getValue().length == max )
                    ) { 
                        for(int i = 0; i < max; i++) {
                            String queryGraph = MessageFormat.format(getFormatting().get(graphs.getType()), new Object[]{ 
                                graphs.getValue().length > 1 ? graphs.getValue()[i].toString() : graphs.getValue()[0].toString()
                            });
                            String querySubject = MessageFormat.format(getFormatting().get(subjects.getType()), new Object[]{ 
                                subjects.getValue().length > 1 ? subjects.getValue()[i].toString() : subjects.getValue()[0].toString()
                            });
                            String queryProperty = MessageFormat.format(getFormatting().get(properties.getType()), new Object[]{ 
                                properties.getValue().length > 1 ? properties.getValue()[i].toString() : properties.getValue()[0].toString()
                            });
                            boolean isUri = new UrlWrapper().fromString(fillers.getValue().length > 1 ? fillers.getValue()[i].toString() : fillers.getValue()[0].toString()) != null;                                                                        
                            String queryFiller = MessageFormat.format(getFormatting().get(isUri ? "uri" : fillers.getType()), new Object[]{ 
                                fillers.getValue().length > 1 ? fillers.getValue()[i].toString() : fillers.getValue()[0].toString()
                            });
                            String deleteWhat = MessageFormat.format("{0} {1} {2} . ", new Object[]{querySubject, queryProperty, queryFiller});
                            TupleQuery tq = this.mRepositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, 
                                    MessageFormat.format("DELETE '{' GRAPH {0} '{' {1} '}' '}' ", 
                                    new Object[]{queryGraph, deleteWhat}));
                            long ts = System.currentTimeMillis();
                            tq.evaluate();
                            getXlogger().log(VirtuosoLoader.class.getName(), Level.INFO, "executed query", MessageFormat.format("DELETE '{' GRAPH {0} '{' {1} '}' '}' ", 
                                new Object[]{queryGraph, deleteWhat}), System.currentTimeMillis() - ts);
                        }
                    }
                    else {
                        for(Object graph:graphs.getValue()!=null?graphs.getValue():new Object[]{}) {
                            for(Object subject:subjects.getValue()!=null?subjects.getValue():new Object[]{}) {
                                for(Object property:properties.getValue()!=null?properties.getValue():new Object[]{}) {
                                    for(Object filler:fillers.getValue()!=null?fillers.getValue():new Object[]{}) {
                                        if(graph != null && subject != null && property != null && filler != null) {
                                            String queryGraph = MessageFormat.format(getFormatting().get(graphs.getType()), new Object[]{graph.toString()});
                                            String querySubject = MessageFormat.format(getFormatting().get(subjects.getType()), new Object[]{subject.toString()});
                                            String queryProperty = MessageFormat.format(getFormatting().get(properties.getType()), new Object[]{property.toString()});
                                            boolean isUri = new UrlWrapper().fromString(filler.toString()) != null;                                          
                                            String queryFiller = MessageFormat.format(getFormatting().get(isUri ? "uri" : fillers.getType()), new Object[]{filler.toString()});
                                            String deleteWhat = MessageFormat.format("{0} {1} {2} . ", new Object[]{querySubject, queryProperty, queryFiller});
                                            TupleQuery tq = this.mRepositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, 
                                                    MessageFormat.format("DELETE '{' GRAPH {0} '{' {1} '}' '}' ", 
                                                    new Object[]{queryGraph, deleteWhat}));
                                            long ts = System.currentTimeMillis();
                                            tq.evaluate();
                                            getXlogger().log(VirtuosoLoader.class.getName(), Level.INFO, "executed query", MessageFormat.format("DELETE '{' GRAPH {0} '{' {1} '}' '}' ", 
                                                new Object[]{queryGraph, deleteWhat}), System.currentTimeMillis() - ts);
                                        }
                                    }
                                } 
                            } 
                        }
                    }
                }
            }
            else {
                if(this.pendingDataForDelete == null) {
                    this.pendingDataForDelete = new ArrayList<>();
                }
                this.pendingDataForDelete.add(data);
            }
        } 
        catch (Exception ex) {
            setStatus(Const.ERROR);
            getXlogger().log(VirtuosoLoader.class.getName(), Level.SEVERE, "unload error", ex);
        } 
    }
    
    private void deleteInsert(boolean deleteOnly) {
        
        try {

            HashMap<String,HashMap<String,String[]>> queryPerGraphAndSubject = new HashMap<>();
            int varnum = 0;
            ArrayList<String> addedToDelete = new ArrayList<>();

            for(Data data: this.pendingDataForInsert != null ? this.pendingDataForInsert : this.pendingDataForDelete) {

                for(Object quad: data.getValue()) {
                    Data graphs = ((Quad)quad).getGraph();
                    Data subjects = ((Quad)quad).getSubject();
                    Data properties = ((Quad)quad).getProperty();
                    Data fillers = ((Quad)quad).getFiller();
                    if(graphs.getValue() == null || subjects.getValue() == null || properties.getValue() == null || fillers.getValue() == null) continue;
                    int max = Collections.max(Arrays.asList(new Integer[]{graphs.getValue().length, subjects.getValue().length, properties.getValue().length, fillers.getValue().length}));
                    if( (graphs.getValue().length == 1 || graphs.getValue().length == max ) &&
                        (subjects.getValue().length == 1 || subjects.getValue().length == max ) &&
                        (properties.getValue().length == 1 || properties.getValue().length == max ) &&
                        (fillers.getValue().length == 1 || fillers.getValue().length == max )
                    ) { 
                        for(int i = 0; i < max; i++) {
                            String queryGraph = MessageFormat.format(getFormatting().get(graphs.getType()), new Object[]{ 
                                graphs.getValue().length > 1 ? graphs.getValue()[i].toString() : graphs.getValue()[0].toString()
                            });
                            String querySubject = MessageFormat.format(getFormatting().get(subjects.getType()), new Object[]{ 
                                subjects.getValue().length > 1 ? subjects.getValue()[i].toString() : subjects.getValue()[0].toString()
                            });
                            String queryProperty = MessageFormat.format(getFormatting().get(properties.getType()), new Object[]{ 
                                properties.getValue().length > 1 ? properties.getValue()[i].toString() : properties.getValue()[0].toString()
                            });
                            boolean isUri = new UrlWrapper().fromString(fillers.getValue().length > 1 ? fillers.getValue()[i].toString() : fillers.getValue()[0].toString()) != null;                                                                        
                            String queryFiller = MessageFormat.format(getFormatting().get(isUri ? "uri" : fillers.getType()), new Object[]{ 
                                fillers.getValue().length > 1 ? fillers.getValue()[i].toString() : fillers.getValue()[0].toString()
                            });
                            if(!queryPerGraphAndSubject.containsKey(queryGraph)) {
                                HashMap<String, String[]> bysubj = new HashMap<>();                                
                                queryPerGraphAndSubject.put(queryGraph, bysubj);
                            }
                            String addToDelete;
                            String _addToDelete;
                            if(VirtuosoLoaderConst.CFG_DS_VAL_DELINS_SUBJ.equals(this.deleteInsert)) {
                                addToDelete = MessageFormat.format("{0} ?p{1} ?v{1} . ", new Object[]{querySubject, varnum});
                                _addToDelete = MessageFormat.format("{0} ?p ?v . ", new Object[]{querySubject});
                            }
                            else {
                                addToDelete = MessageFormat.format("{0} {1} ?v{2} . ", new Object[]{querySubject, queryProperty, varnum});
                                _addToDelete = MessageFormat.format("{0} {1} ?v . ", new Object[]{querySubject, queryProperty});
                            }
                            if(!addedToDelete.contains(_addToDelete)) {
                                varnum++;
                                addedToDelete.add(_addToDelete);
                                if(!queryPerGraphAndSubject.get(queryGraph).containsKey(querySubject)) queryPerGraphAndSubject.get(queryGraph).put(querySubject, new String[]{new String(), new String()});
                                queryPerGraphAndSubject.get(queryGraph).get(querySubject)[0] = queryPerGraphAndSubject.get(queryGraph).get(querySubject)[0].concat(_addToDelete);
                            }
                            String addToInsert = MessageFormat.format("{0} {1} {2} . ", new Object[]{querySubject, queryProperty, queryFiller});
                            if(!queryPerGraphAndSubject.get(queryGraph).containsKey(querySubject)) queryPerGraphAndSubject.get(queryGraph).put(querySubject, new String[]{new String(), new String()});
                            if(!queryPerGraphAndSubject.get(queryGraph).get(querySubject)[1].contains(" . ".concat(addToInsert))) {
                                if(data.isTriggered()) {
                                    queryPerGraphAndSubject.get(queryGraph).get(querySubject)[1] = queryPerGraphAndSubject.get(queryGraph).get(querySubject)[1].concat(addToInsert);
                                }
                            }
                        }
                    }
                    else {
                        for(Object graph:graphs.getValue()!=null?graphs.getValue():new Object[]{}) {
                            for(Object subject:subjects.getValue()!=null?subjects.getValue():new Object[]{}) {
                                for(Object property:properties.getValue()!=null?properties.getValue():new Object[]{}) {
                                    for(Object filler:fillers.getValue()!=null?fillers.getValue():new Object[]{}) {
                                        if(graph != null && subject != null && property != null && filler != null) {
                                            String queryGraph = MessageFormat.format(getFormatting().get(graphs.getType()), new Object[]{graph.toString()});
                                            String querySubject = MessageFormat.format(getFormatting().get(subjects.getType()), new Object[]{subject.toString()});
                                            String queryProperty = MessageFormat.format(getFormatting().get(properties.getType()), new Object[]{property.toString()});
                                            boolean isUri = new UrlWrapper().fromString(filler.toString()) != null;   
                                            String queryFiller = MessageFormat.format(getFormatting().get(isUri ? "uri" : fillers.getType()), new Object[]{filler.toString()});
                                            if(!queryPerGraphAndSubject.containsKey(queryGraph)) {
                                                HashMap<String, String[]> bysubj = new HashMap<>();
                                                queryPerGraphAndSubject.put(queryGraph, bysubj);
                                            }
                                            String addToDelete;
                                            String _addToDelete;
                                            if(VirtuosoLoaderConst.CFG_DS_VAL_DELINS_SUBJ.equals(this.deleteInsert)) {
                                                addToDelete = MessageFormat.format("{0} ?p{1} ?v{1} . ", new Object[]{querySubject, varnum});
                                                _addToDelete = MessageFormat.format("{0} ?p ?v . ", new Object[]{querySubject});
                                            }
                                            else {
                                                addToDelete = MessageFormat.format("{0} {1} ?v{2} . ", new Object[]{querySubject, queryProperty, varnum});
                                                _addToDelete = MessageFormat.format("{0} {1} ?v . ", new Object[]{querySubject, queryProperty});
                                            }
                                            if(!addedToDelete.contains(_addToDelete)) {
                                                addedToDelete.add(_addToDelete);
                                                varnum++;
                                                if(!queryPerGraphAndSubject.get(queryGraph).containsKey(querySubject)) queryPerGraphAndSubject.get(queryGraph).put(querySubject, new String[]{new String(), new String()});
                                                queryPerGraphAndSubject.get(queryGraph).get(querySubject)[0] = queryPerGraphAndSubject.get(queryGraph).get(querySubject)[0].concat(_addToDelete);
                                            }
                                            String addToInsert = MessageFormat.format("{0} {1} {2} . ", new Object[]{querySubject, queryProperty, queryFiller});
                                            if(!queryPerGraphAndSubject.get(queryGraph).containsKey(querySubject)) queryPerGraphAndSubject.get(queryGraph).put(querySubject, new String[]{new String(), new String()});
                                            if(!queryPerGraphAndSubject.get(queryGraph).get(querySubject)[1].contains(" . ".concat(addToInsert))) {
                                                if(data.isTriggered()) {
                                                    queryPerGraphAndSubject.get(queryGraph).get(querySubject)[1] = queryPerGraphAndSubject.get(queryGraph).get(querySubject)[1].concat(addToInsert);
                                                }
                                            }
                                        }
                                    }
                                } 
                            } 
                        }
                    }
                }
            }
            for(String graph: queryPerGraphAndSubject.keySet()) {
                for(String subject: queryPerGraphAndSubject.get(graph).keySet()) {
                    String delete = queryPerGraphAndSubject.get(graph).get(subject)[0];
                    String insert = queryPerGraphAndSubject.get(graph).get(subject)[1];
                    TupleQuery initq = this.mRepositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, MessageFormat.format("INSERT '{' GRAPH {0} '{' {0} {0} {0} '}' '}' ", graph));
                    initq.evaluate();

                    if((!deleteOnly) && (!(delete.isEmpty() || insert.isEmpty()))) {

                        TupleQuery ptq = this.mRepositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, MessageFormat.format("SELECT * '{' GRAPH {0} '{' {1} ?p ?v '}' '}' ", new Object[]{graph, subject})); 
                        TupleQueryResult ptqr = ptq.evaluate();
                        
                        try { 
                            if(ptqr.hasNext()) {

                                TupleQuery tq = this.mRepositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, 
                                MessageFormat.format("DELETE '{' GRAPH {0} '{' {1} '}'  '}' INSERT '{' GRAPH {0} '{' {2} '}' '}' WHERE '{' GRAPH {0} '{' {3} ?p ?v '}' '}' ", 
                                        new Object[]{graph, delete, insert, subject}));
                                long ts = System.currentTimeMillis();
                                tq.evaluate();                             
                                getXlogger().log(VirtuosoLoader.class.getName(), Level.INFO, "executed query", MessageFormat.format("DELETE '{' GRAPH {0} '{' {1} '}'  '}' INSERT '{' GRAPH {0} '{' {2} '}' '}' WHERE '{' GRAPH {0} '{' {3} ?p ?v '}' '}' ", 
                                        new Object[]{graph, delete, insert, subject}), System.currentTimeMillis() - ts);

                            }
                            else {
                                TupleQuery tq = this.mRepositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, 
                                MessageFormat.format("INSERT '{' GRAPH {0} '{' {1} '}' '}' ", 
                                        new Object[]{graph, insert}));
                                long ts = System.currentTimeMillis();
                                tq.evaluate(); 
                                getXlogger().log(VirtuosoLoader.class.getName(), Level.INFO, "executed query", MessageFormat.format("INSERT '{' GRAPH {0} '{' {1} '}' '}' ", 
                                        new Object[]{graph, insert}), System.currentTimeMillis() - ts);
                            }
                        }
                        finally{
                            ptqr.close();
                        }
                        
                    }
                    else if(deleteOnly) {
                        TupleQuery tq = this.mRepositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, 
                                MessageFormat.format("DELETE '{' GRAPH {0} '{' {1} '}'  '}' ", 
                                new Object[]{graph, insert}));
                        long ts = System.currentTimeMillis();
                        tq.evaluate(); 
                        getXlogger().log(VirtuosoLoader.class.getName(), Level.INFO, "executed query", MessageFormat.format("DELETE '{' GRAPH {0} '{' {1} '}'  '}' ", 
                                new Object[]{graph, insert}), System.currentTimeMillis() - ts);
                    }
                }
            }
        }
        catch(Exception e) {
            setStatus(Const.ERROR);
            getXlogger().log(VirtuosoLoader.class.getName(), Level.SEVERE, "delete/insert error", e);
        }
    }
    
}
