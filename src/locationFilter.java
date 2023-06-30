import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class locationFilter {
    public List<String> locations(List<String> products) throws IOException, SQLException {
        DatabaseConnection db = new DatabaseConnection();
        Connection conn = db.DBconnection();
        List<String> locations = new ArrayList<>();
        for (String item : products) {
            String sql = "SELECT product_description, location_ID, product_inventory FROM Kroger_Products WHERE product_description = ? AND product_inventory = ?";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, item);
            stm.setString(2, "HIGH");
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                if (!locations.contains(rs.getString("location_ID"))) {
                    locations.add(rs.getString("location_ID"));
                }
            }
        }
        return locations;
    }

    public void costFilter(List<String> locations, List<String> products) throws IOException, SQLException {
        DatabaseConnection db = new DatabaseConnection();
        Connection conn = db.DBconnection();
        for (String location : locations) {
            Double est_cost = 0.00;
            for (String price : products) {
                String sql = "SELECT product_price FROM Kroger_Products WHERE product_description = ? AND location_ID = ?";
                PreparedStatement stm = conn.prepareStatement(sql);
                stm.setString(1, price);
                stm.setString(2, location);
                ResultSet rs = stm.executeQuery();
                while (rs.next()) {
                    est_cost += (rs.getDouble("product_price"));
                }
            }
            String sql = "UPDATE Kroger_Locations SET estimated_cost = ? WHERE location_ID = ?";
            PreparedStatement state = conn.prepareStatement(sql);
            state.setDouble(1, est_cost);
            state.setString(2, location);
            state.executeUpdate();
        }
    }
}
