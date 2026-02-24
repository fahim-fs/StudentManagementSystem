package frontend.controller;

import backend.model.Student;
import common.SessionManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class StudentDashboardController implements Initializable {

    // ── Sidebar buttons ───────────────────────────────────────────────────────
    @FXML private Button btnHome;
    @FXML private Button btnCourse;
    @FXML private Button btnExamMarks;
    @FXML private Button btnResult;
    @FXML private Button btnAttendance;
    @FXML private Button btnNotices;
    @FXML private Button btnFee;
    @FXML private Button btnDue;
    @FXML private Button btnLogout;

    // ── Top bar ───────────────────────────────────────────────────────────────
    @FXML private Label pageTitle;
    @FXML private Label sessionLabel;

    // ── Student info ──────────────────────────────────────────────────────────
    @FXML private Label studentNameLabel;
    @FXML private Label studentIdLabel;
    @FXML private Label departmentLabel;

    // ── Home summary cards ────────────────────────────────────────────────────
    @FXML private Label overallAttendanceLabel;
    @FXML private Label dueAmountLabel;
    @FXML private Label noticeCountLabel;
    @FXML private Label courseCountLabel;

    // ── Course-wise attendance ────────────────────────────────────────────────
    @FXML private ComboBox<String> courseDropdown;
    @FXML private Label classesHeldLabel;
    @FXML private Label classesAttendedLabel;
    @FXML private Label courseAttendancePctLabel;
    @FXML private Label attendanceStatusLabel;

    // ── Course list ───────────────────────────────────────────────────────────
    @FXML private VBox courseListContainer;
    @FXML private VBox homeContent;

    // ── Active button tracking ────────────────────────────────────────────────
    private Button activeBtn;

    private static final String ACTIVE_STYLE =
            "-fx-background-color: #2a9d8f; -fx-text-fill: white; -fx-font-size: 13px;" +
                    "-fx-alignment: CENTER-LEFT; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand;";
    private static final String INACTIVE_STYLE =
            "-fx-background-color: transparent; -fx-text-fill: #cdd9e0; -fx-font-size: 13px;" +
                    "-fx-alignment: CENTER-LEFT; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        activeBtn = btnHome;

        // Hover effects for all nav buttons
        Button[] navBtns = {btnHome, btnCourse, btnExamMarks, btnResult,
                btnAttendance, btnNotices, btnFee, btnDue};
        for (Button btn : navBtns) {
            btn.setOnMouseEntered(e -> {
                if (btn != activeBtn)
                    btn.setStyle(INACTIVE_STYLE.replace("transparent", "#243b4a"));
            });
            btn.setOnMouseExited(e -> {
                if (btn != activeBtn) btn.setStyle(INACTIVE_STYLE);
            });
        }

        // Populate course dropdown (TODO: load from DB)
        courseDropdown.setItems(FXCollections.observableArrayList(
                "CSE101 - Programming Fundamentals",
                "CSE102 - Data Structures",
                "MATH201 - Discrete Mathematics",
                "ENG101 - English Communication",
                "PHY101 - Physics"
        ));

        // Load student info (TODO: pass logged-in student object)
        loadStudentInfo();
        loadSummaryCards();
        loadCourseList();
    }

    // ── Load student info ─────────────────────────────────────────────────────
    private void loadStudentInfo() {
        Student student = SessionManager.getStudent(); // ← session থেকে নাও
        if (student == null) return;

        studentNameLabel.setText(student.getFirstName() + " " + student.getLastName());
        studentIdLabel.setText("ID: " + student.getId());
        departmentLabel.setText(student.getDepartment());
        sessionLabel.setText("Session: " + student.getSession());
    }

    // ── Load summary cards ────────────────────────────────────────────────────
    private void loadSummaryCards() {
        // TODO: fetch from DB
        overallAttendanceLabel.setText("82%");
        dueAmountLabel.setText("৳ 5,000");
        noticeCountLabel.setText("3");
        courseCountLabel.setText("5");
    }

    // ── Load course list ──────────────────────────────────────────────────────
    private void loadCourseList() {
        // TODO: fetch enrolled courses from DB
        String[][] courses = {
                {"CSE101", "Programming Fundamentals", "Dr. Rahman"},
                {"CSE102", "Data Structures",          "Dr. Islam"},
                {"MATH201","Discrete Mathematics",      "Dr. Hossain"},
                {"ENG101", "English Communication",     "Mr. Karim"},
                {"PHY101", "Physics",                   "Dr. Ahmed"}
        };

        courseListContainer.getChildren().clear();
        for (String[] c : courses) {
            Label row = new Label("• " + c[0] + "  —  " + c[1] + "   |   " + c[2]);
            row.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e; -fx-padding: 6 0;");
            courseListContainer.getChildren().add(row);
        }
    }

    // ── Course-wise attendance dropdown ───────────────────────────────────────
    @FXML
    private void loadCourseAttendance(ActionEvent event) {
        String selected = courseDropdown.getValue();
        if (selected == null) return;

        // TODO: fetch real data from DB based on selected course
        classesHeldLabel.setText("30");
        classesAttendedLabel.setText("25");
        courseAttendancePctLabel.setText("83%");

        int pct = 83;
        if (pct >= 75) {
            attendanceStatusLabel.setText("✔ Regular");
            attendanceStatusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        } else {
            attendanceStatusLabel.setText("✘ Irregular");
            attendanceStatusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        }
    }

    // ── Sidebar navigation ────────────────────────────────────────────────────
    @FXML private void showHome(ActionEvent e)               { setActive(btnHome);       pageTitle.setText("Home"); }
    @FXML private void showCourseRegistration(ActionEvent e) { setActive(btnCourse);     pageTitle.setText("Course Registration"); }
    @FXML private void showExamMarks(ActionEvent e)          { setActive(btnExamMarks);  pageTitle.setText("Exam Marks"); }
    @FXML private void showResult(ActionEvent e)             { setActive(btnResult);     pageTitle.setText("Result"); }
    @FXML private void showAttendance(ActionEvent e)         { setActive(btnAttendance); pageTitle.setText("Attendance"); }
    @FXML private void showNotices(ActionEvent e)            { setActive(btnNotices);    pageTitle.setText("Notices"); }
    @FXML private void showFeePayment(ActionEvent e)         { setActive(btnFee);        pageTitle.setText("Fee Payment"); }
    @FXML private void showDueAmount(ActionEvent e)          { setActive(btnDue);        pageTitle.setText("Due Amount"); }

    private void setActive(Button btn) {
        if (activeBtn != null) activeBtn.setStyle(INACTIVE_STYLE);
        btn.setStyle(ACTIVE_STYLE);
        activeBtn = btn;
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clear();
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/frontend/view/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Logout error: " + e.getMessage());
        }
    }
}
