import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class LocationConverter {
    private static final String apiKey = "AIzaSyDejRQtApk0MeZQ1nGGHalzOC_JFYklfHw";

    public String convertAddress(String theAddress) throws ParseException {

        String LatLong = null;
        try {
            String geocodingUrl = "https://maps.googleapis.com/maps/api/geocode/json?address="
                    + theAddress.replaceAll(" ", "+") + "&key=" + apiKey;

            URL url = new URL(geocodingUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONParser parser = new JSONParser();
            JSONObject jsonResponse = (JSONObject) parser.parse(response.toString());

            if (jsonResponse.get("status").equals("OK")) {
                JSONArray results = (JSONArray) jsonResponse.get("results");
                JSONObject location = (JSONObject) ((JSONObject) results.get(0)).get("geometry");
                JSONObject coordinates = (JSONObject) location.get("location");
                double latitude = (double) coordinates.get("lat");
                double longitude = (double) coordinates.get("lng");
                setAddress_lat(latitude);
                setAddress_lon(longitude);
                LatLong = String.valueOf(latitude) + "," + String.valueOf(longitude);
            } else {
                GroceryFinder gf = new GroceryFinder();
                gf.Error("ADDRESS VALIDATION", "UNABLE TO LOCATE, TRY AGAIN!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return LatLong;
    }

    private static double Address_lat, Address_lon;

    public void setAddress_lat(double lat) {
        LocationConverter.Address_lat = lat;
    }

    public void setAddress_lon(double lon) {
        LocationConverter.Address_lon = lon;
    }

    public double getAddress_lat() {
        return Address_lat;
    }

    public double getAddress_lon() {
        return Address_lon;
    }

    public double distance(double address_Lat, double address_Lon, double store_lat, double store_lon) {
        double EARTH_RADIUS = 6371;
        double distance;
        double lat1Rad = Math.toRadians(address_Lat);
        double lon1Rad = Math.toRadians(address_Lon);
        double lat2Rad = Math.toRadians(store_lat);
        double lon2Rad = Math.toRadians(store_lon);

        double latDiff = lat2Rad - lat1Rad;
        double lonDiff = lon2Rad - lon1Rad;

        double a = Math.pow(Math.sin(latDiff / 2), 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.pow(Math.sin(lonDiff / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        distance = (EARTH_RADIUS * c) / 0.621371;
        return distance;
    }

    public String TravelTimeCalculator(double lat1, double lon1, double lat2, double lon2) throws IOException {
        String originLatLong = String.valueOf(lat1) + "," + String.valueOf(lon1);
        String destinationLatLong = String.valueOf(lat2) + "," + String.valueOf(lon2);

        String requestUrl = "https://maps.googleapis.com/maps/api/distancematrix/json?" +
                "origins=" + URLEncoder.encode(originLatLong, "UTF-8") +
                "&destinations=" + URLEncoder.encode(destinationLatLong, "UTF-8") +
                "&key=" + apiKey;

        URL url = new URL(requestUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        String travelTime = TravelTime(response.toString());
        return travelTime;
    }

    private static String TravelTime(String jsonResponse) {
        JsonObject responseJson = JsonParser.parseReader(new StringReader(jsonResponse)).getAsJsonObject();

        String status = responseJson.get("status").getAsString();
        if (!status.equals("OK")) {
            System.out.println("Error: " + status);
            return null;
        }
        JsonArray rows = responseJson.getAsJsonArray("rows");
        JsonObject row = rows.get(0).getAsJsonObject();
        JsonArray elements = row.getAsJsonArray("elements");
        JsonObject element = elements.get(0).getAsJsonObject();
        String durationText = element.getAsJsonObject("duration").get("text").getAsString();
        return durationText;
    }
}
