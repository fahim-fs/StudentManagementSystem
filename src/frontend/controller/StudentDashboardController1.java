package frontend.controller;

import backend.dao.AttendanceDAO;
import backend.dao.AttendanceDAO.AttendanceRecord;
import backend.dao.CourseRegistrationDAO;
import backend.database.DatabaseConnection;
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
import java.sql.*;
import java.util.List;
import java.util.ResourceBundle;

public class StudentDashboardController1 implements Initializable {

    @FXML private VBox mainContentArea;

    //  Sidebar buttons 
    @FXML private Button btnHome;
    @FXML private Button btnCourse;
    @FXML private Button btnExamMarks;
    @FXML private Button btnResult;
    @FXML private Button btnAttendance;
    @FXML private Button btnNotices;
    @FXML private Button btnFee;
    @FXML private Button btnDue;
    @FXML private Button btnLogout;

    //  Top bar 
    @FXML private Label pageTitle;
    @FXML private Label sessionLabel;

    //  Student info
    @FXML private Label studentNameLabel;
    @FXML private Label studentIdLabel;
    @FXML private Label departmentLabel;

    //  Home summary cards 
    @FXML private Label overallAttendanceLabel;
    @FXML private Label dueAmountLabel;
    @FXML private Label noticeCountLabel;
    @FXML private Label courseCountLabel;

    //  Course-wise attendance 
    @FXML private ComboBox<String> courseDropdown;
    @FXML private Label classesHeldLabel;
    @FXML private Label classesAttendedLabel;
    @FXML private Label courseAttendancePctLabel;
    @FXML private Label attendanceStatusLabel;

    //  Course list 
    @FXML private VBox courseListContainer;
    @FXML private VBox homeContent;

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

        Button[] navBtns = {btnHome, btnCourse, btnExamMarks, btnResult,
                            btnAttendance, btnNotices, btnFee, btnDue};
        for (Button btn : navBtns) {
            btn.setOnMouseEntered(e -> { if (btn != activeBtn) btn.setStyle(INACTIVE_STYLE.replace("transparent", "#243b4a")); });
            btn.setOnMouseExited(e ->  { if (btn != activeBtn) btn.setStyle(INACTIVE_STYLE); });
        }

