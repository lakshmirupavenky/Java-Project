import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CrudOperations extends Application {

    // Database connection details
    private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String DB_USER = "system"; // Replace with your Oracle DB username
    private static final String DB_PASS = "rupa"; // Replace with your Oracle DB password

    private Connection conn; // Database connection object

    private TreeView<String> treeView; // Left navigation for operations
    private VBox mainPane; // Right pane to display operation-specific UI
    private TableView<RowData> tableView = new TableView<>(); // Table to display database data
    private ObservableList<RowData> tableData = FXCollections.observableArrayList(); // Data source for the TableView

    private List<String> currentColumns = null; // Stores column names of the currently displayed table
    private String currentTable = null; // Stores the name of the currently selected table

    /**
     * Inner class to represent a row of data in the TableView.
     * Each row has a 'selected' property for checkbox state and an ObservableList of StringProperty
     * for its actual column data.
     */
    public static class RowData {
        private final BooleanProperty selected = new SimpleBooleanProperty(false);
        private final ObservableList<StringProperty> data;

        public RowData(List<String> data) {
            this.data = FXCollections.observableArrayList();
            for (String d : data) {
                this.data.add(new SimpleStringProperty(d));
            }
        }

        // Getter for the selected BooleanProperty
        public BooleanProperty selectedProperty() {
            return selected;
        }

        // Getter for the current selected state
        public boolean isSelected() {
            return selected.get();
        }

        // Setter for the selected state
        public void setSelected(boolean val) {
            selected.set(val);
        }

        // Getter for the observable list of StringProperty data (column values)
        public ObservableList<StringProperty> getData() {
            return data;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        // Attempt to connect to the database upon application startup
        if (!connectDB()) {
            showAlert(Alert.AlertType.ERROR, "Database Connection Failed", "Failed to connect to the Oracle database. Please verify the credentials, DB URL, and ensure the database server is running.");
            return; // Exit if connection fails
        }

        // Initialize TreeView for database operations
        treeView = new TreeView<>();
        TreeItem<String> root = new TreeItem<>("Operations");
        root.setExpanded(true); // Root node is expanded by default

        // Define the CRUD operations as TreeView items
        String[] ops = {"Create", "Insert", "Update", "Delete", "Drop", "Truncate", "Select"};
        for (String op : ops) {
            root.getChildren().add(new TreeItem<>(op));
        }
        treeView.setRoot(root);
        treeView.setPrefWidth(160); // Fixed width for the TreeView

        // Customize TreeView cell appearance (e.g., text color for operations)
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item); // Display item text or null
                if (!empty && item != null) {
                    // Set text color based on the operation
                    switch (item) {
                        case "Create":
                            setTextFill(Color.DARKGREEN);
                            break;
                        case "Insert":
                            setTextFill(Color.DARKBLUE);
                            break;
                        case "Update":
                            setTextFill(Color.ORANGE);
                            break;
                        case "Delete":
                            setTextFill(Color.RED);
                            break;
                        case "Drop":
                            setTextFill(Color.DARKRED);
                            break;
                        case "Truncate":
                            setTextFill(Color.PURPLE);
                            break;
                        case "Select":
                            setTextFill(Color.DARKCYAN);
                            break;
                        default:
                            setTextFill(Color.BLACK); // Default color
                    }
                } else {
                    setTextFill(Color.BLACK); // Default color for empty or null items
                }
            }
        });

        // Initialize the main content pane
        mainPane = new VBox(12); // Vertical box with 12px spacing
        mainPane.setPadding(new Insets(12)); // Padding around the content
        mainPane.getChildren().add(new Label("Select an operation from the left to begin."));

        // Set up the root layout (HBox combining TreeView and mainPane)
        HBox rootLayout = new HBox(treeView, mainPane);
        rootLayout.setSpacing(10); // Spacing between TreeView and mainPane
        rootLayout.setPadding(new Insets(10)); // Padding around the entire root layout

        // Create the scene and set it on the primary stage
        Scene scene = new Scene(rootLayout, 1000, 650); // Initial window size
        primaryStage.setScene(scene);
        primaryStage.setTitle("Oracle DB CRUD Operations with JavaFX");
        primaryStage.show(); // Display the window

        // Make the TableView editable (though actual editing is handled by dialogs/checkboxes)
        tableView.setEditable(true);

        // Add a listener to the TreeView's selected item property
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.getValue() == null) {
                return; // Do nothing if no item is selected or item value is null
            }

            // Clear the main pane and reset table data/columns for the new operation
            mainPane.getChildren().clear();
            currentColumns = null;
            currentTable = null;
            tableView.getColumns().clear();
            tableView.getItems().clear();
            tableData.clear();

            // Display the appropriate UI based on the selected operation
            switch (newVal.getValue()) {
                case "Create":
                    showCreateTableUI();
                    break;
                case "Insert":
                    showInsertUI();
                    break;
                case "Update":
                    showUpdateUI();
                    break;
                case "Delete":
                    showDeleteUI();
                    break;
                case "Drop":
                    showDropUI();
                    break;
                case "Truncate":
                    showTruncateUI();
                    break;
                case "Select":
                    showSelectUI();
                    break;
                default:
                    mainPane.getChildren().add(new Label("Select an operation from the left."));
            }
        });
    }

    @Override
    public void stop() {
        // Ensure the database connection is closed when the application shuts down
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException ex) {
            System.err.println("Error closing database connection: " + ex.getMessage());
        }
    }

    /**
     * Establishes a connection to the Oracle database.
     * @return true if connection is successful, false otherwise.
     */
    private boolean connectDB() {
        try {
            // Load the Oracle JDBC driver
            Class.forName("oracle.jdbc.driver.OracleDriver");
            // Establish the connection
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            System.out.println("Connected to Oracle DB successfully.");
            return true;
        } catch (ClassNotFoundException ex) {
            System.err.println("Oracle JDBC Driver not found. Make sure ojdbcX.jar is in your classpath.");
            ex.printStackTrace();
            return false;
        } catch (SQLException ex) {
            System.err.println("SQL Exception during DB connection: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Displays an alert dialog with a specified type, title, and message.
     * @param type The type of alert (e.g., INFORMATION, ERROR, CONFIRMATION).
     * @param title The title of the alert window.
     * @param message The content message of the alert.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); // No header text for simplicity
        alert.setContentText(message);
        alert.showAndWait(); // Display and wait for user interaction
    }

    /**
     * Loads table names from the current user's schema in the database into a ComboBox.
     * @param comboBox The ComboBox UI component to populate with table names.
     */
    private void loadTablesInto(ComboBox<String> comboBox) {
        comboBox.getItems().clear(); // Clear any existing items
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT table_name FROM user_tables ORDER BY table_name")) {
            while (rs.next()) {
                comboBox.getItems().add(rs.getString("table_name")); // Add each table name
            }
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Error Loading Tables", "Failed to load tables from the database: " + ex.getMessage());
        }
    }

    /**
     * Retrieves column names for a given table from the database schema.
     * @param table The name of the table to get columns for.
     * @return A List of column names (String), or null if an error occurs.
     */
    private List<String> getColumnsForTable(String table) {
        List<String> colList = new ArrayList<>();
        // Query to get column names and their order for a specific table
        String sql = "SELECT column_name FROM user_tab_columns WHERE table_name = ? ORDER BY column_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table.toUpperCase()); // Oracle table names are typically uppercase
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    colList.add(rs.getString("column_name"));
                }
            }
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Error Getting Columns", "Failed to retrieve columns for table '" + table + "': " + ex.getMessage());
            return null; // Return null to indicate failure
        }
        return colList;
    }

    /**
     * Displays the User Interface for creating a new table in the database.
     * Allows user to input table name and column definitions.
     */
    private void showCreateTableUI() {
        Label title = new Label("Create Table");
        title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");

        TextField tblNameField = new TextField();
        tblNameField.setPromptText("Enter Table Name (e.g., EMPLOYEES)");

        TextArea columnsArea = new TextArea();
        columnsArea.setPromptText("Enter columns, one per line (e.g.,\nID NUMBER PRIMARY KEY,\nNAME VARCHAR2(50) NOT NULL,\nAGE NUMBER)");
        columnsArea.setPrefRowCount(6); // Set preferred number of visible rows

        Button createBtn = new Button("Create Table");
        createBtn.setOnAction(e -> {
            String tname = tblNameField.getText().trim();
            String cols = columnsArea.getText().trim();

            if (tname.isEmpty() || cols.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Input Error", "Table name and column definitions cannot be empty!");
                return;
            }

            // Construct the CREATE TABLE SQL statement
            // Replace newlines with commas to form column definitions
            String sql = "CREATE TABLE " + tname + " (" + cols.replace("\n", ",") + ")";
            System.out.println("Executing SQL: " + sql); // Debugging: print the SQL
            try (Statement st = conn.createStatement()) {
                st.execute(sql); // Execute the DDL (Data Definition Language) statement
                showAlert(Alert.AlertType.INFORMATION, "Success", "Table '" + tname + "' created successfully.");
                tblNameField.clear(); // Clear input fields
                columnsArea.clear();
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Creation Failed", "Failed to create table: " + ex.getMessage());
            }
        });

        // Arrange components in a VBox
        VBox vbox = new VBox(10, title, new Label("Table Name:"), tblNameField,
                new Label("Columns (one per line):"), columnsArea, createBtn);
        mainPane.getChildren().add(vbox);
    }

    /**
     * Displays the User Interface for inserting new rows into a selected table.
     * Dynamically generates input fields based on table columns.
     */
    private void showInsertUI() {
        Label title = new Label("Insert into Table");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        ComboBox<String> cbTables = new ComboBox<>();
        cbTables.setPromptText("Select Table");
        loadTablesInto(cbTables); // Populate the ComboBox with available tables

        VBox inputsBox = new VBox(5); // Container for dynamically generated input TextFields
        Button btnInsert = new Button("Insert");
        btnInsert.setDisable(true); // Disable insert button until a table is selected

        // Listener for table selection in the ComboBox
        cbTables.setOnAction(e -> {
            String selected = cbTables.getSelectionModel().getSelectedItem();
            inputsBox.getChildren().clear(); // Clear old input fields
            btnInsert.setDisable(true); // Disable button until new fields are ready

            if (selected != null) {
                currentTable = selected;
                currentColumns = getColumnsForTable(selected); // Get columns for the selected table

                if (currentColumns == null || currentColumns.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "No Columns Found", "Failed to retrieve columns for table '" + selected + "'. It might be empty or an error occurred.");
                    return;
                }

                // Create a TextField for each column
                for (String col : currentColumns) {
                    TextField tf = new TextField();
                    tf.setPromptText(col + " (Value)"); // Prompt text showing column name
                    inputsBox.getChildren().add(tf);
                }
                btnInsert.setDisable(false); // Enable insert button
            }
        });

        // Action for the Insert button
        btnInsert.setOnAction(e -> {
            if (currentTable == null || currentColumns == null) {
                showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a table first.");
                return;
            }

            List<String> values = new ArrayList<>();
            // Collect values from all generated TextFields
            for (Node node : inputsBox.getChildren()) {
                if (node instanceof TextField) { // Ensure it's a TextField
                    TextField tf = (TextField) node;
                    values.add(tf.getText().trim());
                }
            }

            // Construct the INSERT SQL statement with placeholders (?)
            String placeholders = String.join(",", Collections.nCopies(values.size(), "?"));
            String sql = "INSERT INTO " + currentTable + " (" + String.join(",", currentColumns) + ") VALUES (" + placeholders + ")";
            System.out.println("Executing SQL: " + sql + " with values: " + values); // Debugging

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                // Set each value as a parameter in the prepared statement
                for (int i = 0; i < values.size(); i++) {
                    ps.setString(i + 1, values.get(i)); // JDBC parameters are 1-indexed
                }
                int inserted = ps.executeUpdate(); // Execute the insert statement

                if (inserted > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Row inserted successfully into '" + currentTable + "'.");
                    // Optionally clear the input fields for a new insert
                    inputsBox.getChildren().forEach(node -> {
                        if (node instanceof TextField) {
                            ((TextField) node).clear();
                        }
                    });
                } else {
                    showAlert(Alert.AlertType.WARNING, "No Row Inserted", "Insert operation completed, but no rows were affected. Check your inputs.");
                }
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Insert Failed", "Error inserting row: " + ex.getMessage());
            }
        });

        // Arrange components in a VBox
        VBox vbox = new VBox(10, title, new Label("Select Table:"), cbTables,
                new Label("Enter Values:"), inputsBox, btnInsert);
        mainPane.getChildren().add(vbox);
    }

    /**
     * Displays the User Interface for selecting and viewing data from a table.
     * Data is displayed in a TableView.
     */
    private void showSelectUI() {
        Label title = new Label("Select Data");
        title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");

        ComboBox<String> tablesCombo = new ComboBox<>();
        tablesCombo.setPromptText("Select Table");
        loadTablesInto(tablesCombo); // Populate tables ComboBox

        Button loadBtn = new Button("Load Data");
        loadBtn.setDisable(true); // Disable load button until a table is selected

        // Enable load button when a table is selected
        tablesCombo.setOnAction(e -> loadBtn.setDisable(tablesCombo.getSelectionModel().getSelectedItem() == null));

        // Action for the Load Data button
        loadBtn.setOnAction(e -> {
            String selected = tablesCombo.getSelectionModel().getSelectedItem();
            if (selected == null) return; // Should not happen due to disable logic

            currentTable = selected;
            currentColumns = getColumnsForTable(selected); // Get columns for the selected table

            if (currentColumns == null || currentColumns.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Columns Found", "Failed to retrieve columns for table '" + selected + "'. Cannot load data.");
                return;
            }
            loadTableData(selected); // Call method to load data into TableView
        });

        // Arrange components in a VBox
        VBox vbox = new VBox(10, title, new Label("Select Table:"), tablesCombo, loadBtn, tableView);
        mainPane.getChildren().add(vbox);
    }

    /**
     * Loads all data from the specified table into the TableView (without checkboxes).
     * This method is used for 'Select' and 'Update' operations.
     * @param table The name of the table to load data from.
     */
    private void loadTableData(String table) {
        tableView.getColumns().clear(); // Clear existing columns
        tableData.clear(); // Clear existing data

        // Dynamically create TableColumns based on 'currentColumns'
        for (int i = 0; i < currentColumns.size(); i++) {
            final int index = i; // Effective final for lambda
            TableColumn<RowData, String> col = new TableColumn<>(currentColumns.get(i));
            // Set CellValueFactory to retrieve the corresponding StringProperty from RowData
            col.setCellValueFactory(cd -> cd.getValue().getData().get(index));
            col.setPrefWidth(150); // Set a preferred width for columns
            tableView.getColumns().add(col);
        }

        String sql = "SELECT * FROM " + table;
        System.out.println("Executing SQL: " + sql); // Debugging

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                List<String> rowVals = new ArrayList<>();
                // Extract string value for each column in the current row
                for (String c : currentColumns) {
                    rowVals.add(rs.getString(c));
                }
                tableData.add(new RowData(rowVals)); // Add a new RowData object to the observable list
            }
            tableView.setItems(tableData); // Set the loaded data to the TableView
            if (tableData.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "No Data", "Table '" + table + "' is empty.");
            }
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Data Load Failed", "Failed to load data for table '" + table + "': " + ex.getMessage());
        }
    }

    /**
     * Displays the User Interface for updating existing rows in a table.
     * Allows selection of a single row and opens a dialog for editing.
     */
    private void showUpdateUI() {
        Label title = new Label("Update Rows");
        title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");

        ComboBox<String> tablesCombo = new ComboBox<>();
        tablesCombo.setPromptText("Select Table");
        loadTablesInto(tablesCombo); // Populate tables ComboBox

        Button loadBtn = new Button("Load Data");
        loadBtn.setDisable(true);

        Button updateBtn = new Button("Update Selected Row");
        updateBtn.setDisable(true); // Disable update button until a row is selected

        tablesCombo.setOnAction(e -> loadBtn.setDisable(tablesCombo.getSelectionModel().getSelectedItem() == null));

        loadBtn.setOnAction(e -> {
            String selected = tablesCombo.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            currentTable = selected;
            currentColumns = getColumnsForTable(selected);

            if (currentColumns == null || currentColumns.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Columns Found", "Failed to retrieve columns for table '" + selected + "'. Cannot update.");
                return;
            }
            loadTableData(selected); // Load data without checkboxes for selection clarity
            updateBtn.setDisable(false); // Enable update button once data is loaded
        });

        // Enable update button only when a row is selected in the TableView
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateBtn.setDisable(newVal == null);
        });


        updateBtn.setOnAction(e -> {
            RowData selectedRow = tableView.getSelectionModel().getSelectedItem();
            if (selectedRow == null) {
                showAlert(Alert.AlertType.WARNING, "No Row Selected", "Please select a row to update.");
                return;
            }
            showUpdateDialog(selectedRow); // Open dialog to edit the selected row
        });

        // Arrange components in a VBox
        VBox vbox = new VBox(10, title, new Label("Select Table:"), tablesCombo, loadBtn, tableView, updateBtn);
        mainPane.getChildren().add(vbox);
    }

    /**
     * Displays a dialog window allowing the user to edit data for a specific row.
     * @param row The RowData object whose data needs to be updated.
     */
    private void showUpdateDialog(RowData row) {
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Update Row");
        dialog.setHeaderText("Edit values for the selected row:");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 150, 10, 10));

        List<TextField> fields = new ArrayList<>();
        // Create a label and TextField for each column, pre-filling with current data
        for (int i = 0; i < currentColumns.size(); i++) {
            Label lbl = new Label(currentColumns.get(i) + ":");
            TextField tf = new TextField(row.getData().get(i).get());
            grid.add(lbl, 0, i); // Add label to column 0
            grid.add(tf, 1, i); // Add text field to column 1
            fields.add(tf); // Store TextField for later retrieval of updated values
        }

        dialog.getDialogPane().setContent(grid);
        // Add OK and Cancel buttons to the dialog
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Convert the dialog result (ButtonType) into a list of updated values if OK is pressed
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                List<String> vals = new ArrayList<>();
                for (TextField tf : fields) {
                    vals.add(tf.getText().trim());
                }
                return vals;
            }
            return null; // Return null if Cancel is pressed
        });

        Optional<List<String>> result = dialog.showAndWait(); // Show dialog and wait for result

        result.ifPresent(vals -> {
            try {
                // Assumption: The first column (index 0) is the Primary Key (PK)
                // This is a common but potentially fragile assumption.
                // For robust applications, actual PK metadata should be queried.
                String pkCol = currentColumns.get(0);
                String pkValue = row.getData().get(0).get(); // Original PK value for WHERE clause

                StringBuilder sql = new StringBuilder("UPDATE " + currentTable + " SET ");
                // Build the SET clause for the UPDATE statement
                for (int i = 0; i < currentColumns.size(); i++) {
                    sql.append(currentColumns.get(i)).append(" = ?");
                    if (i < currentColumns.size() - 1) {
                        sql.append(", ");
                    }
                }
                sql.append(" WHERE ").append(pkCol).append(" = ?"); // Add WHERE clause based on PK

                System.out.println("Executing SQL: " + sql.toString()); // Debugging
                System.out.println("With values: " + vals + " and PK: " + pkValue);

                try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                    int paramIndex = 1;
                    // Set parameters for the SET clause (all columns)
                    for (String val : vals) {
                        ps.setString(paramIndex++, val);
                    }
                    // Set the parameter for the WHERE clause (PK value)
                    ps.setString(paramIndex, pkValue);

                    int updated = ps.executeUpdate(); // Execute the update

                    if (updated > 0) {
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Row updated successfully.");
                        loadTableData(currentTable); // Reload table data to reflect changes
                    } else {
                        showAlert(Alert.AlertType.WARNING, "No Row Updated", "Update operation completed, but no rows were affected. Check if the PK value still exists.");
                    }
                }
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Update Failed", "Error updating row: " + ex.getMessage());
            } catch (IndexOutOfBoundsException ex) {
                // Catches if currentColumns is empty or first column is missing
                showAlert(Alert.AlertType.ERROR, "Update Error", "Primary key column could not be determined. Please ensure the table has columns and the first column is suitable for identification.");
            }
        });
    }

    /**
     * Displays the User Interface for deleting rows from a table.
     * Includes a checkbox column to allow selection of multiple rows for deletion.
     */
    private void showDeleteUI() {
        Label title = new Label("Delete Rows");
        title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");

        ComboBox<String> tablesCombo = new ComboBox<>();
        tablesCombo.setPromptText("Select Table");
        loadTablesInto(tablesCombo);

        Button loadBtn = new Button("Load Data");
        loadBtn.setDisable(true);

        Button deleteBtn = new Button("Delete Selected Rows");
        deleteBtn.setDisable(true); // Disable delete button until data is loaded and selected

        tablesCombo.setOnAction(e -> loadBtn.setDisable(tablesCombo.getSelectionModel().getSelectedItem() == null));

        // Action for the Load Data button: loads data with checkboxes
        loadBtn.setOnAction(e -> {
            String selected = tablesCombo.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            currentTable = selected;
            currentColumns = getColumnsForTable(selected);

            if (currentColumns == null || currentColumns.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Columns Found", "Failed to retrieve columns for table '" + selected + "'. Cannot delete.");
                return;
            }
            loadTableDataWithCheckboxes(selected); // Load data with checkboxes
            deleteBtn.setDisable(false); // Enable delete button after data is loaded
        });

        // Action for the Delete Selected Rows button
        deleteBtn.setOnAction(e -> {
            List<RowData> selectedRows = new ArrayList<>();
            // Filter and collect all rows that are marked as selected
            for (RowData row : tableData) {
                if (row.isSelected()) {
                    selectedRows.add(row);
                }
            }

            if (selectedRows.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Rows Selected", "No rows are selected for deletion. Please select one or more rows.");
                return;
            }

            // Confirmation dialog before proceeding with deletion
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Deletion");
            confirm.setHeaderText("Delete Confirmation");
            confirm.setContentText("Are you sure you want to delete " + selectedRows.size() + " selected row(s)? This action cannot be undone.");
            Optional<ButtonType> result = confirm.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    // Assumption: The first column is the Primary Key for deletion.
                    String pkCol = currentColumns.get(0);
                    String sql = "DELETE FROM " + currentTable + " WHERE " + pkCol + " = ?";
                    System.out.println("Preparing batch delete SQL: " + sql); // Debugging

                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        for (RowData rd : selectedRows) {
                            ps.setString(1, rd.getData().get(0).get()); // Set the PK value for current row
                            ps.addBatch(); // Add the delete operation to the batch
                        }
                        int[] results = ps.executeBatch(); // Execute the batch operations

                        int deletedCount = 0;
                        for (int count : results) {
                            if (count > 0) { // If update count is positive, row was deleted
                                deletedCount += count;
                            }
                        }
                        showAlert(Alert.AlertType.INFORMATION, "Deletion Complete", deletedCount + " row(s) deleted successfully.");
                        loadTableDataWithCheckboxes(currentTable); // Reload data to show updated state
                    }
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Deletion Failed", "Error during deletion: " + ex.getMessage());
                } catch (IndexOutOfBoundsException ex) {
                    showAlert(Alert.AlertType.ERROR, "Deletion Error", "Primary key column could not be determined for deletion.");
                }
            }
        });

        // Arrange components in a VBox
        VBox vbox = new VBox(10, title, new Label("Select Table:"), tablesCombo, loadBtn, tableView, deleteBtn);
        mainPane.getChildren().add(vbox);
    }

    /**
     * Loads table data into the TableView, including a checkbox column for row selection.
     * This method is specifically designed for the "Delete Rows" functionality, allowing multi-selection.
     * @param table The name of the table to load data from.
     */
    private void loadTableDataWithCheckboxes(String table) {
        tableView.getColumns().clear(); // Clear existing columns
        tableData.clear(); // Clear existing data

        // Create the "Select" checkbox column
        TableColumn<RowData, Boolean> selectColumn = new TableColumn<>("Select");
        selectColumn.setPrefWidth(60);
        // Bind the checkbox state to the 'selected' property of RowData
        selectColumn.setCellValueFactory(param -> param.getValue().selectedProperty());
        // Use CheckBoxTableCell to render actual checkboxes in the column cells
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));

        // Create a "Select All" checkbox for the header of the 'Select' column
        CheckBox selectAllCheckbox = new CheckBox();
        selectAllCheckbox.setOnAction(event -> {
            boolean selected = selectAllCheckbox.isSelected();
            // Iterate through all rows and set their 'selected' property
            tableData.forEach(rowData -> rowData.setSelected(selected));
        });
        selectColumn.setGraphic(selectAllCheckbox); // Set the "Select All" checkbox as the header graphic

        tableView.getColumns().add(selectColumn); // Add the checkbox column to the TableView first

        // Dynamically add data columns based on 'currentColumns'
        for (int i = 0; i < currentColumns.size(); i++) {
            final int index = i;
            TableColumn<RowData, String> col = new TableColumn<>(currentColumns.get(i));
            col.setCellValueFactory(cd -> cd.getValue().getData().get(index));
            col.setPrefWidth(150);
            tableView.getColumns().add(col);
        }

        String sql = "SELECT * FROM " + table;
        System.out.println("Executing SQL (with checkboxes): " + sql); // Debugging

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                List<String> rowVals = new ArrayList<>();
                for (String c : currentColumns) {
                    rowVals.add(rs.getString(c)); // Get string value for each column
                }
                RowData newRow = new RowData(rowVals);
                // Add a listener to each row's 'selected' property.
                // This listener ensures the "Select All" checkbox's state is updated
                // (selected, unselected, or indeterminate) when individual rows are selected/deselected.
                newRow.selectedProperty().addListener((obs, oldVal, newVal) -> updateSelectAllCheckbox(selectAllCheckbox));
                tableData.add(newRow);
            }
            tableView.setItems(tableData); // Set the loaded data to the TableView

            if (tableData.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "No Data", "Table '" + table + "' is empty.");
            }
            // Perform an initial update of the "Select All" checkbox state
            updateSelectAllCheckbox(selectAllCheckbox);
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Data Load Failed", "Failed to load data for table '" + table + "': " + ex.getMessage());
        }
    }

    /**
     * Updates the state of the "Select All" checkbox (selected, unselected, or indeterminate)
     * based on the current selection status of rows in the tableData.
     * @param selectAllCheckbox The CheckBox instance used as the "Select All" control.
     */
    private void updateSelectAllCheckbox(CheckBox selectAllCheckbox) {
        if (tableData.isEmpty()) {
            selectAllCheckbox.setIndeterminate(false);
            selectAllCheckbox.setSelected(false);
            return;
        }

        // Count how many rows are currently selected
        long selectedCount = tableData.stream().filter(RowData::isSelected).count();

        if (selectedCount == tableData.size()) {
            // All rows are selected
            selectAllCheckbox.setIndeterminate(false);
            selectAllCheckbox.setSelected(true);
        } else if (selectedCount == 0) {
            // No rows are selected
            selectAllCheckbox.setIndeterminate(false);
            selectAllCheckbox.setSelected(false);
        } else {
            // Some rows are selected, but not all
            selectAllCheckbox.setIndeterminate(true);
        }
    }

    /**
     * Displays the User Interface for dropping (deleting) an entire table from the database.
     */
    private void showDropUI() {
        Label title = new Label("Drop Table");
        title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");

        ComboBox<String> tablesCombo = new ComboBox<>();
        tablesCombo.setPromptText("Select Table to Drop");
        loadTablesInto(tablesCombo); // Populate tables ComboBox

        Button dropBtn = new Button("Drop Table");
        dropBtn.setDisable(true); // Disable until a table is selected

        tablesCombo.setOnAction(e -> dropBtn.setDisable(tablesCombo.getSelectionModel().getSelectedItem() == null));

        dropBtn.setOnAction(e -> {
            String selected = tablesCombo.getSelectionModel().getSelectedItem();
            if (selected == null) return; // Should not happen due to disable logic

            // Confirmation dialog for a destructive operation
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Drop");
            confirm.setHeaderText("Irreversible Action!");
            confirm.setContentText("Are you absolutely sure you want to DROP table '" + selected + "'? This action will permanently delete the table and all its data, and cannot be undone.");
            Optional<ButtonType> res = confirm.showAndWait();

            if (res.isPresent() && res.get() == ButtonType.OK) {
                try (Statement st = conn.createStatement()) {
                    // Execute DROP TABLE with CASCADE CONSTRAINTS to handle foreign key dependencies
                    String sql = "DROP TABLE " + selected + " CASCADE CONSTRAINTS";
                    System.out.println("Executing SQL: " + sql); // Debugging
                    st.execute(sql);
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Table '" + selected + "' dropped successfully.");
                    tablesCombo.getItems().remove(selected); // Remove from ComboBox list
                    dropBtn.setDisable(true); // Disable button as the table is gone
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Drop Failed", "Failed to drop table: " + ex.getMessage());
                }
            }
        });

        // Arrange components in a VBox
        VBox vbox = new VBox(10, title, new Label("Select Table:"), tablesCombo, dropBtn);
        mainPane.getChildren().add(vbox);
    }

    /**
     * Displays the User Interface for truncating (emptying) a table in the database.
     */
    private void showTruncateUI() {
        Label title = new Label("Truncate Table");
        title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");

        ComboBox<String> tablesCombo = new ComboBox<>();
        tablesCombo.setPromptText("Select Table to Truncate");
        loadTablesInto(tablesCombo); // Populate tables ComboBox

        Button truncateBtn = new Button("Truncate Table");
        truncateBtn.setDisable(true); // Disable until a table is selected

        tablesCombo.setOnAction(e -> truncateBtn.setDisable(tablesCombo.getSelectionModel().getSelectedItem() == null));

        truncateBtn.setOnAction(e -> {
            String selected = tablesCombo.getSelectionModel().getSelectedItem();
            if (selected == null) return; // Should not happen

            // Confirmation dialog for a destructive operation
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Truncate");
            confirm.setHeaderText("Irreversible Action!");
            confirm.setContentText("Are you sure you want to TRUNCATE table '" + selected + "'? This will remove all rows permanently and reset the high-water mark, and cannot be undone.");
            Optional<ButtonType> res = confirm.showAndWait();

            if (res.isPresent() && res.get() == ButtonType.OK) {
                try (Statement st = conn.createStatement()) {
                    String sql = "TRUNCATE TABLE " + selected;
                    System.out.println("Executing SQL: " + sql); // Debugging
                    st.execute(sql);
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Table '" + selected + "' truncated successfully.");
                    // After truncating, reload the data view (if any) to show it's empty
                    if (currentTable != null && currentTable.equalsIgnoreCase(selected)) {
                        loadTableDataWithCheckboxes(currentTable); // Or loadTableData if you prefer
                    }
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Truncate Failed", "Failed to truncate table: " + ex.getMessage());
                }
            }
        });

        // Arrange components in a VBox
        VBox vbox = new VBox(10, title, new Label("Select Table:"), tablesCombo, truncateBtn);
        mainPane.getChildren().add(vbox);
    }

    public static void main(String[] args) {
        launch(args);
    }
}