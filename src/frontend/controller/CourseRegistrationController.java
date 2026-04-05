package frontend.controller;

import backend.dao.CourseRegistrationDAO;
import backend.model.Student;
import common.CourseData;
import common.CourseData.Course;
import common.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CourseRegistrationController implements Initializable {

    @FXML private ComboBox<String> levelTermDropdown;
    @FXML private Label            totalCreditLabel;
    @FXML private Label            remainingCreditLabel;
    @FXML private Label            messageLabel;
    @FXML private VBox             availableCourseList;
    @FXML private VBox             registeredCourseList;

    private final CourseRegistrationDAO dao = new CourseRegistrationDAO();
    private Student student;
    private int currentLevel;
    private int currentTerm;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        student = SessionManager.getStudent();

        currentLevel = student.getLevel() > 0 ? student.getLevel() : 1;
        currentTerm  = student.getTerm()  > 0 ? student.getTerm()  : 1;

        // Dropdown populate
        levelTermDropdown.getItems().addAll(
            "Level 1 - Term 1", "Level 1 - Term 2",
            "Level 2 - Term 1", "Level 2 - Term 2",
            "Level 3 - Term 1", "Level 3 - Term 2",
            "Level 4 - Term 1", "Level 4 - Term 2"
        );

        int defaultIndex = ((currentLevel - 1) * 2) + (currentTerm - 1);
        levelTermDropdown.getSelectionModel().select(Math.max(0, Math.min(defaultIndex, 7)));

        loadCourses();

        levelTermDropdown.setOnAction(e -> {
            int idx = levelTermDropdown.getSelectionModel().getSelectedIndex();
            currentLevel = (idx / 2) + 1;
            currentTerm  = (idx % 2) + 1;
            loadCourses();
        });
    }

    private void loadCourses() {
        availableCourseList.getChildren().clear();
        registeredCourseList.getChildren().clear();
        clearMessage();

        List<Course> allCourses  = CourseData.getCourses(currentLevel, currentTerm);
        List<String> registered  = dao.getRegisteredCourseCodes(student.getId(), currentLevel, currentTerm);
        List<String> approved    = dao.getApprovedCourseCodes(student.getId(), currentLevel, currentTerm);
        double       totalCredit = dao.getTotalCredit(student.getId(), currentLevel, currentTerm);

        updateCreditDisplay(totalCredit);

        for (Course course : allCourses) {
            if (registered.contains(course.code)) {
                registeredCourseList.getChildren().add(
                        buildRegisteredRow(course, approved.contains(course.code)));
            } else {
                availableCourseList.getChildren().add(buildAvailableRow(course));
            }
        }

        if (availableCourseList.getChildren().isEmpty())
            availableCourseList.getChildren().add(emptyLabel("No available courses."));
        if (registeredCourseList.getChildren().isEmpty())
            registeredCourseList.getChildren().add(emptyLabel("No registered courses yet."));
    }

    private HBox buildAvailableRow(Course course) {
        HBox row = styledRow("#f9f9f9", "#e0e0e0");
        VBox info = courseInfo(course, "#2c3e50", "");
        Region spacer = spacer();
        Button addBtn = new Button("+ Add");
        addBtn.setStyle("-fx-background-color: #2a9d8f; -fx-text-fill: white;" +
                        "-fx-font-size: 12px; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 14;");
        addBtn.setOnAction(e -> handleAdd(course));
        row.getChildren().addAll(info, spacer, addBtn);
        return row;
    }

    private HBox buildRegisteredRow(Course course, boolean isApproved) {
        String bg     = isApproved ? "#eafaf1" : "#fffbf0";
        String border = isApproved ? "#2ecc71" : "#f39c12";
        HBox row = styledRow(bg, border);

        String icon  = isApproved ? "✔  " : "⏳  ";
        String color = isApproved ? "#27ae60" : "#e67e22";
        VBox info = courseInfo(course, color, icon);
        Region spacer = spacer();

        String badgeText  = isApproved ? "APPROVED" : "PENDING";
        Label badge = new Label(badgeText);
        badge.setStyle("-fx-font-size: 10px; -fx-text-fill: " + color + ";" +
                       "-fx-font-weight: bold; -fx-padding: 2 8;" +
                       "-fx-border-color: " + color + "; -fx-border-radius: 10;");

        row.getChildren().addAll(info, spacer, badge);

        if (!isApproved) {
            Button dropBtn = new Button("Drop");
            dropBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;" +
                             "-fx-font-size: 12px; -fx-background-radius: 6;" +
                             "-fx-cursor: hand; -fx-padding: 6 14;");
            dropBtn.setOnAction(e -> handleDrop(course));
            row.getChildren().add(dropBtn);
        }
        return row;
    }

    private void handleAdd(Course course) {
        // Duplicate check
        List<String> registered = dao.getRegisteredCourseCodes(
                student.getId(), currentLevel, currentTerm);
        if (registered.contains(course.code)) {
            showError(course.code + " is already registered."); return;
        }
        // Credit limit check
        double used = dao.getTotalCredit(student.getId(), currentLevel, currentTerm);
        if (used + course.credit > CourseData.MAX_CREDIT) {
            showError("Credit limit exceeded! Remaining: " +
                      (CourseData.MAX_CREDIT - used) + " credits"); return;
        }
        if (dao.registerCourse(student.getId(), currentLevel, currentTerm, course)) {
            showSuccess(course.code + " added! Waiting for admin approval.");
            loadCourses();
        } else {
            showError("Failed to add course. Please try again.");
        }
    }

    private void handleDrop(Course course) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Drop Course");
        confirm.setHeaderText("Drop " + course.code + "?");
        confirm.setContentText("This course is pending approval. Drop it?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                boolean success = dao.dropCourse(
                        student.getId(), currentLevel, currentTerm, course.code);
                if (success) {
                    showSuccess(course.code + " dropped successfully.");
                    loadCourses();
                } else {
                    showError("Could not drop " + course.code +
                              ". Only PENDING courses can be dropped.");
                }
            }
        });
    }

    private void updateCreditDisplay(double total) {
        totalCreditLabel.setText("Total: " + total + " / " +
                                 (int) CourseData.MAX_CREDIT + " credits");
        double rem = CourseData.MAX_CREDIT - total;
        remainingCreditLabel.setText("Remaining: " + rem + " credits");
        remainingCreditLabel.setStyle(rem < 3
                ? "-fx-font-size: 12px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;"
                : "-fx-font-size: 12px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }

    private HBox styledRow(String bg, String border) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10 14; -fx-background-color: " + bg + ";" +
                     "-fx-background-radius: 8; -fx-border-color: " + border +
                     "; -fx-border-radius: 8;");
        return row;
    }

    private VBox courseInfo(Course course, String color, String prefix) {
        VBox v = new VBox(3);
        Label code = new Label(prefix + course.code + "  —  " + course.name);
        code.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label detail = new Label("Credit: " + course.credit + "  |  Faculty: " + course.faculty);
        detail.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        v.getChildren().addAll(code, detail);
        return v;
    }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px; -fx-padding: 8 0;");
        return l;
    }

    private void showError(String msg) {
        messageLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        messageLabel.setText("⚠  " + msg);
    }

    private void showSuccess(String msg) {
        messageLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
        messageLabel.setText("✔  " + msg);
    }

    private void clearMessage() { messageLabel.setText(""); }
}
