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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.util.List;
import java.util.ResourceBundle;

public class StudentDashboardController implements Initializable {

    @FXML private VBox mainContentArea;

    // ── Sidebar buttons — FIX: btnExamMarks removed ───────────────────────────
    @FXML private Button btnHome;
    @FXML private Button btnProfile;
    @FXML private Button btnCourse;
    @FXML private Button btnResult;
    @FXML private Button btnAttendance;
    @FXML private Button btnNotices;
    @FXML private Button btnFee;
    @FXML private Button btnLogout;

    @FXML private Label pageTitle;
    @FXML private Label sessionLabel;
    @FXML private Label studentNameLabel;
    @FXML private Label studentIdLabel;
    @FXML private Label departmentLabel;

    @FXML private Label overallAttendanceLabel;
    @FXML private Label noticeCountLabel;
    @FXML private Label courseCountLabel;

    @FXML private ComboBox<String> courseDropdown;
    @FXML private Label classesHeldLabel;
    @FXML private Label classesAttendedLabel;
    @FXML private Label courseAttendancePctLabel;
    @FXML private Label attendanceStatusLabel;

    @FXML private VBox courseListContainer;
    @FXML private VBox homeContent;

    private Button  activeBtn;
    private Student student;

    private static final String ACTIVE_STYLE =
            "-fx-background-color: #2a9d8f; -fx-text-fill: white; -fx-font-size: 13px;" +
            "-fx-alignment: CENTER-LEFT; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand;";
    private static final String INACTIVE_STYLE =
            "-fx-background-color: transparent; -fx-text-fill: #cdd9e0; -fx-font-size: 13px;" +
            "-fx-alignment: CENTER-LEFT; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        student   = SessionManager.getStudent();
        activeBtn = btnHome;

