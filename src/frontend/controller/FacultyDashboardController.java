package frontend.controller;

import backend.dao.MarksDAO;
import backend.database.DatabaseConnection;
import backend.model.Student;
import backend.util.PasswordUtil;
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

public class FacultyDashboardController implements Initializable {

    @FXML private Button btnHome;
    @FXML private Button btnProfile;
    @FXML private Button btnAttendance;
    @FXML private Button btnResults;
    @FXML private Button btnNotices;
    @FXML private Button btnStudentList;
    @FXML private Button btnLogout;

    @FXML private Label pageTitle;
    @FXML private Label facultyNameLabel;
    @FXML private Label departmentLabel;
    @FXML private VBox  mainContentArea;
    @FXML private VBox  homeContent;
    @FXML private Label totalStudentsLabel;
    @FXML private Label totalCoursesLabel;

    private Student faculty;
    private Button  activeBtn;

    private static final String ACTIVE_STYLE =
            "-fx-background-color: #2a9d8f; -fx-text-fill: white; -fx-font-size: 13px;" +
            "-fx-alignment: CENTER-LEFT; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand;";
    private static final String INACTIVE_STYLE =
            "-fx-background-color: transparent; -fx-text-fill: #cdd9e0; -fx-font-size: 13px;" +
            "-fx-alignment: CENTER-LEFT; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        faculty   = SessionManager.getStudent();
        activeBtn = btnHome;

        facultyNameLabel.setText(faculty.getFirstName() + " " + faculty.getLastName());
        departmentLabel.setText(faculty.getDepartment() != null ? faculty.getDepartment() : "—");

