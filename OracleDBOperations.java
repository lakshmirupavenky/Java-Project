import java.sql.*;
import java.util.*;

public class Oracle {

    static final String URL = "jdbc:oracle:thin:@localhost:1521:xe"; // Change if needed
    static final String USER = "system"; // your username
    static final String PASS = "rupa"; // your password
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            System.out.println("Connected to Oracle Database!");

            while (true) {
                System.out.println("\n=== Oracle DB Operations ===");
                System.out.println("1. Create Table");
                System.out.println("2. Insert Data");
                System.out.println("3. Delete Data");
                System.out.println("4. Update Data");
                System.out.println("5. Select Data");
                System.out.println("6. Truncate Table");
                System.out.println("7. Drop Table");
                System.out.println("8. Exit");
                System.out.print("Enter your choice: ");
                int choice = sc.nextInt();
                sc.nextLine(); // consume newline 

                switch (choice) {
                    case 1 -> createTable(conn);
                    case 2 -> insertData(conn);
                    case 3 -> deleteData(conn);
                    case 4 -> updateData(conn);
                    case 5 -> selectData(conn);
                    case 6 -> truncateTable(conn);
                    case 7 -> dropTable(conn);
                    case 8 -> {
                        System.out.println("Exiting program.");
                        return;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void createTable(Connection conn) throws SQLException {
        System.out.print("Enter table name to create: ");
        String table = sc.nextLine();

        System.out.print("Enter number of columns: ");
        int cols = sc.nextInt();
        sc.nextLine();

        StringBuilder sql = new StringBuilder("CREATE TABLE " + table + " (");
        for (int i = 1; i <= cols; i++) {
            System.out.print("Enter column " + i + " name: ");
            String colName = sc.nextLine();
            System.out.print("Enter data type (e.g., VARCHAR2(20), NUMBER): ");
            String colType = sc.nextLine();

            sql.append(colName).append(" ").append(colType);
            if (i != cols) sql.append(", ");
        }
        sql.append(")");

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql.toString());
            System.out.println("Table " + table + " created successfully.");
        }
    }

    static void insertData(Connection conn) throws SQLException {
        System.out.print("Enter table name to insert into: ");
        String table = sc.nextLine();

        System.out.print("Enter number of columns to insert: ");
        int cols = sc.nextInt();
        sc.nextLine();

        List<String> colNames = new ArrayList<>();
        List<String> values = new ArrayList<>();

        for (int i = 1; i <= cols; i++) {
            System.out.print("Enter column name: ");
            colNames.add(sc.nextLine());
            System.out.print("Enter value: ");
            values.add(sc.nextLine());
        }

        StringBuilder sql = new StringBuilder("INSERT INTO " + table + " (");
        sql.append(String.join(", ", colNames)).append(") VALUES (");
        sql.append("?,".repeat(cols));
        sql.setLength(sql.length() - 1); // remove last comma
        sql.append(")");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < values.size(); i++) {
                ps.setString(i + 1, values.get(i));
            }
            ps.executeUpdate();
            System.out.println("Data inserted successfully.");
        }
    }

    static void deleteData(Connection conn) throws SQLException {
        System.out.print("Enter table name to delete from: ");
        String table = sc.nextLine();
        System.out.print("Enter WHERE condition (e.g., id = 1): ");
        String where = sc.nextLine();

        String sql = "DELETE FROM " + table + " WHERE " + where;

        try (Statement stmt = conn.createStatement()) {
            int rows = stmt.executeUpdate(sql);
            System.out.println(rows + " row(s) deleted.");
        }
    }

    static void updateData(Connection conn) throws SQLException {
        System.out.print("Enter table name to update: ");
        String table = sc.nextLine();
        System.out.print("Enter column=value to set (e.g., name='John'): ");
        String set = sc.nextLine();
        System.out.print("Enter WHERE condition (e.g., id = 1): ");
        String where = sc.nextLine();

        String sql = "UPDATE " + table + " SET " + set + " WHERE " + where;

        try (Statement stmt = conn.createStatement()) {
            int rows = stmt.executeUpdate(sql);
            System.out.println(rows + " row(s) updated.");
        }
    }

    static void selectData(Connection conn) throws SQLException {
        System.out.print("Enter table name to select from: ");
        String table = sc.nextLine();

        String sql = "SELECT * FROM " + table;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            System.out.println("=== Data in Table " + table + " ===");
            while (rs.next()) {
                for (int i = 1; i <= colCount; i++) {
                    System.out.print(meta.getColumnName(i) + ": " + rs.getString(i) + "  ");
                }
                System.out.println();
            }
        }
    }

    static void truncateTable(Connection conn) throws SQLException {
        System.out.print("Enter table name to truncate: ");
        String table = sc.nextLine();
        String sql = "TRUNCATE TABLE " + table;

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("Table " + table + " truncated successfully.");
        }
    }

    static void dropTable(Connection conn) throws SQLException {
        System.out.print("Enter table name to drop: ");
        String table = sc.nextLine();
        String sql = "DROP TABLE " + table;

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("Table " + table + " dropped successfully.");
        }
    }
}