        // ── FIX: btnExamMarks removed from array ──────────────────────────────
        Button[] navBtns = {btnHome, btnProfile, btnCourse,
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

    private void loadStudentInfo() {
        if (student == null) return;
        studentNameLabel.setText(student.getFirstName() + " " + student.getLastName());
        studentIdLabel.setText("ID: " + student.getId());
        departmentLabel.setText(student.getDepartment() != null ? student.getDepartment() : "—");
        sessionLabel.setText("Session: " + (student.getSession() != null ? student.getSession() : "—"));
    }

    private void loadSummaryCards() {
        if (student == null) return;
        AttendanceDAO aDao = new AttendanceDAO();
        double overall = aDao.getOverallAttendance(student.getId());
        overallAttendanceLabel.setText(overall + "%");
        overallAttendanceLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: "
                + (overall >= 75 ? "#27ae60" : "#e74c3c") + ";");

        // ── Unread notice count — notices not yet read by this student ──────────
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM notices n " +
                     "WHERE (n.audience='All' OR n.audience='Students Only') " +
                     "AND n.id NOT IN (SELECT notice_id FROM notice_reads WHERE user_id=?)")) {
            ps.setInt(1, student.getId());
            ResultSet rs = ps.executeQuery();
            int unread = rs.next() ? rs.getInt(1) : 0;
            noticeCountLabel.setText(String.valueOf(unread));
            noticeCountLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: "
                    + (unread > 0 ? "#e67e22" : "#27ae60") + ";");
        } catch (Exception e) { noticeCountLabel.setText("0"); }

        CourseRegistrationDAO crDao = new CourseRegistrationDAO();
        int level = student.getLevel() > 0 ? student.getLevel() : 1;
        int term  = student.getTerm()  > 0 ? student.getTerm()  : 1;
        courseCountLabel.setText(String.valueOf(
                crDao.getApprovedCourseCodes(student.getId(), level, term).size()));
    }

    private void loadCourseDropdown() {
        if (student == null) return;
        CourseRegistrationDAO crDao = new CourseRegistrationDAO();
        int level = student.getLevel() > 0 ? student.getLevel() : 1;
        int term  = student.getTerm()  > 0 ? student.getTerm()  : 1;
        List<String[]> courses = crDao.getApprovedCourseDetails(student.getId(), level, term);
        courseDropdown.getItems().clear();
        if (courses.isEmpty()) { courseDropdown.setPromptText("No approved courses"); }
        else { for (String[] c : courses) courseDropdown.getItems().add(c[0] + " - " + c[1]); }
        classesHeldLabel.setText("—"); classesAttendedLabel.setText("—");
        courseAttendancePctLabel.setText("—"); attendanceStatusLabel.setText("—");
    }

    @FXML
    private void loadCourseAttendance(ActionEvent event) {
        String selected = courseDropdown.getValue(); if (selected == null) return;
        String courseCode = selected.split(" - ")[0].trim();
        AttendanceDAO aDao = new AttendanceDAO();
        List<AttendanceRecord> records = aDao.getAttendanceSummary(student.getId());
        AttendanceRecord found = null;
        for (AttendanceRecord r : records) if (r.courseCode.equals(courseCode)) { found = r; break; }
        if (found == null) {
            classesHeldLabel.setText("0"); classesAttendedLabel.setText("0");
            courseAttendancePctLabel.setText("0%"); attendanceStatusLabel.setText("No data");
            attendanceStatusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #aaa;"); return;
        }
        classesHeldLabel.setText(String.valueOf(found.totalHeld));
        classesAttendedLabel.setText(String.valueOf(found.totalAttended));
        courseAttendancePctLabel.setText(found.percentage + "%");
        attendanceStatusLabel.setText(found.percentage >= 75 ? "✔ Regular" : "✘ Irregular");
        attendanceStatusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                + (found.percentage >= 75 ? "#27ae60" : "#e74c3c") + ";");
    }

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
            courseListContainer.getChildren().add(empty); return;
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
    @FXML private void showHome(ActionEvent e) {
        setActive(btnHome); pageTitle.setText("Home");
        loadSummaryCards(); loadCourseDropdown(); loadCourseList();
        mainContentArea.getChildren().setAll(homeContent);
    }
    @FXML private void showProfile(ActionEvent e) {
        setActive(btnProfile); pageTitle.setText("My Profile");
        mainContentArea.getChildren().setAll(buildProfileView());
    }
    @FXML private void showCourseRegistration(ActionEvent e) {
        setActive(btnCourse); pageTitle.setText("Course Registration");
        try {
            VBox content = FXMLLoader.load(getClass().getResource("/frontend/view/CourseRegistration.fxml"));
            mainContentArea.getChildren().setAll(content);
        } catch (Exception ex) { ex.printStackTrace(); }
    }
    @FXML private void showResult(ActionEvent e) {
        setActive(btnResult); pageTitle.setText("Result");
        mainContentArea.getChildren().setAll(buildResultView());
    }
    @FXML private void showAttendance(ActionEvent e) {
        setActive(btnAttendance); pageTitle.setText("Attendance");
        try {
            VBox content = FXMLLoader.load(getClass().getResource("/frontend/view/Attendance.fxml"));
            mainContentArea.getChildren().setAll(content);
        } catch (Exception ex) { ex.printStackTrace(); }
    }
    @FXML private void showNotices(ActionEvent e) {
        setActive(btnNotices); pageTitle.setText("Notices");
        mainContentArea.getChildren().setAll(buildNoticesView());
    }
    @FXML private void showFeePayment(ActionEvent e) {
        setActive(btnFee); pageTitle.setText("Fee Payment");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  NOTICES VIEW — read/unread tracking সহ
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildNoticesView() {
        VBox root = new VBox(16); root.setStyle("-fx-padding: 24;");

        // header row: title + filter buttons
        Button btnAll    = filterTabBtn("All",    true);
        Button btnUnread = filterTabBtn("Unread", false);
        Button btnRead   = filterTabBtn("Read",   false);
        HBox filterRow = new HBox(8, sectionHeader("📢  Notices"),
                new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }},
                btnAll, btnUnread, btnRead);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().add(filterRow);

        VBox noticeList = new VBox(12);
        ScrollPane sp = new ScrollPane(noticeList);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(sp, Priority.ALWAYS);
        root.getChildren().add(sp);

        // filter: "all" | "unread" | "read"
        final String[] currentFilter = {"all"};

        // Runnable[] wrapper allows self-reference inside the lambda
        final Runnable[] reloadHolder = {null};
        reloadHolder[0] = () -> {
            noticeList.getChildren().clear();
            String filter = currentFilter[0];

            String sql;
            if ("unread".equals(filter)) {
                sql = "SELECT *, 0 AS is_read FROM notices " +
                      "WHERE (audience='All' OR audience='Students Only') " +
                      "AND id NOT IN (SELECT notice_id FROM notice_reads WHERE user_id=" + student.getId() + ") " +
                      "ORDER BY created_at DESC";
            } else if ("read".equals(filter)) {
                sql = "SELECT *, 1 AS is_read FROM notices " +
                      "WHERE (audience='All' OR audience='Students Only') " +
                      "AND id IN (SELECT notice_id FROM notice_reads WHERE user_id=" + student.getId() + ") " +
                      "ORDER BY created_at DESC";
            } else {
                // all — is_read computed via LEFT JOIN
                sql = "SELECT n.*, " +
                      "CASE WHEN nr.notice_id IS NOT NULL THEN 1 ELSE 0 END AS is_read " +
                      "FROM notices n " +
                      "LEFT JOIN notice_reads nr ON n.id = nr.notice_id AND nr.user_id=" + student.getId() + " " +
                      "WHERE (n.audience='All' OR n.audience='Students Only') " +
                      "ORDER BY is_read ASC, n.created_at DESC";
            }

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps2 = conn.prepareStatement(sql);
                 ResultSet rs = ps2.executeQuery()) {

                boolean any = false;
                while (rs.next()) {
                    any = true;
                    final int    noticeId = rs.getInt("id");
                    final boolean isRead  = rs.getInt("is_read") == 1;
                    String audience       = rs.getString("audience");
                    String dateStr        = rs.getString("created_at");

                    // ── Card ──────────────────────────────────────────────────
                    VBox card = new VBox(10);
                    String cardBg = isRead ? "#fafafa" : "white";
                    String cardBorder = isRead ? "#e0e0e0" : "#2a9d8f";
                    card.setStyle(
                            "-fx-background-color: " + cardBg + ";" +
                            "-fx-background-radius: 10;" +
                            "-fx-border-color: " + cardBorder + ";" +
                            "-fx-border-width: 0 0 0 4;" +   // left accent border
                            "-fx-border-radius: 0 10 10 0;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);" +
                            "-fx-padding: 14 16;");

                    // ── Top row ───────────────────────────────────────────────
                    HBox top = new HBox(8); top.setAlignment(Pos.CENTER_LEFT);

                    // unread dot
                    if (!isRead) {
                        Label dot = new Label("●");
                        dot.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 10px;");
                        top.getChildren().add(dot);
                    }

                    Label titleLbl = new Label(rs.getString("title"));
                    titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                            + (isRead ? "#7f8c8d" : "#2c3e50") + ";");

                    String audienceBadgeColor = "All".equals(audience) ? "#2a9d8f" : "#3498db";
                    Label audienceLbl = new Label(audience);
                    audienceLbl.setStyle("-fx-background-color: " + audienceBadgeColor + "; -fx-text-fill: white;" +
                                        "-fx-font-size: 10px; -fx-padding: 2 8; -fx-background-radius: 10;");

                    // read/unread status badge
                    Label statusBadge = new Label(isRead ? "✓ Read" : "● Unread");
                    statusBadge.setStyle("-fx-font-size: 10px; -fx-padding: 2 8; -fx-background-radius: 10;" +
                            "-fx-background-color: " + (isRead ? "#eafaf1" : "#fef9e7") + ";" +
                            "-fx-text-fill: " + (isRead ? "#27ae60" : "#e67e22") + ";");

                    Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
                    Label dateLbl = new Label(dateStr != null && dateStr.length() >= 16 ? dateStr.substring(0, 16) : "");
                    dateLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #aaa;");

                    top.getChildren().addAll(titleLbl, audienceLbl, statusBadge, spacer, dateLbl);

                    // ── Body ─────────────────────────────────────────────────
                    Label bodyLbl = new Label(rs.getString("body"));
                    bodyLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (isRead ? "#aaa" : "#555") + ";");
                    bodyLbl.setWrapText(true);

                    // ── Mark as Read button (only for unread) ─────────────────
                    card.getChildren().addAll(top, bodyLbl);

                    if (!isRead) {
                        Button markReadBtn = new Button("Mark as Read");
                        markReadBtn.setStyle(
                                "-fx-background-color: transparent; -fx-text-fill: #2a9d8f;" +
                                "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2 0;" +
                                "-fx-border-color: transparent; -fx-underline: true;");
                        markReadBtn.setOnAction(ev -> {
                            markNoticeRead(noticeId, student.getId());
                            // update home card count and reload notice list
                            updateNoticeCardCount();
                            reloadHolder[0].run();
                        });
                        card.getChildren().add(markReadBtn);
                    }

                    noticeList.getChildren().add(card);
                }

                if (!any) {
                    String msg = "unread".equals(filter) ? "🎉  No unread notices!"
                               : "read".equals(filter)   ? "No read notices yet."
                               : "No notices at the moment.";
                    Label empty = new Label(msg);
                    empty.setStyle("-fx-font-size: 13px; -fx-text-fill: #aaa; -fx-padding: 20 0;");
                    noticeList.getChildren().add(empty);
                }

            } catch (SQLException ex) { ex.printStackTrace(); }

            // update filter button styles
            setFilterActive(btnAll,    "all".equals(currentFilter[0]));
            setFilterActive(btnUnread, "unread".equals(currentFilter[0]));
            setFilterActive(btnRead,   "read".equals(currentFilter[0]));
        };

        btnAll.setOnAction(ev    -> { currentFilter[0] = "all";    reloadHolder[0].run(); });
        btnUnread.setOnAction(ev -> { currentFilter[0] = "unread"; reloadHolder[0].run(); });
        btnRead.setOnAction(ev   -> { currentFilter[0] = "read";   reloadHolder[0].run(); });

        reloadHolder[0].run();
        return root;
    }

    /** Insert into notice_reads if not already there */
    private void markNoticeRead(int noticeId, int userId) {
        String sql = "INSERT IGNORE INTO notice_reads (notice_id, user_id, read_at) VALUES (?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noticeId); ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Refresh the home-page unread notice count label without full page reload */
    private void updateNoticeCardCount() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM notices n " +
                     "WHERE (n.audience='All' OR n.audience='Students Only') " +
                     "AND n.id NOT IN (SELECT notice_id FROM notice_reads WHERE user_id=?)")) {
            ps.setInt(1, student.getId());
            ResultSet rs = ps.executeQuery();
            int unread = rs.next() ? rs.getInt(1) : 0;
            noticeCountLabel.setText(String.valueOf(unread));
            noticeCountLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: "
                    + (unread > 0 ? "#e67e22" : "#27ae60") + ";");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private Button filterTabBtn(String text, boolean active) {
        Button b = new Button(text);
        setFilterActive(b, active);
        return b;
    }

    private void setFilterActive(Button b, boolean active) {
        b.setStyle(active
                ? "-fx-background-color: #2a9d8f; -fx-text-fill: white; -fx-font-size: 11px;" +
                  "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 4 14;"
                : "-fx-background-color: #f0f0f0; -fx-text-fill: #555; -fx-font-size: 11px;" +
                  "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 4 14;");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PROFILE VIEW
    // ══════════════════════════════════════════════════════════════════════════
    private ScrollPane buildProfileView() {
        VBox root = new VBox(20); root.setStyle("-fx-padding: 24;");
        VBox infoCard = card(); infoCard.getChildren().add(sectionHeader("👤  Profile Information"));
        String[] d = getFullStudentInfo(student.getId());
        String[][] rows = {{"Full Name",d[0]+" "+d[1]},{"Username",d[2]},{"Email",d[3].isBlank()?"—":d[3]},
                {"Phone",d[4].isBlank()?"—":d[4]},{"Department",d[5].isBlank()?"—":d[5]},{"Role","Student"},{"Status",d[6]}};
        for (String[] row : rows) {
            HBox r=new HBox(12); r.setAlignment(Pos.CENTER_LEFT);
            Label key=new Label(row[0]+":"); key.setPrefWidth(120); key.setStyle("-fx-font-weight:bold; -fx-text-fill:#555; -fx-font-size:13px;");
            Label val=new Label(row[1]); val.setStyle("-fx-text-fill:#2c3e50; -fx-font-size:13px;");
            r.getChildren().addAll(key,val); infoCard.getChildren().add(r);
        }
        Button editBtn=styledBtn("✏  Edit Info","#2a9d8f"); Button resetBtn=styledBtn("🔒  Reset Password","#e67e22");
        editBtn.setPrefWidth(160); resetBtn.setPrefWidth(160);
        HBox btnRow=new HBox(14,editBtn,resetBtn); btnRow.setAlignment(Pos.CENTER_LEFT);
        VBox subView=new VBox(0);
        editBtn.setOnAction(ev -> { subView.getChildren().setAll(buildEditInfoCard()); subView.setVisible(true); subView.setManaged(true); });
        resetBtn.setOnAction(ev -> { subView.getChildren().setAll(buildResetPasswordCard()); subView.setVisible(true); subView.setManaged(true); });
        infoCard.getChildren().addAll(new Separator(),btnRow);
        root.getChildren().addAll(infoCard,subView);
        ScrollPane sp=new ScrollPane(root); sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent; -fx-background-color:transparent;"); VBox.setVgrow(sp,Priority.ALWAYS);
        return sp;
    }

    private VBox buildEditInfoCard() {
        VBox card=card(); card.getChildren().add(sectionHeader("✏️  Edit Contact Info"));
        Label note=new Label("ℹ  Name and username can only be changed by Admin."); note.setStyle("-fx-font-size:11px; -fx-text-fill:#7f8c8d;");
        Label msg=new Label();
        String[] d=getFullStudentInfo(student.getId());
        TextField emailFld=formField("Email address"); emailFld.setText(d[3]);
        TextField phoneFld=formField("Phone number");  phoneFld.setText(d[4]);
        Button saveBtn=styledBtn("Save Changes","#2a9d8f");
        saveBtn.setOnAction(ev -> {
            String email=emailFld.getText().trim(), phone=phoneFld.getText().trim();
            if (email.isBlank()) { setMsg(msg,"⚠ Email cannot be empty.",false); return; }
            if (!email.contains("@")) { setMsg(msg,"⚠ Enter a valid email.",false); return; }
            try (Connection conn=DatabaseConnection.getConnection(); PreparedStatement ps=conn.prepareStatement("UPDATE users SET email=?,phone=? WHERE id=?")) {
                ps.setString(1,email); ps.setString(2,phone); ps.setInt(3,student.getId());
                ps.executeUpdate(); setMsg(msg,"✔ Contact info updated.",true);
            } catch (SQLException ex) { setMsg(msg,"⚠ Update failed.",false); }
        });
        card.getChildren().addAll(note,labeledField("Email *",emailFld),labeledField("Phone",phoneFld),saveBtn,msg);
        return card;
    }

    private VBox buildResetPasswordCard() {
        VBox card=card(); card.getChildren().add(sectionHeader("🔒  Reset Password")); Label msg=new Label();
        PasswordField currentFld=new PasswordField(); currentFld.setPromptText("Current password");
        PasswordField newFld=new PasswordField(); newFld.setPromptText("New password (min 6 chars)");
        PasswordField confirmFld=new PasswordField(); confirmFld.setPromptText("Re-enter new password");
        styleField(currentFld); styleField(newFld); styleField(confirmFld);
        Button changeBtn=styledBtn("Change Password","#e67e22");
        changeBtn.setOnAction(ev -> {
            String cur=currentFld.getText(), nw=newFld.getText(), conf=confirmFld.getText();
            if (cur.isBlank()) { setMsg(msg,"⚠ Enter current password.",false); return; }
            if (nw.length()<6) { setMsg(msg,"⚠ Min 6 characters.",false); return; }
            if (!nw.equals(conf)) { setMsg(msg,"⚠ Passwords don't match.",false); return; }
            try (Connection conn=DatabaseConnection.getConnection(); PreparedStatement ps=conn.prepareStatement("SELECT password FROM users WHERE id=?")) {
                ps.setInt(1,student.getId()); ResultSet rs=ps.executeQuery();
                if (!rs.next()||!PasswordUtil.checkPassword(cur,rs.getString("password"))) { setMsg(msg,"⚠ Incorrect current password.",false); return; }
            } catch (SQLException ex) { setMsg(msg,"⚠ Error.",false); return; }
            try (Connection conn=DatabaseConnection.getConnection(); PreparedStatement ps=conn.prepareStatement("UPDATE users SET password=? WHERE id=?")) {
                ps.setString(1,PasswordUtil.hashPassword(nw)); ps.setInt(2,student.getId());
                ps.executeUpdate(); currentFld.clear(); newFld.clear(); confirmFld.clear(); setMsg(msg,"✔ Password changed.",true);
            } catch (SQLException ex) { setMsg(msg,"⚠ Update failed.",false); }
        });
        card.getChildren().addAll(labeledField("Current Password *",currentFld),labeledField("New Password *",newFld),labeledField("Confirm *",confirmFld),changeBtn,msg);
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RESULT VIEW
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildResultView() {
        VBox root=new VBox(0); root.setStyle("-fx-padding:24 24 0 24;"); VBox.setVgrow(root,Priority.ALWAYS);
        Button btnCT=tabBtn("📝  Class Test Results",true); Button btnFinal=tabBtn("📊  Semester Final Results",false);
        HBox tabBar=new HBox(btnCT,btnFinal); tabBar.setStyle("-fx-background-color:#e8f4f8; -fx-background-radius:10 10 0 0;");
        ScrollPane contentScroll=new ScrollPane(); contentScroll.setFitToWidth(true);
        contentScroll.setStyle("-fx-background:white; -fx-background-color:white; -fx-background-radius:0 0 12 12; -fx-border-color:#e0e0e0; -fx-border-width:0 1 1 1;");
        VBox.setVgrow(contentScroll,Priority.ALWAYS);
        contentScroll.setContent(buildCTResultsPane());
        btnCT.setOnAction(e -> { setTabActive(btnCT,btnFinal); contentScroll.setContent(buildCTResultsPane()); });
        btnFinal.setOnAction(e -> { setTabActive(btnFinal,btnCT); contentScroll.setContent(buildFinalResultsPane()); });
        root.getChildren().addAll(tabBar,contentScroll);
        return root;
    }

    // ── CT Results — FIX: getEnrolledCourses uses course_registrations directly ──
    private VBox buildCTResultsPane() {
        VBox pane=new VBox(14); pane.setStyle("-fx-padding:22;");
        Label title=sectionHeader("Class Test Results  (CT1 – CT4)");
        Label note=new Label("ℹ  Best 3 of 4 CT marks (each out of 20) are counted. CT contribution = 60 marks.");
        note.setStyle("-fx-font-size:11px; -fx-text-fill:#7f8c8d;");
        VBox tableArea=new VBox(8);
        HBox header=tableHeader("Course","CT1","CT2","CT3","CT4","Best 3","CT /60");
        int[] hw={230,60,60,60,60,80,80}; applyHeaderWidths(header,hw); tableArea.getChildren().add(header);

        // ── FIX: use getEnrolledCourses() which queries course_registrations ──
        List<String[]> courses=getEnrolledCourses();
        if (courses.isEmpty()) {
            tableArea.getChildren().add(infoLbl("No enrolled courses found. Make sure your courses are approved."));
        } else {
            MarksDAO mDao=new MarksDAO(); boolean any=false;
            for (String[] c : courses) {
                double[] ct=mDao.getStudentCTMarks(student.getId(),c[0]);
                boolean hasData=false; for (double m : ct) if (m>=0) { hasData=true; break; }
                if (!hasData) continue; any=true;
                double best3=MarksDAO.getBest3CTSum(ct); double ctMark=ResultEngine.calcCTMark(best3);
                HBox row=dataRow(); row.getChildren().add(cl(c[0]+" — "+c[1],230));
                for (double m : ct) row.getChildren().add(cl(m>=0?fmt1(m):"—",60));
                row.getChildren().add(cl(fmt1(best3),80));
                Label ctLbl=new Label(fmt1(ctMark)); ctLbl.setPrefWidth(80); ctLbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#2a9d8f;");
                row.getChildren().add(ctLbl); tableArea.getChildren().add(row);
            }
            if (!any) tableArea.getChildren().add(infoLbl("CT marks have not been entered by faculty yet."));
        }
        pane.getChildren().addAll(title,note,tableArea); return pane;
    }

    private VBox buildFinalResultsPane() {
        VBox pane = new VBox(16); pane.setStyle("-fx-padding:22;");

        Label title = sectionHeader("Semester Final Results");
        Label note  = new Label("ℹ  Results are visible only after Admin publishes them.  " +
                                "Total = CT(60) + Attendance(30) + Term Final(210) = 300.");
        note.setStyle("-fx-font-size:11px; -fx-text-fill:#7f8c8d;"); note.setWrapText(true);

        // ── Course result table ───────────────────────────────────────────────
        VBox tableArea = new VBox(8);
        HBox header = tableHeader("Course", "CT /60", "Attend /30", "Final /210", "Total /300", "Grade", "GP");
        int[] hw = {210, 70, 95, 100, 100, 70, 55};
        applyHeaderWidths(header, hw);
        tableArea.getChildren().add(header);

        List<String[]> results = getPublishedResults();
        // results: [code, name, ct_best3, attendance_mark, term_final_mark, total_marks, letter_grade, grade_point]

        if (results.isEmpty()) {
            tableArea.getChildren().add(infoLbl("No published results yet. Please check back after the admin publishes them."));
            pane.getChildren().addAll(title, note, tableArea);
            return pane;
        }

        for (String[] r : results) {
            HBox row = dataRow();
            Label gradeLabel = new Label(r[6]); gradeLabel.setPrefWidth(70);
            gradeLabel.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:" + gradeColor(r[6]) + ";");
            row.getChildren().addAll(cl(r[0] + " — " + r[1], 210), cl(r[2], 70), cl(r[3], 95),
                    cl(r[4], 100), cl(r[5], 100), gradeLabel, cl(r[7], 55));
            tableArea.getChildren().add(row);
        }

        pane.getChildren().addAll(title, note, tableArea);

        // ══════════════════════════════════════════════════════════════════════
        //  GPA / CGPA SUMMARY BLOCK
        //  — results table থেকে সরাসরি calculate করা হয়,
        //    cgpa_summary table optional (থাকলে term history দেখায়)
        // ══════════════════════════════════════════════════════════════════════

        // Step 1: results list থেকেই current term GPA calculate করো
        //         (credit-weighted average of grade_points in this result set)
        double termGPA;
        try {
            // results table-এ credit column থাকলে weighted, না থাকলে simple average
            double[] gpArr = results.stream()
                .mapToDouble(r -> { try { return Double.parseDouble(r[7]); } catch (Exception e) { return 0; } })
                .toArray();
            double[] credArr = getCreditsForResults(results); // credit per course
            termGPA = ResultEngine.calcTermGPA(gpArr, credArr);
        } catch (Exception e) {
            termGPA = results.stream()
                .mapToDouble(r -> { try { return Double.parseDouble(r[7]); } catch (Exception ex) { return 0; } })
                .average().orElse(0);
            termGPA = ResultEngine.round2(termGPA);
        }

        // Step 2: cgpa_summary থেকে term history নাও (থাকলে), না থাকলে শুধু current term দেখাও
        java.util.List<String[]> termHistory = new java.util.ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT term_label, term_gpa, cgpa FROM cgpa_summary " +
                     "WHERE user_id = ? ORDER BY id ASC")) {
            ps.setInt(1, student.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                termHistory.add(new String[]{
                    rs.getString("term_label"),
                    fmt2(rs.getDouble("term_gpa")),
                    fmt2(rs.getDouble("cgpa"))
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }

        // cgpa_summary থেকে পেলে সেটা ব্যবহার করো, না পেলে current term থেকেই calculate
        double cgpa;
        String currentTermLabel;
        if (!termHistory.isEmpty()) {
            String[] latest = termHistory.get(termHistory.size() - 1);
            cgpa = Double.parseDouble(latest[2]);
            currentTermLabel = latest[0];
            // termGPA-ও latest থেকে নাও (admin-calculated value বেশি accurate)
            termGPA = Double.parseDouble(latest[1]);
        } else {
            // fallback: cgpa = termGPA (only one term of data)
            cgpa = termGPA;
            currentTermLabel = "Current Term";
        }

        // ── Summary Card ──────────────────────────────────────────────────────
        VBox summaryCard = new VBox(0);
        summaryCard.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.09), 12, 0, 3, 0);" +
                "-fx-border-color: #d0eaf5;" +
                "-fx-border-radius: 14; -fx-border-width: 1;");

        // Card header
        HBox cardHeader = new HBox();
        cardHeader.setStyle("-fx-background-color: #2a9d8f; -fx-background-radius: 14 14 0 0; -fx-padding: 13 22;");
        Label cardTitle = new Label("📊  Academic Summary");
        cardTitle.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:white;");
        cardHeader.getChildren().add(cardTitle);
        summaryCard.getChildren().add(cardHeader);

        // ── Content row: Term GPA | divider | CGPA | divider | History ─────────
        HBox contentRow = new HBox(0);

        // LEFT — Term GPA
        VBox termBox = new VBox(6);
        termBox.setAlignment(Pos.CENTER_LEFT);
        termBox.setStyle("-fx-padding: 24 28; -fx-min-width: 220;");
        Label termCap    = new Label("THIS TERM");
        termCap.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#7f8c8d;");
        Label termLblLbl = new Label(currentTermLabel);
        termLblLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#555; -fx-padding: 0 0 4 0;");
        Label termGpaNum = new Label(fmt2(termGPA));
        termGpaNum.setStyle("-fx-font-size:42px; -fx-font-weight:bold; -fx-text-fill:" + gpaColor(termGPA) + ";");
        Label termGpaOf  = new Label("Term GPA  /  4.00");
        termGpaOf.setStyle("-fx-font-size:11px; -fx-text-fill:#aaa;");
        termBox.getChildren().addAll(termCap, termLblLbl, termGpaNum, termGpaOf);

        Region div1 = new Region();
        div1.setStyle("-fx-background-color:#e8e8e8; -fx-min-width:1; -fx-max-width:1;");

        // MIDDLE — CGPA
        VBox cgpaBox = new VBox(6);
        cgpaBox.setAlignment(Pos.CENTER_LEFT);
        cgpaBox.setStyle("-fx-padding: 24 28; -fx-min-width: 220;");
        Label cgpaCap    = new Label("CUMULATIVE");
        cgpaCap.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#7f8c8d;");
        Label cgpaSpan   = new Label("All terms combined");
        cgpaSpan.setStyle("-fx-font-size:12px; -fx-text-fill:#555; -fx-padding: 0 0 4 0;");
        Label cgpaNum    = new Label(fmt2(cgpa));
        cgpaNum.setStyle("-fx-font-size:42px; -fx-font-weight:bold; -fx-text-fill:" + gpaColor(cgpa) + ";");
        Label cgpaOf     = new Label("CGPA  /  4.00");
        cgpaOf.setStyle("-fx-font-size:11px; -fx-text-fill:#aaa;");
        cgpaBox.getChildren().addAll(cgpaCap, cgpaSpan, cgpaNum, cgpaOf);

        contentRow.getChildren().addAll(termBox, div1, cgpaBox);

        // RIGHT — Term history (only if multiple terms exist)
        if (!termHistory.isEmpty()) {
            Region div2 = new Region();
            div2.setStyle("-fx-background-color:#e8e8e8; -fx-min-width:1; -fx-max-width:1;");

            VBox histBox = new VBox(0);
            HBox.setHgrow(histBox, Priority.ALWAYS);
            histBox.setStyle("-fx-padding: 24 24;");

            Label histCap = new Label("TERM-WISE HISTORY");
            histCap.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#7f8c8d; -fx-padding: 0 0 10 0;");
            histBox.getChildren().add(histCap);

            // mini header
            HBox histHeader = new HBox();
            histHeader.setStyle("-fx-padding: 5 10; -fx-background-color:#f5f8fa; -fx-background-radius:6;");
            Label hT = new Label("Term");     hT.setPrefWidth(170); hT.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#555;");
            Label hG = new Label("Term GPA"); hG.setPrefWidth(90);  hG.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#555;");
            Label hC = new Label("CGPA");                           hC.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#555;");
            histHeader.getChildren().addAll(hT, hG, hC);
            histBox.getChildren().add(histHeader);

            for (String[] th : termHistory) {
                double tg = Double.parseDouble(th[1]);
                double cg = Double.parseDouble(th[2]);
                boolean isCurrent = th[0].equals(currentTermLabel);

                HBox histRow = new HBox();
                histRow.setAlignment(Pos.CENTER_LEFT);
                histRow.setStyle("-fx-padding: 7 10;" +
                        (isCurrent ? "-fx-background-color:#eaf6f4; -fx-background-radius:6;" : ""));

                Label tL = new Label((isCurrent ? "▶  " : "      ") + th[0]);
                tL.setPrefWidth(170);
                tL.setStyle("-fx-font-size:12px; -fx-text-fill:" + (isCurrent ? "#2a9d8f" : "#555") + ";"
                        + (isCurrent ? "-fx-font-weight:bold;" : ""));

                Label gL = new Label(th[1]); gL.setPrefWidth(90);
                gL.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:" + gpaColor(tg) + ";");

                Label cL = new Label(th[2]);
                cL.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:" + gpaColor(cg) + ";");

                histRow.getChildren().addAll(tL, gL, cL);
                histBox.getChildren().add(histRow);
            }

            contentRow.getChildren().addAll(div2, histBox);
        }

        summaryCard.getChildren().add(contentRow);
        pane.getChildren().add(summaryCard);

        return pane;
    }

    // results list থেকে credit array বানাও (results table-এ credit column থাকলে)
    private double[] getCreditsForResults(List<String[]> results) {
        double[] credits = new double[results.size()];
        String sql = "SELECT course_code, credit FROM results WHERE user_id=? AND course_code=?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            for (int i = 0; i < results.size(); i++) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT credit FROM results WHERE user_id=? AND course_code=?")) {
                    ps.setInt(1, student.getId());
                    ps.setString(2, results.get(i)[0]);
                    ResultSet rs = ps.executeQuery();
                    credits[i] = rs.next() ? rs.getDouble("credit") : 3.0; // default 3 credit
                } catch (SQLException ex) { credits[i] = 3.0; }
            }
        } catch (SQLException ex) {
            java.util.Arrays.fill(credits, 3.0);
        }
        return credits;
    }

    private String gpaColor(double gpa) {
        if (gpa >= 3.5) return "#27ae60";
        if (gpa >= 2.5) return "#f39c12";
        return "#e74c3c";
    }

    private Label summaryLabel(String t) {
        Label l = new Label(t); l.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#2a9d8f;"); return l;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DB HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private String[] getFullStudentInfo(int id) {
        try (Connection conn=DatabaseConnection.getConnection();
             PreparedStatement ps=conn.prepareStatement("SELECT first_name,last_name,username,email,phone,department,status FROM users WHERE id=?")) {
            ps.setInt(1,id); ResultSet rs=ps.executeQuery();
            if (rs.next()) return new String[]{safe(rs.getString("first_name")),safe(rs.getString("last_name")),
                    safe(rs.getString("username")),safe(rs.getString("email")),safe(rs.getString("phone")),
                    safe(rs.getString("department")),safe(rs.getString("status"))};
        } catch (SQLException e) { e.printStackTrace(); }
        return new String[]{"","","","","","",""};
    }

    // ── FIX: query course_registrations directly — no JOIN needed ────────────
    private List<String[]> getEnrolledCourses() {
        java.util.List<String[]> list=new java.util.ArrayList<>();
        String sql="SELECT course_code, course_name FROM course_registrations " +
                   "WHERE user_id=? AND status='APPROVED' ORDER BY level, term";
        try (Connection conn=DatabaseConnection.getConnection(); PreparedStatement ps=conn.prepareStatement(sql)) {
            ps.setInt(1,student.getId()); ResultSet rs=ps.executeQuery();
            while (rs.next()) list.add(new String[]{rs.getString("course_code"),rs.getString("course_name")});
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private List<String[]> getPublishedResults() {
        java.util.List<String[]> list=new java.util.ArrayList<>();
        String sql="SELECT r.course_code,r.course_name,r.ct_best3,r.attendance_mark,r.term_final_mark,r.total_marks,r.letter_grade,r.grade_point " +
                   "FROM results r WHERE r.user_id=? ORDER BY r.level,r.term";
        try (Connection conn=DatabaseConnection.getConnection(); PreparedStatement ps=conn.prepareStatement(sql)) {
            ps.setInt(1,student.getId()); ResultSet rs=ps.executeQuery();
            while (rs.next()) list.add(new String[]{rs.getString("course_code"),rs.getString("course_name"),
                    fmt2(rs.getDouble("ct_best3")),fmt2(rs.getDouble("attendance_mark")),
                    fmt2(rs.getDouble("term_final_mark")),fmt2(rs.getDouble("total_marks")),
                    rs.getString("letter_grade"),fmt2(rs.getDouble("grade_point"))});
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────
    private VBox card() { VBox v=new VBox(12); v.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),10,0,0,2); -fx-padding:22;"); return v; }
    private Label sectionHeader(String t) { Label l=new Label(t); l.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#2c3e50;"); return l; }
    private Label infoLbl(String t) { Label l=new Label(t); l.setStyle("-fx-font-size:13px; -fx-text-fill:#7f8c8d; -fx-padding:16 0;"); return l; }
    private HBox dataRow() { HBox r=new HBox(10); r.setAlignment(Pos.CENTER_LEFT); r.setStyle("-fx-padding:10 14; -fx-background-color:white; -fx-background-radius:8; -fx-border-color:#e8e8e8; -fx-border-radius:8;"); return r; }
    private Label cl(String t,double w) { Label l=new Label(t!=null?t:"—"); l.setPrefWidth(w); l.setStyle("-fx-font-size:12px; -fx-text-fill:#2c3e50;"); return l; }
    private HBox tableHeader(String... cols) { HBox h=new HBox(10); h.setAlignment(Pos.CENTER_LEFT); h.setStyle("-fx-padding:8 14; -fx-background-color:#f0f4f8; -fx-background-radius:6;"); for (String c : cols) { Label l=new Label(c); l.setStyle("-fx-font-weight:bold; -fx-font-size:12px; -fx-text-fill:#555;"); h.getChildren().add(l); } return h; }
    private void applyHeaderWidths(HBox header,int[] widths) { for (int i=0;i<widths.length&&i<header.getChildren().size();i++) ((Label)header.getChildren().get(i)).setPrefWidth(widths[i]); }
    private Button styledBtn(String text,String color) { Button b=new Button(text); b.setStyle("-fx-background-color:"+color+"; -fx-text-fill:white; -fx-background-radius:6; -fx-cursor:hand; -fx-padding:8 18; -fx-font-size:13px;"); return b; }
    private TextField formField(String prompt) { TextField t=new TextField(); t.setPromptText(prompt); t.setStyle("-fx-pref-height:34px; -fx-padding:0 8; -fx-background-radius:6; -fx-border-radius:6; -fx-border-color:#ccc; -fx-border-width:1;"); return t; }
    private void styleField(Control c) { c.setStyle("-fx-pref-height:34px; -fx-padding:0 8; -fx-background-radius:6; -fx-border-radius:6; -fx-border-color:#ccc; -fx-border-width:1;"); }
    private HBox labeledField(String labelText,Control field) { Label lbl=new Label(labelText); lbl.setPrefWidth(160); lbl.setStyle("-fx-font-size:12px; -fx-text-fill:#555; -fx-font-weight:bold;"); HBox.setHgrow(field,Priority.ALWAYS); HBox box=new HBox(10,lbl,field); box.setAlignment(Pos.CENTER_LEFT); return box; }
    private Button tabBtn(String text,boolean active) { Button b=new Button(text); b.setStyle((active?"-fx-background-color:#2a9d8f; -fx-text-fill:white;":"-fx-background-color:#e8f4f8; -fx-text-fill:#555;")+"-fx-font-size:13px; -fx-padding:10 24; -fx-cursor:hand; -fx-background-radius:8 8 0 0; -fx-border-color:transparent;"); HBox.setHgrow(b,Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE); return b; }
    private void setTabActive(Button active,Button... others) { active.setStyle("-fx-background-color:#2a9d8f; -fx-text-fill:white; -fx-font-size:13px; -fx-padding:10 24; -fx-cursor:hand; -fx-background-radius:8 8 0 0;"); for (Button o : others) o.setStyle("-fx-background-color:#e8f4f8; -fx-text-fill:#555; -fx-font-size:13px; -fx-padding:10 24; -fx-cursor:hand; -fx-background-radius:8 8 0 0;"); }
    private void setMsg(Label l,String m,boolean ok) { l.setText(m); l.setStyle("-fx-text-fill:"+(ok?"#27ae60":"#e74c3c")+"; -fx-font-size:12px;"); }
    private void setActive(Button btn) { if(activeBtn!=null) activeBtn.setStyle(INACTIVE_STYLE); btn.setStyle(ACTIVE_STYLE); activeBtn=btn; }
    private String gradeColor(String g) { return switch(g) { case "A+","A","A-"->"#27ae60"; case "B+","B","B-"->"#2980b9"; case "C+","C"->"#e67e22"; case "D"->"#f39c12"; default->"#e74c3c"; }; }
    private String safe(String s) { return s!=null?s:""; }
    private String fmt1(double v) { return String.format("%.1f",v); }
    private String fmt2(double v) { return String.format("%.2f",v); }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clear();
        try { Parent root=FXMLLoader.load(getClass().getResource("/frontend/view/login.fxml")); Stage stage=(Stage)((Node)event.getSource()).getScene().getWindow(); stage.setScene(new Scene(root)); stage.show(); } catch (Exception e) { e.printStackTrace(); }
    }

    public void showExamMarks(ActionEvent actionEvent) {
    }
}
