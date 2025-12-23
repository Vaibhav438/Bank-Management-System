import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

public class AdminPanel extends JFrame {
    private Map<Integer, BankAccount> accounts;
    private JTextArea logArea;

    public AdminPanel(Map<Integer, BankAccount> accounts) {
        this.accounts = accounts;
        setTitle("Admin Panel");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Account list panel
        JPanel accountPanel = new JPanel(new BorderLayout());
        JList<BankAccount> accountList = new JList<>(new Vector<>(accounts.values()));
        accountList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                BankAccount account = (BankAccount) value;
                setText(String.format("Account #%d - %s - Balance: $%.2f", 
                    account.getAccountNumber(), 
                    account.getAccountHolder(), 
                    account.getBalance()));
                return this;
            }
        });
        accountPanel.add(new JScrollPane(accountList), BorderLayout.CENTER);
        
        // Action buttons
        JPanel buttonPanel = new JPanel();
        JButton viewDetailsBtn = new JButton("View Details");
        JButton deleteBtn = new JButton("Delete Account");
        
        buttonPanel.add(viewDetailsBtn);
        buttonPanel.add(deleteBtn);
        accountPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        
        mainPanel.add(accountPanel, BorderLayout.WEST);
        mainPanel.add(logScroll, BorderLayout.CENTER);
        
        // Add action listeners
        viewDetailsBtn.addActionListener(e -> {
            BankAccount selected = accountList.getSelectedValue();
            if (selected != null) {
                showAccountDetails(selected);
            }
        });
        
        deleteBtn.addActionListener(e -> {
            BankAccount selected = accountList.getSelectedValue();
            if (selected != null) {
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete account #" + selected.getAccountNumber() + "?",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    deleteAccount(selected);
                }
            }
        });
        
        add(mainPanel);
    }
    
    private void showAccountDetails(BankAccount account) {
        JDialog dialog = new JDialog(this, "Account Details", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea details = new JTextArea();
        details.setEditable(false);
        details.append("Account Number: " + account.getAccountNumber() + "\n");
        details.append("Account Holder: " + account.getAccountHolder() + "\n");
        details.append("Balance: $" + account.getBalance() + "\n");
        details.append("\nTransaction History:\n");
        for (String transaction : account.getTransactionHistory()) {
            details.append(transaction + "\n");
        }
        
        panel.add(new JScrollPane(details), BorderLayout.CENTER);
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void deleteAccount(BankAccount account) {
        try {
            DatabaseConnection.deleteAccount(account.getAccountNumber());
            accounts.remove(account.getAccountNumber());
            logArea.append("Deleted account #" + account.getAccountNumber() + "\n");
            dispose();
            new AdminPanel(accounts).setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting account: " + ex.getMessage());
        }
    }
} 