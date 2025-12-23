import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import java.sql.SQLException;

public class BankAccount {
    private int accountNumber;
    private String accountHolder;
    private double balance;
    private String pin;
    private int failedPinAttempts = 0;
    private List<String> transactionHistory;

    public BankAccount(int accountNumber, String accountHolder, double balance, String pin) {
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.balance = balance;
        this.pin = pin;
        this.failedPinAttempts = 0;
        this.transactionHistory = new ArrayList<>();
        saveToDatabase();
        logTransaction("Account created with balance: $" + balance);
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public double getBalance() {
        return balance;
    }

    public String getPin() {
        return pin;
    }

    public List<String> getTransactionHistory() {
        return transactionHistory;
    }

    public int getFailedAttempts() {
        return failedPinAttempts;
    }

    public boolean verifyPin(String inputPin) {
        if (isLocked()) {
            throw new RuntimeException("Account is locked due to too many failed attempts.");
        }
        if (this.pin.equals(inputPin)) {
            failedPinAttempts = 0;
            saveToDatabase();
            return true;
        } else {
            failedPinAttempts++;
            saveToDatabase();
            if (isLocked()) {
                logTransaction("Account locked due to 3 incorrect PIN attempts.");
            }
            throw new RuntimeException("Incorrect PIN.");
        }
    }

    public void lockManually() {
        this.failedPinAttempts = 3;
        logTransaction("Account manually locked by admin.");
        saveToDatabase();
    }

    public void unlockManually() {
        this.failedPinAttempts = 0;
        logTransaction("Account manually unlocked by admin.");
        saveToDatabase();
    }

    public boolean isLocked() {
        return failedPinAttempts >= 3;
    }

    public void depositAction(JFrame parent) {
        if (isLocked()) {
            JOptionPane.showMessageDialog(parent, "Account is locked. Please contact admin.");
            return;
        }
        String amtStr = JOptionPane.showInputDialog(parent, "Enter amount to deposit:");
        try {
            double amt = Double.parseDouble(amtStr);
            if (amt <= 0) throw new NumberFormatException();
            balance += amt;
            saveToDatabase();
            logTransaction("Deposited $" + amt);
            JOptionPane.showMessageDialog(parent, "Deposit successful.");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(parent, "Invalid amount.");
        }
    }

    public void withdrawAction(JFrame parent) {
        if (isLocked()) {
            JOptionPane.showMessageDialog(parent, "Account is locked. Please contact admin.");
            return;
        }
        String amtStr = JOptionPane.showInputDialog(parent, "Enter amount to withdraw:");
        if (amtStr == null) return;

        JPasswordField pinField = new JPasswordField();
        int pinResult = JOptionPane.showConfirmDialog(parent, pinField, "Enter PIN", JOptionPane.OK_CANCEL_OPTION);
        if (pinResult != JOptionPane.OK_OPTION) return;

        try {
            double amt = Double.parseDouble(amtStr);
            if (verifyPin(new String(pinField.getPassword()))) {
                if (amt > 0 && amt <= balance) {
                    balance -= amt;
                    saveToDatabase();
                    logTransaction("Withdrew $" + amt);
                    JOptionPane.showMessageDialog(parent, "Withdrawal successful.");
                } else {
                    JOptionPane.showMessageDialog(parent, "Insufficient balance or invalid amount.");
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, e.getMessage());
        }
    }

    public void transferAction(JFrame parent, Map<Integer, BankAccount> accounts) {
        if (isLocked()) {
            JOptionPane.showMessageDialog(parent, "Account is locked. Please contact admin.");
            return;
        }
        JTextField accField = new JTextField();
        JTextField amtField = new JTextField();
        JPasswordField pinField = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("To Account #:")); panel.add(accField);
        panel.add(new JLabel("Amount:")); panel.add(amtField);
        panel.add(new JLabel("Your PIN:")); panel.add(pinField);

        int result = JOptionPane.showConfirmDialog(parent, panel, "Transfer", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int toAcc = Integer.parseInt(accField.getText());
                double amt = Double.parseDouble(amtField.getText());
                String pinInput = new String(pinField.getPassword());

                if (!verifyPin(pinInput)) return;

                BankAccount receiver = accounts.get(toAcc);
                if (receiver == null) {
                    JOptionPane.showMessageDialog(parent, "Target account not found.");
                    return;
                }
                if (amt > 0 && amt <= balance) {
                    balance -= amt;
                    receiver.balance += amt;
                    saveToDatabase();
                    receiver.saveToDatabase();
                    logTransaction("Transferred $" + amt + " to Account #" + toAcc);
                    receiver.logTransaction("Received $" + amt + " from Account #" + accountNumber);
                    JOptionPane.showMessageDialog(parent, "Transfer successful.");
                } else {
                    JOptionPane.showMessageDialog(parent, "Insufficient balance or invalid amount.");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(parent, e.getMessage());
            }
        }
    }

    public void showHistory(JFrame parent) {
        JTextArea area = new JTextArea();
        area.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 14));
        area.setEditable(false);
        for (String t : transactionHistory) {
            area.append(t + "\n");
        }
        JOptionPane.showMessageDialog(parent, new JScrollPane(area), "Transaction History", JOptionPane.INFORMATION_MESSAGE);
    }

    public void changePinAction(JFrame parent) {
        if (isLocked()) {
            JOptionPane.showMessageDialog(parent, "Account is locked. Please contact admin.");
            return;
        }
        JPasswordField oldPin = new JPasswordField();
        JPasswordField newPin = new JPasswordField();
        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Old PIN:")); panel.add(oldPin);
        panel.add(new JLabel("New PIN:")); panel.add(newPin);

        int result = JOptionPane.showConfirmDialog(parent, panel, "Change PIN", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String oldP = new String(oldPin.getPassword());
                String newP = new String(newPin.getPassword());
                
                if (newP.length() != 4 || !newP.matches("\\d{4}")) {
                    JOptionPane.showMessageDialog(parent, "PIN must be exactly 4 digits.");
                    return;
                }
                
                if (verifyPin(oldP)) {
                    this.pin = newP;
                    saveToDatabase();
                    logTransaction("PIN changed successfully.");
                    JOptionPane.showMessageDialog(parent, "PIN changed successfully.");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(parent, e.getMessage());
            }
        }
    }

    private void logTransaction(String msg) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String transaction = "[" + timestamp + "] " + msg;
        transactionHistory.add(transaction);
        try {
            DatabaseConnection.saveTransaction(accountNumber, transaction);
            System.out.println("Transaction logged: " + transaction);
        } catch (SQLException e) {
            System.err.println("Error logging transaction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveToDatabase() {
        try {
            DatabaseConnection.saveAccount(this);
            System.out.println("Account #" + accountNumber + " saved to database");
        } catch (SQLException e) {
            System.err.println("Error saving account to database: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Error saving account data.\nPlease try again or contact support.\nError: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public static BankAccount loadFromDatabase(int accountNumber) {
        try {
            return DatabaseConnection.getAccount(accountNumber);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        return "Account #" + accountNumber + "\nHolder: " + accountHolder + "\nBalance: $" + balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
        saveToDatabase();
    }

    public void setPin(String pin) {
        this.pin = pin;
        saveToDatabase();
    }

    public void setFailedAttempts(int attempts) {
        this.failedPinAttempts = attempts;
        saveToDatabase();
    }

    public void addTransaction(String transaction) {
        transactionHistory.add(transaction);
    }
}
