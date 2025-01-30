package org.disit.TrafficFlowManager.utils;

import java.util.ArrayList;
// import java.util.Arrays;
import java.util.List;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

public class GeoTools {
    public static double[] getCartisianApproximatedCoordinates(double latitude, double longitude, String targetReferenceSystem) throws Exception {
        // Create the source and target CRS (EPSG:4326 to EPSG:32632)
        CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326");  // WGS84

        // target reference systems alternatives
        // - EPSG:3003 ("Monte Mario / Italy zone 1"), northern Italy
        // - EPSG:3004 ("Monte Mario / Italy zone 2"), southern Italy
        // - EPSG:32632 (UTM Zone 32N, part of the WGS 84 projection system)
        // - EPSG:25832 (ETRS89 / UTM zone 32N)

        // String targetReferenceSystem = "EPSG:32632"; 

        CoordinateReferenceSystem targetCRS = CRS.decode(targetReferenceSystem); 

        // Create a transformation object
        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);

        // Sample latitude and longitude in EPSG:4326 (WGS84)
        // double latitude = 43.7592887878418;
        // double longitude = 11.24835681915283;

        // Create a point with the latitude and longitude
        GeometryFactory geometryFactory = new GeometryFactory();
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));

        // Perform the transformation to the target CRS
        Point projectedPoint = (Point) JTS.transform(point, transform);

        // Print the projected coordinates (X and Y in UTM Zone 32N)
        //System.out.println("Projected X (Easting): " + projectedPoint.getX());
        //System.out.println("Projected Y (Northing): " + projectedPoint.getY());

        double[] newCoords = {projectedPoint.getX(), projectedPoint.getY()};
        return newCoords;
    }

    // Helper method to calculate slope
    public static Double computeSlope(double x1, double y1, double x2, double y2) {
        if (x2 == x1) {
            // Slope is undefined for vertical lines
            return null;
        }
        return (y2 - y1) / (x2 - x1);
    }

    // Method to calculate midpoint of a line segment
    public static double[] computeMidpoint(double x1, double y1, double x2, double y2) {
        double midX = (x1 + x2) / 2;
        double midY = (y1 + y2) / 2;
        return new double[]{midX, midY};
    }

    // Method to compute intersection point of two lines (y = m1*x + c1 and y = m2*x + c2)
    public static double[] findIntersection(Double m1, double c1, Double m2, double c2) {
        if (m1 != null && m2 != null && m1.equals(m2)) {
            // Parallel lines do not intersect
            return null;
        }
        double x, y;
        if (m1 == null) { // Line 1 is vertical
            x = c1;
            y = m2 * x + c2;
        } else if (m2 == null) { // Line 2 is vertical
            x = c2;
            y = m1 * x + c1;
        } else {
            x = (c2 - c1) / (m1 - m2);
            y = m1 * x + c1;
        }
        return new double[]{x, y};
    }

    // Method to check if a point is on a line segment
    public static boolean isPointOnLine(double x, double y, double x1, double y1, double x2, double y2) {
        return (x >= Math.min(x1, x2) && x <= Math.max(x1, x2)) && (y >= Math.min(y1, y2) && y <= Math.max(y1, y2));
    }

    // Main method to check if perpendicular to B intersects A
    public static boolean doesPerpendicularIntersect(double[][] lineA, double[][] lineB) {
        // Line B endpoints
        double bx1 = lineB[0][0], by1 = lineB[0][1];
        double bx2 = lineB[1][0], by2 = lineB[1][1];

        // Calculate the midpoint of B
        double[] midpointB = computeMidpoint(bx1, by1, bx2, by2);
        double midX = midpointB[0];
        double midY = midpointB[1];

        // Calculate slope of line B
        Double slopeB = computeSlope(bx1, by1, bx2, by2);
        Double slopePerpendicular;

        if (slopeB == null) {
            // Line B is vertical, so the perpendicular line is horizontal (slope = 0)
            slopePerpendicular = 0.0;
        } else {
            // Calculate slope of perpendicular line (negative reciprocal of slopeB)
            slopePerpendicular = -1 / slopeB;
        }

        // Find y-intercept (c) of the perpendicular line
        double cPerpendicular = (slopePerpendicular == null) ? midX : midY - slopePerpendicular * midX;

        // Line A endpoints
        double ax1 = lineA[0][0], ay1 = lineA[0][1];
        double ax2 = lineA[1][0], ay2 = lineA[1][1];

        // Calculate slope and intercept of line A
        Double slopeA = computeSlope(ax1, ay1, ax2, ay2);
        double interceptA = (slopeA == null) ? ax1 : ay1 - slopeA * ax1;

        // Find the intersection point of the perpendicular line with line A
        double[] intersection = findIntersection(slopeA, interceptA, slopePerpendicular, cPerpendicular);
        if (intersection == null) {
            return false; // No intersection (parallel lines)
        }

        // Check if the intersection point lies within the bounds of line A
        double ix = intersection[0];
        double iy = intersection[1];
        return isPointOnLine(ix, iy, ax1, ay1, ax2, ay2);
    }

    public static double computeDistanceXY(double xA, double yA, double xB, double yB){
        double dx = xA - xB;
        double dy = yA - yB;

        return Math.sqrt( (dx*dx) + (dy*dy) );
    }
    // Method to compute the distance between two points using Haversine formula
    public static double computeDistanceLatLong(double lat1, double lon1, double lat2, double lon2) {
        // Radius of the Earth in meters
        double EARTH_RADIUS_M = 6371000.0;

        // If the points are too close, use a flat Earth approximation
        if (lat1 == lat2 && lon1 == lon2) {
            return 0.0;  // Points are identical
        }

        // Convert latitude and longitude from degrees to radians
        lat1 = Math.toRadians(lat1);
        lon1 = Math.toRadians(lon1);
        lat2 = Math.toRadians(lat2);
        lon2 = Math.toRadians(lon2);

        // Calculate the differences in latitude and longitude
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        // Apply the Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(lat1) * Math.cos(lat2)
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Distance in meters
        return EARTH_RADIUS_M * c;
    }

    public static JSONObject findObjectInArray(JSONArray jsonArray, String key, String value) {
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            if (jsonObject.has(key) && jsonObject.getString(key).equals(value)) {
                return jsonObject; // Return the object if a match is found
            }
        }
        return null; // Return null if no match is found
    }

    // Method to get the minimum value in a double array
    public static double getMinValue(double[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array is null or empty");
        }

        double minValue = array[0]; // Assume the first element is the minimum

        // Iterate through the array to find the minimum value
        for (int i = 1; i < array.length; i++) {
            if (array[i] < minValue) {
                minValue = array[i];
            }
        }

        return minValue;
    }

    public static int getIndexOfMinValue(double[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array is empty or null");
        }

        int minIndex = 0;  // Assume the first element is the minimum initially
        double minValue = array[0];  // Store the minimum value

        // Iterate through the array to find the minimum value and its index
        for (int i = 1; i < array.length; i++) {
            if (array[i] < minValue) {
                minValue = array[i];
                minIndex = i;
            }
        }

        return minIndex;
    }

    public static ArrayList<Integer> getIndexesOfTrueValues(boolean[] array) {
        // System.out.println("INTO getIndexesOfTrueValues :: input = " + Arrays.toString(array));
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array is empty or null");
        }
        ArrayList<Integer> trueIndexes = new ArrayList<>(); 
        // Iterate through the array to find the minimum value and its index
        for (int i = 0; i < array.length; i++) {
            // System.out.println("Index " + i + "= " + array[i]);
            if (array[i]) {
                trueIndexes.add(i);
            }
        }
        // System.out.println(trueIndexes);
        return trueIndexes;
    }


    // Method to find the ordered sequence of JSONObjects and return the start and end points
    public static String[] findCorrectOrder(JSONArray jsonArray) {
        List<JSONObject> orderedList = new ArrayList<>();

        // Step 1: Find the first object (the one whose start doesn't match any other end)
        JSONObject firstObject = findFirstObject(jsonArray);
        if (firstObject == null) {
            throw new RuntimeException("No valid starting point found.");
        }

        // Add the first object to the ordered list
        orderedList.add(firstObject);

        // Step 2: Iteratively find the next object based on the end point of the current object
        while (orderedList.size() < jsonArray.length()) {
            JSONObject lastObject = orderedList.get(orderedList.size() - 1);
            JSONObject nextObject = findNextObject(lastObject, jsonArray, orderedList);

            if (nextObject == null) {
                throw new RuntimeException("No valid continuation found.");
            }
            orderedList.add(nextObject);
        }

        // Check if a unique sequence was found
        if (orderedList.size() != jsonArray.length()) {
            throw new RuntimeException("Unique sequence not found.");
        }

        // Extract the start and end points of the full sequence
        JSONObject first = orderedList.get(0);
        JSONObject last = orderedList.get(orderedList.size() - 1);

        // String startPoint = first.getString("startLat") + ", " + first.getString("startLon");
        // String endPoint = last.getString("endLat") + ", " + last.getString("endLon");
        String startLat = first.getString("startLat");
        String startLon = first.getString("startLon");
        String endLat = last.getString("endLat");
        String endLon = last.getString("endLon");
        
        // Return the start and end points
        return new String[]{startLat, startLon, endLat, endLon};
    }

    // Method to find the first JSONObject in the sequence
    private static JSONObject findFirstObject(JSONArray jsonArray) {
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject candidate = jsonArray.getJSONObject(i);
            boolean isStart = true;

            String startLat = candidate.getString("startLat");
            String startLon = candidate.getString("startLon");

            for (int j = 0; j < jsonArray.length(); j++) {
                if (i != j) {
                    JSONObject other = jsonArray.getJSONObject(j);
                    String endLat = other.getString("endLat");
                    String endLon = other.getString("endLon");

                    if (startLat.equals(endLat) && startLon.equals(endLon)) {
                        isStart = false;
                        break;
                    }
                }
            }

            if (isStart) {
                return candidate;
            }
        }
        return null;
    }

    // Method to find the next JSONObject based on the end point of the current object
    private static JSONObject findNextObject(JSONObject current, JSONArray jsonArray, List<JSONObject> orderedList) {
        String endLat = current.getString("endLat");
        String endLon = current.getString("endLon");

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject candidate = jsonArray.getJSONObject(i);

            // Skip already added objects
            if (orderedList.contains(candidate)) continue;

            String startLat = candidate.getString("startLat");
            String startLon = candidate.getString("startLon");

            if (endLat.equals(startLat) && endLon.equals(startLon)) {
                return candidate;
            }
        }

        return null;
    }
}
