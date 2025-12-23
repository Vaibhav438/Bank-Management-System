import javax.swing.*;
import java.awt.*;

public class ErrorHandler {
    private static final String ERROR_TITLE = "Error";
    private static final String WARNING_TITLE = "Warning";
    private static final String INFO_TITLE = "Information";
    
    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            ERROR_TITLE,
            JOptionPane.ERROR_MESSAGE
        );
    }
    
    public static void showWarning(Component parent, String message) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            WARNING_TITLE,
            JOptionPane.WARNING_MESSAGE
        );
    }
    
    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            INFO_TITLE,
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    public static void showDatabaseError(Component parent, Exception ex) {
        showError(parent, "Database error: " + ex.getMessage());
    }
    
    public static void showValidationError(Component parent, String message) {
        showError(parent, "Validation error: " + message);
    }
    
    public static void showSystemError(Component parent, Exception ex) {
        showError(parent, "System error: " + ex.getMessage());
    }
} 