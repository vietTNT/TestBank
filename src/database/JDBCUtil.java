package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
// Không cần import com.mysql.cj.jdbc.Driver; một cách tường minh nếu dùng JDBC 4.0+
// và Connector/J JAR đã có trong classpath.

public class JDBCUtil {

    public static Connection getConnection() {
        Connection c = null;
        try {
 
            String url = "jdbc:mysql://localhost:3306/TestBank?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            
            String username = "root";
            String password = "1234"; // Để trống nếu không có mật khẩu

            c = DriverManager.getConnection(url, username, password);

        } catch (SQLException e) {
            System.err.println("Lỗi kết nối CSDL: " + e.getMessage());
            e.printStackTrace();
        }
        return c;
    }

    public static void closeConnection(Connection c) {
        try {
            if (c != null && !c.isClosed()) { // Kiểm tra thêm !c.isClosed()
                c.close();
                // System.out.println("Đã đóng kết nối CSDL.");
            }
        } catch (SQLException e) { // Nên bắt SQLException cụ thể
            System.err.println("Lỗi khi đóng kết nối CSDL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void printInfo(Connection c) {
        try {
            if (c != null && !c.isClosed()) {
                DatabaseMetaData metaData = c.getMetaData();
                System.out.println("Tên CSDL: " + metaData.getDatabaseProductName());
                System.out.println("Phiên bản CSDL: " + metaData.getDatabaseProductVersion());
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy thông tin CSDL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
