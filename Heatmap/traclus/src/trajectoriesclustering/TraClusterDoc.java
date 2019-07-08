package trajectoriesclustering;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import static trajectoriesclustering.Main.properties;
import static trajectoriesclustering.Main.EARTH_RADIUS;
import static trajectoriesclustering.Main.LOG_ENABLED;
import static trajectoriesclustering.Main.getConnection;
import static trajectoriesclustering.Main.properties;
import static trajectoriesclustering.Main.writeFile;

public class TraClusterDoc {

    public int m_nDimensions;
    public int m_nTrajectories;
    public int m_nClusters;
    public double m_clusterRatio;
    public int m_maxNPoints;
    public ArrayList<Trajectory> m_trajectoryList;
    public ArrayList<Cluster> m_clusterList;
    // Daniele
    public String profile;

    public TraClusterDoc() {
        m_nTrajectories = 0;
        m_nClusters = 0;
        m_clusterRatio = 0.0;
        m_trajectoryList = new ArrayList<Trajectory>();
        m_clusterList = new ArrayList<Cluster>();
    }

    public class Parameter {

        double epsParam;
        int minLnsParam;
    }

    boolean onOpenDocument(String inputFileName) {
        int nDimensions = 2;		// default dimension = 2
        int nTrajectories = 0;
        int nTotalPoints = 0;		//no use
        int trajectoryId;
        int nPoints;
        double value;

        DataInputStream in;
        BufferedReader inBuffer = null;
        try {
            in = new DataInputStream(new BufferedInputStream(
                    new FileInputStream(inputFileName)));

            inBuffer = new BufferedReader(
                    new InputStreamReader(in));

            nDimensions = Integer.parseInt(inBuffer.readLine());			// the number of dimensions
            m_nDimensions = nDimensions;
            nTrajectories = Integer.parseInt(inBuffer.readLine());		// the number of trajectories
            m_nTrajectories = nTrajectories;

            m_maxNPoints = -1;		// initialize for comparison

            // the trajectory Id, the number of points, the coordinate of a point ...
            for (int i = 0; i < nTrajectories; i++) {

                String str = inBuffer.readLine();

                Scanner sc = new Scanner(str);
                sc.useLocale(Locale.US);

                trajectoryId = sc.nextInt();		//trajectoryID
                nPoints = sc.nextInt();				//nubmer of points in the trajectory

                if (nPoints > m_maxNPoints) {
                    m_maxNPoints = nPoints;
                }
                nTotalPoints += nPoints;

                Trajectory pTrajectoryItem = new Trajectory(trajectoryId, nDimensions);

                for (int j = 0; j < nPoints; j++) {

                    CMDPoint point = new CMDPoint(nDimensions);   // initialize the CMDPoint class for each point

                    for (int k = 0; k < nDimensions; k++) {
                        value = sc.nextDouble();
                        point.setM_coordinate(k, value);
                    }

                    pTrajectoryItem.addPointToArray(point);
                }

                m_trajectoryList.add(pTrajectoryItem);

//				for(int m=0; m<pTrajectoryItem.getM_pointArray().size();m++) {
//					System.out.print(pTrajectoryItem.getM_pointArray().get(m).getM_coordinate(0)+" ");
//				}
//				System.out.println();
            }

//			System.out.println(m_nDimensions+"haha"+m_nTrajectories);
//			System.out.println(inBuffer.readLine());
        } catch (FileNotFoundException ex) {
            if (LOG_ENABLED == true) {
                writeFile(properties.getProperty("log_file"), ex.toString() + "\n");
            }
            Logger.getLogger(TraClusterDoc.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Unable to open input file");
        } catch (NumberFormatException | IOException ex) {
            if (LOG_ENABLED == true) {
                writeFile(properties.getProperty("log_file"), ex.toString() + "\n");
            }
            Logger.getLogger(TraClusterDoc.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                inBuffer.close();
            } catch (IOException ex) {
                Logger.getLogger(TraClusterDoc.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return true;
    }

    boolean onClusterGenerate(String clusterFileName, double epsParam, int minLnsParam) {
//////////////////////////////////////////////////still to be written

        ClusterGen generator = new ClusterGen(this);

        if (m_nTrajectories == 0) {
            System.out.println("Load a trajectory data set first");
        }

        // FIRST STEP: Trajectory Partitioning
        if (!generator.partitionTrajectory()) {
            System.out.println("Unable to partition a trajectory\n");
            return false;
        }

        // SECOND STEP: Density-based Clustering
        if (!generator.performDBSCAN(epsParam, minLnsParam)) {
            System.out.println("Unable to perform the DBSCAN algorithm\n");
            return false;
        }

        // THIRD STEP: Cluster Construction
        if (!generator.constructCluster()) {
            System.out.println("Unable to construct a cluster\n");
            return false;
        }

        for (int i = 0; i < m_clusterList.size(); i++) {
            //m_clusterList.
            System.out.println(m_clusterList.get(i).getM_clusterId());
            for (int j = 0; j < m_clusterList.get(i).getM_PointArray().size(); j++) {

                double x = m_clusterList.get(i).getM_PointArray().get(j).getM_coordinate(0);
                double y = m_clusterList.get(i).getM_PointArray().get(j).getM_coordinate(1);
                // convert xy coordinates to latitude/longitude, use this if coordinates are calculated with
                // double x = (double) (10000 / 180.0) * (90 - Double.parseDouble(rs2.getString("latitude")));
                // double y = (double) (10000 / 360.0) * (180 + Double.parseDouble(rs2.getString("longitude")));
                // in file Main.java
                //System.out.print("   " + (90 - x * 180 / MAP_WIDTH) + " " + (y * 360 / MAP_HEIGHT - 180) + "   ");
                //System.out.print("   " + (2 * Math.atan(Math.exp(x / EARTH_RADIUS)) - Math.PI / 2) * 180 / Math.PI + " " + y / EARTH_RADIUS * 180 / Math.PI + "   ");
                //System.out.print("   " + x + " " + y + "   ");
            }
            System.out.println();
        }
        FileOutputStream fos = null;
        BufferedWriter bw = null;
        OutputStreamWriter osw = null;
        try {
            fos = new FileOutputStream(clusterFileName);
            osw = new OutputStreamWriter(fos);
            bw = new BufferedWriter(osw);

            bw.write("epsParam:" + epsParam + "   minLnsParam:" + minLnsParam);

            for (int i = 0; i < m_clusterList.size(); i++) {
                //m_clusterList.
                //System.out.println(m_clusterList.get(i).getM_clusterId());
                bw.write("\nclusterID: " + m_clusterList.get(i).getM_clusterId() + "  Points Number:  " + m_clusterList.get(i).getM_PointArray().size() + "\n");
                for (int j = 0; j < m_clusterList.get(i).getM_PointArray().size(); j++) {

                    double x = m_clusterList.get(i).getM_PointArray().get(j).getM_coordinate(0);
                    double y = m_clusterList.get(i).getM_PointArray().get(j).getM_coordinate(1);
                    //System.out.print("   "+ x +" "+ y +"   ");
                    bw.write(x + " " + y + "   ");
                }
                //System.out.println();
            }

        } catch (FileNotFoundException ex) {
            if (LOG_ENABLED == true) {
                writeFile(properties.getProperty("log_file"), ex.toString() + "\n");
            }
            Logger.getLogger(TraClusterDoc.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            if (LOG_ENABLED == true) {
                writeFile(properties.getProperty("log_file"), ex.toString() + "\n");
            }
            Logger.getLogger(TraClusterDoc.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(TraClusterDoc.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return true;
    }

    // Daniele: same method as onClusterGenerate but this generates clusters in a javascript file to be used by php with the leaflet plugin
    boolean onClusterGenerateJavascript(String clusterFileName, double epsParam, int minLnsParam) {
        ClusterGen generator = new ClusterGen(this);

        if (m_nTrajectories == 0) {
            System.out.println("Load a trajectory data set first");
        }

        // FIRST STEP: Trajectory Partitioning
        if (!generator.partitionTrajectory()) {
            System.out.println("Unable to partition a trajectory\n");
            return false;
        }

        // SECOND STEP: Density-based Clustering
        if (!generator.performDBSCAN(epsParam, minLnsParam)) {
            System.out.println("Unable to perform the DBSCAN algorithm\n");
            return false;
        }

        // THIRD STEP: Cluster Construction
        if (!generator.constructCluster()) {
            System.out.println("Unable to construct a cluster\n");
            return false;
        }

        for (int i = 0; i < m_clusterList.size(); i++) {
            //m_clusterList.
            System.out.println(m_clusterList.get(i).getM_clusterId());
            System.out.println("#: " + m_clusterList.get(i).getDensity());
            for (int j = 0; j < m_clusterList.get(i).getM_PointArray().size(); j++) {

                double x = m_clusterList.get(i).getM_PointArray().get(j).getM_coordinate(0);
                double y = m_clusterList.get(i).getM_PointArray().get(j).getM_coordinate(1);
                // convert xy coordinates to latitude/longitude, use this if coordinates are calculated with
                // double x = (double) (10000 / 180.0) * (90 - Double.parseDouble(rs2.getString("latitude")));
                // double y = (double) (10000 / 360.0) * (180 + Double.parseDouble(rs2.getString("longitude")));
                // in file Main.java
                //System.out.print("   " + (90 - x * 180 / MAP_HEIGHT) + " " + (y * 360 / MAP_WIDTH - 180) + "   ");
                // inverse coordinates transformation
                System.out.print("   " + (2 * Math.atan(Math.exp(x / EARTH_RADIUS)) - Math.PI / 2) * 180 / Math.PI + " " + y / EARTH_RADIUS * 180 / Math.PI + "   ");
                //System.out.print("   " + x + " " + y + "   ");
            }
            System.out.println();
        }
        FileOutputStream fos = null;
        BufferedWriter bw = null;
        OutputStreamWriter osw = null;
        Connection conn = null; // Daniele
        try {
            fos = new FileOutputStream(clusterFileName);
            osw = new OutputStreamWriter(fos);
            bw = new BufferedWriter(osw);
            //bw.write("epsParam:" + epsParam + "   minLnsParam:" + minLnsParam);

            String trajectories_javascript = "";
            //String trajectories_grouplayer = "var clusteredTrajectories = L.layerGroup([";
            String polylines_list = "";
            String circle_marker_options = "var circleMarkerOptions = {\n"
                    + "    radius: 4,\n"
                    + "    fillColor: \"#ff7800\",\n"
                    + "    color: \"#000\",\n"
                    + "    weight: 1,\n"
                    + "    opacity: 1,\n"
                    + "    fillOpacity: 0.8\n"
                    + "};\n";
            // get trajectories of a cluster, javascript function string
            String cluster_trajectories = "function getTrajectory($id, $profile) {\n"
                    + " $.post('getTrajectory.php', {id: $id, profile:  $profile}, function(data) {\n"
                    + "        if(clusterTrajectories!=\"\"){\n"
                    + "        map.removeLayer(clusterTrajectories);}\n"
                    + "        clusterTrajectories = eval(data);\n"
                    + "        map.addLayer(clusterTrajectories);\n"
                    + "});\n"
                    + "}\n\n";
            trajectories_javascript += cluster_trajectories;

            // delete old clustered trajectories and trajectories in database for this profile
            conn = getConnection(properties.getProperty("db_host_w"),
                    properties.getProperty("db_schema_w"),
                    properties.getProperty("db_username_w"),
                    properties.getProperty("db_password_w"));
            PreparedStatement preparedStatement = null;
            preparedStatement = conn.prepareStatement("DELETE FROM clustered_trajectories WHERE profile = ?");
            preparedStatement.setString(1, profile);
            preparedStatement.executeUpdate();
            preparedStatement = conn.prepareStatement("DELETE FROM trajectories WHERE profile = ?");
            preparedStatement.setString(1, profile);
            preparedStatement.executeUpdate();

            for (int i = 0; i < m_clusterList.size(); i++) {
                String clustered_trajectories = "";
                trajectories_javascript += "// trajectory " + i + "\n";
                String pointlist = "var pointList_" + i + " = [";
                String circlemarkersList = "";
                //bw.write("\nclusterID: " + m_clusterList.get(i).getM_clusterId() + "  Points Number:  " + m_clusterList.get(i).getM_PointArray().size() + "\n");
                for (int j = 0; j < m_clusterList.get(i).getM_PointArray().size(); j++) {
                    double x = m_clusterList.get(i).getM_PointArray().get(j).getM_coordinate(0);
                    double y = m_clusterList.get(i).getM_PointArray().get(j).getM_coordinate(1);
                    // convert the coordinates into decimal degrees
                    //x = (90 - x * 180 / MAP_HEIGHT);
                    //y = (y * 360 / MAP_WIDTH - 180);
                    // inverse coordinates transformation
                    //https://it.wikipedia.org/wiki/Proiezione_cilindrica_centrografica_modificata_di_Mercatore
                    //https://en.wikipedia.org/wiki/Mercator_projection
                    x = (2 * Math.atan(Math.exp(x / EARTH_RADIUS)) - Math.PI / 2) * 180 / Math.PI;
                    y = y / EARTH_RADIUS * 180 / Math.PI;
                    //System.out.print("   "+ x +" "+ y +"   ");
                    //bw.write(x + " " + y + "   ");
                    trajectories_javascript += "var point_" + i + "_" + j + " = new L.LatLng(" + x + "," + y + ");\n";
                    pointlist += (j == 0 ? "" : ",") + "point_" + i + "_" + j;
                    circlemarkersList += ",L.circleMarker(point_" + i + "_" + j + ", circleMarkerOptions)";

                    // write cluster point to string
                    clustered_trajectories += x + " " + y + ";";
                }
                ArrayList<Integer> trajectoryIds = m_clusterList.get(i).getTrajectoriesIdList();
                trajectories_javascript += pointlist + "];\n";
                trajectories_javascript += "var polyline_" + i + " = L.layerGroup([new L.Polyline(pointList_" + i + ", {color: 'blue', weight: " + Math.min(8, m_clusterList.get(i).getDensity()) + ", opacity: 0.5, smoothFactor: 1, className: '" + m_clusterList.get(i).getDensity() + "'}).on('click', function(){getTrajectory(" + m_clusterList.get(i).getM_clusterId() + ",'" + profile + "');}).bindLabel('Cluster Id: " + m_clusterList.get(i).getM_clusterId() + "<br># trajectories: " + m_clusterList.get(i).getDensity() + "')" + circlemarkersList + "]);\n";
                polylines_list += (i == 0 ? "" : ",") + "polyline_" + i;
                trajectories_javascript += "\n";

                // insert trajectories for this profile in the database
                for (int trajectory_id : trajectoryIds) {
                    Trajectory trajectory = m_trajectoryList.get(trajectory_id);
                    String trajectory_points = "";
                    for (CMDPoint point : trajectory.getM_pointArray()) {
                        // inverse coordinates transformation
                        //https://it.wikipedia.org/wiki/Proiezione_cilindrica_centrografica_modificata_di_Mercatore
                        //https://en.wikipedia.org/wiki/Mercator_projection
                        double lat = (2 * Math.atan(Math.exp(point.getM_coordinate(0) / EARTH_RADIUS)) - Math.PI / 2) * 180 / Math.PI;
                        double lon = point.getM_coordinate(1) / EARTH_RADIUS * 180 / Math.PI;
                        trajectory_points += lat + " " + lon + ";";
                    }
                    if (profile != null) {
                        preparedStatement = conn.prepareStatement("INSERT INTO trajectories SET trajectory_id = ?, cluster_id = ?, trajectory = ?, profile = ?");
                        preparedStatement.setInt(1, trajectory_id);
                        preparedStatement.setInt(2, m_clusterList.get(i).getM_clusterId());
                        preparedStatement.setString(3, trajectory_points.substring(0, trajectory_points.length() - 1));
                        preparedStatement.setString(4, profile);
                    } else {
                        preparedStatement = conn.prepareStatement("INSERT INTO trajectories SET trajectory_id = ?, cluster_id = ?, trajectory = ?");
                        preparedStatement.setInt(1, trajectory_id);
                        preparedStatement.setInt(2, m_clusterList.get(i).getM_clusterId());
                        preparedStatement.setString(3, trajectory_points.substring(0, trajectory_points.length() - 1));
                    }
                    preparedStatement.executeUpdate();
                }
                // insert clustered trajectory for this profile in the database
                if (profile != null) {
                    preparedStatement = conn.prepareStatement("INSERT INTO clustered_trajectories SET cluster_id = ?, cluster_size = ?, trajectory = ?, profile = ?");
                    preparedStatement.setInt(1, m_clusterList.get(i).getM_clusterId());
                    preparedStatement.setInt(2, m_clusterList.get(i).getDensity());
                    preparedStatement.setString(3, clustered_trajectories.substring(0, clustered_trajectories.length() - 1));
                    preparedStatement.setString(4, profile);
                } else {
                    preparedStatement = conn.prepareStatement("INSERT INTO clustered_trajectories SET cluster_id = ?, cluster_size = ?, trajectory = ?");
                    preparedStatement.setInt(1, m_clusterList.get(i).getM_clusterId());
                    preparedStatement.setInt(2, m_clusterList.get(i).getDensity());
                    preparedStatement.setString(3, clustered_trajectories.substring(0, clustered_trajectories.length() - 1));
                }
                preparedStatement.executeUpdate();
            }
            if (!trajectories_javascript.equals("")) {
                trajectories_javascript = circle_marker_options + trajectories_javascript;
                trajectories_javascript += "// set the polyline array\n";
                trajectories_javascript += "var polyline_list = [" + polylines_list + "];\n\n";
                trajectories_javascript += "// set the temp polyline array\n";
                trajectories_javascript += "var polyline_list_tmp = polyline_list;\n\n";
                trajectories_javascript += "// set the polylines and circle markers group layer\n";
                trajectories_javascript += "var clusteredTrajectories = L.layerGroup(polyline_list);\n\n";
                // write eps of clustered trajectories to javascript
                trajectories_javascript += "// write eps of clustered trajectories;\n";
                trajectories_javascript += "var eps = " + epsParam + ";\n\n";
                // write minLns of clustered trajectories to javascript
                trajectories_javascript += "// write minLns of clustered trajectories;\n";
                trajectories_javascript += "var minLns = " + minLnsParam + ";\n\n";
                // write the number of clustered trajectories to javascript
                trajectories_javascript += "// write the number of clustered trajectories;\n";
                trajectories_javascript += "var numClusteredTrajectories = " + m_clusterList.size() + ";\n\n";
                bw.write(trajectories_javascript);
            }
        } catch (FileNotFoundException ex) {
            if (LOG_ENABLED == true) {
                writeFile(properties.getProperty("log_file"), ex.toString() + "\n");
            }
            Logger.getLogger(TraClusterDoc.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            if (LOG_ENABLED == true) {
                writeFile(properties.getProperty("log_file"), ex.toString() + "\n");
            }
            Logger.getLogger(TraClusterDoc.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(TraClusterDoc.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                bw.close();
                if (conn != null) {
                    conn.close();
                }
            } catch (IOException | SQLException ex) {
                Logger.getLogger(TraClusterDoc.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return true;
    }

    Parameter onEstimateParameter() {
        Parameter p = new Parameter();

        ClusterGen generator = new ClusterGen(this);

        if (!generator.partitionTrajectory()) {
            System.out.println("Unable to partition a trajectory\n");
            return null;
        }

        //if (!generator.estimateParameterValue(epsParam, minLnsParam))
        if (!generator.estimateParameterValue(p)) {
            System.out.println("Unable to calculate the entropy\n");
            return null;
        }
        return p;
    }
}
