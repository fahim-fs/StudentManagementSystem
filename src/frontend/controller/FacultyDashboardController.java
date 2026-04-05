package frontend.controller;

import backend.dao.AttendanceDAO;
import backend.dao.MarksDAO;
import backend.database.DatabaseConnection;
import backend.model.Student;
import common.CourseData;
import common.CourseData.Course;
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

    //  Sidebar buttons 
    @FXML private Button btnHome;
    @FXML private Button btnAttendance;
    @FXML private Button btnCTMarks;
    @FXML private Button btnQuizMarks;
    @FXML private Button btnStudentList;
    @FXML private Button btnLogout;

    //  Top bar 
    @FXML private Label pageTitle;
    @FXML private Label facultyNameLabel;
    @FXML private Label departmentLabel;

    //  Main content area
    @FXML private VBox mainContentArea;
    @FXML private VBox homeContent;

    //  Home cards
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
        faculty    = SessionManager.getStudent();
        activeBtn  = btnHome;

        facultyNameLabel.setText(faculty.getFirstName() + " " + faculty.getLastName());
        departmentLabel.setText(faculty.getDepartment());

        Button[] navBtns = {btnHome, btnAttendance, btnCTMarks, btnQuizMarks, btnStudentList};
        for (Button btn : navBtns) {
            btn.setOnMouseEntered(e -> { if (btn != activeBtn) btn.setStyle(INACTIVE_STYLE.replace("transparent", "#243b4a")); });
            btn.setOnMouseExited(e ->  { if (btn != activeBtn) btn.setStyle(INACTIVE_STYLE); });
        }

        loadHomeCards();
    }

    private void loadHomeCards() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps1 = conn.prepareStatement(
                    "SELECT COUNT(*) FROM users WHERE role='STUDENT' AND status='APPROVED'");
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) totalStudentsLabel.setText(String.valueOf(rs1.getInt(1)));

            PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT COUNT(DISTINCT course_code) FROM course_registrations");
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) totalCoursesLabel.setText(String.valueOf(rs2.getInt(1)));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    //  Sidebar nav 
    @FXML private void showHome(ActionEvent e) {
        setActive(btnHome);
        pageTitle.setText("Home");
        mainContentArea.getChildren().setAll(homeContent);
    }

    @FXML private void showAttendanceEntry(ActionEvent e) {
        setActive(btnAttendance);
        pageTitle.setText("Attendance Entry");
        mainContentArea.getChildren().setAll(buildAttendanceView());
    }

    @FXML private void showCTMarksEntry(ActionEvent e) {
        setActive(btnCTMarks);
        pageTitle.setText("CT Marks Entry");
        mainContentArea.getChildren().setAll(buildCTMarksView());
    }

    @FXML private void showQuizMarksEntry(ActionEvent e) {
        setActive(btnQuizMarks);
        pageTitle.setText("Quiz Marks Entry");
        mainContentArea.getChildren().setAll(buildQuizMarksView());
    }

    @FXML private void showStudentList(ActionEvent e) {
        setActive(btnStudentList);
        pageTitle.setText("Student List");
        mainContentArea.getChildren().setAll(buildStudentListView());
    }

    
    //  ATTENDANCE ENTRY VIEW
    
    private VBox buildAttendanceView() {
        VBox root = new VBox(16);
        root.setStyle("-fx-padding: 24;");

        ComboBox<String> courseBox = buildCourseCombo();
        Label msgLabel = new Label();
        msgLabel.setStyle("-fx-font-size: 12px;");

        VBox tableArea = new VBox(10);

        courseBox.setOnAction(ev -> {
            tableArea.getChildren().clear();
            String course = courseBox.getValue();
            if (course == null) return;

            List<Student> students = getStudentsByCourse(course);
            if (students.isEmpty()) {
                tableArea.getChildren().add(new Label("No approved students found for this course."));
                return;
            }

            HBox header = buildRowHeader("Student ID", "Name", "Classes Held", "Present", "Action", "Attendance %");
            tableArea.getChildren().add(header);

            AttendanceDAO aDao = new AttendanceDAO();

            for (Student s : students) {
                List<AttendanceDAO.AttendanceRecord> records = aDao.getAttendanceSummary(s.getId());
                AttendanceDAO.AttendanceRecord existing = records.stream()
                        .filter(r -> r.courseCode.equals(course))
                        .findFirst().orElse(null);

                TextField heldField    = new TextField(existing != null ? String.valueOf(existing.totalHeld)     : "0");
                TextField presentField = new TextField(existing != null ? String.valueOf(existing.totalAttended) : "0");
                heldField.setPrefWidth(110);
                presentField.setPrefWidth(110);
                styleField(heldField);
                styleField(presentField);

                Label resultLabel = new Label(existing != null ? existing.percentage + "%" : "—");
                resultLabel.setPrefWidth(100);
                resultLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " +
                        (existing != null && existing.percentage >= 75 ? "#27ae60" : "#e74c3c") + ";");

                Button saveBtn = new Button("Save");
                saveBtn.setStyle("-fx-background-color: #2a9d8f; -fx-text-fill: white;" +
                                 "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 14;");
                saveBtn.setOnAction(ev2 -> {
                    try {
                        int held    = Integer.parseInt(heldField.getText().trim());
                        int present = Integer.parseInt(presentField.getText().trim());
                        if (present > held) {
                            msgLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                            msgLabel.setText("⚠  Present cannot exceed Held.");
                            return;
                        }
                        saveAttendance(s.getId(), course, held, present);

                        double pct = held == 0 ? 0 :
                                Math.round((present * 100.0 / held) * 10.0) / 10.0;
                        resultLabel.setText(pct + "%");
                        resultLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " +
                                (pct >= 75 ? "#27ae60" : "#e74c3c") + ";");

                        msgLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
                        msgLabel.setText("✔  Saved for " + s.getFirstName());
                    } catch (NumberFormatException ex) {
                        msgLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                        msgLabel.setText("⚠  Enter valid numbers.");
                    }
                });

                HBox row = buildDataRow(
                        String.valueOf(s.getId()),
                        s.getFirstName() + " " + s.getLastName(),
                        heldField, presentField, saveBtn, resultLabel
                );
                tableArea.getChildren().add(row);
            }
        });

        root.getChildren().addAll(
                buildSectionHeader("Select Course"), courseBox,
                msgLabel,
                new ScrollPane(tableArea) {{
                    setFitToWidth(true);
                    setStyle("-fx-background: transparent; -fx-background-color: transparent;");
                    VBox.setVgrow(this, Priority.ALWAYS);
                }}
        );
        return root;
    }

    private void saveAttendance(int userId, String courseCode, int held, int present) {
        String sql = """
                INSERT INTO attendance (user_id, course_code, total_held, total_attended)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE total_held = VALUES(total_held),
                                        total_attended = VALUES(total_attended)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, courseCode);
            ps.setInt(3, held);
            ps.setInt(4, present);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    
    //  CT MARKS ENTRY VIEW
    
    private VBox buildCTMarksView() {
        VBox root = new VBox(16);
        root.setStyle("-fx-padding: 24;");

        ComboBox<String> courseBox = buildCourseCombo();
        ComboBox<String> ctBox     = new ComboBox<>(
                FXCollections.observableArrayList("CT 1", "CT 2", "CT 3", "CT 4"));
        ctBox.setPromptText("Select CT");
        styleCombo(ctBox);

        Label msgLabel  = new Label();
        VBox  tableArea = new VBox(10);
        MarksDAO mDao   = new MarksDAO();

        Runnable loadTable = () -> {
            tableArea.getChildren().clear();
            String course = courseBox.getValue();
            String ctVal  = ctBox.getValue();
            if (course == null || ctVal == null) return;
            int ctNum = Integer.parseInt(ctVal.replace("CT ", ""));

            List<Student> students = getStudentsByCourse(course);
            if (students.isEmpty()) { tableArea.getChildren().add(new Label("No students found.")); return; }

            tableArea.getChildren().add(buildRowHeader("Student ID", "Name", "Marks / 20", "", "Action", "Best-3 Sum"));

            for (Student s : students) {
                double[] existing = mDao.getStudentCTMarks(s.getId(), course);
                double   cur      = existing[ctNum - 1];

                TextField marksField = new TextField(cur >= 0 ? String.valueOf(cur) : "");
                marksField.setPromptText("0 - 20");
                marksField.setPrefWidth(110);
                styleField(marksField);

                double initBest3 = MarksDAO.getBest3CTSum(existing);
                Label resultLabel = new Label(initBest3 > 0 ? "Best-3: " + initBest3 : "—");
                resultLabel.setPrefWidth(110);
                resultLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2980b9;");

                Button saveBtn = new Button("Save");
                saveBtn.setStyle("-fx-background-color: #2a9d8f; -fx-text-fill: white;" +
                                 "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 14;");
                saveBtn.setOnAction(ev -> {
                    try {
                        double marks = Double.parseDouble(marksField.getText().trim());
                        if (marks < 0 || marks > 20) {
                            msgLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                            msgLabel.setText("⚠  Marks must be 0-20.");
                            return;
                        }
                        mDao.saveCTMark(s.getId(), course, ctNum, marks, faculty.getId());

                        double[] updated  = mDao.getStudentCTMarks(s.getId(), course);
                        double   best3    = MarksDAO.getBest3CTSum(updated);
                        resultLabel.setText("Best-3: " + best3);
                        resultLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2980b9;");

                        msgLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
                        msgLabel.setText("✔  Saved for " + s.getFirstName());
                    } catch (NumberFormatException ex) {
                        msgLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                        msgLabel.setText("⚠  Enter a valid number.");
                    }
                });

                tableArea.getChildren().add(buildDataRow(
                        String.valueOf(s.getId()),
                        s.getFirstName() + " " + s.getLastName(),
                        marksField, new Label("/ 20"), saveBtn, resultLabel));
            }
        };

        courseBox.setOnAction(ev -> loadTable.run());
        ctBox.setOnAction(ev -> loadTable.run());

        HBox selectors = new HBox(12, courseBox, ctBox);
        root.getChildren().addAll(
                buildSectionHeader("CT Marks Entry — Best 3 of 4 will be counted"),
                selectors, msgLabel,
                new ScrollPane(tableArea) {{
                    setFitToWidth(true);
                    setStyle("-fx-background: transparent; -fx-background-color: transparent;");
                    VBox.setVgrow(this, Priority.ALWAYS);
                }}
        );
        return root;
    }

    

    private VBox buildQuizMarksView() {
        VBox root = new VBox(16);
        root.setStyle("-fx-padding: 24;");

        ComboBox<String> courseBox  = buildCourseCombo();
        TextField        quizNumFld = new TextField();
        quizNumFld.setPromptText("Quiz number (e.g. 1)");
        quizNumFld.setPrefWidth(150);
        styleField(quizNumFld);

        TextField fullMarksFld = new TextField();
        fullMarksFld.setPromptText("Full marks (e.g. 10)");
        fullMarksFld.setPrefWidth(150);
        styleField(fullMarksFld);

        Label msgLabel  = new Label();
        VBox  tableArea = new VBox(10);
        MarksDAO mDao   = new MarksDAO();

        Button loadBtn = new Button("Load Students");
        loadBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;" +
                         "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 18;");
        loadBtn.setOnAction(ev -> {
            tableArea.getChildren().clear();
            String course = courseBox.getValue();
            if (course == null || quizNumFld.getText().isBlank() || fullMarksFld.getText().isBlank()) {
                msgLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                msgLabel.setText("⚠  Fill in all fields first.");
                return;
            }
            int    qNum;
            double fMarks;
            try {
                qNum   = Integer.parseInt(quizNumFld.getText().trim());
                fMarks = Double.parseDouble(fullMarksFld.getText().trim());
            } catch (NumberFormatException ex) {
                msgLabel.setText("⚠  Enter valid numbers."); return;
            }

            List<Student> students = getStudentsByCourse(course);
            if (students.isEmpty()) { tableArea.getChildren().add(new Label("No students found.")); return; }

            tableArea.getChildren().add(buildRowHeader("Student ID", "Name",
                    "Marks / " + fMarks, "", "Action", "Saved"));

            for (Student s : students) {
                List<double[]> quizList = mDao.getStudentQuizMarks(s.getId(), course);
                double existingMark = quizList.stream()
                        .filter(q -> (int) q[0] == qNum)
                        .map(q -> q[1])
                        .findFirst().orElse(-1.0);

                TextField marksField = new TextField(existingMark >= 0 ? String.valueOf(existingMark) : "");
                marksField.setPromptText("0 - " + fMarks);
                marksField.setPrefWidth(110);
                styleField(marksField);

                Label resultLabel = new Label(existingMark >= 0 ? existingMark + " / " + fMarks : "—");
                resultLabel.setPrefWidth(110);
                resultLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #8e44ad;");

                Button saveBtn = new Button("Save");
                saveBtn.setStyle("-fx-background-color: #2a9d8f; -fx-text-fill: white;" +
                                 "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 14;");
                double fm = fMarks;
                saveBtn.setOnAction(ev2 -> {
                    try {
                        double marks = Double.parseDouble(marksField.getText().trim());
                        if (marks < 0 || marks > fm) {
                            msgLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                            msgLabel.setText("⚠  Marks out of range.");
                            return;
                        }
                        mDao.saveQuizMark(s.getId(), course, qNum, marks, fm, faculty.getId());

                        resultLabel.setText(marks + " / " + fm);
                        resultLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #8e44ad;");

                        msgLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
                        msgLabel.setText("✔  Saved for " + s.getFirstName());
                    } catch (NumberFormatException ex) {
                        msgLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                        msgLabel.setText("⚠  Enter a valid number.");
                    }
                });

                tableArea.getChildren().add(buildDataRow(
                        String.valueOf(s.getId()),
                        s.getFirstName() + " " + s.getLastName(),
                        marksField, new Label("/ " + fMarks), saveBtn, resultLabel));
            }
        });

        HBox selectors = new HBox(12, courseBox, quizNumFld, fullMarksFld, loadBtn);
        selectors.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(
                buildSectionHeader("Quiz Marks Entry"),
                selectors, msgLabel,
                new ScrollPane(tableArea) {{
                    setFitToWidth(true);
                    setStyle("-fx-background: transparent; -fx-background-color: transparent;");
                    VBox.setVgrow(this, Priority.ALWAYS);
                }}
        );
        return root;
    }

    
    //  STUDENT LIST VIEW
    
    private VBox buildStudentListView() {
        VBox root = new VBox(16);
        root.setStyle("-fx-padding: 24;");

        ComboBox<String> courseBox = buildCourseCombo();
        VBox tableArea = new VBox(10);

        courseBox.setOnAction(ev -> {
            tableArea.getChildren().clear();
            String course = courseBox.getValue();
            if (course == null) return;

            List<Student> students = getStudentsByCourse(course);
            if (students.isEmpty()) { tableArea.getChildren().add(new Label("No students found.")); return; }

            tableArea.getChildren().add(buildRowHeader("ID", "Name", "Department", "Username", "Status", ""));

            for (Student s : students) {
                HBox row = new HBox();
                row.setStyle("-fx-padding: 10 14; -fx-background-color: white;" +
                             "-fx-background-radius: 8; -fx-border-color: #e0e0e0;" +
                             "-fx-border-radius: 8;");
                row.setAlignment(Pos.CENTER_LEFT);
                row.getChildren().addAll(
                        colLabel(String.valueOf(s.getId()), 60),
                        colLabel(s.getFirstName() + " " + s.getLastName(), 200),
                        colLabel(s.getDepartment() != null ? s.getDepartment() : "—", 220),
                        colLabel(s.getUsername(), 150),
                        colLabel(s.getStatus().name(), 100)
                );
                tableArea.getChildren().add(row);
            }
        });

        root.getChildren().addAll(
                buildSectionHeader("Student List"),
                courseBox,
                new ScrollPane(tableArea) {{
                    setFitToWidth(true);
                    setStyle("-fx-background: transparent; -fx-background-color: transparent;");
                    VBox.setVgrow(this, Priority.ALWAYS);
                }}
        );
        return root;
    }

    //  Helpers 
    private List<Student> getStudentsByCourse(String courseCode) {
        List<Student> list = new ArrayList<>();
        String sql = """
                SELECT u.id, u.first_name, u.last_name, u.department, u.username, u.status
                FROM users u
                JOIN course_registrations cr ON u.id = cr.user_id
                WHERE cr.course_code = ? AND u.role = 'STUDENT'
                ORDER BY u.first_name
                """;
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

    private ComboBox<String> buildCourseCombo() {
        List<String> codes = new ArrayList<>();
        for (int l = 1; l <= 4; l++)
            for (int t = 1; t <= 2; t++)
                for (Course c : CourseData.getCourses(l, t))
                    if (!codes.contains(c.code)) codes.add(c.code);
        ComboBox<String> box = new ComboBox<>(FXCollections.observableArrayList(codes));
        box.setPromptText("Select Course");
        styleCombo(box);
        return box;
    }

    private HBox buildRowHeader(String... cols) {
        HBox h = new HBox();
        h.setStyle("-fx-padding: 8 14; -fx-background-color: #f0f0f0;" +
                   "-fx-background-radius: 6;");
        h.setAlignment(Pos.CENTER_LEFT);
        int[] widths = {60, 200, 110, 110, 80, 110};
        for (int i = 0; i < cols.length; i++) {
            Label lbl = new Label(cols[i]);
            lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #555;");
            lbl.setPrefWidth(i < widths.length ? widths[i] : 110);
            h.getChildren().add(lbl);
        }
        return h;
    }

    private HBox buildDataRow(String id, String name, Node field1, Node field2, Node action, Node result) {
        HBox row = new HBox(10);
        row.setStyle("-fx-padding: 8 14; -fx-background-color: white;" +
                     "-fx-background-radius: 8; -fx-border-color: #e8e8e8;" +
                     "-fx-border-radius: 8;");
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(colLabel(id, 60), colLabel(name, 200), field1, field2, action, result);
        return row;
    }

    private Label colLabel(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50;");
        return l;
    }

    private Label buildSectionHeader(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        return l;
    }

    private void styleField(TextField tf) {
        tf.setStyle("-fx-background-radius: 6; -fx-border-radius: 6;" +
                    "-fx-border-color: #ccc; -fx-border-width: 1;" +
                    "-fx-pref-height: 32px; -fx-font-size: 12px; -fx-padding: 0 8;");
    }

    private void styleCombo(ComboBox<?> box) {
        box.setStyle("-fx-pref-width: 220px; -fx-pref-height: 34px;" +
                     "-fx-background-radius: 8; -fx-border-radius: 8;" +
                     "-fx-border-color: #ccc; -fx-font-size: 13px;");
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
