/* Clusterer of trajectories
 * https://github.com/luborliu/TraClusAlgorithm
 * https://github.com/luborliu/DBSCANSD
 * http://www.slideshare.net/ivansanchez1988/trajectory-clustering-traclus-algorithm
 * http://hanj.cs.illinois.edu/pdf/sigmod07_jglee.pdf
 * http://vgc.poly.edu/~nivan/pdfs/vfkm.pdf
 * https://www.mapbox.com/blog/supercluster/?utm_source=newsletter_april&utm_medium=email&utm_content=supercluster&utm_campaign=newsletter_april
 * http://stackoverflow.com/questions/18820814/trajectory-clustering-which-clustering-method
 * http://dm.kaist.ac.kr/jaegil/papers/sigmod07.pdf
 * http://dm.kaist.ac.kr/jaegil/papers/vldb08.pdf
 * http://dm.kaist.ac.kr/jaegil/papers/icde08.pdf
 * http://dm.kaist.ac.kr/jaegil/#Publications
 *
 * Lee, J., Han, J., and Whang, K., "Trajectory Clustering: A Partition-and-Group Framework" 
 * In Proc. 2007 ACM Int'l Conf. on Management of Data (SIGMOD), Beijing, China, pp. 593 ~ 604, June 2007 (top conference, acceptance rate: 14.6%). 
 * This paper is the most-cited paper on trajectory clustering. The number of citations is over 450 as of February 2014.
 *
 * Lee, J., Han, J., Li, X., and Gonzalez, H., "TraClass: Trajectory Classification Using Hierarchical Region-Based and
 * Trajectory-Based Clustering" In 34th Int'l Conf. on Very Large Data Bases (VLDB) / Proc. of The VLDB Endowment (PVLDB), 
 * Vol. 1, No. 1, pp. 1081 ~ 1094, Aug. 2008 (top conference, acceptance rate: 16.5%)
 *
 * Lee, J., Han, J., and Li, X., "Trajectory Outlier Detection: A Partition-and-Detect Framework" In Proc. 24th Int'l Conf. on Data 
 * Engineering (IEEE ICDE), Cancun, Mexico, pp. 140 ~ 149, Apr. 2008 (top conference, full presentation paper, acceptance rate: 12.1%)
 * 
 * Moritz H., Geodetic Reference System 1980, Journal of Geodesy March 2000, Volume 74, Issue 1, pp 128-133
 * Mean Earth's radius R1 = 6371008.7714 m 
 */
package trajectoriesclustering;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import trajectoriesclustering.TraClusterDoc.Parameter;

public class Main {

    public static ConnectionPool connectionPool;
    public static DataSource dataSource;
    public static Properties properties;
    public static double MAP_WIDTH = 0;
    public static double MAP_HEIGHT = 0;
    public static final int EARTH_RADIUS = 6371000;
    public static boolean LOG_ENABLED;

