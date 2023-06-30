import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class PlaceAPIConnector {

    public static String autoComplete(String input) throws IOException {
        List<String> dbList = new ArrayList<String>();
        BufferedReader bf = new BufferedReader(new FileReader("APIinfo.txt"));
        String line = bf.readLine();
        while (line != null) {
            dbList.add(line);
            line = bf.readLine();
        }
        bf.close();
        dbList.toArray(new String[0]);
        String apiKey = dbList.get(0);
        String API_ENDPOINT = "https://maps.googleapis.com/maps/api/place/autocomplete/json";
    

        String url = API_ENDPOINT + "?input=" + input + "&key=" + apiKey + "&types=address&components=country:us";
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line2;

        while ((line2 = reader.readLine()) != null) {
            response.append(line2);
        }

        return response.toString();
    }

    public boolean validateAddress(String address) throws IOException {
        List<String> dbList = new ArrayList<String>();
        BufferedReader bf = new BufferedReader(new FileReader("APIinfo.txt"));
        String line = bf.readLine();
        while (line != null) {
            dbList.add(line);
            line = bf.readLine();
        }
        bf.close();
        dbList.toArray(new String[0]);
        String apiKey = dbList.get(0);
        String GEOCODING_API_URL = "https://maps.googleapis.com/maps/api/geocode/json";
        try {
            String encodedAddress = URLEncoder.encode(address, "UTF-8");
            String url = GEOCODING_API_URL + "?address=" + encodedAddress + "&key=" + apiKey;
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line3;

                while ((line3 = reader.readLine()) != null) {
                    response.append(line3);
                }

                reader.close();
                String jsonResponse = response.toString();
                boolean isValid = !jsonResponse.contains("\"status\" : \"ZERO_RESULTS\"");
                return isValid;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}