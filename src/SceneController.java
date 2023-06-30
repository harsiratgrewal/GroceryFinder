
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SceneController {

    private Stage stage;
    private Parent root;

    @FXML
    private AnchorPane anchpane1;

    @FXML
    private TextField enter_address;

    @FXML
    private Button ok_btn;

    @FXML
    void btn_ok_clicked(ActionEvent event) throws IOException, org.json.simple.parser.ParseException, SQLException {
        KrogerAPI kroger = new KrogerAPI();
        LocationConverter lc = new LocationConverter();
        GroceryFinder gf = new GroceryFinder();
        PlaceAPIConnector place = new PlaceAPIConnector();
        String address = enter_address.getText();
        if (!address.isEmpty()) {
            boolean isValid = place.validateAddress(address);
            if (isValid) {
                gf.setAddress(address);
                String latLong = lc.convertAddress(gf.getAddress());
                kroger.krogerLocation(latLong);
                kroger.krogerProducts();
                root = FXMLLoader.load(getClass().getResource("Panel2.fxml"));
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
            } else {
                gf.Error("ADDRESS VALIDATION", "Address is invalid.");
            }
        } else {
            gf.Error("ADDRESS VALIDATION", "Please enter an address.");
        }
    }

    @FXML
    private ListView<String> grocery_sugg;

    @FXML
    private ListView<String> grocery_list;

    @FXML
    private AnchorPane anchpane2;

    @FXML
    private TextField enter_grocery;

    @FXML
    void search_grocery_actn(KeyEvent event) throws IOException, SQLException {
        ObservableList<String> grocery_sugg_list = FXCollections.observableArrayList();
        try {
            DatabaseConnection db = new DatabaseConnection();
            Connection conn = db.DBconnection();
            grocery_sugg_list.clear();
            grocery_sugg.setVisible(true);
            String grocery = enter_grocery.getText();
            if (grocery != null) {
                String sql = "SELECT * FROM Kroger_Products WHERE product_description LIKE '%" + grocery + "%'";
                PreparedStatement statement = conn.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String description = resultSet.getString("product_description");
                    if (!grocery_sugg_list.contains(description)) {
                        grocery_sugg_list.add(description);
                        grocery_sugg.setItems(grocery_sugg_list);
                    }
                }
            } else {
                grocery_sugg_list.clear();
                grocery_sugg.setVisible(false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    ObservableList<String> groceryList = FXCollections.observableArrayList();

    @FXML
    void select_listItem(MouseEvent event) {
        String selectedItem = grocery_sugg.getSelectionModel().getSelectedItem();
        if (selectedItem != null && !groceryList.contains(selectedItem)) {
            groceryList.add(selectedItem);
            enter_grocery.clear();
            grocery_list.setItems(groceryList);
            grocery_sugg.setVisible(false);
        } else {
            GroceryFinder gf = new GroceryFinder();
            gf.Error("SHOPPING LIST", "Item Already in List!");
        }
    }

    @FXML
    void handleRootMouse(MouseEvent event) {
        anchpane2.requestFocus();
        enter_grocery.getParent().requestFocus();
        grocery_sugg.setVisible(false);
    }

    @FXML
    private Button delete_btn;

    @FXML
    void delete_btn_action(ActionEvent event) {
        String selectedItem = grocery_list.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            groceryList.remove(selectedItem);
        }
    }

    @FXML
    void RootMouse(MouseEvent event) {
        address_sugg.getParent().requestFocus();
        address_sugg.setVisible(false);
    }

    @FXML
    void address_type(KeyEvent event) throws ParseException, org.json.simple.parser.ParseException {
        ObservableList<String> suggestions = FXCollections.observableArrayList();
        try {
            address_sugg.setVisible(true);
            String input = enter_address.getText();
            input = input.replace(" ", "+");
            String response = PlaceAPIConnector.autoComplete(input);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(response);
            JSONArray predictions = (JSONArray) json.get("predictions");
            for (Object prediction : predictions) {
                JSONObject place = (JSONObject) prediction;
                String placeName = (String) place.get("description");
                suggestions.add(placeName);
                address_sugg.setItems(suggestions);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void sugg_action(KeyEvent event) {
        if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
            address_sugg.requestFocus();
            address_sugg.getSelectionModel().select(0);
        }
        if (event.getCode() == KeyCode.ENTER) {
            String selectedValue = address_sugg.getSelectionModel().getSelectedItem();
            if (selectedValue != null) {
                enter_address.setText(selectedValue);
                address_sugg.setVisible(false);
            }
        }
    }

    @FXML
    private ListView<String> address_sugg;

    @FXML
    void select_address(MouseEvent event) {
        String address = address_sugg.getSelectionModel().getSelectedItem();
        if (address != null) {
            enter_address.setText(address);
            enter_address.positionCaret(address.length());
            address_sugg.setVisible(false);
        }
    }

    @FXML
    private Button search_btn;
    @FXML
    private Button return_btn1;

    @FXML
    void btn_return1_clicked(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("Panel1.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }

    @FXML
    private AnchorPane leftPane;

    @FXML
    private MenuButton radius_select;

    @FXML
    private Label show_within;

    @FXML
    private Label type;

    @FXML
    private MenuButton type_select;

    @FXML
    private Label address;

    @FXML
    private Label chain;

    @FXML
    private Label cost;

    @FXML
    private Label distance;

    @FXML
    private ImageView logo;

    @FXML
    private Label time;

    @FXML
    private AnchorPane displayPane;

    @FXML
    private VBox vbox;

    @FXML
    private HBox hbox;

    @FXML
    private AnchorPane anchpane3;

    @FXML
    void btn_search_clicked(ActionEvent event) throws IOException, SQLException {
        if (!groceryList.isEmpty()) {
            locationFilter lc = new locationFilter();
            DatabaseConnection db = new DatabaseConnection();
            Connection conn = db.DBconnection();
            List<String> locations = lc.locations(groceryList);
            lc.costFilter(locations, groceryList);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Panel3.fxml"));
            Parent root = loader.load();
            AnchorPane anchorPane = (AnchorPane) root.lookup("#anchpane3");
            HBox HB = (HBox) anchorPane.lookup("#hbox");
            VBox VB = (VBox) HB.lookup("#vbox");
            for (String ID : locations) {
            FXMLLoader paneLoader = new FXMLLoader(getClass().getResource("displayPane.fxml"));
            AnchorPane anchorPaneTemplate = paneLoader.load();
            AnchorPane listItemPane = new AnchorPane(anchorPaneTemplate);
                String sql = "SELECT * FROM Kroger_Locations WHERE location_ID = ?";
                PreparedStatement statement = conn.prepareStatement(sql);
                statement.setString(1, ID);
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    String chain = rs.getString("chain");
                    String distance = String.valueOf((rs.getDouble("distance")) * 2) + " miles";
                    String address = rs.getString("location_address");
                    String time = rs.getString("time");
                    String cost = String.valueOf(rs.getDouble("estimated_cost")) + "$";
                    byte[] imageData = rs.getBytes("logo");
                    InputStream inputStream = new ByteArrayInputStream(imageData);
                    Image image = new Image(inputStream);
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(38);
                    imageView.setFitHeight(129);
                    Label addres = (Label) listItemPane.lookup("#address");
                    addres.setText(address);
                    Label dist = (Label) listItemPane.lookup("#distance");
                    dist.setText(distance);
                    Label title = (Label) listItemPane.lookup("#chain");
                    title.setText(chain);
                    Label travel = (Label) listItemPane.lookup("#time");
                    travel.setText(time);
                    Label price = (Label) listItemPane.lookup("#cost");
                    price.setText(cost);   
                    ImageView img = (ImageView) listItemPane.lookup("#logo");
                    img.setImage(image);
                }
                VB.getChildren().add(listItemPane);
            }
            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } else {
            GroceryFinder gf = new GroceryFinder();
            gf.Error("SHOPPING LIST", "Please add atleast one Item!");
        }
    }

    @FXML
    private Button return_btn2;

    @FXML
    void return_btn2_clicked(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("Panel2.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}