        loadStudentInfo();
        loadSummaryCards();
        loadCourseDropdown();
        loadCourseList();
    }

    //  Student info
    private void loadStudentInfo() {
        Student s = SessionManager.getStudent();
        if (s == null) return;
        studentNameLabel.setText(s.getFirstName() + " " + s.getLastName());
        studentIdLabel.setText("ID: " + s.getId());
        departmentLabel.setText(s.getDepartment() != null ? s.getDepartment() : "—");
        sessionLabel.setText("Session: " + (s.getSession() != null ? s.getSession() : "—"));
    }

    //  Summary cards — DB থেকে real data 
    private void loadSummaryCards() {
        Student s = SessionManager.getStudent();
        if (s == null) return;

        // Overall attendance
        AttendanceDAO aDao = new AttendanceDAO();
        double overall = aDao.getOverallAttendance(s.getId());
        overallAttendanceLabel.setText(overall + "%");
        overallAttendanceLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: "
                + (overall >= 75 ? "#27ae60" : "#e74c3c") + ";");

        // Due amount
        dueAmountLabel.setText("৳ 0"); // TODO: fee table হলে এখান থেকে নেবে

        // Notices count
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notices");
             ResultSet rs = ps.executeQuery()) {
            noticeCountLabel.setText(rs.next() ? String.valueOf(rs.getInt(1)) : "0");
        } catch (Exception e) { noticeCountLabel.setText("0"); }

        // Enrolled (approved) courses count
        CourseRegistrationDAO crDao = new CourseRegistrationDAO();
        int level = s.getLevel() > 0 ? s.getLevel() : 1;
        int term  = s.getTerm()  > 0 ? s.getTerm()  : 1;
        List<String> approved = crDao.getApprovedCourseCodes(s.getId(), level, term);
        courseCountLabel.setText(String.valueOf(approved.size()));
    }

    //  Course dropdown — DB থেকে approved courses
    private void loadCourseDropdown() {
        Student s = SessionManager.getStudent();
        if (s == null) return;

        CourseRegistrationDAO crDao = new CourseRegistrationDAO();
        int level = s.getLevel() > 0 ? s.getLevel() : 1;
        int term  = s.getTerm()  > 0 ? s.getTerm()  : 1;

        // Approved course details নিয়ে dropdown populate করা
        List<String[]> courses = crDao.getApprovedCourseDetails(s.getId(), level, term);

        courseDropdown.getItems().clear();
        if (courses.isEmpty()) {
            courseDropdown.setPromptText("No approved courses");
        } else {
            for (String[] c : courses) {
                // format: "CSE101 - Programming Fundamentals"
                courseDropdown.getItems().add(c[0] + " - " + c[1]);
            }
        }

        // Reset attendance labels
        classesHeldLabel.setText("—");
        classesAttendedLabel.setText("—");
        courseAttendancePctLabel.setText("—");
        attendanceStatusLabel.setText("—");
    }

    //  Course-wise attendance — DB থেকে real data
    @FXML
    private void loadCourseAttendance(ActionEvent event) {
        String selected = courseDropdown.getValue();
        if (selected == null) return;

        // "CSE101 - Programming Fundamentals" থেকে course code বের করা
        String courseCode = selected.split(" - ")[0].trim();

        Student s = SessionManager.getStudent();
        AttendanceDAO aDao = new AttendanceDAO();

        // DB থেকে এই course এর attendance নেওয়া
        List<AttendanceRecord> records = aDao.getAttendanceSummary(s.getId());
        AttendanceRecord found = null;
        for (AttendanceRecord r : records) {
            if (r.courseCode.equals(courseCode)) { found = r; break; }
        }

        if (found == null) {
            classesHeldLabel.setText("0");
            classesAttendedLabel.setText("0");
            courseAttendancePctLabel.setText("0%");
            attendanceStatusLabel.setText("No data");
            attendanceStatusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #aaa;");
            return;
        }

        classesHeldLabel.setText(String.valueOf(found.totalHeld));
        classesAttendedLabel.setText(String.valueOf(found.totalAttended));
        courseAttendancePctLabel.setText(found.percentage + "%");

        if (found.percentage >= 75) {
            attendanceStatusLabel.setText("✔ Regular");
            attendanceStatusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        } else {
            attendanceStatusLabel.setText("✘ Irregular");
            attendanceStatusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        }
    }

    //  Course list — DB থেকে real data 
    private void loadCourseList() {
        Student s = SessionManager.getStudent();
        if (s == null) return;

        courseListContainer.getChildren().clear();

        CourseRegistrationDAO crDao = new CourseRegistrationDAO();
        int level = s.getLevel() > 0 ? s.getLevel() : 1;
        int term  = s.getTerm()  > 0 ? s.getTerm()  : 1;

        List<String[]> courses = crDao.getApprovedCourseDetails(s.getId(), level, term);

        if (courses.isEmpty()) {
            Label empty = new Label("No approved courses yet.");
            empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaa;");
            courseListContainer.getChildren().add(empty);
            return;
        }

        for (String[] c : courses) {
            // c = [course_code, course_name, faculty]
            Label row = new Label("• " + c[0] + "  —  " + c[1] + "   |   " + c[2]);
            row.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e; -fx-padding: 6 0;");
            courseListContainer.getChildren().add(row);
        }
    }

    //  Sidebar navigation 
    @FXML
    private void showHome(ActionEvent e) {
        setActive(btnHome);
        pageTitle.setText("Home");
        // Home এ ফিরলে সব refresh হবে
        loadSummaryCards();
        loadCourseDropdown();
        loadCourseList();
        mainContentArea.getChildren().setAll(homeContent);
    }

    @FXML
    private void showCourseRegistration(ActionEvent e) {
        setActive(btnCourse);
        pageTitle.setText("Course Registration");
        try {
            VBox content = FXMLLoader.load(
                    getClass().getResource("/frontend/view/CourseRegistration.fxml"));
            mainContentArea.getChildren().setAll(content);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @FXML
    private void showExamMarks(ActionEvent e) {
        setActive(btnExamMarks);
        pageTitle.setText("Exam Marks");
        // TODO: ExamMarks.fxml load করুন
    }

    @FXML
    private void showResult(ActionEvent e) {
        setActive(btnResult);
        pageTitle.setText("Result");
        // TODO: Result.fxml load করুন
    }

    @FXML
    private void showAttendance(ActionEvent e) {
        setActive(btnAttendance);
        pageTitle.setText("Attendance");
        try {
            VBox content = FXMLLoader.load(
                    getClass().getResource("/frontend/view/Attendance.fxml"));
            mainContentArea.getChildren().setAll(content);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @FXML
    private void showNotices(ActionEvent e) {
        setActive(btnNotices);
        pageTitle.setText("Notices");
        // TODO: Notices.fxml load করুন
    }

    @FXML
    private void showFeePayment(ActionEvent e) {
        setActive(btnFee);
        pageTitle.setText("Fee Payment");
    }

    @FXML
    private void showDueAmount(ActionEvent e) {
        setActive(btnDue);
        pageTitle.setText("Due Amount");
    }

    private void setActive(Button btn) {
        if (activeBtn != null) activeBtn.setStyle(INACTIVE_STYLE);
        btn.setStyle(ACTIVE_STYLE);
        activeBtn = btn;
    }

    //  Logout
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
        }
    }
}
