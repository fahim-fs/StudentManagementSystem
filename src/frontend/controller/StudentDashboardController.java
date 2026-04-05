package frontend.controller;

import backend.dao.AttendanceDAO;
import backend.dao.AttendanceDAO.AttendanceRecord;
import backend.dao.CourseRegistrationDAO;
import backend.dao.MarksDAO;
import backend.database.DatabaseConnection;
import backend.model.Student;
import backend.util.PasswordUtil;
import backend.util.ResultEngine;
import common.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class StudentDashboardController implements Initializable {

    @FXML private VBox mainContentArea;

    // ── Sidebar buttons ───────────────────────────────────────────────────────
    @FXML private Button btnHome;
    @FXML private Button btnProfile;
    @FXML private Button btnCourse;
    @FXML private Button btnExamMarks;
    @FXML private Button btnResult;
    @FXML private Button btnAttendance;
    @FXML private Button btnNotices;
    @FXML private Button btnFee;
    @FXML private Button btnLogout;

    // ── Top bar ───────────────────────────────────────────────────────────────
    @FXML private Label pageTitle;
    @FXML private Label sessionLabel;

    // ── Student info (sidebar) ────────────────────────────────────────────────
    @FXML private Label studentNameLabel;
    @FXML private Label studentIdLabel;
    @FXML private Label departmentLabel;

    // ── Home summary cards ────────────────────────────────────────────────────
    @FXML private Label overallAttendanceLabel;
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

    private Button activeBtn;
    private Student student;

    private static final String ACTIVE_STYLE =
            "-fx-background-color: #2a9d8f; -fx-text-fill: white; -fx-font-size: 13px;" +
            "-fx-alignment: CENTER-LEFT; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand;";
    private static final String INACTIVE_STYLE =
            "-fx-background-color: transparent; -fx-text-fill: #cdd9e0; -fx-font-size: 13px;" +
            "-fx-alignment: CENTER-LEFT; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand;";

    // ── initialize ────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        student   = SessionManager.getStudent();
        activeBtn = btnHome;

        Button[] navBtns = {btnHome, btnProfile, btnCourse, btnExamMarks,
                            btnResult, btnAttendance, btnNotices, btnFee};
        for (Button btn : navBtns) {
            btn.setOnMouseEntered(e -> { if (btn != activeBtn) btn.setStyle(INACTIVE_STYLE.replace("transparent", "#243b4a")); });
            btn.setOnMouseExited(e ->  { if (btn != activeBtn) btn.setStyle(INACTIVE_STYLE); });
        }

        loadStudentInfo();
        loadSummaryCards();
        loadCourseDropdown();
        loadCourseList();
    }

    // ── Student info ──────────────────────────────────────────────────────────
    private void loadStudentInfo() {
        if (student == null) return;
        studentNameLabel.setText(student.getFirstName() + " " + student.getLastName());
        studentIdLabel.setText("ID: " + student.getId());
        departmentLabel.setText(student.getDepartment() != null ? student.getDepartment() : "—");
        sessionLabel.setText("Session: " + (student.getSession() != null ? student.getSession() : "—"));
    }

    // ── Summary cards ─────────────────────────────────────────────────────────
    private void loadSummaryCards() {
        if (student == null) return;

        // Overall attendance
        AttendanceDAO aDao = new AttendanceDAO();
        double overall = aDao.getOverallAttendance(student.getId());
        overallAttendanceLabel.setText(overall + "%");
        overallAttendanceLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: "
                + (overall >= 75 ? "#27ae60" : "#e74c3c") + ";");

        // Notices count
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notices");
             ResultSet rs = ps.executeQuery()) {
            noticeCountLabel.setText(rs.next() ? String.valueOf(rs.getInt(1)) : "0");
        } catch (Exception e) { noticeCountLabel.setText("0"); }

        // Enrolled courses count
        CourseRegistrationDAO crDao = new CourseRegistrationDAO();
        int level = student.getLevel() > 0 ? student.getLevel() : 1;
        int term  = student.getTerm()  > 0 ? student.getTerm()  : 1;
        List<String> approved = crDao.getApprovedCourseCodes(student.getId(), level, term);
        courseCountLabel.setText(String.valueOf(approved.size()));
    }

    // ── Course dropdown ───────────────────────────────────────────────────────
    private void loadCourseDropdown() {
        if (student == null) return;
        CourseRegistrationDAO crDao = new CourseRegistrationDAO();
        int level = student.getLevel() > 0 ? student.getLevel() : 1;
        int term  = student.getTerm()  > 0 ? student.getTerm()  : 1;
        List<String[]> courses = crDao.getApprovedCourseDetails(student.getId(), level, term);
        courseDropdown.getItems().clear();
        if (courses.isEmpty()) {
            courseDropdown.setPromptText("No approved courses");
        } else {
            for (String[] c : courses)
                courseDropdown.getItems().add(c[0] + " - " + c[1]);
        }
        classesHeldLabel.setText("—");
        classesAttendedLabel.setText("—");
        courseAttendancePctLabel.setText("—");
        attendanceStatusLabel.setText("—");
    }

    // ── Course-wise attendance ────────────────────────────────────────────────
    @FXML
    private void loadCourseAttendance(ActionEvent event) {
        String selected = courseDropdown.getValue();
        if (selected == null) return;
        String courseCode = selected.split(" - ")[0].trim();
        AttendanceDAO aDao = new AttendanceDAO();
        List<AttendanceRecord> records = aDao.getAttendanceSummary(student.getId());
        AttendanceRecord found = null;
        for (AttendanceRecord r : records)
            if (r.courseCode.equals(courseCode)) { found = r; break; }

        if (found == null) {
            classesHeldLabel.setText("0"); classesAttendedLabel.setText("0");
            courseAttendancePctLabel.setText("0%"); attendanceStatusLabel.setText("No data");
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

    // ── Course list ───────────────────────────────────────────────────────────
    private void loadCourseList() {
        if (student == null) return;
        courseListContainer.getChildren().clear();
        CourseRegistrationDAO crDao = new CourseRegistrationDAO();
        int level = student.getLevel() > 0 ? student.getLevel() : 1;
        int term  = student.getTerm()  > 0 ? student.getTerm()  : 1;
        List<String[]> courses = crDao.getApprovedCourseDetails(student.getId(), level, term);
        if (courses.isEmpty()) {
            Label empty = new Label("No approved courses yet.");
            empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaa;");
            courseListContainer.getChildren().add(empty);
            return;
        }
        for (String[] c : courses) {
            Label row = new Label("• " + c[0] + "  —  " + c[1] + "   |   " + c[2]);
            row.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e; -fx-padding: 6 0;");
            courseListContainer.getChildren().add(row);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SIDEBAR NAVIGATION
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void showHome(ActionEvent e) {
        setActive(btnHome); pageTitle.setText("Home");
        loadSummaryCards(); loadCourseDropdown(); loadCourseList();
        mainContentArea.getChildren().setAll(homeContent);
    }

    // ── Profile ───────────────────────────────────────────────────────────────
    @FXML
    private void showProfile(ActionEvent e) {
        setActive(btnProfile); pageTitle.setText("My Profile");
        mainContentArea.getChildren().setAll(buildProfileView());
    }

    // ── Course Registration ───────────────────────────────────────────────────
    @FXML
    private void showCourseRegistration(ActionEvent e) {
        setActive(btnCourse); pageTitle.setText("Course Registration");
        try {
            VBox content = FXMLLoader.load(
                    getClass().getResource("/frontend/view/CourseRegistration.fxml"));
            mainContentArea.getChildren().setAll(content);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @FXML
    private void showExamMarks(ActionEvent e) {
        setActive(btnExamMarks); pageTitle.setText("Exam Marks");
        // TODO: ExamMarks view
    }

    // ── Result ────────────────────────────────────────────────────────────────
    @FXML
    private void showResult(ActionEvent e) {
        setActive(btnResult); pageTitle.setText("Result");
        mainContentArea.getChildren().setAll(buildResultView());
    }

    @FXML
    private void showAttendance(ActionEvent e) {
        setActive(btnAttendance); pageTitle.setText("Attendance");
        try {
            VBox content = FXMLLoader.load(
                    getClass().getResource("/frontend/view/Attendance.fxml"));
            mainContentArea.getChildren().setAll(content);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @FXML
    private void showNotices(ActionEvent e) {
        setActive(btnNotices); pageTitle.setText("Notices");
        // TODO: Notices view
    }

    @FXML
    private void showFeePayment(ActionEvent e) {
        setActive(btnFee); pageTitle.setText("Fee Payment");
        // TODO: Fee view
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PROFILE VIEW
    //  — Shows student info + two buttons: Edit | Reset Password
    //  — Each button swaps to its own sub-view inside mainContentArea
    // ══════════════════════════════════════════════════════════════════════════
    private ScrollPane buildProfileView() {
        VBox root = new VBox(20); root.setStyle("-fx-padding: 24;");

        // ── Info card ─────────────────────────────────────────────────────────
        VBox infoCard = card();
        infoCard.getChildren().add(sectionHeader("👤  Profile Information"));

        String[] d = getFullStudentInfo(student.getId());
        // d = [first_name, last_name, username, email, phone, department, status]
        String[][] rows = {
            {"Full Name",   d[0] + " " + d[1]},
            {"Username",    d[2]},
            {"Email",       d[3].isBlank() ? "—" : d[3]},
            {"Phone",       d[4].isBlank() ? "—" : d[4]},
            {"Department",  d[5].isBlank() ? "—" : d[5]},
            {"Role",        "Student"},
            {"Status",      d[6]}
        };
        for (String[] row : rows) {
            HBox r = new HBox(12); r.setAlignment(Pos.CENTER_LEFT);
            Label key = new Label(row[0] + ":"); key.setPrefWidth(120);
            key.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 13px;");
            Label val = new Label(row[1]);
            val.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 13px;");
            r.getChildren().addAll(key, val);
            infoCard.getChildren().add(r);
        }

        // ── Action buttons ────────────────────────────────────────────────────
        Button editBtn  = styledBtn("✏  Edit Info",       "#2a9d8f");
        Button resetBtn = styledBtn("🔒  Reset Password", "#e67e22");
        editBtn.setPrefWidth(160); resetBtn.setPrefWidth(160);

        HBox btnRow = new HBox(14, editBtn, resetBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        // Sub-view container — shown below buttons when either is clicked
        VBox subView = new VBox(0);

        editBtn.setOnAction(ev -> {
            subView.getChildren().setAll(buildEditInfoCard());
            subView.setVisible(true); subView.setManaged(true);
        });
        resetBtn.setOnAction(ev -> {
            subView.getChildren().setAll(buildResetPasswordCard());
            subView.setVisible(true); subView.setManaged(true);
        });

        infoCard.getChildren().addAll(new Separator(), btnRow);
        root.getChildren().addAll(infoCard, subView);

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(sp, Priority.ALWAYS);
        return sp;
    }

    // ── Edit Info sub-card ────────────────────────────────────────────────────
    private VBox buildEditInfoCard() {
        VBox card = card();
        card.getChildren().add(sectionHeader("✏️  Edit Contact Info"));
        Label note = new Label("ℹ  Name and username can only be changed by Admin.");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        Label msg = new Label();

        String[] d    = getFullStudentInfo(student.getId());
        TextField emailFld = formField("Email address"); emailFld.setText(d[3]);
        TextField phoneFld = formField("Phone number");  phoneFld.setText(d[4]);

        Button saveBtn = styledBtn("Save Changes", "#2a9d8f");
        saveBtn.setOnAction(ev -> {
            String email = emailFld.getText().trim();
            String phone = phoneFld.getText().trim();
            if (email.isBlank()) { setMsg(msg, "⚠ Email cannot be empty.", false); return; }
            if (!email.contains("@")) { setMsg(msg, "⚠ Enter a valid email.", false); return; }
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE users SET email=?, phone=? WHERE id=?")) {
                ps.setString(1, email); ps.setString(2, phone); ps.setInt(3, student.getId());
                ps.executeUpdate();
                setMsg(msg, "✔ Contact info updated successfully.", true);
            } catch (SQLException ex) { setMsg(msg, "⚠ Update failed.", false); }
        });

        card.getChildren().addAll(note,
                labeledField("Email *", emailFld),
                labeledField("Phone",   phoneFld),
                saveBtn, msg);
        return card;
    }

    // ── Reset Password sub-card ───────────────────────────────────────────────
    private VBox buildResetPasswordCard() {
        VBox card = card();
        card.getChildren().add(sectionHeader("🔒  Reset Password"));
        Label msg = new Label();

        PasswordField currentFld = new PasswordField(); currentFld.setPromptText("Current password");
        PasswordField newFld     = new PasswordField(); newFld.setPromptText("New password (min 6 chars)");
        PasswordField confirmFld = new PasswordField(); confirmFld.setPromptText("Re-enter new password");
        styleField(currentFld); styleField(newFld); styleField(confirmFld);

        Button changeBtn = styledBtn("Change Password", "#e67e22");
        changeBtn.setOnAction(ev -> {
            String cur  = currentFld.getText();
            String nw   = newFld.getText();
            String conf = confirmFld.getText();

            if (cur.isBlank())  { setMsg(msg, "⚠ Enter your current password.", false); return; }
            if (nw.length() < 6){ setMsg(msg, "⚠ New password must be at least 6 characters.", false); return; }
            if (!nw.equals(conf)){ setMsg(msg, "⚠ New passwords do not match.", false); return; }
            if (cur.equals(nw)) { setMsg(msg, "⚠ New password must differ from current.", false); return; }

            // Verify current password
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT password FROM users WHERE id=?")) {
                ps.setInt(1, student.getId());
                ResultSet rs = ps.executeQuery();
                if (!rs.next() || !PasswordUtil.checkPassword(cur, rs.getString("password"))) {
                    setMsg(msg, "⚠ Current password is incorrect.", false); return;
                }
            } catch (SQLException ex) { setMsg(msg, "⚠ Verification error.", false); return; }

            // Save new password
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE users SET password=? WHERE id=?")) {
                ps.setString(1, PasswordUtil.hashPassword(nw)); ps.setInt(2, student.getId());
                ps.executeUpdate();
                currentFld.clear(); newFld.clear(); confirmFld.clear();
                setMsg(msg, "✔ Password changed successfully.", true);
            } catch (SQLException ex) { setMsg(msg, "⚠ Update failed.", false); }
        });

        card.getChildren().addAll(
                labeledField("Current Password *", currentFld),
                labeledField("New Password *",     newFld),
                labeledField("Confirm Password *", confirmFld),
                changeBtn, msg);
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RESULT VIEW
    //  Two option buttons at the top: Class Test Results | Final Results
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildResultView() {
        VBox root = new VBox(0); root.setStyle("-fx-padding: 24 24 0 24;");
        VBox.setVgrow(root, Priority.ALWAYS);

        // ── Tab buttons ───────────────────────────────────────────────────────
        Button btnCT    = tabBtn("📝  Class Test Results",      true);
        Button btnFinal = tabBtn("📊  Semester Final Results",  false);
        HBox tabBar = new HBox(btnCT, btnFinal);
        tabBar.setStyle("-fx-background-color: #e8f4f8; -fx-background-radius: 10 10 0 0;");

        // ── Content panel (white card below tabs) ─────────────────────────────
        ScrollPane contentScroll = new ScrollPane();
        contentScroll.setFitToWidth(true);
        contentScroll.setStyle("-fx-background: white; -fx-background-color: white;" +
                               "-fx-background-radius: 0 0 12 12;" +
                               "-fx-border-color: #e0e0e0; -fx-border-width: 0 1 1 1;");
        VBox.setVgrow(contentScroll, Priority.ALWAYS);

        // Load CT by default
        contentScroll.setContent(buildCTResultsPane());

        btnCT.setOnAction(e -> {
            setTabActive(btnCT, btnFinal);
            contentScroll.setContent(buildCTResultsPane());
        });
        btnFinal.setOnAction(e -> {
            setTabActive(btnFinal, btnCT);
            contentScroll.setContent(buildFinalResultsPane());
        });

        root.getChildren().addAll(tabBar, contentScroll);
        return root;
    }

    // ── CT Results ────────────────────────────────────────────────────────────
    private VBox buildCTResultsPane() {
        VBox pane = new VBox(14); pane.setStyle("-fx-padding: 22;");

        Label title = sectionHeader("Class Test Results  (CT1 – CT4)");
        Label note  = new Label("ℹ  Best 3 of 4 CT marks (each out of 20) are counted. Total CT = 60 marks.");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        VBox tableArea = new VBox(8);

        // Header
        HBox header = tableHeader("Course", "CT1", "CT2", "CT3", "CT4", "Best 3", "CT /60");
        int[] hw = {230, 60, 60, 60, 60, 80, 80};
        applyHeaderWidths(header, hw);
        tableArea.getChildren().add(header);

        List<String[]> courses = getEnrolledCourses();
        if (courses.isEmpty()) {
            tableArea.getChildren().add(infoLbl("No enrolled courses found."));
        } else {
            MarksDAO mDao = new MarksDAO();
            boolean any = false;
            for (String[] c : courses) {
                double[] ct = mDao.getStudentCTMarks(student.getId(), c[0]);
                boolean hasData = false;
                for (double m : ct) if (m >= 0) { hasData = true; break; }
                if (!hasData) continue;
                any = true;

                double best3  = MarksDAO.getBest3CTSum(ct);
                double ctMark = ResultEngine.calcCTMark(best3);

                HBox row = dataRow();
                row.getChildren().add(cl(c[0] + " — " + c[1], 230));
                for (double m : ct) row.getChildren().add(cl(m >= 0 ? fmt1(m) : "—", 60));
                row.getChildren().add(cl(fmt1(best3), 80));

                Label ctLbl = new Label(fmt1(ctMark)); ctLbl.setPrefWidth(80);
                ctLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2a9d8f;");
                row.getChildren().add(ctLbl);
                tableArea.getChildren().add(row);
            }
            if (!any)
                tableArea.getChildren().add(infoLbl("CT marks have not been entered by faculty yet."));
        }

        pane.getChildren().addAll(title, note, tableArea);
        return pane;
    }

    // ── Final Results ─────────────────────────────────────────────────────────
    private VBox buildFinalResultsPane() {
        VBox pane = new VBox(14); pane.setStyle("-fx-padding: 22;");

        Label title = sectionHeader("Semester Final Results");
        Label note  = new Label("ℹ  Results are visible only after Admin publishes them.  Total = CT(60) + Attendance(30) + Term Final(210) = 300.");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        note.setWrapText(true);

        VBox tableArea = new VBox(8);

        // Header
        HBox header = tableHeader("Course", "CT /60", "Attend /30", "Final /210", "Total /300", "Grade", "GP");
        int[] hw = {210, 70, 95, 100, 100, 70, 55};
        applyHeaderWidths(header, hw);
        tableArea.getChildren().add(header);

        List<String[]> results = getPublishedResults();
        // each: [code, name, ct_mark, attendance_mark, term_final_mark, total, letter_grade, grade_point]

        if (results.isEmpty()) {
            tableArea.getChildren().add(infoLbl("No published results yet. Please check back after the admin publishes them."));
        } else {
            for (String[] r : results) {
                HBox row = dataRow();
                row.getChildren().add(cl(r[0] + " — " + r[1], 210));
                row.getChildren().add(cl(r[2], 70));
                row.getChildren().add(cl(r[3], 95));
                row.getChildren().add(cl(r[4], 100));
                row.getChildren().add(cl(r[5], 100));

                Label gradeL = new Label(r[6]); gradeL.setPrefWidth(70);
                gradeL.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + gradeColor(r[6]) + ";");
                row.getChildren().add(gradeL);
                row.getChildren().add(cl(r[7], 55));
                tableArea.getChildren().add(row);
            }

            // Term GPA summary
            double avg = results.stream()
                .mapToDouble(r -> { try { return Double.parseDouble(r[7]); } catch (Exception ex) { return 0; } })
                .average().orElse(0);
            double termGPA = ResultEngine.round2(avg);

            HBox gpaRow = new HBox(10); gpaRow.setAlignment(Pos.CENTER_LEFT);
            gpaRow.setStyle("-fx-padding: 10 14; -fx-background-color: #eaf6f6; -fx-background-radius: 8; -fx-margin: 6 0 0 0;");
            Label gpaKey = new Label("Term GPA:"); gpaKey.setPrefWidth(210);
            gpaKey.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
            Label gpaVal = new Label(String.format("%.2f / 4.00", termGPA));
            gpaVal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2a9d8f;");
            gpaRow.getChildren().addAll(gpaKey, gpaVal);
            tableArea.getChildren().addAll(new Separator(), gpaRow);
        }

        pane.getChildren().addAll(title, note, tableArea);
        return pane;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DATABASE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /** [first_name, last_name, username, email, phone, department, status] */
    private String[] getFullStudentInfo(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT first_name,last_name,username,email,phone,department,status FROM users WHERE id=?")) {
            ps.setInt(1, id); ResultSet rs = ps.executeQuery();
            if (rs.next()) return new String[]{
                safe(rs.getString("first_name")), safe(rs.getString("last_name")),
                safe(rs.getString("username")),   safe(rs.getString("email")),
                safe(rs.getString("phone")),       safe(rs.getString("department")),
                safe(rs.getString("status"))};
        } catch (SQLException e) { e.printStackTrace(); }
        return new String[]{"","","","","","",""};
    }

    /** [code, name] for all APPROVED enrollments */
    private List<String[]> getEnrolledCourses() {
        java.util.List<String[]> list = new java.util.ArrayList<>();
        String sql = "SELECT c.code, c.name FROM courses c " +
                     "JOIN course_registrations cr ON c.code=cr.course_code " +
                     "WHERE cr.user_id=? AND cr.status='APPROVED' ORDER BY c.level, c.term";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, student.getId()); ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new String[]{rs.getString("code"), rs.getString("name")});
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * Reads from `published_results` table.
     * Expected columns: user_id, course_code, ct_mark, attendance_mark,
     *                   term_final_mark, total, letter_grade, grade_point
     * Returns: [code, name, ct_mark, attendance_mark, term_final_mark, total, letter_grade, grade_point]
     */
    private List<String[]> getPublishedResults() {
        java.util.List<String[]> list = new java.util.ArrayList<>();
        String sql = "SELECT pr.course_code, c.name, pr.ct_mark, pr.attendance_mark, " +
                     "       pr.term_final_mark, pr.total, pr.letter_grade, pr.grade_point " +
                     "FROM published_results pr JOIN courses c ON pr.course_code=c.code " +
                     "WHERE pr.user_id=? ORDER BY c.level, c.term";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, student.getId()); ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new String[]{
                rs.getString("course_code"), rs.getString("name"),
                fmt2(rs.getDouble("ct_mark")),          fmt2(rs.getDouble("attendance_mark")),
                fmt2(rs.getDouble("term_final_mark")),  fmt2(rs.getDouble("total")),
                rs.getString("letter_grade"),            fmt2(rs.getDouble("grade_point"))});
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private VBox card() {
        VBox v = new VBox(12);
        v.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                   "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2); -fx-padding: 22;");
        return v;
    }

    private Label sectionHeader(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        return l;
    }

    private Label infoLbl(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d; -fx-padding: 16 0;");
        return l;
    }

    private HBox dataRow() {
        HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10 14; -fx-background-color: white;" +
                     "-fx-background-radius: 8; -fx-border-color: #e8e8e8; -fx-border-radius: 8;");
        return row;
    }

    private Label cl(String t, double w) {
        Label l = new Label(t != null ? t : "—"); l.setPrefWidth(w);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50;");
        return l;
    }

    private HBox tableHeader(String... cols) {
        HBox h = new HBox(10); h.setAlignment(Pos.CENTER_LEFT);
        h.setStyle("-fx-padding: 8 14; -fx-background-color: #f0f4f8; -fx-background-radius: 6;");
        for (String c : cols) {
            Label l = new Label(c);
            l.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #555;");
            h.getChildren().add(l);
        }
        return h;
    }

    private void applyHeaderWidths(HBox header, int[] widths) {
        for (int i = 0; i < widths.length && i < header.getChildren().size(); i++)
            ((Label) header.getChildren().get(i)).setPrefWidth(widths[i]);
    }

    private Button styledBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white;" +
                   "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 18; -fx-font-size: 13px;");
        return b;
    }

    private TextField formField(String prompt) {
        TextField t = new TextField(); t.setPromptText(prompt);
        t.setStyle("-fx-pref-height: 34px; -fx-padding: 0 8; -fx-background-radius: 6;" +
                   "-fx-border-radius: 6; -fx-border-color: #ccc; -fx-border-width: 1;");
        return t;
    }

    private void styleField(Control c) {
        c.setStyle("-fx-pref-height: 34px; -fx-padding: 0 8; -fx-background-radius: 6;" +
                   "-fx-border-radius: 6; -fx-border-color: #ccc; -fx-border-width: 1;");
    }

    private HBox labeledField(String labelText, Control field) {
        Label lbl = new Label(labelText); lbl.setPrefWidth(160);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-font-weight: bold;");
        HBox.setHgrow(field, Priority.ALWAYS);
        HBox box = new HBox(10, lbl, field); box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Button tabBtn(String text, boolean active) {
        Button b = new Button(text);
        b.setStyle((active
                ? "-fx-background-color: #2a9d8f; -fx-text-fill: white;"
                : "-fx-background-color: #e8f4f8; -fx-text-fill: #555;") +
                "-fx-font-size: 13px; -fx-padding: 10 24; -fx-cursor: hand;" +
                "-fx-background-radius: 8 8 0 0; -fx-border-color: transparent;");
        HBox.setHgrow(b, Priority.ALWAYS);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private void setTabActive(Button active, Button... others) {
        active.setStyle("-fx-background-color: #2a9d8f; -fx-text-fill: white;" +
                        "-fx-font-size: 13px; -fx-padding: 10 24; -fx-cursor: hand;" +
                        "-fx-background-radius: 8 8 0 0;");
        for (Button o : others)
            o.setStyle("-fx-background-color: #e8f4f8; -fx-text-fill: #555;" +
                       "-fx-font-size: 13px; -fx-padding: 10 24; -fx-cursor: hand;" +
                       "-fx-background-radius: 8 8 0 0;");
    }

    private void setMsg(Label l, String m, boolean ok) {
        l.setText(m);
        l.setStyle("-fx-text-fill: " + (ok ? "#27ae60" : "#e74c3c") + "; -fx-font-size: 12px;");
    }

    private void setActive(Button btn) {
        if (activeBtn != null) activeBtn.setStyle(INACTIVE_STYLE);
        btn.setStyle(ACTIVE_STYLE); activeBtn = btn;
    }

    private String gradeColor(String g) {
        return switch (g) {
            case "A+","A","A-" -> "#27ae60";
            case "B+","B","B-" -> "#2980b9";
            case "C+","C"      -> "#e67e22";
            case "D"           -> "#f39c12";
            default            -> "#e74c3c";
        };
    }

    private String safe(String s) { return s != null ? s : ""; }
    private String fmt1(double v) { return String.format("%.1f", v); }
    private String fmt2(double v) { return String.format("%.2f", v); }

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
        } catch (Exception e) { e.printStackTrace(); }
    }
}
