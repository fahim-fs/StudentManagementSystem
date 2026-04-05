package backend.dao;

import backend.database.DatabaseConnection;

import java.sql.*;
import java.util.*;

public class MarksDAO {

    public Map<Integer, double[]> getCTMarksForCourse(String courseCode) {
        // Map<userId, double[4]> — index 0=CT1, 1=CT2, 2=CT3, 3=CT4
        Map<Integer, double[]> map = new LinkedHashMap<>();
        String sql = "SELECT user_id, ct_number, marks FROM ct_marks " +
                     "WHERE course_code = ? ORDER BY user_id, ct_number";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int uid = rs.getInt("user_id");
                int ct  = rs.getInt("ct_number") - 1;
                map.computeIfAbsent(uid, k -> new double[]{-1,-1,-1,-1});
                map.get(uid)[ct] = rs.getDouble("marks");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return map;
    }

    // CT mark save/update
    public boolean saveCTMark(int userId, String courseCode,
                               int ctNumber, double marks, int facultyId) {
        String sql = """
                INSERT INTO ct_marks (user_id, course_code, ct_number, marks, entered_by)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE marks = VALUES(marks), entered_by = VALUES(entered_by)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, courseCode);
            ps.setInt(3, ctNumber);
            ps.setDouble(4, marks);
            ps.setInt(5, facultyId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public double[] getStudentCTMarks(int userId, String courseCode) {
        double[] marks = {-1, -1, -1, -1};
        String sql = "SELECT ct_number, marks FROM ct_marks " +
                     "WHERE user_id = ? AND course_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, courseCode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                marks[rs.getInt("ct_number") - 1] = rs.getDouble("marks");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return marks;
    }

    public static double getBest3CTSum(double[] marks) {
        List<Double> valid = new ArrayList<>();
        for (double m : marks) if (m >= 0) valid.add(m);
        valid.sort(Collections.reverseOrder());
        double sum = 0;
        for (int i = 0; i < Math.min(3, valid.size()); i++) sum += valid.get(i);
        return sum;
    }

    public Map<Integer, Map<Integer, double[]>> getQuizMarksForCourse(String courseCode) {
        // Map<userId, Map<quizNumber, [marks, fullMarks]>>
        Map<Integer, Map<Integer, double[]>> map = new LinkedHashMap<>();
        String sql = "SELECT user_id, quiz_number, marks, full_marks FROM quiz_marks " +
                     "WHERE course_code = ? ORDER BY user_id, quiz_number";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int uid  = rs.getInt("user_id");
                int qnum = rs.getInt("quiz_number");
                map.computeIfAbsent(uid, k -> new LinkedHashMap<>());
                map.get(uid).put(qnum, new double[]{
                        rs.getDouble("marks"),
                        rs.getDouble("full_marks")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return map;
    }

    // Quiz mark save/update
    public boolean saveQuizMark(int userId, String courseCode,
                                 int quizNumber, double marks,
                                 double fullMarks, int facultyId) {
        String sql = """
                INSERT INTO quiz_marks (user_id, course_code, quiz_number, marks, full_marks, entered_by)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE marks = VALUES(marks),
                                        full_marks = VALUES(full_marks),
                                        entered_by = VALUES(entered_by)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, courseCode);
            ps.setInt(3, quizNumber);
            ps.setDouble(4, marks);
            ps.setDouble(5, fullMarks);
            ps.setInt(6, facultyId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public List<double[]> getStudentQuizMarks(int userId, String courseCode) {
        // List of [quizNumber, marks, fullMarks]
        List<double[]> list = new ArrayList<>();
        String sql = "SELECT quiz_number, marks, full_marks FROM quiz_marks " +
                     "WHERE user_id = ? AND course_code = ? ORDER BY quiz_number";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, courseCode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new double[]{
                        rs.getDouble("quiz_number"),
                        rs.getDouble("marks"),
                        rs.getDouble("full_marks")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<int[]> getEnrolledStudents(String courseCode) {
        // List of [userId]
        List<int[]> list = new ArrayList<>();
        String sql = "SELECT DISTINCT cr.user_id FROM course_registrations cr " +
                     "WHERE cr.course_code = ? AND cr.status = 'APPROVED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new int[]{rs.getInt("user_id")});
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}