        Button[] navBtns = {btnHome, btnProfile, btnAttendance, btnResults, btnNotices, btnStudentList};
        for (Button btn : navBtns) {
            btn.setOnMouseEntered(e -> { if (btn != activeBtn) btn.setStyle(INACTIVE_STYLE.replace("transparent", "#243b4a")); });
            btn.setOnMouseExited(e ->  { if (btn != activeBtn) btn.setStyle(INACTIVE_STYLE); });
        }
        loadHomeCards();
    }

    private void loadHomeCards() {
        List<String> courses = getMyCourses();
        totalCoursesLabel.setText(String.valueOf(courses.size()));

        // count distinct students enrolled in my courses
        if (courses.isEmpty()) { totalStudentsLabel.setText("0"); return; }
        try (Connection conn = DatabaseConnection.getConnection()) {
            String placeholders = String.join(",", Collections.nCopies(courses.size(), "?"));
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(DISTINCT user_id) FROM course_registrations " +
                    "WHERE course_code IN (" + placeholders + ")");
            for (int i = 0; i < courses.size(); i++) ps.setString(i + 1, courses.get(i));
            ResultSet rs = ps.executeQuery();
            totalStudentsLabel.setText(rs.next() ? String.valueOf(rs.getInt(1)) : "0");
        } catch (SQLException e) { e.printStackTrace(); totalStudentsLabel.setText("0"); }
    }

    // ── Get courses assigned to this faculty ──────────────────────────────────
    private List<String> getMyCourses() {
        List<String> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT code FROM courses WHERE faculty_id = ?")) {
            ps.setInt(1, faculty.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getString("code"));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ── Get courses with details assigned to this faculty ─────────────────────
    private List<String[]> getMyCourseDetails() {
        // returns [code, name, level, term]
        List<String[]> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT code, name, level, term FROM courses WHERE faculty_id = ? ORDER BY level, term")) {
            ps.setInt(1, faculty.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new String[]{
                rs.getString("code"), rs.getString("name"),
                String.valueOf(rs.getInt("level")), String.valueOf(rs.getInt("term"))
            });
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ── Build course ComboBox from assigned courses only ──────────────────────
    private ComboBox<String> buildMyCourseCombo() {
        List<String[]> courses = getMyCourseDetails();
        List<String>   items   = new ArrayList<>();
        for (String[] c : courses)
            items.add(c[0] + " — " + c[1] + " (L" + c[2] + "T" + c[3] + ")");

        ComboBox<String> box = new ComboBox<>(FXCollections.observableArrayList(items));
        box.setPromptText(items.isEmpty() ? "No courses assigned" : "Select your course");
        styleCombo(box);
        return box;
    }

    // ── Extract course code from combo display string ─────────────────────────
    private String extractCode(String display) {
        if (display == null) return null;
        return display.split(" — ")[0].trim();
    }

    // ── Get students enrolled in a course ─────────────────────────────────────
    private List<Student> getStudentsByCourse(String courseCode) {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT u.id, u.first_name, u.last_name, u.department, u.username, u.status " +
                     "FROM users u JOIN course_registrations cr ON u.id = cr.user_id " +
                     "WHERE cr.course_code = ? AND u.role = 'STUDENT' ORDER BY u.first_name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Student s = new Student();
                s.setId(rs.getInt("id"));
                s.setFirstName(rs.getString("first_name"));
                s.setLastName(rs.getString("last_name"));
                s.setDepartment(rs.getString("department"));
                s.setUsername(rs.getString("username"));
                s.setStatus(backend.model.Status.valueOf(rs.getString("status")));
                list.add(s);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ── Sidebar nav ───────────────────────────────────────────────────────────
    @FXML private void showHome(ActionEvent e) {
        setActive(btnHome); pageTitle.setText("Home");
        loadHomeCards();
        mainContentArea.getChildren().setAll(homeContent);
    }

    @FXML private void showProfile(ActionEvent e) {
        setActive(btnProfile); pageTitle.setText("My Profile");
        mainContentArea.getChildren().setAll(buildProfileView());
    }

    @FXML private void showAttendanceEntry(ActionEvent e) {
        setActive(btnAttendance); pageTitle.setText("Attendance Entry");
        mainContentArea.getChildren().setAll(buildAttendanceView());
    }

    @FXML private void showResultsEntry(ActionEvent e) {
        setActive(btnResults); pageTitle.setText("Results Entry");
        mainContentArea.getChildren().setAll(buildResultsView());
    }

    @FXML private void showNotices(ActionEvent e) {
        setActive(btnNotices); pageTitle.setText("Notices");
        mainContentArea.getChildren().setAll(buildNoticesView());
    }

    @FXML private void showStudentList(ActionEvent e) {
        setActive(btnStudentList); pageTitle.setText("Student List");
        mainContentArea.getChildren().setAll(buildStudentListView());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PROFILE VIEW
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildProfileView() {
        VBox root = new VBox(20); root.setStyle("-fx-padding: 24;");

        // ── Profile Info Card ─────────────────────────────────────────────────
        VBox infoCard = card();
        infoCard.getChildren().add(sectionHeader("Profile Information"));

        String[] contact = getCurrentContact(faculty.getId());
        String[][] info = {
            {"Name",       faculty.getFirstName() + " " + faculty.getLastName()},
            {"Username",   faculty.getUsername() != null ? faculty.getUsername() : "—"},
            {"Email",      contact[0].isEmpty() ? "—" : contact[0]},
            {"Phone",      contact[1].isEmpty() ? "—" : contact[1]},
            {"Department", faculty.getDepartment() != null ? faculty.getDepartment() : "—"},
            {"Role",       "Faculty"},
            {"Status",     faculty.getStatus() != null ? faculty.getStatus().name() : "APPROVED"}
        };
        for (String[] f : info) {
            HBox row = new HBox(10);
            Label key = new Label(f[0] + ":"); key.setPrefWidth(110);
            key.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 13px;");
            Label val = new Label(f[1]);
            val.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 13px;");
            row.getChildren().addAll(key, val);
            infoCard.getChildren().add(row);
        }

        // ── Toggle buttons ────────────────────────────────────────────────────
        Button editToggleBtn = new Button("✏  Edit Info");
        editToggleBtn.setStyle(
                "-fx-background-color: #2a9d8f; -fx-text-fill: white; -fx-font-size: 13px;" +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 22;");

        Button passToggleBtn = new Button("🔒  Reset Password");
        passToggleBtn.setStyle(
                "-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 13px;" +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 22;");

        HBox btnRow = new HBox(12, editToggleBtn, passToggleBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        // ── Edit Info Panel (hidden by default) ───────────────────────────────
        VBox editPanel = buildEditInfoPanel();
        editPanel.setVisible(false);
        editPanel.setManaged(false);

        // ── Reset Password Panel (hidden by default) ──────────────────────────
        VBox passPanel = buildResetPasswordPanel();
        passPanel.setVisible(false);
        passPanel.setManaged(false);

        // ── Toggle logic ──────────────────────────────────────────────────────
        editToggleBtn.setOnAction(ev -> {
            boolean nowVisible = !editPanel.isVisible();
            editPanel.setVisible(nowVisible);
            editPanel.setManaged(nowVisible);
            // close the other panel
            passPanel.setVisible(false);
            passPanel.setManaged(false);
            // button style: active = darker shade
            editToggleBtn.setStyle(
                "-fx-background-color: " + (nowVisible ? "#1f7a6e" : "#2a9d8f") + ";" +
                "-fx-text-fill: white; -fx-font-size: 13px;" +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 22;");
            passToggleBtn.setStyle(
                "-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 13px;" +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 22;");
        });

        passToggleBtn.setOnAction(ev -> {
            boolean nowVisible = !passPanel.isVisible();
            passPanel.setVisible(nowVisible);
            passPanel.setManaged(nowVisible);
            // close the other panel
            editPanel.setVisible(false);
            editPanel.setManaged(false);
            // button style
            passToggleBtn.setStyle(
                "-fx-background-color: " + (nowVisible ? "#b7510a" : "#e67e22") + ";" +
                "-fx-text-fill: white; -fx-font-size: 13px;" +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 22;");
            editToggleBtn.setStyle(
                "-fx-background-color: #2a9d8f; -fx-text-fill: white; -fx-font-size: 13px;" +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 22;");
        });

        root.getChildren().addAll(infoCard, btnRow, editPanel, passPanel);
        return root;
    }

    // ── Edit Info Panel ───────────────────────────────────────────────────────
    private VBox buildEditInfoPanel() {
        VBox panel = card();
        panel.getChildren().add(sectionHeader("Edit Contact Info"));
        Label editMsg = new Label();

        String[] currentContact = getCurrentContact(faculty.getId());
        TextField emailFld = formField("Email"); emailFld.setText(currentContact[0]);
        TextField phoneFld = formField("Phone"); phoneFld.setText(currentContact[1]);

        Button saveBtn = styledBtn("Save Changes", "#2a9d8f");
        saveBtn.setOnAction(ev -> {
            String email = emailFld.getText().trim();
            String phone = phoneFld.getText().trim();
            if (email.isBlank()) { setMsg(editMsg, "⚠ Email cannot be empty.", false); return; }
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE users SET email=?, phone=? WHERE id=?")) {
                ps.setString(1, email); ps.setString(2, phone); ps.setInt(3, faculty.getId());
                ps.executeUpdate();
                setMsg(editMsg, "✔ Contact info updated successfully.", true);
            } catch (SQLException e) { setMsg(editMsg, "⚠ Update failed.", false); }
        });

        panel.getChildren().addAll(
                labeledField("Email *", emailFld),
                labeledField("Phone",   phoneFld),
                saveBtn, editMsg);
        return panel;
    }

    // ── Reset Password Panel ──────────────────────────────────────────────────
    private VBox buildResetPasswordPanel() {
        VBox panel = card();
        panel.getChildren().add(sectionHeader("Reset Password"));
        Label passMsg = new Label();

        PasswordField currentPassFld = new PasswordField(); currentPassFld.setPromptText("Current Password");
        PasswordField newPassFld     = new PasswordField(); newPassFld.setPromptText("New Password (min 6 chars)");
        PasswordField confirmPassFld = new PasswordField(); confirmPassFld.setPromptText("Confirm New Password");
        styleField(currentPassFld); styleField(newPassFld); styleField(confirmPassFld);

        Button changePassBtn = styledBtn("Change Password", "#e67e22");
        changePassBtn.setOnAction(ev -> {
            if (currentPassFld.getText().isBlank()) {
                setMsg(passMsg, "⚠ Enter your current password.", false); return; }
            if (newPassFld.getText().length() < 6) {
                setMsg(passMsg, "⚠ New password must be at least 6 characters.", false); return; }
            if (!newPassFld.getText().equals(confirmPassFld.getText())) {
                setMsg(passMsg, "⚠ Passwords do not match.", false); return; }

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT password FROM users WHERE id=?")) {
                ps.setInt(1, faculty.getId()); ResultSet rs = ps.executeQuery();
                if (!rs.next() || !PasswordUtil.checkPassword(
                        currentPassFld.getText(), rs.getString("password"))) {
                    setMsg(passMsg, "⚠ Current password is incorrect.", false); return;
                }
            } catch (SQLException e) { setMsg(passMsg, "⚠ Error.", false); return; }

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE users SET password=? WHERE id=?")) {
                ps.setString(1, PasswordUtil.hashPassword(newPassFld.getText()));
                ps.setInt(2, faculty.getId()); ps.executeUpdate();
                currentPassFld.clear(); newPassFld.clear(); confirmPassFld.clear();
                setMsg(passMsg, "✔ Password changed successfully.", true);
            } catch (SQLException e) { setMsg(passMsg, "⚠ Update failed.", false); }
        });

        panel.getChildren().addAll(
                labeledField("Current Password", currentPassFld),
                labeledField("New Password",     newPassFld),
                labeledField("Confirm",          confirmPassFld),
                changePassBtn, passMsg);
        return panel;
    }

    private String[] getCurrentContact(int userId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT email, phone FROM users WHERE id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new String[]{
                rs.getString("email") != null ? rs.getString("email") : "",
                rs.getString("phone") != null ? rs.getString("phone") : ""
            };
        } catch (SQLException e) { e.printStackTrace(); }
        return new String[]{"", ""};
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ATTENDANCE ENTRY VIEW — only assigned courses
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildAttendanceView() {
        VBox root = new VBox(16); root.setStyle("-fx-padding: 24;");
        Label msgLabel = new Label();
        VBox  tableArea = new VBox(10);

        ComboBox<String> courseBox = buildMyCourseCombo();
        if (courseBox.getItems().isEmpty()) {
            root.getChildren().add(noCoursesLabel());
            return root;
        }

        courseBox.setOnAction(ev -> {
            tableArea.getChildren().clear();
            String code = extractCode(courseBox.getValue());
            if (code == null) return;
            List<Student> students = getStudentsByCourse(code);
            if (students.isEmpty()) {
                tableArea.getChildren().add(new Label("No enrolled students for this course.")); return;
            }
            tableArea.getChildren().add(buildRowHeader("ID", "Name", "Classes Held", "Present", "Action"));
            for (Student s : students) {
                TextField heldFld    = formField("0"); heldFld.setPrefWidth(100);
                TextField presentFld = formField("0"); presentFld.setPrefWidth(100);
                Button saveBtn = styledBtn("Save", "#2a9d8f");
                saveBtn.setOnAction(e -> {
                    try {
                        int held    = Integer.parseInt(heldFld.getText().trim());
                        int present = Integer.parseInt(presentFld.getText().trim());
                        if (present > held) { setMsg(msgLabel, "⚠ Present > Held for " + s.getFirstName(), false); return; }
                        saveAttendance(s.getId(), code, held, present);
                        setMsg(msgLabel, "✔ Saved for " + s.getFirstName(), true);
                    } catch (NumberFormatException ex) { setMsg(msgLabel, "⚠ Enter valid numbers.", false); }
                });
                HBox row = dataRow();
                row.getChildren().addAll(colLabel(String.valueOf(s.getId()), 60),
                        colLabel(s.getFirstName() + " " + s.getLastName(), 200),
                        heldFld, presentFld, saveBtn);
                tableArea.getChildren().add(row);
            }
        });

        root.getChildren().addAll(sectionHeader("Attendance Entry — Select your course"),
                courseBox, msgLabel, scrollWrap(tableArea));
        return root;
    }

    private void saveAttendance(int userId, String courseCode, int held, int present) {
        String sql = "INSERT INTO attendance (user_id, course_code, total_held, total_attended) " +
                     "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                     "total_held=VALUES(total_held), total_attended=VALUES(total_attended)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, courseCode);
            ps.setInt(3, held); ps.setInt(4, present);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RESULTS ENTRY — CT / Quiz / Term Final with tabs
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildResultsView() {
        VBox root = new VBox(16); root.setStyle("-fx-padding: 24;");

        if (getMyCourses().isEmpty()) {
            root.getChildren().add(noCoursesLabel()); return root;
        }

        // ── Tab buttons ───────────────────────────────────────────────────────
        Button tabCT        = tabBtn("📝  CT Marks",         true);
        Button tabQuiz      = tabBtn("📋  Quiz Marks",       false);
        Button tabTermFinal = tabBtn("🏆  Term Final Marks", false);

        HBox tabBar = new HBox(0, tabCT, tabQuiz, tabTermFinal);
        tabBar.setStyle("-fx-background-color: white; -fx-background-radius: 10 10 0 0;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 1);");

        VBox contentPane = new VBox(16);
        contentPane.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 12 12;" +
                             "-fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");

        // Load CT by default
        loadCTView(contentPane);

        tabCT.setOnAction(e -> {
            setTabActive(tabCT, tabQuiz, tabTermFinal);
            contentPane.getChildren().clear();
            loadCTView(contentPane);
        });
        tabQuiz.setOnAction(e -> {
            setTabActive(tabQuiz, tabCT, tabTermFinal);
            contentPane.getChildren().clear();
            loadQuizView(contentPane);
        });
        tabTermFinal.setOnAction(e -> {
            setTabActive(tabTermFinal, tabCT, tabQuiz);
            contentPane.getChildren().clear();
            loadTermFinalView(contentPane);
        });

        root.getChildren().addAll(sectionHeader("Results Entry"), tabBar, contentPane);
        return root;
    }

    // ── CT Marks ──────────────────────────────────────────────────────────────
    private void loadCTView(VBox pane) {
        Label msgLabel = new Label();
        VBox  tableArea = new VBox(10);

        ComboBox<String> courseBox = buildMyCourseCombo();
        ComboBox<String> ctBox = new ComboBox<>(FXCollections.observableArrayList(
                "CT 1", "CT 2", "CT 3", "CT 4"));
        ctBox.setPromptText("Select CT");
        styleCombo(ctBox); ctBox.setPrefWidth(120);

        MarksDAO mDao = new MarksDAO();
        Runnable load = () -> {
            tableArea.getChildren().clear();
            String code = extractCode(courseBox.getValue());
            String ctV  = ctBox.getValue();
            if (code == null || ctV == null) return;
            int ctNum = Integer.parseInt(ctV.replace("CT ", ""));
            List<Student> students = getStudentsByCourse(code);
            if (students.isEmpty()) { tableArea.getChildren().add(new Label("No students.")); return; }
            tableArea.getChildren().add(buildRowHeader("ID", "Name", "Marks / 20", "Best 3 Sum", "Action"));
            for (Student s : students) {
                double[] existing = mDao.getStudentCTMarks(s.getId(), code);
                double   cur = existing[ctNum - 1];
                double   best3 = MarksDAO.getBest3CTSum(existing);
                TextField marksFld = formField("0-20"); marksFld.setPrefWidth(100);
                if (cur >= 0) marksFld.setText(String.valueOf(cur));
                Label best3Label = new Label(best3 > 0 ? String.valueOf(best3) : "—");
                best3Label.setStyle("-fx-font-size: 12px; -fx-text-fill: #2a9d8f; -fx-font-weight: bold;");
                Button saveBtn = styledBtn("Save", "#2a9d8f");
                saveBtn.setOnAction(e -> {
                    try {
                        double marks = Double.parseDouble(marksFld.getText().trim());
                        if (marks < 0 || marks > 20) { setMsg(msgLabel, "⚠ Marks must be 0-20.", false); return; }
                        mDao.saveCTMark(s.getId(), code, ctNum, marks, faculty.getId());
                        double[] updated = mDao.getStudentCTMarks(s.getId(), code);
                        best3Label.setText(String.valueOf(MarksDAO.getBest3CTSum(updated)));
                        setMsg(msgLabel, "✔ CT" + ctNum + " saved for " + s.getFirstName(), true);
                    } catch (NumberFormatException ex) { setMsg(msgLabel, "⚠ Invalid number.", false); }
                });
                HBox row = dataRow();
                row.getChildren().addAll(colLabel(String.valueOf(s.getId()), 60),
                        colLabel(s.getFirstName() + " " + s.getLastName(), 200),
                        marksFld, best3Label, saveBtn);
                tableArea.getChildren().add(row);
            }
        };

        courseBox.setOnAction(ev -> load.run());
        ctBox.setOnAction(ev -> load.run());

        Label note = new Label("ℹ  Best 3 of 4 CT marks will be counted automatically.");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        pane.getChildren().addAll(note, new HBox(12, courseBox, ctBox), msgLabel, scrollWrap(tableArea));
    }

    // ── Quiz Marks ────────────────────────────────────────────────────────────
    private void loadQuizView(VBox pane) {
        Label msgLabel  = new Label();
        VBox  tableArea = new VBox(10);

        ComboBox<String> courseBox  = buildMyCourseCombo();
        TextField        quizNumFld = formField("Quiz No. (e.g. 1)"); quizNumFld.setPrefWidth(140);
        TextField        fullMrkFld = formField("Full Marks"); fullMrkFld.setPrefWidth(120);
        MarksDAO         mDao       = new MarksDAO();

        Button loadBtn = styledBtn("Load Students", "#3498db");
        loadBtn.setOnAction(ev -> {
            tableArea.getChildren().clear();
            String code = extractCode(courseBox.getValue());
            if (code == null || quizNumFld.getText().isBlank() || fullMrkFld.getText().isBlank()) {
                setMsg(msgLabel, "⚠ Fill all fields first.", false); return;
            }
            int    qNum; double fMarks;
            try {
                qNum   = Integer.parseInt(quizNumFld.getText().trim());
                fMarks = Double.parseDouble(fullMrkFld.getText().trim());
            } catch (NumberFormatException e) { setMsg(msgLabel, "⚠ Invalid numbers.", false); return; }

            List<Student> students = getStudentsByCourse(code);
            if (students.isEmpty()) { tableArea.getChildren().add(new Label("No students.")); return; }
            tableArea.getChildren().add(buildRowHeader("ID", "Name", "Marks / " + fMarks, "", "Action"));
            double fm = fMarks;
            for (Student s : students) {
                TextField marksFld = formField("0-" + fm); marksFld.setPrefWidth(120);
                Button saveBtn = styledBtn("Save", "#2a9d8f");
                saveBtn.setOnAction(f -> {
                    try {
                        double marks = Double.parseDouble(marksFld.getText().trim());
                        if (marks < 0 || marks > fm) { setMsg(msgLabel, "⚠ Marks out of range.", false); return; }
                        mDao.saveQuizMark(s.getId(), code, qNum, marks, fm, faculty.getId());
                        setMsg(msgLabel, "✔ Quiz" + qNum + " saved for " + s.getFirstName(), true);
                    } catch (NumberFormatException e) { setMsg(msgLabel, "⚠ Invalid number.", false); }
                });
                HBox row = dataRow();
                row.getChildren().addAll(colLabel(String.valueOf(s.getId()), 60),
                        colLabel(s.getFirstName() + " " + s.getLastName(), 200),
                        marksFld, new Label("/ " + fm), saveBtn);
                tableArea.getChildren().add(row);
            }
        });

        Label note = new Label("ℹ  Faculty decides quiz number and full marks.");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        pane.getChildren().addAll(note,
                new HBox(12, courseBox, quizNumFld, fullMrkFld, loadBtn),
                msgLabel, scrollWrap(tableArea));
    }

    // ── Term Final Marks ──────────────────────────────────────────────────────
    private void loadTermFinalView(VBox pane) {
        Label msgLabel  = new Label();
        VBox  tableArea = new VBox(10);

        ComboBox<String> courseBox  = buildMyCourseCombo();
        TextField        fullMrkFld = formField("Full Marks"); fullMrkFld.setPrefWidth(120);

        Button loadBtn = styledBtn("Load Students", "#3498db");
        loadBtn.setOnAction(ev -> {
            tableArea.getChildren().clear();
            String code = extractCode(courseBox.getValue());
            if (code == null || fullMrkFld.getText().isBlank()) {
                setMsg(msgLabel, "⚠ Select course and enter full marks.", false); return;
            }
            double fMarks;
            try { fMarks = Double.parseDouble(fullMrkFld.getText().trim()); }
            catch (NumberFormatException e) { setMsg(msgLabel, "⚠ Invalid full marks.", false); return; }

            List<Student> students = getStudentsByCourse(code);
            if (students.isEmpty()) { tableArea.getChildren().add(new Label("No students.")); return; }
            tableArea.getChildren().add(buildRowHeader("ID", "Name", "Marks / " + fMarks, "", "Action"));
            double fm = fMarks;
            for (Student s : students) {
                // Load existing term final mark if any
                double existing = getTermFinalMark(s.getId(), code);
                TextField marksFld = formField("0-" + fm); marksFld.setPrefWidth(120);
                if (existing >= 0) marksFld.setText(String.valueOf(existing));
                Button saveBtn = styledBtn("Save", "#2a9d8f");
                saveBtn.setOnAction(f -> {
                    try {
                        double marks = Double.parseDouble(marksFld.getText().trim());
                        if (marks < 0 || marks > fm) { setMsg(msgLabel, "⚠ Marks out of range.", false); return; }
                        saveTermFinalMark(s.getId(), code, marks, fm);
                        setMsg(msgLabel, "✔ Term Final saved for " + s.getFirstName(), true);
                    } catch (NumberFormatException e) { setMsg(msgLabel, "⚠ Invalid number.", false); }
                });
                HBox row = dataRow();
                row.getChildren().addAll(colLabel(String.valueOf(s.getId()), 60),
                        colLabel(s.getFirstName() + " " + s.getLastName(), 200),
                        marksFld, new Label("/ " + fm), saveBtn);
                tableArea.getChildren().add(row);
            }
        });

        Label note = new Label("ℹ  Term Final marks are stored separately.");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        pane.getChildren().addAll(note,
                new HBox(12, courseBox, fullMrkFld, loadBtn),
                msgLabel, scrollWrap(tableArea));
    }

    private double getTermFinalMark(int userId, String courseCode) {
        String sql = "SELECT marks FROM term_final_marks WHERE user_id=? AND course_code=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, courseCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("marks");
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    private void saveTermFinalMark(int userId, String courseCode, double marks, double fullMarks) {
        String sql = "INSERT INTO term_final_marks (user_id, course_code, marks, full_marks, entered_by) " +
                     "VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE marks=VALUES(marks), full_marks=VALUES(full_marks)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, courseCode);
            ps.setDouble(3, marks); ps.setDouble(4, fullMarks); ps.setInt(5, faculty.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  NOTICES VIEW — admin posted notices পড়া
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildNoticesView() {
        VBox root = new VBox(16); root.setStyle("-fx-padding: 24;");

        Label msgLabel = new Label();
        VBox  noticeList = new VBox(12);

        Runnable load = () -> {
            noticeList.getChildren().clear();
            String sql = "SELECT * FROM notices " +
                         "WHERE audience = 'All' OR audience = 'Faculty Only' " +
                         "ORDER BY created_at DESC";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                boolean any = false;
                while (rs.next()) {
                    any = true;
                    VBox card = new VBox(8);
                    card.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                                  "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 0, 2);" +
                                  "-fx-padding: 16 18;");

                    // ── Top row: title + audience badge + time ────────────────
                    HBox top = new HBox(10);
                    top.setAlignment(Pos.CENTER_LEFT);

                    Label titleLbl = new Label(rs.getString("title"));
                    titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                    Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

                    String audience = rs.getString("audience");
                    Label audienceLbl = new Label(audience);
                    audienceLbl.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;" +
                                         "-fx-font-size: 10px; -fx-background-radius: 10; -fx-padding: 2 8;");

                    Timestamp ts = rs.getTimestamp("created_at");
                    String timeStr = ts != null ? ts.toLocalDateTime()
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "";
                    Label timeLbl = new Label(timeStr);
                    timeLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #aaa;");

                    top.getChildren().addAll(titleLbl, spacer, audienceLbl, timeLbl);

                    // ── Body ──────────────────────────────────────────────────
                    Label bodyLbl = new Label(rs.getString("body"));
                    bodyLbl.setWrapText(true);
                    bodyLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-padding: 4 0 0 0;");

                    card.getChildren().addAll(top, bodyLbl);
                    noticeList.getChildren().add(card);
                }

                if (!any) {
                    Label empty = new Label("📭  No notices at the moment.");
                    empty.setStyle("-fx-font-size: 13px; -fx-text-fill: #aaa; -fx-padding: 20 0;");
                    noticeList.getChildren().add(empty);
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                setMsg(msgLabel, "⚠ Could not load notices.", false);
            }
        };

        load.run();

        Button refreshBtn = styledBtn("↻  Refresh", "#3498db");
        refreshBtn.setOnAction(ev -> load.run());

        root.getChildren().addAll(
                sectionHeader("Notices from Admin"),
                refreshBtn,
                msgLabel,
                scrollWrap(noticeList));
        return root;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STUDENT LIST — only my assigned courses
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildStudentListView() {
        VBox root = new VBox(16); root.setStyle("-fx-padding: 24;");

        if (getMyCourses().isEmpty()) {
            root.getChildren().add(noCoursesLabel()); return root;
        }

        Label msgLabel = new Label();
        VBox  tableArea = new VBox(10);
        ComboBox<String> courseBox = buildMyCourseCombo();

        courseBox.setOnAction(ev -> {
            tableArea.getChildren().clear();
            String code = extractCode(courseBox.getValue());
            if (code == null) return;
            List<Student> students = getStudentsByCourse(code);
            if (students.isEmpty()) {
                tableArea.getChildren().add(new Label("No students enrolled.")); return;
            }
            tableArea.getChildren().add(
                    buildRowHeader("ID", "Name", "Department", "Username", "Status"));
            for (Student s : students) {
                HBox row = dataRow();
                row.getChildren().addAll(
                        colLabel(String.valueOf(s.getId()), 60),
                        colLabel(s.getFirstName() + " " + s.getLastName(), 200),
                        colLabel(s.getDepartment() != null ? s.getDepartment() : "—", 200),
                        colLabel(s.getUsername(), 150),
                        colLabel(s.getStatus().name(), 100));
                tableArea.getChildren().add(row);
            }
        });

        root.getChildren().addAll(sectionHeader("Students in My Courses"),
                courseBox, msgLabel, scrollWrap(tableArea));
        return root;
    }

    // ── Tab helpers ───────────────────────────────────────────────────────────
    private Button tabBtn(String text, boolean active) {
        Button b = new Button(text);
        b.setStyle((active
                ? "-fx-background-color: #2a9d8f; -fx-text-fill: white;"
                : "-fx-background-color: #f0f0f0; -fx-text-fill: #555;") +
                "-fx-font-size: 13px; -fx-padding: 10 20; -fx-cursor: hand;" +
                "-fx-background-radius: 0; -fx-border-color: transparent;");
        HBox.setHgrow(b, Priority.ALWAYS);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private void setTabActive(Button active, Button... others) {
        active.setStyle("-fx-background-color: #2a9d8f; -fx-text-fill: white;" +
                        "-fx-font-size: 13px; -fx-padding: 10 20; -fx-cursor: hand;" +
                        "-fx-background-radius: 0;");
        for (Button o : others) o.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #555;" +
                        "-fx-font-size: 13px; -fx-padding: 10 20; -fx-cursor: hand;" +
                        "-fx-background-radius: 0;");
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────
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

    private Label noCoursesLabel() {
        Label l = new Label("⚠  No courses assigned yet. Please contact the admin.");
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: #e67e22; -fx-padding: 20;");
        return l;
    }

    private HBox buildRowHeader(String... cols) {
        HBox h = new HBox(); h.setAlignment(Pos.CENTER_LEFT);
        h.setStyle("-fx-padding: 8 14; -fx-background-color: #f0f0f0; -fx-background-radius: 6;");
        int[] widths = {60, 200, 120, 150, 100};
        for (int i = 0; i < cols.length; i++) {
            Label l = new Label(cols[i]); l.setPrefWidth(i < widths.length ? widths[i] : 110);
            l.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #555;");
            h.getChildren().add(l);
        }
        return h;
    }

    private HBox dataRow() {
        HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 8 14; -fx-background-color: white;" +
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
                   "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 14; -fx-font-size: 12px;");
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

    private void styleCombo(ComboBox<?> b) {
        b.setStyle("-fx-pref-height: 34px; -fx-pref-width: 260px; -fx-background-radius: 6;" +
                   "-fx-border-radius: 6; -fx-border-color: #ccc; -fx-font-size: 13px;");
    }

    private HBox labeledField(String labelText, Control field) {
        Label lbl = new Label(labelText); lbl.setPrefWidth(130);
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

    private void setMsg(Label l, String m, boolean success) {
        l.setText(m);
        l.setStyle("-fx-text-fill: " + (success ? "#27ae60" : "#e74c3c") + "; -fx-font-size: 12px;");
    }

    private void setActive(Button btn) {
        if (activeBtn != null) activeBtn.setStyle(INACTIVE_STYLE);
        btn.setStyle(ACTIVE_STYLE);
        activeBtn = btn;
    }

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
