package backend.dao;

import backend.database.DatabaseConnection;
import common.CourseData.Course;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseRegistrationDAO {

    public List<String> getRegisteredCourseCodes(int userId, int level, int term) {
        List<String> codes = new ArrayList<>();
        String sql = "SELECT course_code FROM course_registrations " +
                     "WHERE user_id = ? AND level = ? AND term = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, level); ps.setInt(3, term);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) codes.add(rs.getString("course_code"));
        } catch (SQLException e) { e.printStackTrace(); }
        return codes;
    }

    public List<String> getApprovedCourseCodes(int userId, int level, int term) {
        List<String> codes = new ArrayList<>();
        String sql = "SELECT course_code FROM course_registrations " +
                     "WHERE user_id = ? AND level = ? AND term = ? AND status = 'APPROVED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, level); ps.setInt(3, term);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) codes.add(rs.getString("course_code"));
        } catch (SQLException e) { e.printStackTrace(); }
        return codes;
    }

    public List<String[]> getApprovedCourseDetails(int userId, int level, int term) {
        // returns List of [course_code, course_name, faculty]
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT course_code, course_name, faculty FROM course_registrations " +
                     "WHERE user_id = ? AND level = ? AND term = ? AND status = 'APPROVED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, level); ps.setInt(3, term);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("course_code"),
                    rs.getString("course_name"),
                    rs.getString("faculty")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // Course register
    public boolean registerCourse(int userId, int level, int term, Course course) {
        String sql = "INSERT INTO course_registrations " +
                     "(user_id, level, term, course_code, course_name, credit, faculty, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, level); ps.setInt(3, term);
            ps.setString(4, course.code); ps.setString(5, course.name);
            ps.setDouble(6, course.credit); ps.setString(7, course.faculty);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean dropCourse(int userId, int level, int term, String courseCode) {
        String sql = "DELETE FROM course_registrations " +
                     "WHERE user_id = ? AND level = ? AND term = ? AND course_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, level); ps.setInt(3, term);
            ps.setString(4, courseCode);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // Total credit
    public double getTotalCredit(int userId, int level, int term) {
        String sql = "SELECT SUM(credit) FROM course_registrations " +
                     "WHERE user_id = ? AND level = ? AND term = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, level); ps.setInt(3, term);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}
