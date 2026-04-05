package frontend.controller;

import backend.dao.MarksDAO;
import backend.database.DatabaseConnection;
import backend.model.Student;
import backend.util.PasswordUtil;
import backend.util.ResultEngine;
import common.SessionManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.util.*;

public class StudentDashboardController2 implements Initializable {

    // ── FXML Sidebar buttons ──────────────────────────────────────────────────
    @FXML private Button btnHome;
    @FXML private Button btnProfile;
    @FXML private Button btnResults;
    @FXML private Button btnCourses;
    @FXML private Button btnLogout;

    // ── FXML Labels & layout ──────────────────────────────────────────────────
    @FXML private Label pageTitle;
    @FXML private Label studentNameLabel;
    @FXML private Label departmentLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label enrolledCoursesLabel;
    @FXML private Label publishedResultsLabel;
    @FXML private VBox  mainContentArea;
    @FXML private VBox  homeContent;

    private Student student;
    private Button  activeBtn;

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

        studentNameLabel.setText(student.getFirstName() + " " + student.getLastName());
        departmentLabel.setText(student.getDepartment() != null ? student.getDepartment() : "—");
        welcomeLabel.setText("Welcome, " + student.getFirstName() + "! 👋");

        Button[] navBtns = {btnHome, btnProfile, btnResults, btnCourses};
        for (Button btn : navBtns) {
            btn.setOnMouseEntered(e -> { if (btn != activeBtn) btn.setStyle(INACTIVE_STYLE.replace("transparent", "#243b4a")); });
            btn.setOnMouseExited(e ->  { if (btn != activeBtn) btn.setStyle(INACTIVE_STYLE); });
        }
        loadHomeCards();
    }

    // ── Home card counters ────────────────────────────────────────────────────
    private void loadHomeCards() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Count enrolled + approved courses
            PreparedStatement ps1 = conn.prepareStatement(
                "SELECT COUNT(*) FROM course_registrations WHERE user_id=? AND status='APPROVED'");
            ps1.setInt(1, student.getId());
            ResultSet rs1 = ps1.executeQuery();
            enrolledCoursesLabel.setText(rs1.next() ? String.valueOf(rs1.getInt(1)) : "0");

            // Count published results (where admin has published a term final mark for this student)
            PreparedStatement ps2 = conn.prepareStatement(
                "SELECT COUNT(DISTINCT course_code) FROM published_results WHERE user_id=?");
            ps2.setInt(1, student.getId());
            ResultSet rs2 = ps2.executeQuery();
            publishedResultsLabel.setText(rs2.next() ? String.valueOf(rs2.getInt(1)) : "0");

        } catch (SQLException e) {
            e.printStackTrace();
            enrolledCoursesLabel.setText("0");
            publishedResultsLabel.setText("0");
        }
    }

    // ── Sidebar navigation ────────────────────────────────────────────────────
    @FXML private void showHome(ActionEvent e) {
        setActive(btnHome); pageTitle.setText("Home");
        loadHomeCards();
        mainContentArea.getChildren().setAll(homeContent);
    }

    @FXML private void showProfile(ActionEvent e) {
        setActive(btnProfile); pageTitle.setText("My Profile");
        mainContentArea.getChildren().setAll(buildProfileView());
    }

    @FXML private void showResults(ActionEvent e) {
        setActive(btnResults); pageTitle.setText("My Results");
        mainContentArea.getChildren().setAll(buildResultsView());
    }

    @FXML private void showCourses(ActionEvent e) {
        setActive(btnCourses); pageTitle.setText("My Courses");
        mainContentArea.getChildren().setAll(buildCoursesView());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PROFILE VIEW
    // ══════════════════════════════════════════════════════════════════════════
    private ScrollPane buildProfileView() {
        VBox root = new VBox(20); root.setStyle("-fx-padding: 24;");

        // ── 1. Profile Info Card ───────────────────────────────────────────────
        VBox infoCard = card();
        infoCard.getChildren().add(sectionHeader("👤  Profile Information"));

        // Reload fresh data from DB so it's always current
        String[] freshData = getFullStudentInfo(student.getId());
        // freshData: [first_name, last_name, username, email, phone, department, status]

        String[][] infoRows = {
            {"Full Name",   freshData[0] + " " + freshData[1]},
            {"Username",    freshData[2]},
            {"Email",       freshData[3].isBlank() ? "—" : freshData[3]},
            {"Phone",       freshData[4].isBlank() ? "—" : freshData[4]},
            {"Department",  freshData[5].isBlank() ? "—" : freshData[5]},
            {"Role",        "Student"},
            {"Status",      freshData[6]}
        };
        for (String[] row : infoRows) {
            HBox r = new HBox(12);
            Label key = new Label(row[0] + ":"); key.setPrefWidth(120);
            key.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 13px;");
            Label val = new Label(row[1]);
            val.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 13px;");
            r.getChildren().addAll(key, val);
            infoCard.getChildren().add(r);
        }

        // ── 2. Edit Contact Info Card ─────────────────────────────────────────
        VBox editCard = card();
        editCard.getChildren().add(sectionHeader("✏️  Edit Contact Info"));
        Label editNote = new Label("ℹ  Name and username cannot be changed. Contact admin if needed.");
        editNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        Label editMsg = new Label();

        TextField emailFld = formField("Your email address");
        emailFld.setText(freshData[3]);
        TextField phoneFld = formField("Your phone number");
        phoneFld.setText(freshData[4]);

        Button saveBtn = styledBtn("Save Changes", "#2a9d8f");
        saveBtn.setOnAction(ev -> {
            String email = emailFld.getText().trim();
            String phone = phoneFld.getText().trim();
            if (email.isBlank()) { setMsg(editMsg, "⚠ Email cannot be empty.", false); return; }
            if (!email.contains("@")) { setMsg(editMsg, "⚠ Enter a valid email address.", false); return; }
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE users SET email=?, phone=? WHERE id=?")) {
                ps.setString(1, email); ps.setString(2, phone); ps.setInt(3, student.getId());
                ps.executeUpdate();
                setMsg(editMsg, "✔ Contact information updated successfully.", true);
            } catch (SQLException ex) {
                setMsg(editMsg, "⚠ Update failed: " + ex.getMessage(), false);
            }
        });

        editCard.getChildren().addAll(editNote,
                labeledField("Email *",  emailFld),
                labeledField("Phone",    phoneFld),
                saveBtn, editMsg);

        // ── 3. Change Password Card ───────────────────────────────────────────
        VBox passCard = card();
        passCard.getChildren().add(sectionHeader("🔒  Change Password"));
        Label passMsg = new Label();

        PasswordField currentPassFld = new PasswordField();
        currentPassFld.setPromptText("Enter your current password");
        PasswordField newPassFld = new PasswordField();
        newPassFld.setPromptText("New password (min 6 characters)");
        PasswordField confirmPassFld = new PasswordField();
        confirmPassFld.setPromptText("Re-enter new password");
        styleField(currentPassFld); styleField(newPassFld); styleField(confirmPassFld);

        Button changePassBtn = styledBtn("Change Password", "#e67e22");
        changePassBtn.setOnAction(ev -> {
            String current = currentPassFld.getText();
            String newPass  = newPassFld.getText();
            String confirm  = confirmPassFld.getText();

            if (current.isBlank()) {
                setMsg(passMsg, "⚠ Please enter your current password.", false); return; }
            if (newPass.length() < 6) {
                setMsg(passMsg, "⚠ New password must be at least 6 characters.", false); return; }
            if (!newPass.equals(confirm)) {
                setMsg(passMsg, "⚠ New passwords do not match.", false); return; }
            if (current.equals(newPass)) {
                setMsg(passMsg, "⚠ New password must differ from current password.", false); return; }

            // Verify current password against DB hash
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT password FROM users WHERE id=?")) {
                ps.setInt(1, student.getId());
                ResultSet rs = ps.executeQuery();
                if (!rs.next() || !PasswordUtil.checkPassword(current, rs.getString("password"))) {
                    setMsg(passMsg, "⚠ Current password is incorrect.", false); return;
                }
            } catch (SQLException ex) { setMsg(passMsg, "⚠ Verification error.", false); return; }

            // Save new hashed password
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE users SET password=? WHERE id=?")) {
                ps.setString(1, PasswordUtil.hashPassword(newPass));
                ps.setInt(2, student.getId());
                ps.executeUpdate();
                currentPassFld.clear(); newPassFld.clear(); confirmPassFld.clear();
                setMsg(passMsg, "✔ Password changed successfully.", true);
            } catch (SQLException ex) {
                setMsg(passMsg, "⚠ Password update failed.", false);
            }
        });

        passCard.getChildren().addAll(
                labeledField("Current Password *", currentPassFld),
                labeledField("New Password *",     newPassFld),
                labeledField("Confirm New Pass *", confirmPassFld),
                changePassBtn, passMsg);

        root.getChildren().addAll(infoCard, editCard, passCard);

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(sp, Priority.ALWAYS);
        return sp;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RESULTS VIEW  — CT Results | Final Results
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildResultsView() {
        VBox root = new VBox(0); root.setStyle("-fx-padding: 24 24 0 24;");

        // ── Tab bar ───────────────────────────────────────────────────────────
        Button btnCT    = tabBtn("📝  Class Test Results", true);
        Button btnFinal = tabBtn("📊  Semester Final Results", false);
        HBox tabBar = new HBox(btnCT, btnFinal);
        tabBar.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 10 10 0 0; -fx-padding: 0;");

        // ── Content pane (swapped by tabs) ────────────────────────────────────
        VBox contentPane = new VBox(16);
        contentPane.setStyle("-fx-background-color: white; -fx-padding: 20;" +
                             "-fx-background-radius: 0 0 12 12;" +
                             "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);");
        VBox.setVgrow(contentPane, Priority.ALWAYS);

        // Load CT view by default
        loadCTResultsPane(contentPane);

        btnCT.setOnAction(e -> {
            setTabActive(btnCT, btnFinal);
            contentPane.getChildren().clear();
            loadCTResultsPane(contentPane);
        });
        btnFinal.setOnAction(e -> {
            setTabActive(btnFinal, btnCT);
            contentPane.getChildren().clear();
            loadFinalResultsPane(contentPane);
        });

        ScrollPane sp = scrollWrap(contentPane);
        root.getChildren().addAll(tabBar, sp);
        VBox.setVgrow(root, Priority.ALWAYS);
        return root;
    }

    // ── CT Results tabular view ───────────────────────────────────────────────
    private void loadCTResultsPane(VBox pane) {
        Label title = sectionHeader("Class Test Results (CT1 – CT4)");
        Label note  = new Label("ℹ  Best 3 of 4 CT marks (each out of 20) are counted. Total CT contribution = 60 marks.");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        VBox tableArea = new VBox(10);

        // Header row
        HBox header = new HBox();
        header.setStyle("-fx-padding: 8 14; -fx-background-color: #f0f4f8; -fx-background-radius: 6;");
        header.setAlignment(Pos.CENTER_LEFT);
        String[] cols = {"Course", "CT1", "CT2", "CT3", "CT4", "Best 3 Sum", "CT Mark /60"};
        int[]    widths = {220, 60, 60, 60, 60, 100, 110};
        for (int i = 0; i < cols.length; i++) {
            Label lbl = new Label(cols[i]); lbl.setPrefWidth(widths[i]);
            lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #555;");
            header.getChildren().add(lbl);
        }
        tableArea.getChildren().add(header);

        // Data rows — one per enrolled course
        List<String[]> courses = getEnrolledCourseDetails(student.getId());
        if (courses.isEmpty()) {
            tableArea.getChildren().add(infoLabel("No enrolled courses found."));
        } else {
            MarksDAO mDao = new MarksDAO();
            boolean anyData = false;
            for (String[] c : courses) {
                // c = [code, name, level, term]
                double[] ctMarks = mDao.getStudentCTMarks(student.getId(), c[0]);
                // Check if faculty has entered any CT data
                boolean hasData = false;
                for (double m : ctMarks) if (m >= 0) { hasData = true; break; }
                if (!hasData) continue;
                anyData = true;

                double best3  = MarksDAO.getBest3CTSum(ctMarks);
                double ctMark = ResultEngine.calcCTMark(best3);

                HBox row = dataRow();
                row.getChildren().add(colLabel(c[0] + " — " + c[1], 220));
                for (double mark : ctMarks)
                    row.getChildren().add(colLabel(mark >= 0 ? String.format("%.0f", mark) : "—", 60));
                row.getChildren().add(colLabel(String.format("%.1f", best3), 100));

                Label ctMarkLabel = new Label(String.format("%.1f", ctMark));
                ctMarkLabel.setPrefWidth(110);
                ctMarkLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2a9d8f;");
                row.getChildren().add(ctMarkLabel);
                tableArea.getChildren().add(row);
            }
            if (!anyData)
                tableArea.getChildren().add(infoLabel("CT marks have not been entered by faculty yet."));
        }

        pane.getChildren().addAll(title, note, tableArea);
    }

    // ── Semester Final Results tabular view ───────────────────────────────────
    private void loadFinalResultsPane(VBox pane) {
        Label title = sectionHeader("Semester Final Results");
        Label note  = new Label("ℹ  Results are visible only after the admin publishes them. Total = CT(60) + Attendance(30) + Term Final(210) = 300.");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        note.setWrapText(true);

        VBox tableArea = new VBox(10);

        // Header
        HBox header = new HBox();
        header.setStyle("-fx-padding: 8 14; -fx-background-color: #f0f4f8; -fx-background-radius: 6;");
        header.setAlignment(Pos.CENTER_LEFT);
        String[] cols = {"Course", "CT /60", "Attend /30", "Final /210", "Total /300", "Grade", "GP"};
        int[]    widths = {200, 70, 90, 100, 100, 70, 60};
        for (int i = 0; i < cols.length; i++) {
            Label lbl = new Label(cols[i]); lbl.setPrefWidth(widths[i]);
            lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #555;");
            header.getChildren().add(lbl);
        }
        tableArea.getChildren().add(header);

        // Fetch published results from DB
        List<String[]> results = getPublishedResults(student.getId());
        // Each row: [course_code, course_name, ct_mark, attendance_mark, term_final_mark, total, letter_grade, grade_point]

        if (results.isEmpty()) {
            tableArea.getChildren().add(infoLabel("No published results yet. Please check back after the admin publishes them."));
        } else {
            for (String[] r : results) {
                HBox row = dataRow();
                row.getChildren().add(colLabel(r[0] + " — " + r[1], 200));
                row.getChildren().add(colLabel(r[2], 70));   // CT
                row.getChildren().add(colLabel(r[3], 90));   // Attendance
                row.getChildren().add(colLabel(r[4], 100));  // Term Final
                row.getChildren().add(colLabel(r[5], 100));  // Total

                // Grade label with colour
                Label gradeLabel = new Label(r[6]); gradeLabel.setPrefWidth(70);
                String gradeColor = gradeColor(r[6]);
                gradeLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + gradeColor + ";");
                row.getChildren().add(gradeLabel);

                row.getChildren().add(colLabel(r[7], 60));   // GP
                tableArea.getChildren().add(row);
            }

            // GPA summary row
            double[] gps = results.stream()
                .mapToDouble(r -> {
                    try { return Double.parseDouble(r[7]); } catch (Exception ex) { return 0; }
                }).toArray();
            double termGPA = gps.length > 0
                ? ResultEngine.round2(Arrays.stream(gps).average().orElse(0)) : 0;

            Separator sep = new Separator(); sep.setStyle("-fx-padding: 4 0;");
            HBox gpaRow = new HBox();
            gpaRow.setStyle("-fx-padding: 10 14; -fx-background-color: #eaf6f6; -fx-background-radius: 8;");
            gpaRow.setAlignment(Pos.CENTER_LEFT);
            Label gpaKey = new Label("Term GPA:"); gpaKey.setPrefWidth(200);
            gpaKey.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
            Label gpaVal = new Label(String.format("%.2f / 4.00", termGPA));
            gpaVal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2a9d8f;");
            gpaRow.getChildren().addAll(gpaKey, gpaVal);
            tableArea.getChildren().addAll(sep, gpaRow);
        }

        pane.getChildren().addAll(title, note, tableArea);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  COURSES VIEW
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildCoursesView() {
        VBox root = new VBox(16); root.setStyle("-fx-padding: 24;");
        root.getChildren().add(sectionHeader("📚  My Enrolled Courses"));

        VBox tableArea = new VBox(10);

        HBox header = new HBox();
        header.setStyle("-fx-padding: 8 14; -fx-background-color: #f0f4f8; -fx-background-radius: 6;");
        header.setAlignment(Pos.CENTER_LEFT);
        String[] cols = {"Course Code", "Course Name", "Level", "Term", "Credits", "Faculty", "Status"};
        int[]    widths = {110, 230, 60, 60, 70, 170, 100};
        for (int i = 0; i < cols.length; i++) {
            Label lbl = new Label(cols[i]); lbl.setPrefWidth(widths[i]);
            lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #555;");
            header.getChildren().add(lbl);
        }
        tableArea.getChildren().add(header);

        List<String[]> courses = getAllEnrolledCoursesFull(student.getId());
        if (courses.isEmpty()) {
            tableArea.getChildren().add(infoLabel("You have no course registrations yet."));
        } else {
            for (String[] c : courses) {
                // c = [code, name, level, term, credit, faculty_name, reg_status]
                HBox row = dataRow();
                row.getChildren().add(colLabel(c[0], 110));
                row.getChildren().add(colLabel(c[1], 230));
                row.getChildren().add(colLabel(c[2], 60));
                row.getChildren().add(colLabel(c[3], 60));
                row.getChildren().add(colLabel(c[4], 70));
                row.getChildren().add(colLabel(c[5] != null ? c[5] : "—", 170));

                Label statusLabel = new Label(c[6]); statusLabel.setPrefWidth(100);
                String statusColor = c[6].equals("APPROVED") ? "#27ae60"
                        : c[6].equals("PENDING") ? "#e67e22" : "#e74c3c";
                statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + statusColor + ";");
                row.getChildren().add(statusLabel);
                tableArea.getChildren().add(row);
            }
        }

        root.getChildren().add(scrollWrap(tableArea));
        return root;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DATABASE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /** Returns [first_name, last_name, username, email, phone, department, status] */
    private String[] getFullStudentInfo(int userId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT first_name,last_name,username,email,phone,department,status FROM users WHERE id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new String[]{
                safe(rs.getString("first_name")),
                safe(rs.getString("last_name")),
                safe(rs.getString("username")),
                safe(rs.getString("email")),
                safe(rs.getString("phone")),
                safe(rs.getString("department")),
                safe(rs.getString("status"))
            };
        } catch (SQLException e) { e.printStackTrace(); }
        return new String[]{"", "", "", "", "", "", ""};
    }

    /** Returns [code, name, level, term] for all APPROVED enrollments */
    private List<String[]> getEnrolledCourseDetails(int userId) {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT c.code, c.name, c.level, c.term " +
                     "FROM courses c JOIN course_registrations cr ON c.code = cr.course_code " +
                     "WHERE cr.user_id=? AND cr.status='APPROVED' ORDER BY c.level, c.term";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new String[]{
                rs.getString("code"), rs.getString("name"),
                String.valueOf(rs.getInt("level")), String.valueOf(rs.getInt("term"))
            });
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * Returns published result rows for this student.
     * Each row: [code, name, ct_mark, attendance_mark, term_final_mark, total, letter_grade, grade_point]
     *
     * Assumes a `published_results` table with columns:
     *   user_id, course_code, ct_mark, attendance_mark, term_final_mark, total, letter_grade, grade_point
     *
     * If your schema stores raw marks instead, apply ResultEngine here to compute.
     */
    private List<String[]> getPublishedResults(int userId) {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT pr.course_code, c.name, pr.ct_mark, pr.attendance_mark, " +
                     "       pr.term_final_mark, pr.total, pr.letter_grade, pr.grade_point " +
                     "FROM published_results pr JOIN courses c ON pr.course_code = c.code " +
                     "WHERE pr.user_id=? ORDER BY c.level, c.term";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new String[]{
                rs.getString("course_code"),
                rs.getString("name"),
                fmt(rs.getDouble("ct_mark")),
                fmt(rs.getDouble("attendance_mark")),
                fmt(rs.getDouble("term_final_mark")),
                fmt(rs.getDouble("total")),
                rs.getString("letter_grade"),
                fmt(rs.getDouble("grade_point"))
            });
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Full course info including faculty name and registration status */
    private List<String[]> getAllEnrolledCoursesFull(int userId) {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT c.code, c.name, c.level, c.term, c.credit, " +
                     "       u.first_name, u.last_name, cr.status " +
                     "FROM course_registrations cr " +
                     "JOIN courses c ON cr.course_code = c.code " +
                     "LEFT JOIN users u ON c.faculty_id = u.id " +
                     "WHERE cr.user_id=? ORDER BY cr.status DESC, c.level, c.term";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String fn = rs.getString("first_name");
                String ln = rs.getString("last_name");
                String faculty = (fn != null) ? fn + " " + ln : null;
                list.add(new String[]{
                    rs.getString("code"),
                    rs.getString("name"),
                    String.valueOf(rs.getInt("level")),
                    String.valueOf(rs.getInt("term")),
                    fmt(rs.getDouble("credit")),
                    faculty,
                    rs.getString("status")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private VBox card() {
        VBox v = new VBox(12);
        v.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                   "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);" +
                   "-fx-padding: 22;");
        return v;
    }

    private Label sectionHeader(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        return l;
    }

    private Label infoLabel(String t) {
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

    private Label colLabel(String t, double w) {
        Label l = new Label(t != null ? t : "—"); l.setPrefWidth(w);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50;");
        return l;
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

    private ScrollPane scrollWrap(VBox content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(sp, Priority.ALWAYS);
        return sp;
    }

    private Button tabBtn(String text, boolean active) {
        Button b = new Button(text);
        b.setStyle((active
                ? "-fx-background-color: #2a9d8f; -fx-text-fill: white;"
                : "-fx-background-color: #f0f0f0; -fx-text-fill: #555;") +
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
            o.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #555;" +
                       "-fx-font-size: 13px; -fx-padding: 10 24; -fx-cursor: hand;" +
                       "-fx-background-radius: 8 8 0 0;");
    }

    private void setMsg(Label l, String m, boolean success) {
        l.setText(m);
        l.setStyle("-fx-text-fill: " + (success ? "#27ae60" : "#e74c3c") + "; -fx-font-size: 12px;");
    }

    private void setActive(Button btn) {
        if (activeBtn != null) activeBtn.setStyle(INACTIVE_STYLE);
        btn.setStyle(ACTIVE_STYLE);
        activeBtn = btn;
    }

    /** Grade → display colour */
    private String gradeColor(String grade) {
        return switch (grade) {
            case "A+", "A", "A-" -> "#27ae60";
            case "B+", "B", "B-" -> "#2980b9";
            case "C+", "C"       -> "#e67e22";
            case "D"             -> "#f39c12";
            default              -> "#e74c3c";   // F
        };
    }

    private String safe(String s) { return s != null ? s : ""; }
    private String fmt(double v)  { return String.format("%.2f", v); }

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
