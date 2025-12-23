import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankGUI {
    private static final Map<Integer, BankAccount> accounts = new HashMap<>();
    private static int accountNumberCounter = 1001;
    private static JFrame mainFrame;
    private static JLabel statusBar;

    static {
        try {
            int maxAccountNumber = DatabaseConnection.getMaxAccountNumber();
            accountNumberCounter = maxAccountNumber + 1;
            System.out.println("Initialized account counter to: " + accountNumberCounter);
            
            loadExistingAccounts();
        } catch (SQLException e) {
            e.printStackTrace();
            ErrorHandler.showDatabaseError(null, e);
        }
    }

    private static void loadExistingAccounts() {
        try {
            List<BankAccount> existingAccounts = DatabaseConnection.searchAccountsByName("");
            for (BankAccount account : existingAccounts) {
                accounts.put(account.getAccountNumber(), account);
            }
            System.out.println("Loaded " + existingAccounts.size() + " existing accounts");
        } catch (SQLException e) {
            System.err.println("Error loading existing accounts: " + e.getMessage());
            ErrorHandler.showDatabaseError(null, e);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                LoginScreen.showLoginFrame();
            } catch (Exception e) {
                ErrorHandler.showSystemError(null, e);
            }
        });
    }

    public static BankAccount getAccount(int accNo) {
        try {
            BankAccount acc = accounts.get(accNo);
            if (acc != null) {
                return acc;
            }

            acc = DatabaseConnection.getAccount(accNo);
            if (acc != null) {
                accounts.put(accNo, acc);
                System.out.println("Loaded account " + accNo + " from database");
            }
            return acc;
        } catch (SQLException e) {
            System.err.println("Error getting account " + accNo + ": " + e.getMessage());
            e.printStackTrace();
            ErrorHandler.showDatabaseError(null, e);
            return null;
        }
    }

    public static void showUserPanel(BankAccount account) {
        JFrame userFrame = new JFrame("Welcome, " + account.getAccountHolder());
        userFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        userFrame.setSize(700, 400);
        userFrame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        JLabel welcomeLabel = new JLabel("Welcome, " + account.getAccountHolder(), SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        mainPanel.add(welcomeLabel, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        String[] options = {"Deposit", "Withdraw", "Transfer", "Check Balance", "Transaction History", "Change PIN", "Logout"};

        for (String option : options) {
            JButton btn = new JButton(option);
            btn.addActionListener(e -> {
                try {
                    switch (option) {
                        case "Deposit":
                            account.depositAction(userFrame);
                            break;
                        case "Withdraw":
                            account.withdrawAction(userFrame);
                            break;
                        case "Transfer":
                            account.transferAction(userFrame, accounts);
                            break;
                        case "Check Balance":
                            ErrorHandler.showInfo(userFrame, "Balance: $" + account.getBalance());
                            break;
                        case "Transaction History":
                            account.showHistory(userFrame);
                            break;
                        case "Change PIN":
                            account.changePinAction(userFrame);
                            break;
                        case "Logout":
                            userFrame.dispose();
                            LoginScreen.showLoginFrame();
                            break;
                    }
                } catch (RuntimeException ex) {
                    ErrorHandler.showError(userFrame, ex.getMessage());
                }
            });
            panel.add(btn);
        }

        mainPanel.add(panel, BorderLayout.CENTER);
        userFrame.add(mainPanel);
        userFrame.setVisible(true);
    }

    public static void showAdminPanel() {
        mainFrame = new JFrame("Admin - Bank Management");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(800, 500);
        mainFrame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JLabel headerLabel = new JLabel("Admin - Bank Management", SwingConstants.CENTER);
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        String[] buttons = {"Create Account", "Delete Account", "Check Balance", "Transaction History", "Search Account", "Lock Account", "Unlock Account", "Logout"};

        for (String label : buttons) {
            JButton btn = new JButton(label);
            btn.addActionListener(e -> {
                try {
                    switch (label) {
                        case "Create Account":
                            createAccount();
                            break;
                        case "Delete Account":
                            deleteAccount();
                            break;
                        case "Check Balance":
                            checkBalance();
                            break;
                        case "Transaction History":
                            showTransactionHistory();
                            break;
                        case "Search Account":
                            searchAccount();
                            break;
                        case "Lock Account":
                            lockAccount();
                            break;
                        case "Unlock Account":
                            unlockAccount();
                            break;
                        case "Logout":
                            logout();
                            break;
                    }
                } catch (Exception ex) {
                    ErrorHandler.showSystemError(mainFrame, ex);
                }
            });
            buttonPanel.add(btn);
        }

        statusBar = new JLabel(" Logged in as Admin");
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        mainFrame.add(mainPanel);
        mainFrame.setVisible(true);
    }

    private static void createAccount() {
        JPanel panel = new JPanel(new GridLayout(3, 2));
        JTextField nameField = new JTextField();
        JTextField depositField = new JTextField();
        JPasswordField pinField = new JPasswordField();

        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Initial Deposit:"));
        panel.add(depositField);
        panel.add(new JLabel("PIN (4 digits):"));
        panel.add(pinField);

        int result = JOptionPane.showConfirmDialog(mainFrame, panel, "Create Account", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText().trim();
                double deposit = Double.parseDouble(depositField.getText().trim());
                String pin = new String(pinField.getPassword()).trim();

                if (name.isEmpty() || pin.isEmpty()) {
                    throw new Exception("All fields must be filled.");
                }
                if (pin.length() != 4 || !pin.matches("\\d{4}")) {
                    throw new Exception("PIN must be exactly 4 digits.");
                }
                if (deposit < 0) {
                    throw new Exception("Deposit must be non-negative.");
                }

                BankAccount acc = new BankAccount(accountNumberCounter, name, deposit, pin);
                try {
                    acc.saveToDatabase();
                    accounts.put(accountNumberCounter, acc);
                    ErrorHandler.showInfo(mainFrame, "Account created successfully. Account #: " + accountNumberCounter);
                    accountNumberCounter++;
                } catch (Exception e) {
                    ErrorHandler.showDatabaseError(mainFrame, e);
                }
            } catch (Exception e) {
                ErrorHandler.showValidationError(mainFrame, e.getMessage());
            }
        }
    }

    private static void deleteAccount() {
        String accStr = JOptionPane.showInputDialog(mainFrame, "Enter account number to delete:");
        try {
            int accNo = Integer.parseInt(accStr);
            DatabaseConnection.deleteAccount(accNo);
            BankAccount removed = accounts.remove(accNo);
            ErrorHandler.showInfo(mainFrame, (removed != null) ? "Account deleted." : "Account not found.");
        } catch (Exception e) {
            ErrorHandler.showSystemError(mainFrame, e);
        }
    }

    private static void checkBalance() {
        String accStr = JOptionPane.showInputDialog(mainFrame, "Enter account number:");
        try {
            int accNo = Integer.parseInt(accStr);
            BankAccount acc = getAccount(accNo);
            if (acc != null) {
                ErrorHandler.showInfo(mainFrame, 
                    "Account #" + accNo + "\n" +
                    "Holder: " + acc.getAccountHolder() + "\n" +
                    "Balance: $" + acc.getBalance() + "\n" +
                    "Status: " + (acc.isLocked() ? "LOCKED" : "Active"));
            } else {
                ErrorHandler.showError(mainFrame, "Account not found.");
            }
        } catch (Exception e) {
            ErrorHandler.showValidationError(mainFrame, "Invalid input.");
        }
    }

    private static void showTransactionHistory() {
        String accStr = JOptionPane.showInputDialog(mainFrame, "Enter account number:");
        try {
            int accNo = Integer.parseInt(accStr);
            BankAccount acc = getAccount(accNo);
            if (acc != null) {
                acc.showHistory(mainFrame);
            } else {
                ErrorHandler.showError(mainFrame, "Account not found.");
            }
        } catch (Exception e) {
            ErrorHandler.showValidationError(mainFrame, "Invalid input.");
        }
    }

    private static void searchAccount() {
        String keyword = JOptionPane.showInputDialog(mainFrame, "Enter account number or name to search:");
        if (keyword == null || keyword.trim().isEmpty()) {
            ErrorHandler.showValidationError(mainFrame, "Search term cannot be empty.");
            return;
        }

        try {
            int accNo = Integer.parseInt(keyword);
            BankAccount acc = getAccount(accNo);
            if (acc != null) {
                ErrorHandler.showInfo(mainFrame, 
                    "Account #" + accNo + "\n" +
                    "Holder: " + acc.getAccountHolder() + "\n" +
                    "Balance: $" + acc.getBalance() + "\n" +
                    "Status: " + (acc.isLocked() ? "LOCKED" : "Active"));
                return;
            }
        } catch (NumberFormatException e) {
        }

        try {
            List<BankAccount> foundAccounts = DatabaseConnection.searchAccountsByName(keyword);
            if (!foundAccounts.isEmpty()) {
                StringBuilder results = new StringBuilder();
                for (BankAccount acc : foundAccounts) {
                    accounts.put(acc.getAccountNumber(), acc);
                    results.append("Account #" + acc.getAccountNumber() + "\n")
                           .append("Holder: " + acc.getAccountHolder() + "\n")
                           .append("Balance: $" + acc.getBalance() + "\n")
                           .append("Status: " + (acc.isLocked() ? "LOCKED" : "Active") + "\n\n");
                }
                JTextArea area = new JTextArea(results.toString());
                area.setEditable(false);
                area.setFont(new Font("Monospaced", Font.PLAIN, 14));
                JOptionPane.showMessageDialog(mainFrame, new JScrollPane(area), "Search Results", JOptionPane.INFORMATION_MESSAGE);
            } else {
                ErrorHandler.showInfo(mainFrame, "No accounts found.");
            }
        } catch (SQLException ex) {
            ErrorHandler.showDatabaseError(mainFrame, ex);
        }
    }

    private static void lockAccount() {
        String accStr = JOptionPane.showInputDialog(mainFrame, "Enter account number to lock:");
        try {
            int accNo = Integer.parseInt(accStr);
            BankAccount acc = getAccount(accNo);
            if (acc != null) {
                acc.lockManually();
                ErrorHandler.showInfo(mainFrame, "Account locked successfully.");
            } else {
                ErrorHandler.showError(mainFrame, "Account not found.");
            }
        } catch (Exception e) {
            ErrorHandler.showSystemError(mainFrame, e);
        }
    }

    private static void unlockAccount() {
        String accStr = JOptionPane.showInputDialog(mainFrame, "Enter account number to unlock:");
        try {
            int accNo = Integer.parseInt(accStr);
            BankAccount acc = getAccount(accNo);
            if (acc != null) {
                acc.unlockManually();
                ErrorHandler.showInfo(mainFrame, "Account unlocked successfully.");
            } else {
                ErrorHandler.showError(mainFrame, "Account not found.");
            }
        } catch (Exception e) {
            ErrorHandler.showSystemError(mainFrame, e);
        }
    }

    private static void logout() {
        mainFrame.dispose();
        LoginScreen.showLoginFrame();
    }
}