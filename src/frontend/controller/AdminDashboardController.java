package frontend.controller;

import backend.database.DatabaseConnection;
import backend.model.Status;
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

public class AdminDashboardController implements Initializable {

    @FXML private Button btnHome;
    @FXML private Button btnStudentApproval;
    @FXML private Button btnCourseApproval;
    @FXML private Button btnAddTeacher;
    @FXML private Button btnManageCourses;
    @FXML private Button btnAssignCourse;
    @FXML private Button btnNoticeBoard;
    @FXML private Button btnStudentDetails;
    @FXML private Button btnPublishResult;
    @FXML private Button btnResetCredentials;
    @FXML private Button btnLogout;

    @FXML private Label pageTitle;
    @FXML private Label adminNameLabel;
    @FXML private VBox  mainContentArea;
    @FXML private VBox  homeContent;

    @FXML private Label totalStudentsLabel;
    @FXML private Label pendingStudentsLabel;
    @FXML private Label totalTeachersLabel;
    @FXML private Label totalCoursesLabel;
    @FXML private Label pendingCourseRegLabel;

    private VBox teacherListContainer;
    private VBox courseListContainer;
    private VBox assignmentListContainer;
    private VBox noticeListContainer;
    private VBox studentApprovalTableArea;
    private VBox courseApprovalTableArea;
    private Button activeBtn;

