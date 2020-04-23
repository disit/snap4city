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
package org.disit.iotdeviceapi.providers.virtuoso;

import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import org.disit.iotdeviceapi.loaders.virtuoso.VirtuosoLoaderConst;
import org.disit.iotdeviceapi.providers.Provider;
import org.disit.iotdeviceapi.repos.Repos;
import org.disit.iotdeviceapi.utils.Const;
import org.openrdf.query.BindingSet;
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
public class VirtuosoProvider extends Provider{
    
    public VirtuosoProvider(Repos datasource) {
        super(datasource);
    }
    
    @Override
    public Object[] get(Object[] queries) {
        if(queries == null) return null;
        RepositoryConnection conn = null;
        try {
            Repository repo;
            ArrayList<String> output = new ArrayList<>();
            String virtuosoEndpoint = getDatasource().getParameter(VirtuosoLoaderConst.CFG_DS_PAR_VIRTENDPT);
            String virtuosoUser = getDatasource().getParameter(VirtuosoLoaderConst.CFG_DS_PAR_VIRTUSER);
            if(virtuosoUser == null) virtuosoUser = VirtuosoLoaderConst.DEFAULT_VIRTUSER;
            String virtuosoPwd = getDatasource().getParameter(VirtuosoLoaderConst.CFG_DS_PAR_VIRTPWD);
            if(virtuosoPwd == null) virtuosoPwd = VirtuosoLoaderConst.DEFAULT_VIRTPWD;
            String virtuosoTimeout = getDatasource().getParameter(VirtuosoLoaderConst.CFG_DS_PAR_VIRTIMEOUT);
            if(virtuosoTimeout == null) virtuosoTimeout = VirtuosoLoaderConst.DEFAULT_TIMEOUT;
            if(virtuosoEndpoint!=null && !virtuosoEndpoint.trim().isEmpty()) {
              VirtuosoRepository vrepo = new VirtuosoRepository(virtuosoEndpoint, virtuosoUser, virtuosoPwd);
              vrepo.setQueryTimeout(Integer.parseInt(virtuosoTimeout)); 
              repo = vrepo;
            }
            else {
              String sparqlEndpoint = getDatasource().getParameter(VirtuosoLoaderConst.CFG_DS_PAR_SPARQLENDPT);
              repo = new SPARQLRepository(sparqlEndpoint);
            }
            repo.initialize();
            conn = repo.getConnection();
            for(Object query: queries) {
                getXlogger().log(VirtuosoProvider.class.getName(), Level.INFO, "query", query.toString());
                TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
                TupleQueryResult tqr = tq.evaluate();
                while (tqr.hasNext()) {
                    BindingSet bs = tqr.next();
                    Set<String> variables = bs.getBindingNames();
                    String row = "{\n";
                    for(String variable: variables) {
                        row+= "\t\""+variable+"\": \""+bs.getValue(variable).stringValue()+"\",\n";
                    }
                    row = row.substring(0, row.length()-2).concat("\n");
                    row+="}";                    
                    output.add(row);
               }
            }
           conn.close();
           return output.toArray();

        }
        catch(Exception e) {
            try { if(conn != null && conn.isOpen()) conn.close(); } catch(Exception ex) {}
            setStatus(Const.ERROR);
            getXlogger().log(VirtuosoProvider.class.getName(), Level.SEVERE, "connect error", e);
            return null;
        }
    }
    
}
