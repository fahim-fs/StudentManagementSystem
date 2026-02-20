package frontend.controller;

import common.User;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import backend.service.UserService;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    // ── FXML fields ──────────────────────────────────────────────────────────
    @FXML private TextField        f_name;
    @FXML private TextField        l_name;
    @FXML private TextField        father_name;
    @FXML private TextField        mother_name;
    @FXML private DatePicker       date_birth;
    @FXML private TextField        phone_number;
    @FXML private TextField        address;
    @FXML private RadioButton      g_male;
    @FXML private RadioButton      g_female;
    @FXML private ToggleGroup      genderGroup;
    @FXML private TextField        session;
    @FXML private ComboBox<String> department;
    @FXML private TextField        username;
    @FXML private PasswordField    password;
    @FXML private PasswordField    confirm_password;
    @FXML private Label            errorLabel;
    @FXML private Button           registerBtn;
    @FXML private Hyperlink        signInLink;

    // ── Initialization ────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        department.setItems(FXCollections.observableArrayList(
                "Computer Science & Engineering",
                "Electrical & Electronic Engineering",
                "Business Administration",
                "Civil Engineering",
                "Mechanical Engineering",
                "Architecture",
                "Law",
                "English",
                "Mathematics",
                "Physics"
        ));

        // Button hover effect
        registerBtn.setOnMouseEntered(e ->
                registerBtn.setStyle(registerBtn.getStyle().replace("#3498db", "#2980b9")));
        registerBtn.setOnMouseExited(e ->
                registerBtn.setStyle(registerBtn.getStyle().replace("#2980b9", "#3498db")));

        // Real-time password match indicator
        confirm_password.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !newVal.equals(password.getText())) {
                confirm_password.setStyle(confirm_password.getStyle()
                        + "-fx-border-color: #e74c3c;");
            } else {
                confirm_password.setStyle(confirm_password.getStyle()
                        .replace("-fx-border-color: #e74c3c;", "-fx-border-color: #2ecc71;"));
            }
        });
    }

    // ── Register handler ──────────────────────────────────────────────────────
    @FXML
    private void handleRegister(ActionEvent event) {
        clearError();

        // ── Validation ────────────────────────────────────────────────────────
        if (isBlank(f_name))               { showError("First name is required.");         return; }
        if (isBlank(l_name))               { showError("Last name is required.");          return; }
        if (isBlank(father_name))          { showError("Father's name is required.");      return; }
        if (isBlank(mother_name))          { showError("Mother's name is required.");      return; }
        if (date_birth.getValue() == null) { showError("Date of birth is required.");      return; }
        if (date_birth.getValue().isAfter(LocalDate.now())) {
                                             showError("Date of birth cannot be in the future."); return; }
        if (isBlank(phone_number))         { showError("Phone number is required.");       return; }
        if (!phone_number.getText().trim().matches("^\\+?880\\d{10}$")) {
                                             showError("Enter a valid Bangladeshi phone number."); return; }
        if (isBlank(address))              { showError("Address is required.");            return; }
        if (genderGroup.getSelectedToggle() == null) {
                                             showError("Please select a gender.");         return; }
        if (isBlank(session))              { showError("Session is required.");            return; }
        if (department.getValue() == null || department.getValue().isBlank()) {
                                             showError("Please select a department.");     return; }
        if (isBlank(username))             { showError("Username is required.");           return; }
        if (username.getText().trim().length() < 4) {
                                             showError("Username must be at least 4 characters."); return; }
        if (isBlank(password))             { showError("Password is required.");           return; }
        if (password.getText().length() < 6) {
                                             showError("Password must be at least 6 characters."); return; }
        if (!password.getText().equals(confirm_password.getText())) {
                                             showError("Passwords do not match.");         return; }

        String gender = g_male.isSelected() ? "Male" : "Female";

        User newUser = new User(
                f_name.getText().trim(),
                l_name.getText().trim(),
                father_name.getText().trim(),
                mother_name.getText().trim(),
                date_birth.getValue(),
                phone_number.getText().trim(),
                address.getText().trim(),
                gender,
                session.getText().trim(),
                department.getValue(),
                username.getText().trim(),
                password.getText()
        );

        boolean success = UserService.register(newUser);

        if (success) {
            showSuccess("Account created successfully!");
            clearForm();
        } else {
            showError("Registration failed. Username may already exist.");
        }
    }

    // ── Navigate to Sign-In ───────────────────────────────────────────────────
    @FXML
    private void goToSignIn(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/frontend/view/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not open the Sign-In page.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private boolean isBlank(TextField tf) {
        return tf.getText() == null || tf.getText().isBlank();
    }

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-padding: 0 0 6 0;");
        errorLabel.setText("⚠  " + msg);
    }

    private void showSuccess(String msg) {
        errorLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px; -fx-padding: 0 0 6 0;");
        errorLabel.setText("✔  " + msg);
    }

    private void clearError() {
        errorLabel.setText("");
    }

    private void clearForm() {
        f_name.clear();          l_name.clear();
        father_name.clear();     mother_name.clear();
        date_birth.setValue(null);
        phone_number.clear();    address.clear();
        genderGroup.selectToggle(null);
        session.clear();         department.setValue(null);
        username.clear();        password.clear();
        confirm_password.clear();
    }
}