    private static final String ACTIVE_STYLE =
            "-fx-background-color: #2a9d8f; -fx-text-fill: white; -fx-font-size: 13px;" +
            "-fx-alignment: CENTER-LEFT; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand;";
    private static final String INACTIVE_STYLE =
            "-fx-background-color: transparent; -fx-text-fill: #cdd9e0; -fx-font-size: 13px;" +
            "-fx-alignment: CENTER-LEFT; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Student s = SessionManager.getStudent();
        adminNameLabel.setText(s != null ? s.getFirstName() + " " + s.getLastName() : "Admin");
        activeBtn = btnHome;
        Button[] navBtns = {btnHome, btnStudentApproval, btnCourseApproval, btnAddTeacher,
                btnManageCourses, btnAssignCourse, btnNoticeBoard, btnStudentDetails,
                btnPublishResult, btnResetCredentials};
        for (Button btn : navBtns) {
            btn.setOnMouseEntered(e -> { if (btn != activeBtn) btn.setStyle(INACTIVE_STYLE.replace("transparent", "#243b4a")); });
            btn.setOnMouseExited(e ->  { if (btn != activeBtn) btn.setStyle(INACTIVE_STYLE); });
        }
        loadHomeCards();
    }

    private void loadHomeCards() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            totalStudentsLabel.setText(String.valueOf(queryCount(conn,
                    "SELECT COUNT(*) FROM users WHERE role='STUDENT' AND status='APPROVED'")));
            pendingStudentsLabel.setText(String.valueOf(queryCount(conn,
                    "SELECT COUNT(*) FROM users WHERE role='STUDENT' AND status='PENDING'")));
            totalTeachersLabel.setText(String.valueOf(queryCount(conn,
                    "SELECT COUNT(*) FROM users WHERE role='FACULTY' AND status='APPROVED'")));
            totalCoursesLabel.setText(String.valueOf(queryCount(conn, "SELECT COUNT(*) FROM courses")));
            pendingCourseRegLabel.setText(String.valueOf(queryCount(conn,
                    "SELECT COUNT(*) FROM course_registrations WHERE status='PENDING'")));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private int queryCount(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Sidebar navigation ────────────────────────────────────────────────────
    @FXML private void showHome(ActionEvent e) {
        setActive(btnHome); pageTitle.setText("Dashboard");
        loadHomeCards(); mainContentArea.getChildren().setAll(homeContent);
    }
    @FXML private void showStudentApproval(ActionEvent e) {
        setActive(btnStudentApproval); pageTitle.setText("Student Approval");
        mainContentArea.getChildren().setAll(buildStudentApprovalView());
    }
    @FXML private void showCourseApproval(ActionEvent e) {
        setActive(btnCourseApproval); pageTitle.setText("Course Registration Approval");
        mainContentArea.getChildren().setAll(buildCourseApprovalView());
    }
    @FXML private void showAddTeacher(ActionEvent e) {
        setActive(btnAddTeacher); pageTitle.setText("Add New Teacher");
        mainContentArea.getChildren().setAll(buildAddTeacherView());
    }
    @FXML private void showManageCourses(ActionEvent e) {
        setActive(btnManageCourses); pageTitle.setText("Manage Courses");
        mainContentArea.getChildren().setAll(buildManageCoursesView());
    }
    @FXML private void showAssignCourse(ActionEvent e) {
        setActive(btnAssignCourse); pageTitle.setText("Assign Course to Teacher");
        mainContentArea.getChildren().setAll(buildAssignCourseView());
    }
    @FXML private void showNoticeBoard(ActionEvent e) {
        setActive(btnNoticeBoard); pageTitle.setText("Notice Board");
        mainContentArea.getChildren().setAll(buildNoticeBoardView());
    }
    @FXML private void showStudentDetails(ActionEvent e) {
        setActive(btnStudentDetails); pageTitle.setText("Student Details");
        mainContentArea.getChildren().setAll(buildStudentDetailsView());
    }
    @FXML private void showPublishResult(ActionEvent e) {
        setActive(btnPublishResult); pageTitle.setText("Publish Term Result");
        mainContentArea.getChildren().setAll(buildPublishResultView());
    }
    @FXML private void showResetCredentials(ActionEvent e) {
        setActive(btnResetCredentials); pageTitle.setText("Reset Credentials");
        mainContentArea.getChildren().setAll(buildResetCredentialsView());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  1. STUDENT APPROVAL
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildStudentApprovalView() {
        VBox root = new VBox(16); root.setStyle("-fx-padding: 24;");
        Label msgLabel = new Label();
        studentApprovalTableArea = new VBox(10);
        refreshStudentApprovalList(msgLabel);
        Button refreshBtn = styledBtn("↻  Refresh", "#3498db");
        refreshBtn.setOnAction(ev -> refreshStudentApprovalList(msgLabel));
        root.getChildren().addAll(buildSectionHeader("Pending Student Registrations"),
                refreshBtn, msgLabel, scrollWrap(studentApprovalTableArea));
        return root;
    }

    private void refreshStudentApprovalList(Label msgLabel) {
        studentApprovalTableArea.getChildren().clear();
        List<Student> pending = getPendingStudents();
        if (pending.isEmpty()) {
            studentApprovalTableArea.getChildren().add(greenLabel("✔  No pending registrations.")); return;
        }
        studentApprovalTableArea.getChildren().add(
                buildRowHeader("ID", "Name", "Username", "Department", "Phone", "Actions"));
        for (Student s : pending) {
            HBox row = dataRow();
            Button approveBtn = styledBtn("Approve", "#27ae60");
            Button rejectBtn  = styledBtn("Reject",  "#e74c3c");
            approveBtn.setOnAction(ev -> {
                updateStatus(s.getId(), "APPROVED", "users");
                setMsg(msgLabel, "✔  " + s.getFirstName() + " approved.", true);
                studentApprovalTableArea.getChildren().remove(row);
            });
            rejectBtn.setOnAction(ev -> {
                updateStatus(s.getId(), "REJECTED", "users");
                setMsg(msgLabel, "✘  " + s.getFirstName() + " rejected.", false);
                studentApprovalTableArea.getChildren().remove(row);
            });
            row.getChildren().addAll(colLabel(String.valueOf(s.getId()), 50),
                    colLabel(s.getFirstName() + " " + s.getLastName(), 160),
                    colLabel(s.getUsername(), 130),
                    colLabel(s.getDepartment() != null ? s.getDepartment() : "—", 180),
                    colLabel(s.getPhone() != null ? s.getPhone() : "—", 130),
                    new HBox(8, approveBtn, rejectBtn));
            studentApprovalTableArea.getChildren().add(row);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  2. COURSE APPROVAL
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildCourseApprovalView() {
        VBox root = new VBox(16); root.setStyle("-fx-padding: 24;");
        Label msgLabel = new Label();
        courseApprovalTableArea = new VBox(10);
        refreshCourseApprovalList(msgLabel);
        Button refreshBtn = styledBtn("↻  Refresh", "#3498db");
        refreshBtn.setOnAction(ev -> refreshCourseApprovalList(msgLabel));
        root.getChildren().addAll(buildSectionHeader("Pending Course Registrations"),
                refreshBtn, msgLabel, scrollWrap(courseApprovalTableArea));
        return root;
    }

    private void refreshCourseApprovalList(Label msgLabel) {
        courseApprovalTableArea.getChildren().clear();
        String sql = "SELECT cr.id, u.first_name, u.last_name, cr.course_code, " +
                     "cr.course_name, cr.credit, cr.level, cr.term " +
                     "FROM course_registrations cr JOIN users u ON cr.user_id = u.id " +
                     "WHERE cr.status = 'PENDING' ORDER BY u.first_name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            courseApprovalTableArea.getChildren().add(
                    buildRowHeader("Student", "Course", "Name", "Credit", "Level/Term", "Actions"));
            boolean any = false;
            while (rs.next()) {
                any = true;
                final int regId = rs.getInt("id");
                final String code = rs.getString("course_code");
                final String sName = rs.getString("first_name") + " " + rs.getString("last_name");
                HBox row = dataRow();
                Button approveBtn = styledBtn("Approve", "#27ae60");
                Button rejectBtn  = styledBtn("Reject",  "#e74c3c");
                approveBtn.setOnAction(ev -> {
                    exec("UPDATE course_registrations SET status='APPROVED' WHERE id=?", regId);
                    setMsg(msgLabel, "✔  " + code + " approved for " + sName, true);
                    courseApprovalTableArea.getChildren().remove(row);
                });
                rejectBtn.setOnAction(ev -> {
                    exec("UPDATE course_registrations SET status='REJECTED' WHERE id=?", regId);
                    setMsg(msgLabel, "✘  " + code + " rejected for " + sName, false);
                    courseApprovalTableArea.getChildren().remove(row);
                });
                row.getChildren().addAll(colLabel(sName, 150), colLabel(code, 80),
                        colLabel(rs.getString("course_name"), 180),
                        colLabel(String.valueOf(rs.getDouble("credit")), 55),
                        colLabel("L" + rs.getInt("level") + "T" + rs.getInt("term"), 70),
                        new HBox(8, approveBtn, rejectBtn));
                courseApprovalTableArea.getChildren().add(row);
            }
            if (!any) courseApprovalTableArea.getChildren().add(greenLabel("✔  No pending course registrations."));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  3. ADD TEACHER
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildAddTeacherView() {
        VBox root = new VBox(20); root.setStyle("-fx-padding: 24;");
        Label msgLabel = new Label();
        teacherListContainer = new VBox(8);
        refreshTeacherList();
        TextField fNameFld = formField("First Name"), lNameFld = formField("Last Name");
        TextField emailFld = formField("Email"), phoneFld = formField("Phone");
        TextField usernameFld = formField("Username");
        ComboBox<String> deptBox = new ComboBox<>(FXCollections.observableArrayList(
                "Computer Science & Engineering", "Electrical & Electronic Engineering",
                "Civil Engineering", "Mechanical Engineering", "Business Administration",
                "Mathematics", "Physics", "English"));
        deptBox.setPromptText("Select Department"); styleCombo(deptBox);
        PasswordField passFld = new PasswordField(); passFld.setPromptText("Password");
        PasswordField confirmFld = new PasswordField(); confirmFld.setPromptText("Confirm Password");
        styleField(passFld); styleField(confirmFld);
        Button addBtn = styledBtn("Add Teacher", "#2a9d8f");
        addBtn.setStyle(addBtn.getStyle() + " -fx-padding: 10 24; -fx-font-size: 13px;");
        addBtn.setOnAction(ev -> {
            if (fNameFld.getText().isBlank() || lNameFld.getText().isBlank()) { setMsg(msgLabel, "⚠ Names required.", false); return; }
            if (usernameFld.getText().length() < 4) { setMsg(msgLabel, "⚠ Username min 4 chars.", false); return; }
            if (emailFld.getText().isBlank()) { setMsg(msgLabel, "⚠ Email required.", false); return; }
            if (deptBox.getValue() == null) { setMsg(msgLabel, "⚠ Select department.", false); return; }
            if (passFld.getText().length() < 6) { setMsg(msgLabel, "⚠ Password min 6 chars.", false); return; }
            if (!passFld.getText().equals(confirmFld.getText())) { setMsg(msgLabel, "⚠ Passwords don't match.", false); return; }
            if (insertTeacher(fNameFld.getText().trim(), lNameFld.getText().trim(),
                    emailFld.getText().trim(), phoneFld.getText().trim(),
                    deptBox.getValue(), usernameFld.getText().trim(), passFld.getText())) {
                setMsg(msgLabel, "✔ Teacher added!", true);
                fNameFld.clear(); lNameFld.clear(); emailFld.clear(); phoneFld.clear();
                usernameFld.clear(); passFld.clear(); confirmFld.clear(); deptBox.setValue(null);
                refreshTeacherList();
            } else { setMsg(msgLabel, "⚠ Failed — username or email may exist.", false); }
        });
        VBox form = new VBox(12);
        form.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);" +
                      "-fx-padding: 24; -fx-pref-width: 480;");
        form.getChildren().addAll(buildSectionHeader("Register New Teacher"),
                new HBox(12, labeledField("First Name *", fNameFld), labeledField("Last Name *", lNameFld)),
                labeledField("Username *", usernameFld), labeledField("Email *", emailFld),
                labeledField("Phone", phoneFld), labeledField("Department *", deptBox),
                labeledField("Password *", passFld), labeledField("Confirm *", confirmFld), addBtn, msgLabel);
        VBox listPanel = new VBox(10);
        listPanel.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                           "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2); -fx-padding: 20;");
        HBox.setHgrow(listPanel, Priority.ALWAYS);
        listPanel.getChildren().addAll(buildSectionHeader("Current Teachers"), scrollWrap(teacherListContainer));
        root.getChildren().add(new HBox(20, form, listPanel));
        return root;
    }

    private void refreshTeacherList() {
        teacherListContainer.getChildren().clear();
        try (Connection conn = DatabaseConnection.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                     "SELECT id, first_name, last_name, username, department, status FROM users WHERE role='FACULTY' ORDER BY first_name")) {
            teacherListContainer.getChildren().add(buildRowHeader("Name", "Username", "Department", "Status", "Action"));
            while (rs.next()) {
                int tid = rs.getInt("id"); HBox row = dataRow();
                Button removeBtn = styledBtn("Remove", "#e74c3c");
                removeBtn.setOnAction(e -> { removeTeacher(tid); refreshTeacherList(); });
                row.getChildren().addAll(
                        colLabel(rs.getString("first_name") + " " + rs.getString("last_name"), 160),
                        colLabel(rs.getString("username") != null ? rs.getString("username") : "—", 110),
                        colLabel(rs.getString("department") != null ? rs.getString("department") : "—", 160),
                        colLabel(rs.getString("status"), 90), removeBtn);
                teacherListContainer.getChildren().add(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  4. MANAGE COURSES — BUG FIX: pre-check duplicate before insert
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildManageCoursesView() {
        VBox root = new VBox(20); root.setStyle("-fx-padding: 24;");
        Label msgLabel = new Label();
        courseListContainer = new VBox(8);

        TextField codeFld   = formField("Course Code (e.g. CSE501)"); codeFld.setPrefWidth(170);
        TextField nameFld   = formField("Course Name");               nameFld.setPrefWidth(220);
        TextField creditFld = formField("Credit (e.g. 3.0)");         creditFld.setPrefWidth(110);
        Label lvlLabel = new Label("Level:"); lvlLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#555; -fx-font-weight:bold;");
        ComboBox<String> lvlBox = new ComboBox<>(FXCollections.observableArrayList("1","2","3","4"));
        lvlBox.setPromptText("Level"); lvlBox.setPrefWidth(80);
        Label trmLabel = new Label("Term:"); trmLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#555; -fx-font-weight:bold;");
        ComboBox<String> trmBox = new ComboBox<>(FXCollections.observableArrayList("1","2"));
        trmBox.setPromptText("Term"); trmBox.setPrefWidth(80);

        Button addBtn = styledBtn("+ Add Course", "#2a9d8f");
        addBtn.setOnAction(e -> {
            String code = codeFld.getText().trim().toUpperCase();
            String name = nameFld.getText().trim();
            String cStr = creditFld.getText().trim();
            if (code.isEmpty() || name.isEmpty() || cStr.isEmpty()
                    || lvlBox.getValue() == null || trmBox.getValue() == null) {
                setMsg(msgLabel, "⚠ Please fill in all fields.", false); return;
            }
            double credit;
            try { credit = Double.parseDouble(cStr); }
            catch (NumberFormatException ex) { setMsg(msgLabel, "⚠ Credit must be a number.", false); return; }

            // ── KEY FIX: check duplicate BEFORE insert ────────────────────────
            if (courseCodeExists(code)) {
                setMsg(msgLabel, "⚠ Course code '" + code + "' already exists. Choose a different code.", false); return;
            }

            if (insertCourse(code, name, credit, Integer.parseInt(lvlBox.getValue()), Integer.parseInt(trmBox.getValue()))) {
                setMsg(msgLabel, "✔ Course " + code + " added!", true);
                codeFld.clear(); nameFld.clear(); creditFld.clear(); lvlBox.setValue(null); trmBox.setValue(null);
                refreshCourseList(msgLabel);
            } else { setMsg(msgLabel, "⚠ Failed to add course.", false); }
        });

        VBox formCard = new VBox(12);
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2); -fx-padding: 20;");
        HBox lvlRow = new HBox(6, lvlLabel, lvlBox); lvlRow.setAlignment(Pos.CENTER_LEFT);
        HBox trmRow = new HBox(6, trmLabel, trmBox); trmRow.setAlignment(Pos.CENTER_LEFT);
        formCard.getChildren().addAll(buildSectionHeader("Add New Course"),
                new HBox(10, codeFld, nameFld, creditFld, lvlRow, trmRow),
                new HBox(10, addBtn, new Label("* All fields required") {{
                    setStyle("-fx-font-size:11px; -fx-text-fill:#aaa;"); }}),
                msgLabel);

        refreshCourseList(msgLabel);
        root.getChildren().addAll(formCard, buildSectionHeader("All Courses"), scrollWrap(courseListContainer));
        return root;
    }

    private boolean courseCodeExists(String code) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM courses WHERE code=?")) {
            ps.setString(1, code); ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private void refreshCourseList(Label msgLabel) {
        courseListContainer.getChildren().clear();
        courseListContainer.getChildren().add(buildRowHeader("Code", "Name", "Credit", "Level", "Term", "Action"));
        try (Connection conn = DatabaseConnection.getConnection();
             ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM courses ORDER BY level, term, code")) {
            while (rs.next()) {
                int id = rs.getInt("id"); String code = rs.getString("code"); HBox row = dataRow();
                Button delBtn = styledBtn("Remove", "#e74c3c");
                delBtn.setOnAction(e -> { deleteCourse(id); setMsg(msgLabel, "✘ Removed " + code, false); refreshCourseList(msgLabel); });
                row.getChildren().addAll(colLabel(code, 80), colLabel(rs.getString("name"), 200),
                        colLabel(String.valueOf(rs.getDouble("credit")), 60),
                        colLabel("Level " + rs.getInt("level"), 70), colLabel("Term " + rs.getInt("term"), 60), delBtn);
                courseListContainer.getChildren().add(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  5. ASSIGN COURSE
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildAssignCourseView() {
        VBox root = new VBox(16); root.setStyle("-fx-padding: 24;");
        Label msgLabel = new Label();
        assignmentListContainer = new VBox(8);
        ComboBox<String> tBox = new ComboBox<>(); tBox.setPromptText("Select Teacher"); tBox.setPrefWidth(250);
        ComboBox<String> cBox = new ComboBox<>(); cBox.setPromptText("Select Course");  cBox.setPrefWidth(300);
        Map<String, Integer> tMap = new LinkedHashMap<>();
        Map<String, String>  cMap = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            ResultSet rsT = conn.createStatement().executeQuery(
                    "SELECT id, first_name, last_name FROM users WHERE role='FACULTY' AND status='APPROVED' ORDER BY first_name");
            while (rsT.next()) { String n = rsT.getString("first_name")+" "+rsT.getString("last_name"); tMap.put(n, rsT.getInt("id")); tBox.getItems().add(n); }
            ResultSet rsC = conn.createStatement().executeQuery("SELECT code, name, level, term FROM courses ORDER BY level, term, code");
            while (rsC.next()) { String d = rsC.getString("code")+" — "+rsC.getString("name")+" (L"+rsC.getInt("level")+"T"+rsC.getInt("term")+")"; cMap.put(d, rsC.getString("code")); cBox.getItems().add(d); }
        } catch (SQLException e) { e.printStackTrace(); }
        Button btn = styledBtn("Assign", "#2a9d8f"); btn.setStyle(btn.getStyle() + " -fx-padding: 9 24;");
        btn.setOnAction(e -> {
            if (tBox.getValue() == null) { setMsg(msgLabel, "⚠ Select a teacher.", false); return; }
            if (cBox.getValue() == null) { setMsg(msgLabel, "⚠ Select a course.", false); return; }
            if (assignCourseToTeacher(cMap.get(cBox.getValue()), tMap.get(tBox.getValue()), tBox.getValue())) {
                setMsg(msgLabel, "✔ Assigned to " + tBox.getValue(), true); refreshAssignments(msgLabel);
            } else { setMsg(msgLabel, "⚠ Assignment failed.", false); }
        });
        VBox formCard = new VBox(12);
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2); -fx-padding: 20;");
        Label tLabel = new Label("Select Teacher:"); tLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#555; -fx-font-weight:bold;");
        Label cLabel = new Label("Select Course:");  cLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#555; -fx-font-weight:bold;");
        formCard.getChildren().addAll(buildSectionHeader("Assign Course to Teacher"),
                new VBox(4, tLabel, tBox), new VBox(4, cLabel, cBox), btn, msgLabel);
        refreshAssignments(msgLabel);
        root.getChildren().addAll(formCard, buildSectionHeader("Current Assignments"), scrollWrap(assignmentListContainer));
        return root;
    }

    private void refreshAssignments(Label msgLabel) {
        assignmentListContainer.getChildren().clear();
        assignmentListContainer.getChildren().add(buildRowHeader("Code", "Course Name", "Teacher", "Level/Term", "Action"));
        try (Connection conn = DatabaseConnection.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                     "SELECT c.id, c.code, c.name, c.level, c.term, u.first_name, u.last_name FROM courses c JOIN users u ON c.faculty_id = u.id ORDER BY c.level, c.term")) {
            while (rs.next()) {
                int id = rs.getInt("id"); HBox row = dataRow();
                Button ub = styledBtn("Unassign", "#e67e22");
                ub.setOnAction(e -> { unassignCourse(id); refreshAssignments(msgLabel); });
                row.getChildren().addAll(colLabel(rs.getString("code"), 80), colLabel(rs.getString("name"), 200),
                        colLabel(rs.getString("first_name")+" "+rs.getString("last_name"), 160),
                        colLabel("L"+rs.getInt("level")+"T"+rs.getInt("term"), 70), ub);
                assignmentListContainer.getChildren().add(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  6. NOTICE BOARD
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildNoticeBoardView() {
        VBox root = new VBox(16); root.setStyle("-fx-padding: 24;");
        Label msgLabel = new Label();
        noticeListContainer = new VBox(10);
        TextField tFld = formField("Notice Title");
        TextArea  bArea = new TextArea(); bArea.setPrefRowCount(3); bArea.setPromptText("Notice content...");
        bArea.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ccc; -fx-font-size: 13px;");
        ComboBox<String> aBox = new ComboBox<>(FXCollections.observableArrayList("All","Students Only","Faculty Only"));
        aBox.setValue("All"); styleCombo(aBox);
        Button pBtn = styledBtn("Post Notice", "#2a9d8f"); pBtn.setStyle(pBtn.getStyle() + " -fx-padding: 9 22;");
        pBtn.setOnAction(e -> {
            if (tFld.getText().isBlank() || bArea.getText().isBlank()) { setMsg(msgLabel, "⚠ Title and content required.", false); return; }
            if (insertNotice(tFld.getText(), bArea.getText(), aBox.getValue())) { setMsg(msgLabel, "✔ Posted!", true); tFld.clear(); bArea.clear(); refreshNoticeList(); }
        });
        refreshNoticeList();
        root.getChildren().addAll(buildSectionHeader("Post Notice"), tFld,
                new HBox(10, new Label("Audience:") {{ setStyle("-fx-font-size:12px; -fx-text-fill:#555;"); }}, aBox),
                bArea, pBtn, msgLabel, buildSectionHeader("Posted Notices"), scrollWrap(noticeListContainer));
        return root;
    }

    private void refreshNoticeList() {
        noticeListContainer.getChildren().clear();
        try (Connection conn = DatabaseConnection.getConnection();
             ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM notices ORDER BY created_at DESC")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                VBox card = new VBox(5); card.setStyle("-fx-background-color: white; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #eee; -fx-border-radius: 8;");
                HBox top = new HBox(10); Label tl = new Label(rs.getString("title")); tl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                Button db = styledBtn("Delete", "#e74c3c"); db.setOnAction(e -> { deleteNotice(id); refreshNoticeList(); });
                top.getChildren().addAll(tl, sp, db);
                Label body = new Label(rs.getString("body")); body.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;"); body.setWrapText(true);
                card.getChildren().addAll(top, body); noticeListContainer.getChildren().add(card);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  7. STUDENT DETAILS
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildStudentDetailsView() {
        VBox root = new VBox(16); root.setStyle("-fx-padding: 24;");
        TextField sFld = formField("Search by name or username...");
        VBox res = new VBox(5);
        VBox det = new VBox(10); det.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12;");
        sFld.setOnKeyReleased(e -> {
            res.getChildren().clear(); String q = sFld.getText().trim(); if (q.isEmpty()) return;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT id, first_name, last_name, username, status FROM users WHERE role='STUDENT' AND (first_name LIKE ? OR username LIKE ?)")) {
                ps.setString(1, "%"+q+"%"); ps.setString(2, "%"+q+"%"); ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt("id"); String name = rs.getString("first_name")+" "+rs.getString("last_name");
                    Label l = new Label(id+"  |  "+name+"  ("+rs.getString("username")+")");
                    l.setStyle("-fx-cursor: hand; -fx-padding: 6 10; -fx-background-color: white; -fx-background-radius: 6; -fx-border-color: #eee;");
                    l.setOnMouseClicked(ev -> showStudentFullDetail(id, det)); res.getChildren().add(l);
                }
            } catch (SQLException ex) { ex.printStackTrace(); }
        });
        root.getChildren().addAll(buildSectionHeader("Search Student"), sFld, res, det);
        return root;
    }

    private void showStudentFullDetail(int studentId, VBox panel) {
        panel.getChildren().clear();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id=?")) {
            ps.setInt(1, studentId); ResultSet rs = ps.executeQuery(); if (!rs.next()) return;
            panel.getChildren().add(buildSectionHeader(rs.getString("first_name")+" "+rs.getString("last_name")));
            String[][] fields = {{"ID",String.valueOf(rs.getInt("id"))},{"Username",rs.getString("username")},{"Email",rs.getString("email")},
                {"Phone",rs.getString("phone")},{"Department",rs.getString("department")},{"Status",rs.getString("status")},{"Role",rs.getString("role")}};
            for (String[] f : fields) {
                HBox row = new HBox(10); Label key = new Label(f[0]+":"); key.setPrefWidth(100); key.setStyle("-fx-font-weight:bold; -fx-text-fill:#555;");
                Label val = new Label(f[1]!=null?f[1]:"—"); val.setStyle("-fx-text-fill:#2c3e50;"); row.getChildren().addAll(key, val); panel.getChildren().add(row);
            }
            panel.getChildren().add(new Separator());
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  8. PUBLISH TERM RESULT
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildPublishResultView() {
        VBox root = new VBox(20); root.setStyle("-fx-padding: 24;");
        ComboBox<String> lvlBox = new ComboBox<>(FXCollections.observableArrayList("1","2","3","4"));
        lvlBox.setPromptText("Level"); lvlBox.setPrefWidth(90);
        ComboBox<String> trmBox = new ComboBox<>(FXCollections.observableArrayList("1","2"));
        trmBox.setPromptText("Term"); trmBox.setPrefWidth(80);
        TextField termLabelFld = formField("Term Label (e.g. L2T1-2024)"); termLabelFld.setPrefWidth(200);
        Label  msgLabel    = new Label();
        VBox   previewArea = new VBox(10);
        Button previewBtn  = styledBtn("Preview Results", "#3498db");
        Button publishBtn  = styledBtn("✔  Publish Results", "#27ae60");
        publishBtn.setStyle(publishBtn.getStyle() + " -fx-padding: 10 22; -fx-font-size: 13px;");
        publishBtn.setDisable(true);
        List<ResultRow> resultRows = new ArrayList<>();

        previewBtn.setOnAction(e -> {
            previewArea.getChildren().clear(); resultRows.clear(); publishBtn.setDisable(true);
            if (lvlBox.getValue() == null || trmBox.getValue() == null || termLabelFld.getText().isBlank()) {
                setMsg(msgLabel, "⚠ Select Level, Term and enter a Term Label.", false); return;
            }
            int level = Integer.parseInt(lvlBox.getValue()), term = Integer.parseInt(trmBox.getValue());
            String termLabel = termLabelFld.getText().trim();
            List<ResultRow> rows = calculateResults(level, term, termLabel);
            if (rows.isEmpty()) { setMsg(msgLabel, "⚠ No approved registrations found for L"+level+"T"+term, false); return; }
            resultRows.addAll(rows);
            previewArea.getChildren().add(buildResultPreview(rows));
            publishBtn.setDisable(false);
            setMsg(msgLabel, "Preview ready. Click Publish to save.", true);
        });

        publishBtn.setOnAction(e -> {
            if (resultRows.isEmpty()) return;
            int adminId = SessionManager.getStudent() != null ? SessionManager.getStudent().getId() : 0;
            publishResults(resultRows, adminId);
            publishBtn.setDisable(true);
            setMsg(msgLabel, "✔ Results published for " + resultRows.size() + " entries!", true);
        });

        VBox controlCard = new VBox(14);
        controlCard.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                             "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2); -fx-padding: 22;");
        Label note = new Label(
            "ℹ  Result = 300 marks total\n" +
            "   CT (20%) = Best 3 CT × (each/20) → max 60  |  " +
            "Attendance (10%) → max 30: ≥80%=full, per 5% drop=−3, <60%=15\n" +
            "   Term Final (70%) = faculty input scaled to 210");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;"); note.setWrapText(true);
        HBox selRow = new HBox(12, lvlBox, trmBox, termLabelFld, previewBtn); selRow.setAlignment(Pos.CENTER_LEFT);
        controlCard.getChildren().addAll(buildSectionHeader("Publish Term Result"), note, selRow, publishBtn, msgLabel);
        root.getChildren().addAll(controlCard, scrollWrap(previewArea));
        return root;
    }

    // ── Result row data class ─────────────────────────────────────────────────
    private static class ResultRow {
        int userId; String studentName, courseCode, courseName, termLabel, letterGrade;
        double credit, ctMark, attendanceMark, termFinalMark, totalMarks, gradePoint;
        int level, term;
    }

    // ── Calculate results ─────────────────────────────────────────────────────
    private List<ResultRow> calculateResults(int level, int term, String termLabel) {
        List<ResultRow> rows = new ArrayList<>();
        String sql = "SELECT cr.user_id, cr.course_code, cr.course_name, cr.credit, u.first_name, u.last_name " +
                     "FROM course_registrations cr JOIN users u ON cr.user_id = u.id " +
                     "WHERE cr.level=? AND cr.term=? AND cr.status='APPROVED' ORDER BY u.first_name, cr.course_code";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, level); ps.setInt(2, term); ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int uid = rs.getInt("user_id"); String code = rs.getString("course_code");
                double[] ctMarks = getStudentCTMarks(conn, uid, code);
                double   ctMark  = ResultEngine.calcCTMark(backend.dao.MarksDAO.getBest3CTSum(ctMarks));
                double   attPct  = getAttendancePct(conn, uid, code);
                double   attMark = ResultEngine.calcAttendanceMark(attPct);
                double[] tf      = getTermFinalRaw(conn, uid, code);
                double   tfMark  = ResultEngine.calcTermFinalMark(tf[0], tf[1]);
                double   total   = ResultEngine.calcTotal(ctMark, attMark, tfMark);
                ResultRow r = new ResultRow();
                r.userId = uid; r.studentName = rs.getString("first_name")+" "+rs.getString("last_name");
                r.courseCode = code; r.courseName = rs.getString("course_name");
                r.credit = rs.getDouble("credit"); r.level = level; r.term = term; r.termLabel = termLabel;
                r.ctMark = ResultEngine.round2(ctMark); r.attendanceMark = ResultEngine.round2(attMark);
                r.termFinalMark = ResultEngine.round2(tfMark); r.totalMarks = ResultEngine.round2(total);
                r.gradePoint = ResultEngine.gradePoint(total); r.letterGrade = ResultEngine.letterGrade(total);
                rows.add(r);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return rows;
    }

    // ── Preview table ─────────────────────────────────────────────────────────
    private VBox buildResultPreview(List<ResultRow> rows) {
        VBox area = new VBox(6);
        area.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2); -fx-padding: 20;");
        area.getChildren().add(buildSectionHeader("Result Preview"));
        HBox hdr = new HBox(); hdr.setStyle("-fx-padding: 8 12; -fx-background-color: #f0f0f0; -fx-background-radius: 6;");
        String[] cols = {"Student","Course","Credit","CT/60","Att/30","Final/210","Total/300","Grade","GP"};
        int[]    wids = {160,80,55,55,55,80,80,55,50};
        for (int i = 0; i < cols.length; i++) {
            Label l = new Label(cols[i]); l.setPrefWidth(wids[i]);
            l.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #555;"); hdr.getChildren().add(l);
        }
        area.getChildren().add(hdr);
        for (ResultRow r : rows) {
            HBox row = new HBox(); row.setStyle("-fx-padding: 7 12; -fx-background-color: white; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");
            String gpColor = r.gradePoint >= 3.5 ? "#27ae60" : r.gradePoint >= 2.5 ? "#f39c12" : "#e74c3c";
            Label gl = new Label(r.letterGrade); gl.setPrefWidth(55); gl.setStyle("-fx-font-weight:bold; -fx-text-fill:"+gpColor+";");
            Label gpl = new Label(String.valueOf(r.gradePoint)); gpl.setPrefWidth(50); gpl.setStyle("-fx-font-weight:bold; -fx-text-fill:"+gpColor+";");
            row.getChildren().addAll(cl(r.studentName,160),cl(r.courseCode,80),cl(String.valueOf(r.credit),55),
                    cl(String.valueOf(r.ctMark),55),cl(String.valueOf(r.attendanceMark),55),
                    cl(String.valueOf(r.termFinalMark),80),cl(String.valueOf(r.totalMarks),80),gl,gpl);
            area.getChildren().add(row);
        }
        area.getChildren().add(new Separator());
        area.getChildren().add(buildSectionHeader("Term GPA Summary"));
        Map<Integer, List<ResultRow>> byStudent = new LinkedHashMap<>();
        for (ResultRow r : rows) byStudent.computeIfAbsent(r.userId, k -> new ArrayList<>()).add(r);
        for (Map.Entry<Integer, List<ResultRow>> entry : byStudent.entrySet()) {
            List<ResultRow> sRows = entry.getValue();
            double termGPA = ResultEngine.calcTermGPA(
                sRows.stream().mapToDouble(r -> r.gradePoint).toArray(),
                sRows.stream().mapToDouble(r -> r.credit).toArray());
            double cgpa = calcCGPAWithNew(entry.getKey(), sRows.get(0).termLabel, termGPA);
            HBox gpRow = new HBox(10); gpRow.setStyle("-fx-padding: 5 12;");
            gpRow.getChildren().addAll(cl(sRows.get(0).studentName,200),
                    cl("Term GPA: "+termGPA,150),cl("CGPA: "+cgpa,150));
            area.getChildren().add(gpRow);
        }
        return area;
    }

    // ── Publish to DB ─────────────────────────────────────────────────────────
    private void publishResults(List<ResultRow> rows, int adminId) {
        String resSql = "INSERT INTO results (user_id,course_code,course_name,credit,level,term,term_label," +
                "ct_best3,attendance_mark,term_final_mark,total_marks,grade_point,letter_grade,published_by) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
                "ct_best3=VALUES(ct_best3),attendance_mark=VALUES(attendance_mark)," +
                "term_final_mark=VALUES(term_final_mark),total_marks=VALUES(total_marks)," +
                "grade_point=VALUES(grade_point),letter_grade=VALUES(letter_grade),published_at=NOW()";
        String cgpaSql = "INSERT INTO cgpa_summary (user_id,term_label,level,term,term_gpa,cgpa) VALUES (?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE term_gpa=VALUES(term_gpa),cgpa=VALUES(cgpa)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(resSql)) {
                for (ResultRow r : rows) {
                    ps.setInt(1,r.userId); ps.setString(2,r.courseCode); ps.setString(3,r.courseName);
                    ps.setDouble(4,r.credit); ps.setInt(5,r.level); ps.setInt(6,r.term); ps.setString(7,r.termLabel);
                    ps.setDouble(8,r.ctMark); ps.setDouble(9,r.attendanceMark); ps.setDouble(10,r.termFinalMark);
                    ps.setDouble(11,r.totalMarks); ps.setDouble(12,r.gradePoint); ps.setString(13,r.letterGrade);
                    ps.setInt(14,adminId); ps.addBatch();
                }
                ps.executeBatch();
            }
            Map<Integer, List<ResultRow>> byStudent = new LinkedHashMap<>();
            for (ResultRow r : rows) byStudent.computeIfAbsent(r.userId, k -> new ArrayList<>()).add(r);
            try (PreparedStatement ps = conn.prepareStatement(cgpaSql)) {
                for (Map.Entry<Integer, List<ResultRow>> entry : byStudent.entrySet()) {
                    List<ResultRow> sRows = entry.getValue();
                    double termGPA = ResultEngine.calcTermGPA(sRows.stream().mapToDouble(r->r.gradePoint).toArray(), sRows.stream().mapToDouble(r->r.credit).toArray());
                    double cgpa    = calcCGPAWithNew(entry.getKey(), sRows.get(0).termLabel, termGPA);
                    ps.setInt(1,entry.getKey()); ps.setString(2,sRows.get(0).termLabel);
                    ps.setInt(3,sRows.get(0).level); ps.setInt(4,sRows.get(0).term);
                    ps.setDouble(5,termGPA); ps.setDouble(6,cgpa); ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private double calcCGPAWithNew(int userId, String newTermLabel, double newTermGPA) {
        List<Double> all = new ArrayList<>(); all.add(newTermGPA);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT term_gpa FROM cgpa_summary WHERE user_id=? AND term_label!=?")) {
            ps.setInt(1, userId); ps.setString(2, newTermLabel); ResultSet rs = ps.executeQuery();
            while (rs.next()) all.add(rs.getDouble("term_gpa"));
        } catch (SQLException e) { e.printStackTrace(); }
        return ResultEngine.calcCGPA(all.stream().mapToDouble(Double::doubleValue).toArray());
    }

    private double[] getStudentCTMarks(Connection conn, int uid, String code) throws SQLException {
        double[] m = {-1,-1,-1,-1};
        PreparedStatement ps = conn.prepareStatement("SELECT ct_number,marks FROM ct_marks WHERE user_id=? AND course_code=?");
        ps.setInt(1,uid); ps.setString(2,code); ResultSet rs = ps.executeQuery();
        while (rs.next()) m[rs.getInt("ct_number")-1] = rs.getDouble("marks"); return m;
    }
    private double getAttendancePct(Connection conn, int uid, String code) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT total_held,total_attended FROM attendance WHERE user_id=? AND course_code=?");
        ps.setInt(1,uid); ps.setString(2,code); ResultSet rs = ps.executeQuery();
        if (rs.next()) { int h=rs.getInt("total_held"),a=rs.getInt("total_attended"); return h==0?0:(a*100.0/h); } return 0;
    }
    private double[] getTermFinalRaw(Connection conn, int uid, String code) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT marks,full_marks FROM term_final_marks WHERE user_id=? AND course_code=?");
        ps.setInt(1,uid); ps.setString(2,code); ResultSet rs = ps.executeQuery();
        if (rs.next()) return new double[]{rs.getDouble("marks"),rs.getDouble("full_marks")}; return new double[]{0,210};
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  9. RESET CREDENTIALS
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildResetCredentialsView() {
        VBox root = new VBox(16); root.setStyle("-fx-padding: 24;");
        Label msgLabel = new Label();
        TextField searchFld = formField("Search by name or username...");
        VBox resultList = new VBox(5);
        VBox resetPanel = new VBox(16);
        resetPanel.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);");
        searchFld.setOnKeyReleased(e -> {
            resultList.getChildren().clear(); String q = searchFld.getText().trim(); if (q.isEmpty()) return;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT id,first_name,last_name,username,role FROM users WHERE first_name LIKE ? OR username LIKE ?")) {
                ps.setString(1,"%"+q+"%"); ps.setString(2,"%"+q+"%"); ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int id=rs.getInt("id"); String name=rs.getString("first_name")+" "+rs.getString("last_name"); String uname=rs.getString("username");
                    Label l = new Label(name+"  |  "+uname+"  ["+rs.getString("role")+"]");
                    l.setStyle("-fx-cursor:hand; -fx-padding:6 10; -fx-background-color:white; -fx-background-radius:6; -fx-border-color:#e0e0e0;");
                    l.setOnMouseClicked(ev -> showResetPanel(id,name,uname,resetPanel,msgLabel)); resultList.getChildren().add(l);
                }
            } catch (SQLException ex) { ex.printStackTrace(); }
        });
        root.getChildren().addAll(buildSectionHeader("Reset Username / Password"), searchFld, resultList, resetPanel, msgLabel);
        return root;
    }

    private void showResetPanel(int userId, String name, String currentUsername, VBox panel, Label msgLabel) {
        panel.getChildren().clear();
        Label title = new Label("Editing: "+name); title.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#2c3e50;");
        Label curULabel = new Label("Current Username: "+currentUsername); curULabel.setStyle("-fx-font-size:12px; -fx-text-fill:#555;");
        TextField newUsernameFld = formField("New Username");
        Button saveUsernameBtn = styledBtn("Update Username","#3498db");
        saveUsernameBtn.setOnAction(e -> {
            String nu = newUsernameFld.getText().trim();
            if (nu.length()<4) { setMsg(msgLabel,"⚠ Username min 4 chars.",false); return; }
            exec("UPDATE users SET username=? WHERE id=?",nu,userId); curULabel.setText("Current Username: "+nu); newUsernameFld.clear(); setMsg(msgLabel,"✔ Username updated to: "+nu,true);
        });
        PasswordField newPassFld=new PasswordField(); newPassFld.setPromptText("New Password");
        PasswordField confirmPassFld=new PasswordField(); confirmPassFld.setPromptText("Confirm Password");
        styleField(newPassFld); styleField(confirmPassFld);
        Button savePassBtn = styledBtn("Reset Password","#e67e22");
        savePassBtn.setOnAction(e -> {
            if (newPassFld.getText().length()<6) { setMsg(msgLabel,"⚠ Password min 6 chars.",false); return; }
            if (!newPassFld.getText().equals(confirmPassFld.getText())) { setMsg(msgLabel,"⚠ Passwords don't match.",false); return; }
            exec("UPDATE users SET password=? WHERE id=?",PasswordUtil.hashPassword(newPassFld.getText()),userId);
            newPassFld.clear(); confirmPassFld.clear(); setMsg(msgLabel,"✔ Password reset for "+name,true);
        });
        panel.getChildren().addAll(title,new Separator(),buildSectionHeader("Update Username"),curULabel,
                new HBox(10,newUsernameFld,saveUsernameBtn),new Separator(),
                buildSectionHeader("Reset Password"),newPassFld,confirmPassFld,savePassBtn);
    }

    // ── DB helpers ────────────────────────────────────────────────────────────
    private List<Student> getPendingStudents() {
        List<Student> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM users WHERE role='STUDENT' AND status='PENDING'")) {
            while (rs.next()) { Student s=new Student(); s.setId(rs.getInt("id")); s.setFirstName(rs.getString("first_name")); s.setLastName(rs.getString("last_name")); s.setUsername(rs.getString("username")); s.setDepartment(rs.getString("department")); s.setPhone(rs.getString("phone")); list.add(s); }
        } catch (SQLException e) { e.printStackTrace(); } return list;
    }
    private void updateStatus(int id,String status,String table) { exec("UPDATE "+table+" SET status=? WHERE id=?",status,id); }
    private void removeTeacher(int id)  { exec("UPDATE users SET status='REMOVED' WHERE id=?",id); }
    private void deleteCourse(int id)   { exec("DELETE FROM courses WHERE id=?",id); }
    private void unassignCourse(int id) { exec("UPDATE courses SET faculty_id=NULL WHERE id=?",id); }
    private void deleteNotice(int id)   { exec("DELETE FROM notices WHERE id=?",id); }

    private boolean insertTeacher(String fn,String ln,String email,String phone,String dept,String username,String pw) {
        try (Connection conn=DatabaseConnection.getConnection(); PreparedStatement ps=conn.prepareStatement(
                "INSERT INTO users (first_name,last_name,email,phone,department,username,password,role,status) VALUES (?,?,?,?,?,?,?,'FACULTY','APPROVED')")) {
            ps.setString(1,fn);ps.setString(2,ln);ps.setString(3,email);ps.setString(4,phone);ps.setString(5,dept);ps.setString(6,username);ps.setString(7,PasswordUtil.hashPassword(pw));
            return ps.executeUpdate()>0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
    private boolean insertCourse(String c,String n,double cr,int l,int t) {
        try (Connection conn=DatabaseConnection.getConnection(); PreparedStatement ps=conn.prepareStatement(
                "INSERT INTO courses (code,name,credit,level,term) VALUES (?,?,?,?,?)")) {
            ps.setString(1,c);ps.setString(2,n);ps.setDouble(3,cr);ps.setInt(4,l);ps.setInt(5,t); return ps.executeUpdate()>0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
    private boolean assignCourseToTeacher(String cCode,int fId,String fName) {
        try (Connection conn=DatabaseConnection.getConnection(); PreparedStatement ps=conn.prepareStatement("UPDATE courses SET faculty_id=?,faculty=? WHERE code=?")) {
            ps.setInt(1,fId);ps.setString(2,fName);ps.setString(3,cCode); return ps.executeUpdate()>0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
    private boolean insertNotice(String t,String b,String a) {
        try (Connection conn=DatabaseConnection.getConnection(); PreparedStatement ps=conn.prepareStatement("INSERT INTO notices (title,body,audience,created_at) VALUES (?,?,?,NOW())")) {
            ps.setString(1,t);ps.setString(2,b);ps.setString(3,a); return ps.executeUpdate()>0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
    private void exec(String sql,Object... params) {
        try (Connection conn=DatabaseConnection.getConnection(); PreparedStatement ps=conn.prepareStatement(sql)) {
            for (int i=0;i<params.length;i++) ps.setObject(i+1,params[i]); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────
    private HBox buildRowHeader(String... cols) {
        HBox h=new HBox(); h.setStyle("-fx-padding:8 14; -fx-background-color:#f0f0f0; -fx-background-radius:6;"); h.setAlignment(Pos.CENTER_LEFT);
        int[] widths={50,150,120,180,100,100};
        for (int i=0;i<cols.length;i++) { Label l=new Label(cols[i]); l.setPrefWidth(i<widths.length?widths[i]:100); l.setStyle("-fx-font-weight:bold; -fx-font-size:12px; -fx-text-fill:#555;"); h.getChildren().add(l); }
        return h;
    }
    private HBox dataRow() {
        HBox r=new HBox(10); r.setAlignment(Pos.CENTER_LEFT);
        r.setStyle("-fx-padding:10 14; -fx-background-color:white; -fx-background-radius:8; -fx-border-color:#e0e0e0; -fx-border-radius:8;"); return r;
    }
    private Label colLabel(String t,double w) { Label l=new Label(t!=null?t:"—"); l.setPrefWidth(w); l.setStyle("-fx-font-size:12px; -fx-text-fill:#2c3e50;"); return l; }
    private Label cl(String t,double w)       { Label l=new Label(t!=null?t:"—"); l.setPrefWidth(w); l.setStyle("-fx-font-size:11px; -fx-text-fill:#2c3e50;"); return l; }
    private Label buildSectionHeader(String t){ Label l=new Label(t); l.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#2c3e50;"); return l; }
    private Button styledBtn(String text,String color){ Button b=new Button(text); b.setStyle("-fx-background-color:"+color+"; -fx-text-fill:white; -fx-background-radius:6; -fx-cursor:hand; -fx-padding:6 14; -fx-font-size:12px;"); return b; }
    private TextField formField(String prompt){ TextField t=new TextField(); t.setPromptText(prompt); t.setStyle("-fx-pref-height:34px; -fx-padding:0 8; -fx-background-radius:6; -fx-border-radius:6; -fx-border-color:#ccc; -fx-border-width:1;"); return t; }
    private void styleField(Control c){ c.setStyle("-fx-pref-height:34px; -fx-padding:0 8; -fx-background-radius:6; -fx-border-radius:6; -fx-border-color:#ccc; -fx-border-width:1;"); }
    private void styleCombo(ComboBox<?> b){ b.setStyle("-fx-pref-height:34px; -fx-background-radius:6; -fx-border-radius:6; -fx-border-color:#ccc; -fx-font-size:13px;"); }
    private HBox labeledField(String labelText,Control field){ Label lbl=new Label(labelText); lbl.setPrefWidth(110); lbl.setStyle("-fx-font-size:12px; -fx-text-fill:#555; -fx-font-weight:bold;"); HBox.setHgrow(field,Priority.ALWAYS); HBox box=new HBox(10,lbl,field); box.setAlignment(Pos.CENTER_LEFT); return box; }
    private ScrollPane scrollWrap(VBox content){ ScrollPane sp=new ScrollPane(content); sp.setFitToWidth(true); sp.setStyle("-fx-background:transparent; -fx-background-color:transparent;"); VBox.setVgrow(sp,Priority.ALWAYS); return sp; }
    private Label greenLabel(String text){ Label l=new Label(text); l.setStyle("-fx-text-fill:#27ae60; -fx-font-size:13px;"); return l; }
    private void setMsg(Label l,String m,boolean success){ l.setText(m); l.setStyle("-fx-text-fill:"+(success?"#27ae60":"#e74c3c")+"; -fx-font-size:12px;"); }
    private void setActive(Button btn){ if(activeBtn!=null) activeBtn.setStyle(INACTIVE_STYLE); btn.setStyle(ACTIVE_STYLE); activeBtn=btn; }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clear();
        try { Parent root=FXMLLoader.load(getClass().getResource("/frontend/view/login.fxml")); Stage stage=(Stage)((Node)event.getSource()).getScene().getWindow(); stage.setScene(new Scene(root)); stage.show(); } catch (Exception e) { e.printStackTrace(); }
    }
}
