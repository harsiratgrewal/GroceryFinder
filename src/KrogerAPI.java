import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.json.*;
import org.json.simple.parser.ParseException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class KrogerAPI {

    public String auth() throws IOException, ParseException {
        List<String> dbList = new ArrayList<String>();
        BufferedReader bf = new BufferedReader(new FileReader("krogerInfo.txt"));
        String line = bf.readLine();
        while (line != null) {
            dbList.add(line);
            line = bf.readLine();
        }
        bf.close();
        dbList.toArray(new String[0]);
        String encode = dbList.get(0);
        
        String access_token = null;
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        String requestBodyContent = "grant_type=client_credentials&scope=product.compact";
        Request request = new Request.Builder()
                .url("https://api.kroger.com/v1/connect/oauth2/token")
                .post(RequestBody.create(requestBodyContent, mediaType))
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Authorization",
                        "Basic " + encode)
                .build();
        Response response = client.newCall(request).execute();
        int responseCode = response.code();
        if (responseCode != 200) {
            GroceryFinder gf = new GroceryFinder();
            gf.Error("ERROR", "RESPONSE CODE ERROR");
        } else {
            ResponseBody responseBody = response.body();
            String token = responseBody.string();
            JSONObject data = new JSONObject(token);
            access_token = (String) data.getString("access_token");
        }
        return access_token;
    }

    public void krogerLocation(String latLong) throws IOException, ParseException, SQLException {
        String token = auth();
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.kroger.com/v1/locations?filter.latLong.near=" + latLong
                        + "&filter.radiusInMiles=10&filter.chain=KROGER&filter.department=02&filter.department=04")
                .get()
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + token)
                .build();
        Response response = client.newCall(request).execute();
        int responseCode = response.code();
        if (responseCode != 200) {
            GroceryFinder gf = new GroceryFinder();
            gf.Error("ERROR", "RESPONSE CODE ERROR");
        } else {
            LocationConverter lc = new LocationConverter();
            DatabaseConnection db = new DatabaseConnection();
            Connection conn = db.DBconnection();
            String clear = "TRUNCATE TABLE Kroger_Locations";
            PreparedStatement stm = conn.prepareStatement(clear);
            stm.executeUpdate();
            stm.close();
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                String responseString = responseBody.string();
                JSONObject objData = new JSONObject(responseString);
                JSONArray data = new JSONArray(objData.getJSONArray("data"));

                for (int i = 0; i < data.length(); i++) {
                    JSONObject info = data.getJSONObject(i);
                    String locationID = (String) info.getString("locationId");
                    String storeName = (String) info.getString("chain");
                    JSONObject ad = info.getJSONObject("address");
                    JSONObject geo = info.getJSONObject("geolocation");
                    double distance = lc.distance(lc.getAddress_lat(), lc.getAddress_lon(), geo.getDouble("latitude"),
                            geo.getDouble("longitude"));
                    String travelTime = lc.TravelTimeCalculator(lc.getAddress_lat(), lc.getAddress_lon(),
                            geo.getDouble("latitude"),
                            geo.getDouble("longitude"));
                    String imagePath = "src/Images/krogerLogo.png";
                    File imageFile = new File(imagePath);
                    byte[] imageData = new byte[(int) imageFile.length()];
                    FileInputStream fis = new FileInputStream(imageFile);
                    fis.read(imageData);
                    fis.close();
                    String Address = ((String) ad.getString("addressLine1")) + ", " + ((String) ad.getString("city"))
                            + ", " +
                            ((String) ad.getString("state")) + ", " + ((String) ad.getString("zipCode"));
                    String sql = "INSERT INTO Kroger_Locations (location_ID, chain, location_address, distance, logo, time) VALUES (?,?,?,?,?,?)";
                    PreparedStatement statement = conn.prepareStatement(sql);
                    statement.setString(1, locationID);
                    statement.setString(2, storeName);
                    statement.setString(3, Address);
                    statement.setDouble(4, distance);
                    statement.setBytes(5, imageData);
                    statement.setString(6, travelTime);
                    statement.executeUpdate();
                    statement.close();
                }
            }
        }
    }

    public void krogerProducts() throws IOException, ParseException, SQLException {
        String[] grocery = { "milk", "cheese", "eggs" };
        DatabaseConnection db = new DatabaseConnection();
        Connection conn = db.DBconnection();
        String clear = "TRUNCATE TABLE Kroger_Products";
        PreparedStatement stm = conn.prepareStatement(clear);
        stm.executeUpdate();
        stm.close();
        for (int j = 0; j < grocery.length; j++) {
            String token = auth();
            OkHttpClient client = new OkHttpClient();
            String filter = "SELECT location_ID FROM Kroger_Locations";
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(filter);
            while (rs.next()) {
                String lid = rs.getString("location_ID");
                Request request = new Request.Builder()
                        .url("https://api.kroger.com/v1/products?filter.locationId=" + lid
                                + "&filter.fulfillment=ais&filter.term=" + grocery[j] + "&filter.size=medium")
                        .get()
                        .addHeader("Accept", "application/json")
                        .addHeader("Authorization", "Bearer " + token)
                        .build();
                Response response = client.newCall(request).execute();
                int responseCode = response.code();
                if (responseCode != 200) {
                    GroceryFinder gf = new GroceryFinder();
                    gf.Error("ERROR", "RESPONSE CODE ERROR");
                } else {
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        String responseString = responseBody.string();
                        JSONObject objData = new JSONObject(responseString);
                        JSONArray prod_data = new JSONArray(objData.getJSONArray("data"));

                        for (int x = 0; x < prod_data.length(); x++) {
                            JSONObject info = prod_data.getJSONObject(x);
                            String description = info.getString("description");
                            JSONArray items = new JSONArray(info.getJSONArray("items"));
                            for (int z = 0; z < items.length(); z++) {
                                JSONObject itemsData = items.getJSONObject(z);
                                String size = (String) itemsData.getString("size");
                                Double price = (Double) itemsData.getJSONObject("price").getDouble("regular");
                                String inventory = (String) itemsData.getJSONObject("inventory")
                                        .getString("stockLevel");
                                JSONArray imgArr = new JSONArray(info.getJSONArray("images"));
                                for (int v = 0; v < imgArr.length(); v++) {
                                    JSONObject imagesData = imgArr.getJSONObject(v);
                                    JSONArray sizeArr = new JSONArray(imagesData.getJSONArray("sizes"));
                                    for (int k = 0; k < sizeArr.length(); k++) {
                                        String imgUrl = null;
                                        String front = "front";
                                        JSONObject url = sizeArr.getJSONObject(k);
                                        if (url.getString("size").equals("medium")
                                                && url.getString("url").contains(front)) {
                                            imgUrl = (String) url.getString("url");
                                            String sql = "INSERT INTO Kroger_Products (product_description, product_size, location_ID, product_inventory, product_price, product_image) VALUES (?,?,?,?,?,?)";
                                            PreparedStatement state = conn.prepareStatement(sql);
                                            state.setString(1, description);
                                            state.setString(2, size);
                                            state.setString(3, lid);
                                            state.setString(4, inventory);
                                            state.setDouble(5, price);
                                            state.setString(6, imgUrl);
                                            state.executeUpdate();
                                            state.close();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            rs.close();
            statement.close();
        }
    }
}