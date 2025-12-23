import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/bank_db";
    private static final String USER = "root";
    private static final String PASSWORD = "vaibhav1209";
    private static Connection connection;

    static {
        try {
            System.out.println("Attempting to load MySQL driver...");
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Driver loaded successfully");
            
            System.out.println("Attempting to connect to database...");
            System.out.println("URL: " + URL);
            System.out.println("User: " + USER);
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connection successful");
            
            System.out.println("Creating tables...");
            createTables();
            System.out.println("Tables created successfully");
        } catch (ClassNotFoundException e) {
            ErrorHandler.showError(null, 
                "MySQL JDBC Driver not found.\n" +
                "Please ensure mysql-connector-java is in your classpath.\n" +
                "Error: " + e.getMessage());
            System.exit(1);
        } catch (SQLException e) {
            ErrorHandler.showError(null, 
                "Failed to connect to database.\n" +
                "Please check:\n" +
                "1. MySQL is running\n" +
                "2. Username and password are correct\n" +
                "3. Database 'bank_db' exists\n" +
                "Error Code: " + e.getErrorCode() + "\n" +
                "SQL State: " + e.getSQLState() + "\n" +
                "Message: " + e.getMessage());
            System.exit(1);
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createTables() throws SQLException {
        try {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE DATABASE IF NOT EXISTS bank_db");
                stmt.execute("USE bank_db");
            }
    
            String createAdminsTable = "CREATE TABLE IF NOT EXISTS admins (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(50) UNIQUE NOT NULL," +
                    "password VARCHAR(255) NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

            String createAccountsTable = "CREATE TABLE IF NOT EXISTS accounts (" +
                    "account_number INT PRIMARY KEY," +
                    "account_holder VARCHAR(100) NOT NULL," +
                    "balance DOUBLE NOT NULL," +
                    "pin VARCHAR(4) NOT NULL," +
                    "failed_attempts INT DEFAULT 0," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)";

            String createTransactionsTable = "CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "account_number INT NOT NULL," +
                    "timestamp DATETIME NOT NULL," +
                    "description VARCHAR(255) NOT NULL," +
                    "FOREIGN KEY (account_number) REFERENCES accounts(account_number) ON DELETE CASCADE)";

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createAdminsTable);
                stmt.execute(createAccountsTable);
                stmt.execute(createTransactionsTable);
            }

            createDefaultAdmin();
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
            throw e;
        }
    }

    private static void createDefaultAdmin() throws SQLException {
        String checkAdmin = "SELECT COUNT(*) FROM admins";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(checkAdmin)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertAdmin = "INSERT INTO admins (username, password) VALUES (?, ?)";
                try (PreparedStatement pstmt = connection.prepareStatement(insertAdmin)) {
                    pstmt.setString(1, "admin");
                    pstmt.setString(2, "admin123");
                    pstmt.executeUpdate();
                }
            }
        }
    }

    public static boolean verifyAdmin(String username, String password) throws SQLException {
        String sql = "SELECT password FROM admins WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password").equals(password);
            }
        }
        return false;
    }

    public static boolean createAdmin(String username, String password) throws SQLException {
        String sql = "INSERT INTO admins (username, password) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password); 
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                return false;
            }
            throw e;
        }
    }

    public static void saveAccount(BankAccount account) throws SQLException {
        connection.setAutoCommit(false);
        try {
            String sql = "INSERT INTO accounts (account_number, account_holder, balance, pin, failed_attempts) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "account_holder = VALUES(account_holder), " +
                    "balance = VALUES(balance), " +
                    "pin = VALUES(pin), " +
                    "failed_attempts = VALUES(failed_attempts)";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, account.getAccountNumber());
                pstmt.setString(2, account.getAccountHolder());
                pstmt.setDouble(3, account.getBalance());
                pstmt.setString(4, account.getPin());
                pstmt.setInt(5, account.getFailedAttempts());
                
                int result = pstmt.executeUpdate();
                connection.commit();
                System.out.println("Account #" + account.getAccountNumber() + " " + 
                    (result == 1 ? "inserted" : "updated") + " successfully");
            }
        } catch (SQLException e) {
            connection.rollback();
            System.err.println("Error saving account: " + e.getMessage());
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public static void saveTransaction(int accountNumber, String description) throws SQLException {
        connection.setAutoCommit(false);
        try {
            String sql = "INSERT INTO transactions (account_number, timestamp, description) VALUES (?, NOW(), ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, accountNumber);
                pstmt.setString(2, description);
                pstmt.executeUpdate();
                connection.commit();
                System.out.println("Transaction saved for account " + accountNumber + ": " + description);
            }
        } catch (SQLException e) {
            connection.rollback();
            System.err.println("Error saving transaction: " + e.getMessage());
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public static int getMaxAccountNumber() throws SQLException {
        String sql = "SELECT MAX(account_number) FROM accounts";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int maxAccount = rs.getInt(1);
                return maxAccount > 0 ? maxAccount : 1000;
            }
            return 1000;
        }
    }

    public static BankAccount getAccount(int accountNumber) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                BankAccount account = new BankAccount(
                    rs.getInt("account_number"),
                    rs.getString("account_holder"),
                    rs.getDouble("balance"),
                    rs.getString("pin")
                );
                account.setFailedAttempts(rs.getInt("failed_attempts"));
                
                // Load transaction history
                List<String> transactions = getTransactions(accountNumber);
                for (String transaction : transactions) {
                    account.addTransaction(transaction);
                }
                
                return account;
            }
        }
        return null;
    }

    public static List<String> getTransactions(int accountNumber) throws SQLException {
        List<String> transactions = new ArrayList<>();
        String sql = "SELECT timestamp, description FROM transactions WHERE account_number = ? ORDER BY timestamp DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, accountNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String timestamp = rs.getTimestamp("timestamp").toString();
                    String description = rs.getString("description");
                    transactions.add("[" + timestamp + "] " + description);
                }
            }
        }
        return transactions;
    }

    public static void deleteAccount(int accountNumber) throws SQLException {
        connection.setAutoCommit(false);
        try {
            String deleteTransactions = "DELETE FROM transactions WHERE account_number = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteTransactions)) {
                pstmt.setInt(1, accountNumber);
                pstmt.executeUpdate();
            }
            
            String deleteAccount = "DELETE FROM accounts WHERE account_number = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteAccount)) {
                pstmt.setInt(1, accountNumber);
                pstmt.executeUpdate();
            }
            
            connection.commit();
            System.out.println("Account #" + accountNumber + " deleted successfully");
        } catch (SQLException e) {
            connection.rollback();
            System.err.println("Error deleting account: " + e.getMessage());
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public static List<String> getAllAdmins() throws SQLException {
        List<String> adminList = new ArrayList<>();
        String sql = "SELECT username FROM admins";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                adminList.add(rs.getString("username"));
            }
        }
        return adminList;
    }

    public static boolean deleteAdmin(String username) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM admins";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) <= 1) {
                return false;
            }
        }

        String sql = "DELETE FROM admins WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            int result = pstmt.executeUpdate();
            return result > 0;
        }
    }

    public static List<BankAccount> searchAccountsByName(String name) throws SQLException {
        List<BankAccount> foundAccounts = new ArrayList<>();
        String sql = "SELECT * FROM accounts WHERE account_holder LIKE ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + name + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BankAccount account = new BankAccount(
                        rs.getInt("account_number"),
                        rs.getString("account_holder"),
                        rs.getDouble("balance"),
                        rs.getString("pin")
                    );
                    foundAccounts.add(account);
                }
            }
        }
        return foundAccounts;
    }
} 