/* Connection to HBase via Phoenix from Spoon
   Copyright (C) 2018 DISIT Lab http://www.disit.org - University of Florence

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


//use in a js Spoon block and replace tablenames and database connection information
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ParameterMetaData;
import java.util.Date;
import java.util.Arrays;
import java.io.*;

import org.pentaho.di.core.database.*;

public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException, ParseException,SQLException {

if(first)
{
//	do somthing
	first=false;
}

		String tableName = "Electric_vehicle_charging";//table name
		
		Connection conn=null;
		//Statement st = null;
		PreparedStatement pSt =null;
		String sql;
		String sql2;
try {
		logBasic("Register Driver");
		Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
		
		logBasic("Creating connection");
		conn=DriverManager.getConnection("jdbc:phoenix:127.0.0.1:2181/hbase");//Database connection
		
		logBasic("Creating statement");
		
		Object [] r= getRow();

		if (r == null) {
      		setOutputDone();
      		return false;
		}
		RowMetaInterface inputRowMeta = getInputRowMeta();
		String [] fields = inputRowMeta.getFieldNames();
		//sql ="upsert into " + tableName + "("+Arrays.toString(fields).replace("[","").replace("]","")+") values (";
		sql ="upsert into " + tableName + "(";
		sql2 ="";

		int c, index=0;
		for (c=0;c<fields.length ;c++){
			if(get(Fields.In,fields[c]).getString(r) != null){
				logBasic("Variable is: "+inputRowMeta.getValueMeta(c).getName() + " | value: " + get(Fields.In,fields[c]).getString(r));			
				if(c==0){
					sql = sql + inputRowMeta.getValueMeta(c).getName();
					
					sql2=sql2+"?";
				}
				else{
					sql = sql + "," + inputRowMeta.getValueMeta(c).getName();
					
					sql2=sql2+",?";
				}
			}
		}

		sql = sql +") values ("+sql2+")";
		pSt=conn.prepareStatement(sql);
		//prepare statement
		for (c=0;c<fields.length  ;c++){
			//logBasic("PASSO");	
			logBasic("Value meta type is: "+inputRowMeta.searchValueMeta(fields[c]).toString());
			if(get(Fields.In,fields[c]).getString(r) != null){
				index = index+1;
				if (inputRowMeta.searchValueMeta(fields[c]).toString().contains("BigNumber")){
				//if(get(Fields.In,fields[c]).getString(r)!=null){
					logBasic("BigNumber index: " + index);
					pSt.setDouble(index, Double.parseDouble( get(Fields.In,fields[c]).getBigNumber(r).toString() ));
					//logBasic("Value  is: "+get(Fields.In,fields[c]).getBigNumber(r).toString());
				//}
				}
				else if(inputRowMeta.searchValueMeta(fields[c]).toString().contains("Date")){
					String s = get(Fields.In,fields[c]).getString(r);
					String[] parts = s.split("/");
					String aa = parts[0];
					String MM = parts[1];
					String temp = parts[2];
					String[] temp_2 = temp.split(" ");
					String gg = temp_2[0];
					String orario_temp = temp_2[1];
					String[] orario = orario_temp.split(":");
					String hh = orario[0];
					String mm = orario[1];
					String ss_temp = orario[2];
					String[] ss_temp2 = ss_temp.split("\\.");
					String ss = ss_temp2 [0];
					String ms = ss_temp2[1];
					//int hh_CEST = Integer.parseInt(hh+2);
					//logBasic("");
					//String date_CEST = aa+"/"+MM+"/"+gg+
					logBasic("Anno: "+ aa+"; Mese: "+MM+"; Giorno: "+gg + "Ore: "+hh+ "; Minuti: "+mm + "; Secondi: "+ss+ "; Millisecondi: "+ms);
					//logBasic("Anno: "+ aa+"; Mese: "+MM+"; Giorno: "+gg + "Ore: "+hh+ "; Minuti: "+mm );
					java.sql.Date d = new java.sql.Date( get(Fields.In,fields[c]).getInteger(r) );
					//d.setHours(d.getHours()+2);
					d.setTime(d.getTime() + (2*60*60*1000)); 
					//java.sql.Date d = new java.sql.Date(2017,08,07);
					//logBasic("Cosa passo: "+d.toString());
					logBasic("Date index: " + index);
					pSt.setDate(index, d);
					//logBasic("Value  is: "+get(Fields.In,fields[c]).getString(r));		
				}
				else{
					//logBasic("Value is ---: "+get(Fields.In, fields[c]).getString(r));
					logBasic("Else index: " + index);
					pSt.setString(index, get(Fields.In,fields[c]).getString(r) );
					//	pSt.setDouble(index, Double.parseDouble( get(Fields.In,fields[c]).getBigNumber(r).toString() ));
				}
			}
		}
		
		pSt.executeUpdate(sql);
		conn.commit();
}
catch (Exception e)
{
logBasic("Generic Error: Apache or kettle -> that's the problem: " + e.toString());
}
return true;

}