    public static void main(String[] args) {
        FileInputStream inputStream = null;
        try {
            // load configuration trajectories.properties
            properties = new Properties();
            inputStream = new FileInputStream(args[0]);
            properties.load(inputStream);
            MAP_WIDTH = Double.parseDouble(properties.getProperty("maxWidth"));
            MAP_HEIGHT = Double.parseDouble(properties.getProperty("mapHeight"));
            LOG_ENABLED = properties.getProperty("log_enabled").equalsIgnoreCase("true") ? true : false;
            String[] profiles = new String[]{"S4CHelsinkiTrackerLocation", "S4CAntwerpTrackerLocation", "S4CTuscanyTrackerLocation"};
            for (String profile : profiles) {
                // generate trajectories file
                if (LOG_ENABLED == true) {
                    String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
                    writeFile(properties.getProperty("log_file"), timeStamp + " - Generating trajectories file (" + profile + ")\n");
                }
                if (properties.getProperty("preClustered").equals("false")) {
                    generateTrajectories(profile);
                } else {
                    generateTrajectoriesPreClustered(profile);
                }
                if (LOG_ENABLED == true) {
                    String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
                    writeFile(properties.getProperty("log_file"), timeStamp + " - Trajectories file (" + profile + ") generation completed\n");
                }
                if (LOG_ENABLED == true) {
                    String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
                    writeFile(properties.getProperty("log_file"), timeStamp + " - Clustering trajectories (" + profile + ")\n");
                }
                if (properties.getProperty("trajectories_file") != null
                        && properties.getProperty("javascript_path") != null
                        && properties.getProperty("eps") != null
                        && properties.getProperty("minLns") != null) {
                    TraClusterDoc tcd = new TraClusterDoc();
                    tcd.onOpenDocument(properties.getProperty("trajectories_file"));
                    //tcd.onClusterGenerate(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));//25,5~7
                    tcd.profile = profile;
                    tcd.onClusterGenerateJavascript(properties.getProperty("javascript_path") + "clusteredTrajectories_" + (profile != null ? profile : "") + ".js", Double.parseDouble(properties.getProperty("eps")), Integer.parseInt(properties.getProperty("minLns")));//25,5~7
                } else if (properties.getProperty("trajectories_file") != null && properties.getProperty("javascript_path") != null) {
                    TraClusterDoc tcd = new TraClusterDoc();
                    tcd.onOpenDocument(properties.getProperty("trajectories_file"));
                    Parameter p = tcd.onEstimateParameter();
                    // eps: distance threshold (float)
                    // minLns: core segments (integer)
                    if (p != null) {
                        System.out.println("Based on the algorithm, the suggested parameters are:\n" + "eps:" + p.epsParam + "  minLns:" + p.minLnsParam);
                    }
                    //tcd.onClusterGenerate(args[1], p.epsParam, p.minLnsParam);
                    tcd.profile = profile;
                    tcd.onClusterGenerateJavascript(properties.getProperty("javascript_path") + "clusteredTrajectories_" + (profile != null ? profile : "") + ".js", p.epsParam, p.minLnsParam);
                } else {
                    System.out.println("Please give me 2 or 4 input parameters! \n "
                            + "If you have no idea how to decide eps and minLns, just feed in 2 parameters (inputFilePath, outputFilePath):\n"
                            + "--e.g. java boliu.Main deer_1995.tra testOut.txt \n"
                            + "If you know the two parameters, just feed in all the 4 parameters (inputFilePath, outputFilePath, eps, minLns)"
                            + "--e.g. java boliu.Main deer_1995.tra testOut.txt 29 8 \n");
                }
                if (LOG_ENABLED == true) {
                    String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
                    writeFile(properties.getProperty("log_file"), timeStamp + " - Trajectories clustering (" + profile + ") completed \n");
                }

                /*
                 TraClusterDoc tcd = new TraClusterDoc();
            
                 //tcd.onOpenDocument("src/deer_1995.tra");
                 //tcd.onClusterGenerate("testDeerResult.txt", 29, 8);
            
                 //tcd.onOpenDocument("src/hurricane1950_2006.tra");
                 //tcd.onClusterGenerate("testHurricaneResult.txt", 32, 6);
            
                 tcd.onOpenDocument("src/elk_1993.tra");
                 tcd.onClusterGenerate("testElkResult.txt", 25, 5);//25,5~7
            
            
                 MainFrame mf = new MainFrame(tcd.m_trajectoryList, tcd.m_clusterList);
            
            
                 //		for(int i=0; i<tcd.m_trajectoryList.size();i++) {
                 //			for(int m=0; m<tcd.m_trajectoryList.get(i).getM_pointArray().size(); m++) {
                 //				System.out.print(tcd.m_trajectoryList.get(i).getM_pointArray().get(m).getM_coordinate(0)+" ");
                 //				System.out.print(tcd.m_trajectoryList.get(i).getM_pointArray().get(m).getM_coordinate(1)+" ");
            
                 //			}
            
                 //		System.out.println();
                 //		}
            
                 Parameter p = tcd.onEstimateParameter();
                 if(p != null) {
                 System.out.println("Based on the algorithm, the suggested parameters are:\n"+"eps:"+p.epsParam+"  minLns:"+p.minLnsParam);
            
                 }
                 */
            }
        } catch (FileNotFoundException ex) {
            if (LOG_ENABLED == true) {
                writeFile(properties.getProperty("log_file"), ex.toString() + "\n");
            }
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            if (LOG_ENABLED == true) {
                writeFile(properties.getProperty("log_file"), ex.toString() + "\n");
            }
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // generate trajectories file from recommender's data by sortmerging two jdbc resultsets
    //http://www.coderanch.com/t/523306/JDBC/databases/comparing-ResultSet
    public static void generateTrajectoriesMerge(String profile) {
        HashMap<String, HashMap<String, ArrayList<String>>> map = new HashMap<>(); // user => (YY-mm-dd => ArrayList of {timestamp, latitude + " " + longitude})
        try {
            Connection conn1 = getConnection(properties.getProperty("db_recommender_host"),
                    properties.getProperty("db_recommender_database"),
                    properties.getProperty("db_recommender_username"),
                    properties.getProperty("db_recommender_password"));

            // get banned users
            ArrayList<String> bannedUsers = new ArrayList<>();
            PreparedStatement preparedStatement1;
            if (profile != null) {
                preparedStatement1 = conn1.prepareStatement("SELECT user FROM recommender.users WHERE label IS NOT NULL OR profile != ?");
                preparedStatement1.setString(1, profile);
            } else {
                preparedStatement1 = conn1.prepareStatement("SELECT user FROM recommender.users WHERE label IS NOT NULL");
            }
            ResultSet rs1 = preparedStatement1.executeQuery();
            while (rs1.next()) {
                bannedUsers.add(rs1.getString("user"));
            }

            // get recommendations trajectories
            PreparedStatement preparedStatement2 = conn1.prepareStatement("SELECT user, latitude, longitude, "
                    + "EXTRACT(YEAR FROM timestamp) AS year, "
                    + "EXTRACT(MONTH FROM timestamp) AS month, "
                    + "EXTRACT(DAY FROM timestamp) AS day, "
                    + "UNIX_TIMESTAMP(timestamp) AS unix_timestamp "
                    + "FROM recommender.recommendations_log ORDER BY user, timestamp ASC");
            ResultSet rs2 = preparedStatement2.executeQuery();

            Connection conn2 = getConnection(properties.getProperty("db_sensors_host"),
                    properties.getProperty("db_sensors_database"),
                    properties.getProperty("db_sensors_username"),
                    properties.getProperty("db_sensors_password"));

            // get sensors trajectories
            PreparedStatement preparedStatement3 = conn2.prepareStatement("SELECT device_id AS user, latitude, longitude, "
                    + "EXTRACT(YEAR FROM date) AS year, "
                    + "EXTRACT(MONTH FROM date) AS month, "
                    + "EXTRACT(DAY FROM date) AS day, "
                    + "UNIX_TIMESTAMP(date) AS unix_timestamp "
                    + "FROM sensors.sensors WHERE device_id IS NOT NULL ORDER BY user, date ASC");
            ResultSet rs3 = preparedStatement3.executeQuery();

            boolean next1 = true;
            boolean next2 = true;

            while (true) {
                next1 = next1 && rs2.next();
                next2 = next2 && rs3.next();

                if (!(next1 || next2)) {
                    break; // reached end of both resultsets
                }
                /*if (bannedUsers.contains(rs2.getString("user"))) {
                 continue;
                 }*/
                if (next1 != next2) {
                    if (next1) {
                        // the current row in rs2 does not exist in rs3 (rs3 is at the end already)
                        addToMap(map, rs2, bannedUsers);
                    } else {
                        // the current row in rs3 does not exist in rs2 (rs2 is at the end already)
                        addToMap(map, rs3, bannedUsers);
                    }
                } else {
                    int result = rs2.getString("user").compareTo(rs3.getString("user"));
                    if (result != 0) {
                        // rs2 user != rs3 user
                        addToMap(map, rs2, rs3, false, bannedUsers);
                    } else {
                        // rs2 user = rs3 user
                        addToMap(map, rs2, rs3, true, bannedUsers);
                    }
                }
            }
            conn1.close();
            conn2.close();

            // write trajectories to file
            FileWriter fstream = new FileWriter(properties.getProperty("trajectories_file"));
            BufferedWriter out = new BufferedWriter(fstream);
            String data = "";
            int counter = 0;

            Iterator it1 = map.keySet().iterator();
            while (it1.hasNext()) {
                String user = (String) it1.next();
                HashMap<String, ArrayList<String>> tmp = map.get(user);
                Iterator it2 = tmp.keySet().iterator();
                while (it2.hasNext()) {
                    String date = (String) it2.next();
                    ArrayList<String> latlon = tmp.get(date);
                    if (latlon.size() > 1) {
                        for (int i = 0; i < latlon.size(); i++) {
                            data += (i != 0 ? " " : counter + " " + latlon.size() + " ") + latlon.get(i);
                        }
                        data += "\n";
                        counter++;
                    }
                }
            }
            out.write("2\n"); // number of dimensions
            out.write(counter + "\n"); // number of trajectories
            out.write(data);
            out.close();
        } catch (SQLException | IOException ex) {
            if (LOG_ENABLED == true) {
                writeFile(properties.getProperty("log_file"), ex.toString() + "\n");
            }
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // generate trajectories file from data
    public static void generateTrajectories(String profile) {
        HashMap<String, HashMap<String, ArrayList<String[]>>> map = new HashMap<>(); // user => (YY-mm-dd => ArrayList of {timestamp, latitude + " " + longitude})
        try {
            if (LOG_ENABLED == true) {
                String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
                writeFile(properties.getProperty("log_file"), timeStamp + " - Connecting to database (" + profile + ")\n");
            }
            Connection conn = getConnection(properties.getProperty("db_host"),
                    properties.getProperty("db_schema"),
                    properties.getProperty("db_username"),
                    properties.getProperty("db_password"));

            // get banned users
            /*ArrayList<String> bannedUsers = new ArrayList<>();
             PreparedStatement preparedStatement1 = null;
             if (profile != null) {
             preparedStatement1 = conn1.prepareStatement("SELECT user FROM recommender.users WHERE label IS NOT NULL OR profile != ?");
             preparedStatement1.setString(1, profile);
             } else {
             preparedStatement1 = conn1.prepareStatement("SELECT user FROM recommender.users WHERE label IS NOT NULL");
             }
             ResultSet rs1 = preparedStatement1.executeQuery();
             while (rs1.next()) {
             bannedUsers.add(rs1.getString("user"));
             }
             // add banned user
             bannedUsers.add("0000000011111111222222223333333344444444555555556666666677777777");*/
            // get users
            ArrayList<String> allowedUsers = new ArrayList<>();
            if (LOG_ENABLED == true) {
                String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
                writeFile(properties.getProperty("log_file"), timeStamp + " - Getting allowed users' list (" + profile + ")\n");
            }
            /*PreparedStatement preparedStatement1 = conn.prepareStatement("SELECT id AS user FROM kpidata WHERE value_name = ?");
            preparedStatement1.setString(1, profile);
            ResultSet rs1 = preparedStatement1.executeQuery();
            while (rs1.next()) {
                allowedUsers.add(rs1.getString("user"));
            }*/

            // get trajectories
            if (LOG_ENABLED == true) {
                String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
                writeFile(properties.getProperty("log_file"), timeStamp + " - Getting trajectories (" + profile + ")\n");
            }
            PreparedStatement preparedStatement2 = conn.prepareStatement("SELECT kpi_id AS user, latitude, longitude,"
                    + " EXTRACT(YEAR FROM data_time) AS year,"
                    + " EXTRACT(MONTH FROM data_time) AS month,"
                    + " EXTRACT(DAY FROM data_time) AS day,"
                    + " UNIX_TIMESTAMP(data_time) AS unix_timestamp"
                    + " FROM kpivalues WHERE latitude >= " + Double.parseDouble(properties.getProperty("minLatitude_" + profile))
                    + " AND latitude <= " + Double.parseDouble(properties.getProperty("maxLatitude_" + profile))
                    + " AND longitude >=  " + Double.parseDouble(properties.getProperty("minLongitude_" + profile))
                    + " AND longitude <= " + Double.parseDouble(properties.getProperty("maxLongitude_" + profile))
                    + " AND kpi_id IN (SELECT id FROM kpidata WHERE value_name = ?) ORDER BY kpi_id, data_time ASC");
            preparedStatement2.setString(1, profile);
            ResultSet rs2 = preparedStatement2.executeQuery();
            while (rs2.next()) {
                /*boolean boundaries = Double.parseDouble(rs2.getString("latitude")) >= Double.parseDouble(properties.getProperty("minLatitude_" + profile));
                boundaries = boundaries & Double.parseDouble(rs2.getString("latitude")) <= Double.parseDouble(properties.getProperty("maxLatitude_" + profile));
                boundaries = boundaries & Double.parseDouble(rs2.getString("longitude")) >= Double.parseDouble(properties.getProperty("minLongitude_" + profile));
                boundaries = boundaries & Double.parseDouble(rs2.getString("longitude")) <= Double.parseDouble(properties.getProperty("maxLongitude_" + profile));
                if (!boundaries || !allowedUsers.contains(rs2.getString("user"))) {
                    continue;
                }*/
                // xy coordinates, http://stackoverflow.com/questions/1369512/converting-longitude-latitude-to-x-y-on-a-map-with-calibration-points
                // if use this, convert results to latitude/longitude in TraClusterDoc.java
                //double latitude = (MAP_HEIGHT / 180.0) * (90 - Double.parseDouble(rs2.getString("latitude")));
                //double longitude = (MAP_WIDTH / 360.0) * (180 + Double.parseDouble(rs2.getString("longitude")));
                // coordinates transformation
                //https://it.wikipedia.org/wiki/Proiezione_cilindrica_centrografica_modificata_di_Mercatore
                //https://en.wikipedia.org/wiki/Mercator_projection
                double latitude = EARTH_RADIUS * Math.log(Math.tan(Math.PI / 4 + Double.parseDouble(rs2.getString("latitude")) / 180 * Math.PI / 2));
                double longitude = Double.parseDouble(rs2.getString("longitude")) / 180 * Math.PI * EARTH_RADIUS;
                String date = rs2.getString("year") + "-" + rs2.getString("month") + "-" + rs2.getString("day");
                if (map.get(rs2.getString("user")) == null) {
                    HashMap<String, ArrayList<String[]>> tmp = new HashMap<>();
                    ArrayList<String[]> timestamp_latlon = new ArrayList<>();
                    timestamp_latlon.add(new String[]{rs2.getString("unix_timestamp"), latitude + " " + longitude});
                    tmp.put(date, timestamp_latlon);
                    map.put(rs2.getString("user"), tmp);
                } else {
                    HashMap<String, ArrayList<String[]>> tmp = map.get(rs2.getString("user"));
                    if (tmp.get(date) == null) {
                        ArrayList<String[]> timestamp_latlon = new ArrayList<>();
                        timestamp_latlon.add(new String[]{rs2.getString("unix_timestamp"), latitude + " " + longitude});
                        tmp.put(date, timestamp_latlon);
                    } else {
                        ArrayList<String[]> timestamp_latlon = tmp.get(date);
                        timestamp_latlon.add(new String[]{rs2.getString("unix_timestamp"), latitude + " " + longitude});
                        tmp.put(date, timestamp_latlon);
                    }
                    map.put(rs2.getString("user"), tmp);
                }
            }
            System.out.println(map.size() + " users found for " + profile);
            /*for (Map.Entry<String, HashMap<String, ArrayList<String[]>>> entry : map.entrySet()) {
                for (Map.Entry<String, ArrayList<String[]>> entry2 : entry.getValue().entrySet()) {
                    System.out.println(entry.getKey() + ": " + entry2.getValue().size());
                }
            }*/
            conn.close();

            // write trajectories to .tra and .js files
            if (LOG_ENABLED == true) {
                String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
                writeFile(properties.getProperty("log_file"), timeStamp + " - Writing trajectories to files (" + profile + ")\n");
            }

            String data = "";
            String neat = ""; // trajectory file for the NEAT clustering algorithm
            String trajectories_javascript = "";
            String polylines_list = "";
            String polyline_decorator_list = "";
            String polyline_decorator_pattern = "var polyline_decorator_pattern = {\n"
                    + "    patterns: [\n"
                    + "        {offset: 5, repeat: 300, symbol: L.Symbol.arrowHead({pixelSize: 10})}\n"
                    + "    ]\n"
                    + "}\n\n";
            trajectories_javascript += "// set polyline decorator pattern;\n";
            trajectories_javascript += polyline_decorator_pattern;
            int counter = 0;

            if (LOG_ENABLED == true) {
                String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
                writeFile(properties.getProperty("log_file"), timeStamp + " - Sorting trajectories (" + profile + ")\n");
            }
            Iterator it1 = map.keySet().iterator();
            while (it1.hasNext()) {
                // get the user's map
                String user = (String) it1.next();
                HashMap<String, ArrayList<String[]>> userMap = map.get(user);
                // sort the user's map by ascending date
                Map<String, ArrayList<String[]>> sortedUserMap = new TreeMap<>(userMap);
                Iterator it2 = sortedUserMap.keySet().iterator();
                while (it2.hasNext()) {
                    String pointlist = "var tpointList_" + counter + " = [";
                    // get the user's trajectories list for this date ({timestamp, latitude + " " + longitude})
                    String date = (String) it2.next();
                    ArrayList<String[]> timestamp_latlon = sortedUserMap.get(date);

                    // sort the user's trajectories list by ascending timestamp (key)
                    Collections.sort(timestamp_latlon, new Comparator<String[]>() {
                        @Override
                        public int compare(String[] t1, String[] t2) {
                            final String time1 = t1[0];
                            final String time2 = t2[0];
                            return time1.compareTo(time2);
                        }
                    });

                    if (timestamp_latlon.size() >= Integer.parseInt(properties.getProperty("minTrajectoryLength"))) {
                        for (int i = 0; i < timestamp_latlon.size(); i++) {
                            String[] timestamp_latlon_array = timestamp_latlon.get(i);
                            String[] lat_lon = timestamp_latlon_array[1].split(" ");
                            //double latitude = (90 - Double.parseDouble(lat_lon[0]) * 180 / MAP_HEIGHT);
                            //double longitude = (Double.parseDouble(lat_lon[1]) * 360 / MAP_WIDTH - 180);
                            // inverse coordinates transformation
                            //https://it.wikipedia.org/wiki/Proiezione_cilindrica_centrografica_modificata_di_Mercatore
                            //https://en.wikipedia.org/wiki/Mercator_projection
                            double latitude = (2 * Math.atan(Math.exp(Double.parseDouble(lat_lon[0]) / EARTH_RADIUS)) - Math.PI / 2) * 180 / Math.PI;
                            double longitude = Double.parseDouble(lat_lon[1]) / EARTH_RADIUS * 180 / Math.PI;
                            data += (i != 0 ? " " : counter + " " + timestamp_latlon.size() + " ") + timestamp_latlon_array[1];
                            neat += counter + " " + i + " " + timestamp_latlon_array[1] + "\n";
                            trajectories_javascript += "var tpoint_" + counter + "_" + i + " = new L.LatLng(" + latitude + "," + longitude + ");\n";
                            pointlist += (i == 0 ? "" : ",") + "tpoint_" + counter + "_" + i;
                        }
                        data += "\n";
                        trajectories_javascript += pointlist + "];\n";
                        trajectories_javascript += "var tpolyline_" + counter + " = new L.Polyline(tpointList_" + counter + ", {color: 'red', weight: 1, opacity: 0.5, smoothFactor: 1});\n";
                        trajectories_javascript += "var tpolyline_decorator_" + counter + " = L.polylineDecorator(tpolyline_" + counter + ", polyline_decorator_pattern);\n";
                        trajectories_javascript += "\n";
                        //trajectories_grouplayer += (counter == 0 ? "" : ",") + "tpolyline_" + counter;
                        polylines_list += (counter == 0 ? "" : ",") + "tpolyline_" + counter;
                        polyline_decorator_list += (counter == 0 ? "" : ",") + "tpolyline_decorator_" + counter;
                        counter++;
                    }
                }
            }
            if (LOG_ENABLED == true) {
                String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
                writeFile(properties.getProperty("log_file"), timeStamp + " - Writing trajectories to .js file (" + profile + ")\n");
            }
            if (!trajectories_javascript.equals("")) {
                trajectories_javascript += "// set the polyline array\n";
                trajectories_javascript += "var tpolyline_list = [" + polylines_list + "];\n\n";
                trajectories_javascript += "// set the polyline group layer\n";
                trajectories_javascript += "var trajectories = L.layerGroup(tpolyline_list);\n\n";
                trajectories_javascript += "// set the polyline decorator array\n";
                trajectories_javascript += "var tpolyline_decorator_list = [" + polyline_decorator_list + "];\n\n";
                trajectories_javascript += "// set the polyline decorator group layer\n";
                trajectories_javascript += "var trajectories_decorator = L.layerGroup(tpolyline_decorator_list);\n\n";
                // write the minimum length of trajectories to be clustered to javascript
                trajectories_javascript += "// write the minimum length of tracks to be clustered;\n";
                trajectories_javascript += "var minTrajectoryLength = " + properties.getProperty("minTrajectoryLength") + ";\n\n";
                // write the number of trajectories to javascript
                trajectories_javascript += "// write the minimum length of tracks to be clustered;\n";
                trajectories_javascript += "var numTrajectories = " + counter + ";\n\n";

                // write trajectories to .js
                FileWriter fstream2 = new FileWriter(properties.getProperty("javascript_path") + "trajectories_" + (profile != null ? profile : "") + ".js");
                BufferedWriter out2 = new BufferedWriter(fstream2);
                //out2.write(trajectories_javascript);
                // compress and save the javascript with yuicompressor (http://yui.github.io/yuicompressor/)
                if (LOG_ENABLED == true) {
                    String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
                    writeFile(properties.getProperty("log_file"), timeStamp + " - Compressing .js file (" + profile + ")\n");
                }
                JavaScriptCompressor compressor = new JavaScriptCompressor(new StringReader(trajectories_javascript), null);
                compressor.compress(out2, -1, true, false, true, true);
                out2.close();
            }

            // write .tra file
            FileWriter fstream = new FileWriter(properties.getProperty("trajectories_file"));
            BufferedWriter out = new BufferedWriter(fstream);
            if (LOG_ENABLED == true) {
                String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
                writeFile(properties.getProperty("log_file"), timeStamp + " - Writing trajectories to .tra file (" + profile + ")\n");
            }
            out.write("2\n"); // number of dimensions
            out.write(counter + "\n"); // number of trajectories
            out.write(data);
            out.close();

            // write .tra file for the NEAT algorithm
            /*FileWriter fstream_neat = new FileWriter(properties.getProperty("trajectories_file") + ".neat");
             BufferedWriter out_neat = new BufferedWriter(fstream_neat);
             if (LOG_ENABLED == true) {
             String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
             writeFile(properties.getProperty("log_file"), timeStamp + " - Writing trajectories to .tra.neat file (" + profile + ")\n");
             }
             out_neat.write(neat);
             out_neat.close();*/
        } catch (SQLException | IOException ex) {
            if (LOG_ENABLED == true) {
                writeFile(properties.getProperty("log_file"), ex.toString() + "\n");
            }
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // generate trajectories file from recommender's data
    public static void generateTrajectoriesPreClustered(String profile) {
        HashMap<String, HashMap<String, ArrayList<String[]>>> map = new HashMap<>(); // user => (YY-mm-dd => ArrayList of {timestamp, latitude + " " + longitude})
        try {
            Connection conn = getConnection(properties.getProperty("db_host"),
                    properties.getProperty("db_schema"),
                    properties.getProperty("db_username"),
                    properties.getProperty("db_password"));

            // get banned users
            /*ArrayList<String> bannedUsers = new ArrayList<>();
             PreparedStatement preparedStatement1 = null;
             if (profile != null) {
             preparedStatement1 = conn1.prepareStatement("SELECT user FROM recommender.users WHERE label IS NOT NULL OR profile != ?");
             preparedStatement1.setString(1, profile);
             } else {
             preparedStatement1 = conn1.prepareStatement("SELECT user FROM recommender.users WHERE label IS NOT NULL");
             }
             ResultSet rs1 = preparedStatement1.executeQuery();
             while (rs1.next()) {
             bannedUsers.add(rs1.getString("user"));
             }
             // add banned user
             bannedUsers.add("0000000011111111222222223333333344444444555555556666666677777777");*/
            // get users
            ArrayList<String> allowedUsers = new ArrayList<>();
            PreparedStatement preparedStatement1 = conn.prepareStatement("SELECT id AS user FROM kpidata WHERE value_name = ?");
            preparedStatement1.setString(1, profile);
            ResultSet rs1 = preparedStatement1.executeQuery();
            while (rs1.next()) {
                allowedUsers.add(rs1.getString("user"));
            }

            // get recommendations trajectories
            PreparedStatement preparedStatement2 = conn.prepareStatement("SELECT id AS user, "
                    + "(2*atan(exp(y/" + EARTH_RADIUS + "))-PI()/2)*180/PI() AS latitude, "
                    + "x/" + EARTH_RADIUS + "*180/PI() AS longitude, "
                    + "EXTRACT(YEAR FROM data_time) AS year, "
                    + "EXTRACT(MONTH FROM data_time) AS month, "
                    + "EXTRACT(DAY FROM data_time) AS day, "
                    + "UNIX_TIMESTAMP(data_time) AS unix_timestamp "
                    + "FROM "
                    + "(SELECT id AS user, round(longitude/180*PI()* " + EARTH_RADIUS + " / 138) * 138 AS x, "
                    + "round(" + EARTH_RADIUS + "*ln(tan(PI()/4+latitude/180*PI()/2)) / 138) * 138 AS y, "
                    + "timestamp FROM kpivalues WHERE latitude >= " + Double.parseDouble(properties.getProperty("minLatitude_" + profile))
                    + " AND latitude <= " + Double.parseDouble(properties.getProperty("maxLatitude_" + profile))
                    + "AND longitude >=" + Double.parseDouble(properties.getProperty("minLongitude_" + profile))
                    + "AND longitude <=" + Double.parseDouble(properties.getProperty("maxLongitude_" + profile))
                    + " WHERE kpi_id IN (SELECT id FROM kpidata WHERE value_name = ?)) AS a ORDER BY id, data_time ASC");
            ResultSet rs2 = preparedStatement2.executeQuery();
            while (rs2.next()) {
                /*boolean boundaries = Double.parseDouble(rs2.getString("latitude")) >= Double.parseDouble(properties.getProperty("minLatitude_" + profile));
                boundaries = boundaries & Double.parseDouble(rs2.getString("latitude")) <= Double.parseDouble(properties.getProperty("maxLatitude_" + profile));
                boundaries = boundaries & Double.parseDouble(rs2.getString("longitude")) >= Double.parseDouble(properties.getProperty("minLongitude_" + profile));
                boundaries = boundaries & Double.parseDouble(rs2.getString("longitude")) <= Double.parseDouble(properties.getProperty("maxLongitude_" + profile));
                if (!boundaries || !allowedUsers.contains(rs2.getString("user" + profile))) {
                    continue;
                }*/
                // xy coordinates, http://stackoverflow.com/questions/1369512/converting-longitude-latitude-to-x-y-on-a-map-with-calibration-points
                // if use this, convert results to latitude/longitude in TraClusterDoc.java
                //double latitude = (MAP_HEIGHT / 180.0) * (90 - Double.parseDouble(rs2.getString("latitude")));
                //double longitude = (MAP_WIDTH / 360.0) * (180 + Double.parseDouble(rs2.getString("longitude")));
                // coordinates transformation
                //https://it.wikipedia.org/wiki/Proiezione_cilindrica_centrografica_modificata_di_Mercatore
                //https://en.wikipedia.org/wiki/Mercator_projection
                double latitude = EARTH_RADIUS * Math.log(Math.tan(Math.PI / 4 + Double.parseDouble(rs2.getString("latitude")) / 180 * Math.PI / 2));
                double longitude = Double.parseDouble(rs2.getString("longitude")) / 180 * Math.PI * EARTH_RADIUS;
                String date = rs2.getString("year") + "-" + rs2.getString("month") + "-" + rs2.getString("day");
                if (map.get(rs2.getString("user")) == null) {
                    HashMap<String, ArrayList<String[]>> tmp = new HashMap<>();
                    ArrayList<String[]> timestamp_latlon = new ArrayList<>();
                    timestamp_latlon.add(new String[]{rs2.getString("unix_timestamp"), latitude + " " + longitude});
                    tmp.put(date, timestamp_latlon);
                    map.put(rs2.getString("user"), tmp);
                } else {
                    HashMap<String, ArrayList<String[]>> tmp = map.get(rs2.getString("user"));
                    if (tmp.get(date) == null) {
                        ArrayList<String[]> timestamp_latlon = new ArrayList<>();
                        timestamp_latlon.add(new String[]{rs2.getString("unix_timestamp"), latitude + " " + longitude});
                        tmp.put(date, timestamp_latlon);
                    } else {
                        ArrayList<String[]> timestamp_latlon = tmp.get(date);
                        timestamp_latlon.add(new String[]{rs2.getString("unix_timestamp"), latitude + " " + longitude});
                        tmp.put(date, timestamp_latlon);
                    }
                    map.put(rs2.getString("user"), tmp);
                }
            }
            conn.close();

            // write trajectories to .tra and .js files
            FileWriter fstream = new FileWriter(properties.getProperty("trajectories_file"));
            BufferedWriter out = new BufferedWriter(fstream);

            FileWriter fstream2 = new FileWriter(properties.getProperty("javascript_path") + "trajectories_" + (profile != null ? profile : "") + ".js");
            BufferedWriter out2 = new BufferedWriter(fstream2);

            String data = "";
            String trajectories_javascript = "";
            String polylines_list = "";
            String polyline_decorator_list = "";
            String polyline_decorator_pattern = "var polyline_decorator_pattern = {\n"
                    + "    patterns: [\n"
                    + "        {offset: 5, repeat: 300, symbol: L.Symbol.arrowHead({pixelSize: 10})}\n"
                    + "    ]\n"
                    + "}\n\n";
            trajectories_javascript += "// set polyline decorator pattern;\n";
            trajectories_javascript += polyline_decorator_pattern;
            int counter = 0;

            Iterator it1 = map.keySet().iterator();
            while (it1.hasNext()) {
                // get the user's map
                String user = (String) it1.next();
                HashMap<String, ArrayList<String[]>> userMap = map.get(user);
                // sort the user's map by ascending date
                Map<String, ArrayList<String[]>> sortedUserMap = new TreeMap<>(userMap);
                Iterator it2 = sortedUserMap.keySet().iterator();
                while (it2.hasNext()) {
                    String pointlist = "var tpointList_" + counter + " = [";
                    // get the user's trajectories list for this date ({timestamp, latitude + " " + longitude})
                    String date = (String) it2.next();
                    ArrayList<String[]> timestamp_latlon = sortedUserMap.get(date);

                    // sort the user's trajectories list by ascending timestamp (key)
                    Collections.sort(timestamp_latlon, new Comparator<String[]>() {
                        @Override
                        public int compare(String[] t1, String[] t2) {
                            final String time1 = t1[0];
                            final String time2 = t2[0];
                            return time1.compareTo(time2);
                        }
                    });

                    if (timestamp_latlon.size() >= Integer.parseInt(properties.getProperty("minTrajectoryLength"))) {
                        for (int i = 0; i < timestamp_latlon.size(); i++) {
                            String[] timestamp_latlon_array = timestamp_latlon.get(i);
                            String[] lat_lon = timestamp_latlon_array[1].split(" ");
                            //double latitude = (90 - Double.parseDouble(lat_lon[0]) * 180 / MAP_HEIGHT);
                            //double longitude = (Double.parseDouble(lat_lon[1]) * 360 / MAP_WIDTH - 180);
                            // inverse coordinates transformation
                            //https://it.wikipedia.org/wiki/Proiezione_cilindrica_centrografica_modificata_di_Mercatore
                            //https://en.wikipedia.org/wiki/Mercator_projection
                            double latitude = (2 * Math.atan(Math.exp(Double.parseDouble(lat_lon[0]) / EARTH_RADIUS)) - Math.PI / 2) * 180 / Math.PI;
                            double longitude = Double.parseDouble(lat_lon[1]) / EARTH_RADIUS * 180 / Math.PI;
                            data += (i != 0 ? " " : counter + " " + timestamp_latlon.size() + " ") + timestamp_latlon_array[1];
                            trajectories_javascript += "var tpoint_" + counter + "_" + i + " = new L.LatLng(" + latitude + "," + longitude + ");\n";
                            pointlist += (i == 0 ? "" : ",") + "tpoint_" + counter + "_" + i;
                        }
                        data += "\n";
                        trajectories_javascript += pointlist + "];\n";
                        trajectories_javascript += "var tpolyline_" + counter + " = new L.Polyline(tpointList_" + counter + ", {color: 'red', weight: 1, opacity: 0.5, smoothFactor: 1});\n";
                        trajectories_javascript += "var tpolyline_decorator_" + counter + " = L.polylineDecorator(tpolyline_" + counter + ", polyline_decorator_pattern);\n";
                        trajectories_javascript += "\n";
                        //trajectories_grouplayer += (counter == 0 ? "" : ",") + "tpolyline_" + counter;
                        polylines_list += (counter == 0 ? "" : ",") + "tpolyline_" + counter;
                        polyline_decorator_list += (counter == 0 ? "" : ",") + "tpolyline_decorator_" + counter;
                        counter++;
                    }
                }
            }
            if (!trajectories_javascript.equals("")) {
                trajectories_javascript += "// set the polyline array\n";
                trajectories_javascript += "var tpolyline_list = [" + polylines_list + "];\n\n";
                trajectories_javascript += "// set the polyline group layer\n";
                trajectories_javascript += "var trajectories = L.layerGroup(tpolyline_list);\n\n";
                trajectories_javascript += "// set the polyline decorator array\n";
                trajectories_javascript += "var tpolyline_decorator_list = [" + polyline_decorator_list + "];\n\n";
                trajectories_javascript += "// set the polyline decorator group layer\n";
                trajectories_javascript += "var trajectories_decorator = L.layerGroup(tpolyline_decorator_list);\n\n";
                // write the minimum length of trajectories to be clustered to javascript
                trajectories_javascript += "// write the minimum length of tracks to be clustered;\n";
                trajectories_javascript += "var minTrajectoryLength = " + properties.getProperty("minTrajectoryLength") + ";\n\n";
                // write the number of trajectories to javascript
                trajectories_javascript += "// write the minimum length of tracks to be clustered;\n";
                trajectories_javascript += "var numTrajectories = " + counter + ";\n\n";
                //out2.write(trajectories_javascript);
                // compress and save the javascript with yuicompressor (http://yui.github.io/yuicompressor/)
                JavaScriptCompressor compressor = new JavaScriptCompressor(new StringReader(trajectories_javascript), null);
                compressor.compress(out2, -1, true, false, true, true);
                out2.close();
            }

            out.write("2\n"); // number of dimensions
            out.write(counter + "\n"); // number of trajectories
            out.write(data);
            out.close();
        } catch (SQLException | IOException ex) {
            if (LOG_ENABLED == true) {
                writeFile(properties.getProperty("log_file"), ex.toString() + "\n");
            }
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void addToMap(HashMap<String, HashMap<String, ArrayList<String>>> map, ResultSet rs, ArrayList<String> bannedUsers) {
        try {
            if (bannedUsers.contains(rs.getString("user"))) {
                return;
            }
            // xy coordinates, http://stackoverflow.com/questions/1369512/converting-longitude-latitude-to-x-y-on-a-map-with-calibration-points
            // if use this, convert results to latitude/longitude in TraClusterDoc.java
            double latitude = (MAP_HEIGHT / 180.0) * (90 - Double.parseDouble(rs.getString("latitude")));
            double longitude = (MAP_WIDTH / 360.0) * (180 + Double.parseDouble(rs.getString("longitude")));
            String date = rs.getString("year") + "-" + rs.getString("month") + "-" + rs.getString("day");
            if (map.get(rs.getString("user")) == null) {
                HashMap<String, ArrayList<String>> tmp = new HashMap<>();
                ArrayList<String> timestamp_latlon = new ArrayList<>();
                timestamp_latlon.add(latitude + " " + longitude);
                tmp.put(date, timestamp_latlon);
                map.put(rs.getString("user"), tmp);
            } else {
                HashMap<String, ArrayList<String>> tmp = map.get(rs.getString("user"));
                if (tmp.get(date) == null) {
                    ArrayList<String> timestamp_latlon = new ArrayList<>();
                    timestamp_latlon.add(latitude + " " + longitude);
                    tmp.put(date, timestamp_latlon);
                } else {
                    ArrayList<String> timestamp_latlon = tmp.get(date);
                    timestamp_latlon.add(latitude + " " + longitude);
                    tmp.put(date, timestamp_latlon);
                }
                map.put(rs.getString("user"), tmp);
            }
        } catch (SQLException ex) {
            if (LOG_ENABLED == true) {
                writeFile(properties.getProperty("log_file"), ex.toString() + "\n");
            }
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void addToMap(HashMap<String, HashMap<String, ArrayList<String>>> map, ResultSet rs1, ResultSet rs2, boolean userMatch, ArrayList<String> bannedUsers) {
        try {
            // xy coordinates, http://stackoverflow.com/questions/1369512/converting-longitude-latitude-to-x-y-on-a-map-with-calibration-points
            // if use this, convert results to latitude/longitude in TraClusterDoc.java
            double latitude1 = (MAP_HEIGHT / 180.0) * (90 - Double.parseDouble(rs1.getString("latitude")));
            double longitude1 = (MAP_WIDTH / 360.0) * (180 + Double.parseDouble(rs1.getString("longitude")));
            double latitude2 = (MAP_HEIGHT / 180.0) * (90 - Double.parseDouble(rs2.getString("latitude")));
            double longitude2 = (MAP_WIDTH / 360.0) * (180 + Double.parseDouble(rs2.getString("longitude")));
            String date1 = rs1.getString("year") + "-" + rs1.getString("month") + "-" + rs1.getString("day");
            String date2 = rs2.getString("year") + "-" + rs2.getString("month") + "-" + rs2.getString("day");
            // users are the same and not banned
            if (userMatch && !bannedUsers.contains(rs1.getString("user"))) {
                String timestamp1 = rs1.getString("unix_timestamp");
                String timestamp2 = rs2.getString("unix_timestamp");
                if (map.get(rs1.getString("user")) == null) {
                    HashMap<String, ArrayList<String>> tmp = new HashMap<>();
                    ArrayList<String> latlon = new ArrayList<>();
                    if (timestamp1.compareTo(timestamp2) < 0) {
                        latlon.add(latitude1 + " " + longitude1);
                        latlon.add(latitude2 + " " + longitude2);
                    } else {
                        latlon.add(latitude2 + " " + longitude2);
                        latlon.add(latitude1 + " " + longitude1);
                    }
                    tmp.put(date1, latlon);
                    map.put(rs1.getString("user"), tmp);
                } else {
                    HashMap<String, ArrayList<String>> tmp = map.get(rs1.getString("user"));
                    if (tmp.get(date1) == null) {
                        ArrayList<String> latlon = new ArrayList<>();
                        if (timestamp1.compareTo(timestamp2) < 0) {
                            latlon.add(latitude1 + " " + longitude1);
                            latlon.add(latitude2 + " " + longitude2);
                        } else {
                            latlon.add(latitude2 + " " + longitude2);
                            latlon.add(latitude1 + " " + longitude1);
                        }
                        tmp.put(date1, latlon);
                    } else {
                        ArrayList<String> latlon = tmp.get(date1);
                        if (timestamp1.compareTo(timestamp2) < 0) {
                            latlon.add(latitude1 + " " + longitude1);
                            latlon.add(latitude2 + " " + longitude2);
                        } else {
                            latlon.add(latitude2 + " " + longitude2);
                            latlon.add(latitude1 + " " + longitude1);
                        }
                        tmp.put(date1, latlon);
                    }
                    map.put(rs1.getString("user"), tmp);
                }
            } // users are not the same
            else {
                if (!bannedUsers.contains(rs1.getString("user"))) {
                    if (map.get(rs1.getString("user")) == null) {
                        HashMap<String, ArrayList<String>> tmp = new HashMap<>();
                        ArrayList<String> latlon = new ArrayList<>();
                        latlon.add(latitude1 + " " + longitude1);
                        tmp.put(date1, latlon);
                        map.put(rs1.getString("user"), tmp);
                    } else {
                        HashMap<String, ArrayList<String>> tmp = map.get(rs1.getString("user"));
                        if (tmp.get(date1) == null) {
                            ArrayList<String> latlon = new ArrayList<>();
                            latlon.add(latitude1 + " " + longitude1);
                            tmp.put(date1, latlon);
                        } else {
                            ArrayList<String> latlon = tmp.get(date1);
                            latlon.add(latitude1 + " " + longitude1);
                            tmp.put(date1, latlon);
                        }
                        map.put(rs1.getString("user"), tmp);
                    }
                }
                if (!bannedUsers.contains(rs2.getString("user"))) {
                    if (map.get(rs2.getString("user")) == null) {
                        HashMap<String, ArrayList<String>> tmp = new HashMap<>();
                        ArrayList<String> latlon = new ArrayList<>();
                        latlon.add(latitude2 + " " + longitude2);
                        tmp.put(date2, latlon);
                        map.put(rs2.getString("user"), tmp);
                    } else {
                        HashMap<String, ArrayList<String>> tmp = map.get(rs2.getString("user"));
                        if (tmp.get(date2) == null) {
                            ArrayList<String> latlon = new ArrayList<>();
                            latlon.add(latitude2 + " " + longitude2);
                            tmp.put(date2, latlon);
                        } else {
                            ArrayList<String> latlon = tmp.get(date2);
                            latlon.add(latitude2 + " " + longitude2);
                            tmp.put(date2, latlon);
                        }
                        map.put(rs2.getString("user"), tmp);
                    }
                }
            }
        } catch (SQLException ex) {
            if (LOG_ENABLED == true) {
                writeFile(properties.getProperty("log_file"), ex.toString() + "\n");
            }
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //get the database connection (used by other classes too)
    public static Connection getConnection(String hostname, String schema, String userName, String password) {
        try {
            //if (connectionPool == null) {
            connectionPool = new ConnectionPool("jdbc:mysql://" + hostname + "/" + schema, userName, password);
            //if (dataSource == null) {
            dataSource = connectionPool.setUp();
            //}
            //}
            return dataSource.getConnection();
        } catch (IOException | SQLException ex) {
            if (LOG_ENABLED == true) {
                writeFile(properties.getProperty("log_file"), ex.toString() + "\n");
            }
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (Exception ex) {
            if (LOG_ENABLED == true) {
                writeFile(properties.getProperty("log_file"), ex.toString() + "\n");
            }
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    // convert decimal coordinates to cartesian
    public static double[] toCartesian(double[] coordinates) {
        int R = EARTH_RADIUS / 1000; // earth radius in km
        double[] cartesian = new double[3];
        double[] coordinates_radians = new double[]{Math.toRadians(coordinates[0]), Math.toRadians(coordinates[1])};
        cartesian[0] = R * Math.cos(coordinates_radians[0]) * Math.cos(coordinates_radians[1]);
        cartesian[1] = R * Math.cos(coordinates_radians[0]) * Math.sin(coordinates_radians[1]);
        cartesian[2] = R * Math.sin(coordinates_radians[0]);
        return cartesian;
    }

    // convert cartesian coordinates to decimal
    public static double[] toDecimal(double[] coordinates) {
        int R = EARTH_RADIUS / 1000; // earth radius in km
        double[] decimal = new double[2];
        decimal[0] = Math.toDegrees(Math.asin(coordinates[2] / R));
        decimal[1] = Math.toDegrees(Math.atan2(coordinates[1], coordinates[0]));
        return decimal;
    }

    // not used
    public static double[] Deg2UTM(double Lat, double Lon) {
        double Easting;
        double Northing;
        int Zone;
        char Letter;
        Zone = (int) Math.floor(Lon / 6 + 31);
        if (Lat < -72) {
            Letter = 'C';
        } else if (Lat < -64) {
            Letter = 'D';
        } else if (Lat < -56) {
            Letter = 'E';
        } else if (Lat < -48) {
            Letter = 'F';
        } else if (Lat < -40) {
            Letter = 'G';
        } else if (Lat < -32) {
            Letter = 'H';
        } else if (Lat < -24) {
            Letter = 'J';
        } else if (Lat < -16) {
            Letter = 'K';
        } else if (Lat < -8) {
            Letter = 'L';
        } else if (Lat < 0) {
            Letter = 'M';
        } else if (Lat < 8) {
            Letter = 'N';
        } else if (Lat < 16) {
            Letter = 'P';
        } else if (Lat < 24) {
            Letter = 'Q';
        } else if (Lat < 32) {
            Letter = 'R';
        } else if (Lat < 40) {
            Letter = 'S';
        } else if (Lat < 48) {
            Letter = 'T';
        } else if (Lat < 56) {
            Letter = 'U';
        } else if (Lat < 64) {
            Letter = 'V';
        } else if (Lat < 72) {
            Letter = 'W';
        } else {
            Letter = 'X';
        }
        Easting = 0.5 * Math.log((1 + Math.cos(Lat * Math.PI / 180) * Math.sin(Lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)) / (1 - Math.cos(Lat * Math.PI / 180) * Math.sin(Lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) * 0.9996 * 6399593.62 / Math.pow((1 + Math.pow(0.0820944379, 2) * Math.pow(Math.cos(Lat * Math.PI / 180), 2)), 0.5) * (1 + Math.pow(0.0820944379, 2) / 2 * Math.pow((0.5 * Math.log((1 + Math.cos(Lat * Math.PI / 180) * Math.sin(Lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)) / (1 - Math.cos(Lat * Math.PI / 180) * Math.sin(Lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)))), 2) * Math.pow(Math.cos(Lat * Math.PI / 180), 2) / 3) + 500000;
        Easting = Math.round(Easting * 100) * 0.01;
        Northing = (Math.atan(Math.tan(Lat * Math.PI / 180) / Math.cos((Lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) - Lat * Math.PI / 180) * 0.9996 * 6399593.625 / Math.sqrt(1 + 0.006739496742 * Math.pow(Math.cos(Lat * Math.PI / 180), 2)) * (1 + 0.006739496742 / 2 * Math.pow(0.5 * Math.log((1 + Math.cos(Lat * Math.PI / 180) * Math.sin((Lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) / (1 - Math.cos(Lat * Math.PI / 180) * Math.sin((Lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)))), 2) * Math.pow(Math.cos(Lat * Math.PI / 180), 2)) + 0.9996 * 6399593.625 * (Lat * Math.PI / 180 - 0.005054622556 * (Lat * Math.PI / 180 + Math.sin(2 * Lat * Math.PI / 180) / 2) + 4.258201531e-05 * (3 * (Lat * Math.PI / 180 + Math.sin(2 * Lat * Math.PI / 180) / 2) + Math.sin(2 * Lat * Math.PI / 180) * Math.pow(Math.cos(Lat * Math.PI / 180), 2)) / 4 - 1.674057895e-07 * (5 * (3 * (Lat * Math.PI / 180 + Math.sin(2 * Lat * Math.PI / 180) / 2) + Math.sin(2 * Lat * Math.PI / 180) * Math.pow(Math.cos(Lat * Math.PI / 180), 2)) / 4 + Math.sin(2 * Lat * Math.PI / 180) * Math.pow(Math.cos(Lat * Math.PI / 180), 2) * Math.pow(Math.cos(Lat * Math.PI / 180), 2)) / 3);
        if (Letter < 'M') {
            Northing = Northing + 10000000;
        }
        Northing = Math.round(Northing * 100) * 0.01;
        return new double[]{Zone, Letter, Easting, Northing};
    }

    // not used
    public static double[] UTM2Deg(String UTM) {
        double latitude;
        double longitude;
        String[] parts = UTM.split(" ");
        int Zone = Integer.parseInt(parts[0]);
        char Letter = parts[1].toUpperCase(Locale.ENGLISH).charAt(0);
        double Easting = Double.parseDouble(parts[2]);
        double Northing = Double.parseDouble(parts[3]);
        double Hem;
        if (Letter > 'M') {
            Hem = 'N';
        } else {
            Hem = 'S';
        }
        double north;
        if (Hem == 'S') {
            north = Northing - 10000000;
        } else {
            north = Northing;
        }
        latitude = (north / 6366197.724 / 0.9996 + (1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) - 0.006739496742 * Math.sin(north / 6366197.724 / 0.9996) * Math.cos(north / 6366197.724 / 0.9996) * (Math.atan(Math.cos(Math.atan((Math.exp((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3)) - Math.exp(-(Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))) / 2 / Math.cos((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996))) * Math.tan((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996)) - north / 6366197.724 / 0.9996) * 3 / 2) * (Math.atan(Math.cos(Math.atan((Math.exp((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3)) - Math.exp(-(Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))) / 2 / Math.cos((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996))) * Math.tan((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996)) - north / 6366197.724 / 0.9996)) * 180 / Math.PI;
        latitude = Math.round(latitude * 10000000);
        latitude = latitude / 10000000;
        longitude = Math.atan((Math.exp((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3)) - Math.exp(-(Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))) / 2 / Math.cos((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((Easting - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996)) * 180 / Math.PI + Zone * 6 - 183;
        longitude = Math.round(longitude * 10000000);
        longitude = longitude / 10000000;
        return new double[]{latitude, longitude};
    }

    public static void writeFile(String file, String log) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileOutputStream(new File(file), true));
            writer.println(log);
            writer.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
