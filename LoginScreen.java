import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class LoginScreen {
    public static void showLoginFrame() {
        JFrame frame = new JFrame("Login");
        frame.setSize(400, 350);  // Made taller to accommodate new button
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JComboBox<String> roleBox = new JComboBox<>(new String[]{"Admin", "User"});
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JButton registerButton = new JButton("Register New Admin");
        JButton deleteAdminButton = new JButton("Delete Admin");

        panel.add(new JLabel("Role:"));
        panel.add(roleBox);
        panel.add(new JLabel("Username / Account #:"));
        panel.add(userField);
        panel.add(new JLabel("Password / PIN:"));
        panel.add(passField);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> {
            String role = roleBox.getSelectedItem().toString();
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();

            if (role.equals("Admin")) {
                try {
                    if (DatabaseConnection.verifyAdmin(username, password)) {
                        frame.dispose();
                        BankGUI.showAdminPanel();
                    } else {
                        ErrorHandler.showError(frame, "Invalid admin credentials.");
                    }
                } catch (SQLException ex) {
                    ErrorHandler.showDatabaseError(frame, ex);
                }
            } else {
                try {
                    int accNo = Integer.parseInt(username);
                    BankAccount acc = BankGUI.getAccount(accNo);
                    if (acc == null) {
                        ErrorHandler.showError(frame, "Account not found.");
                        return;
                    }
                    if (acc.isLocked()) {
                        ErrorHandler.showWarning(frame, 
                            "Account is locked due to too many failed attempts.\nPlease contact admin to unlock.");
                        return;
                    }
                    try {
                        if (acc.verifyPin(password)) {
                            frame.dispose();
                            BankGUI.showUserPanel(acc);
                        }
                    } catch (RuntimeException ex) {
                        ErrorHandler.showError(frame, ex.getMessage());
                    }
                } catch (NumberFormatException ex) {
                    ErrorHandler.showValidationError(frame, "Invalid account number format.");
                } catch (Exception ex) {
                    ErrorHandler.showSystemError(frame, ex);
                }
            }
        });

        registerButton.addActionListener(e -> {
            if (roleBox.getSelectedItem().toString().equals("Admin")) {
                String username = JOptionPane.showInputDialog(frame, "Enter new admin username:");
                if (username == null || username.trim().isEmpty()) return;

                JPasswordField passwordField = new JPasswordField();
                int result = JOptionPane.showConfirmDialog(frame, passwordField, "Enter new admin password", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    String password = new String(passwordField.getPassword());
                    try {
                        if (DatabaseConnection.createAdmin(username, password)) {
                            ErrorHandler.showInfo(frame, "Admin registered successfully!");
                        } else {
                            ErrorHandler.showError(frame, "Username already exists.");
                        }
                    } catch (SQLException ex) {
                        ErrorHandler.showDatabaseError(frame, ex);
                    }
                }
            } else {
                ErrorHandler.showWarning(frame, "Only admin registration is available.");
            }
        });

        deleteAdminButton.addActionListener(e -> {
            if (roleBox.getSelectedItem().toString().equals("Admin")) {
                try {
                    String username = userField.getText().trim();
                    String password = new String(passField.getPassword()).trim();
                    
                    if (DatabaseConnection.verifyAdmin(username, password)) {
                        List<String> admins = DatabaseConnection.getAllAdmins();
                        String[] adminArray = admins.toArray(new String[0]);
                        
                        if (adminArray.length <= 1) {
                            ErrorHandler.showWarning(frame, "Cannot delete the last admin account.");
                            return;
                        }

                        String selectedAdmin = (String) JOptionPane.showInputDialog(
                            frame,
                            "Select admin to delete:",
                            "Delete Admin",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            adminArray,
                            adminArray[0]
                        );

                        if (selectedAdmin != null) {
                            int confirm = JOptionPane.showConfirmDialog(
                                frame,
                                "Are you sure you want to delete admin: " + selectedAdmin + "?",
                                "Confirm Deletion",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE
                            );

                            if (confirm == JOptionPane.YES_OPTION) {
                                if (DatabaseConnection.deleteAdmin(selectedAdmin)) {
                                    ErrorHandler.showInfo(frame, "Admin deleted successfully!");
                                } else {
                                    ErrorHandler.showError(frame, "Failed to delete admin.");
                                }
                            }
                        }
                    } else {
                        ErrorHandler.showWarning(frame, "Please login as admin first.");
                    }
                } catch (SQLException ex) {
                    ErrorHandler.showDatabaseError(frame, ex);
                }
            } else {
                ErrorHandler.showWarning(frame, "Please select Admin role to delete admin accounts.");
            }
        });

        JPanel bottom = new JPanel(new GridLayout(3, 1, 5, 5));
        bottom.add(loginButton);
        bottom.add(registerButton);
        bottom.add(deleteAdminButton);

        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);
        frame.setVisible(true);
    }
}