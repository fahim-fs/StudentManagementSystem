package backend.dao;

import backend.database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {

    public static class AttendanceRecord {
        public String courseCode;
        public String courseName;
        public int    totalHeld;
        public int    totalAttended;
        public double percentage;

        public AttendanceRecord(String courseCode, String courseName,
                                int totalHeld, int totalAttended) {
            this.courseCode     = courseCode;
            this.courseName     = courseName;
            this.totalHeld      = totalHeld;
            this.totalAttended  = totalAttended;
            this.percentage     = totalHeld == 0 ? 0 :
                                  Math.round((totalAttended * 100.0 / totalHeld) * 10.0) / 10.0;
        }
    }

    public List<AttendanceRecord> getAttendanceSummary(int userId) {
        List<AttendanceRecord> list = new ArrayList<>();
        String sql = """
                SELECT a.course_code, cr.course_name,
                       a.total_held, a.total_attended
                FROM attendance a
                LEFT JOIN course_registrations cr
                       ON a.user_id = cr.user_id AND a.course_code = cr.course_code
                WHERE a.user_id = ?
                ORDER BY a.course_code
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new AttendanceRecord(
                        rs.getString("course_code"),
                        rs.getString("course_name") != null
                                ? rs.getString("course_name") : rs.getString("course_code"),
                        rs.getInt("total_held"),
                        rs.getInt("total_attended")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public double getOverallAttendance(int userId) {
        String sql = "SELECT SUM(total_held), SUM(total_attended) " +
                     "FROM attendance WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int held     = rs.getInt(1);
                int attended = rs.getInt(2);
                if (held > 0) return Math.round((attended * 100.0 / held) * 10.0) / 10.0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
