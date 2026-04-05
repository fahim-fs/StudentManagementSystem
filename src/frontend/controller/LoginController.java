package frontend.controller;

import backend.model.Role;
import backend.model.Student;
import backend.service.UserService;
import common.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField     username;
    @FXML private PasswordField password;
    @FXML private Label         messageLabel;
    @FXML private Button        loginBtn;
    @FXML private Hyperlink     forgotLink;
    @FXML private Hyperlink     registerLink;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Button hover effect — same as RegisterController
        loginBtn.setOnMouseEntered(e ->
                loginBtn.setStyle(loginBtn.getStyle().replace("#3498db", "#2980b9")));
        loginBtn.setOnMouseExited(e ->
                loginBtn.setStyle(loginBtn.getStyle().replace("#2980b9", "#3498db")));

        // Allow pressing Enter on password field to trigger login
        password.setOnAction(e -> handleLogin(new ActionEvent(password, null)));
    }

    //  Login handler 
    @FXML
    private void handleLogin(ActionEvent event) {
        clearMessage();

        String user = username.getText().trim();
        String pass = password.getText();

        if (user.isEmpty()) { showError("Username is required."); return; }
        if (pass.isEmpty()) { showError("Password is required."); return; }

        // Student object পাবো
        Student student = UserService.login(user, pass);

        if (student != null) {
            SessionManager.setStudent(student); // ← session এ save
            showSuccess("Login successful! Redirecting...");
            navigateByRole(event, student.getRole());
        } else {
            showError("Invalid username or password.");
            password.clear();
        }
    }

    //  Forgot password ─
    @FXML
    private void handleForgotPassword(ActionEvent event) {
        // TODO: navigate to a forgot-password / reset screen
        showError("Please contact your administrator to reset your password.");
    }

    //  Go to Register 
    @FXML
    private void goToRegister(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/frontend/view/registerForm.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not open the Register page.");
        }
    }

    //  Navigate to Dashboard after login 
    private void navigateToDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/frontend/view/StudentDashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  Helpers
    private void showError(String msg) {
        messageLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-padding: 0 0 8 0;");
        messageLabel.setText("⚠  " + msg);
    }

    private void showSuccess(String msg) {
        messageLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px; -fx-padding: 0 0 8 0;");
        messageLabel.setText("✔  " + msg);
    }

    private void clearMessage() {
        messageLabel.setText("");
    }
    private void navigateByRole(ActionEvent event, Role role) {
        String fxmlPath;
        switch (role) {
            case STUDENT: fxmlPath = "/frontend/view/StudentDashboard.fxml"; break;
            case ADMIN:   fxmlPath = "/frontend/view/AdminDashboard.fxml";   break;
            case FACULTY: fxmlPath = "/frontend/view/FacultyDashboard.fxml"; break;
            default: showError("Unknown role."); return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 720));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Dashboard load failed: " + e.getMessage());
        }
    }
}
