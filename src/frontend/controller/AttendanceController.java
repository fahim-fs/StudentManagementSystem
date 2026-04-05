package frontend.controller;

import backend.dao.AttendanceDAO;
import backend.dao.AttendanceDAO.AttendanceRecord;
import backend.model.Student;
import common.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AttendanceController implements Initializable {

    @FXML private Label    overallPctLabel;
    @FXML private Label    overallStatusLabel;
    @FXML private Label    totalHeldLabel;
    @FXML private Label    totalAttendedLabel;
    @FXML private VBox     courseAttendanceList;

    private final AttendanceDAO dao     = new AttendanceDAO();
    private       Student       student;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        student = SessionManager.getStudent();
        loadAttendance();
    }

    private void loadAttendance() {
        courseAttendanceList.getChildren().clear();

        double overall = dao.getOverallAttendance(student.getId());
        List<AttendanceRecord> records = dao.getAttendanceSummary(student.getId());

        // Overall summary
        overallPctLabel.setText(overall + "%");
        if (overall >= 75) {
            overallPctLabel.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            overallStatusLabel.setText("✔  Regular");
            overallStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else {
            overallPctLabel.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            overallStatusLabel.setText("✘  Irregular — Below 75%");
            overallStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }

        // Total held / attended
        int sumHeld = records.stream().mapToInt(r -> r.totalHeld).sum();
        int sumAtt  = records.stream().mapToInt(r -> r.totalAttended).sum();
        totalHeldLabel.setText(String.valueOf(sumHeld));
        totalAttendedLabel.setText(String.valueOf(sumAtt));

        // Course-wise rows
        if (records.isEmpty()) {
            Label empty = new Label("No attendance records found.");
            empty.setStyle("-fx-text-fill: #aaa; -fx-font-size: 13px; -fx-padding: 10 0;");
            courseAttendanceList.getChildren().add(empty);
            return;
        }

        for (AttendanceRecord rec : records) {
            courseAttendanceList.getChildren().add(buildRow(rec));
        }
    }

    private VBox buildRow(AttendanceRecord rec) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white;" +
                      "-fx-background-radius: 10;" +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 0, 2);" +
                      "-fx-padding: 16 18;");

        // Top row: course name + percentage
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox courseInfo = new VBox(3);
        Label codeLbl = new Label(rec.courseCode + "  —  " + rec.courseName);
        codeLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label statsLbl = new Label("Held: " + rec.totalHeld +
                                   "   Attended: " + rec.totalAttended);
        statsLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        courseInfo.getChildren().addAll(codeLbl, statsLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Percentage badge
        String badgeColor = rec.percentage >= 75 ? "#27ae60" : "#e74c3c";
        Label pctBadge = new Label(rec.percentage + "%");
        pctBadge.setStyle("-fx-background-color: " + badgeColor + ";" +
                          "-fx-text-fill: white;" +
                          "-fx-font-size: 13px;" +
                          "-fx-font-weight: bold;" +
                          "-fx-background-radius: 20;" +
                          "-fx-padding: 4 12;");

        top.getChildren().addAll(courseInfo, spacer, pctBadge);

        // Progress bar
        ProgressBar bar = new ProgressBar(rec.percentage / 100.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(8);
        String barColor = rec.percentage >= 75 ? "#27ae60" : "#e74c3c";
        bar.setStyle("-fx-accent: " + barColor + ";");

        card.getChildren().addAll(top, bar);
        return card;
    }
}
