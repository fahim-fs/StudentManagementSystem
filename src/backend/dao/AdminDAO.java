package backend.dao;

import backend.database.DatabaseConnection;
import backend.model.Admin;
import backend.model.Role;
import backend.model.Status;
import backend.model.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

    // Admin ID Generation

    private String generateAdminId(Connection conn) throws SQLException {
        String sql = "SELECT MAX(admin_id) FROM users WHERE role = 'ADMIN'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            if (rs.next() && rs.getString(1) != null) {
                String lastId = rs.getString(1); // e.g. "02102"
                int numericPart = Integer.parseInt(lastId.substring(2)); // 102
                int nextCode = numericPart + 1; // 103
                return String.format("02%03d", nextCode); // "02103"
            } else {
                return "02100"; // very first admin
            }
        }
    }

    // Manual Admin

    /**
     * Manually inserts an admin into the database.
     * This is the ONLY way to add an admin — not accessible via client app.
     */
    public static void addAdminManually(String firstName, String lastName,
            String email, String plainPassword) {
        String sql = """
                INSERT INTO users
                (admin_id, first_name, last_name, email, password, role, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            // generate ID within the same connection to avoid race conditions
            AdminDAO dao = new AdminDAO();
            String adminId = dao.generateAdminId(conn);

            String hashedPassword = backend.util.PasswordUtil.hashPassword(plainPassword);

            ps.setString(1, adminId);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, email);
            ps.setString(5, hashedPassword);
            ps.setString(6, Role.ADMIN.name());
            ps.setString(7, Status.APPROVED.name()); // admins are always active

            ps.executeUpdate();
            System.out.println("Admin added successfully with ID: " + adminId);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Fetch Admin by Email

    public Admin getAdminByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ? AND role = 'ADMIN'";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Fetch Pending Students

    public List<Student> getPendingStudents() {
        String sql = "SELECT * FROM users WHERE role = 'STUDENT' AND status = 'PENDING'";
        List<Student> pendingList = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                pendingList.add(mapStudentRow(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pendingList;
    }

    // Update Student Status

    public boolean updateStudentStatus(int studentId, Status status) {
        String sql = "UPDATE users SET status = ? WHERE id = ? AND role = 'STUDENT'";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setInt(2, studentId);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Row Mappers

    private Admin mapRow(ResultSet rs) throws SQLException {
        Admin admin = new Admin();
        admin.setId(rs.getInt("id"));
        admin.setAdminId(rs.getString("admin_id"));
        admin.setFirstName(rs.getString("first_name"));
        admin.setLastName(rs.getString("last_name"));
        admin.setEmail(rs.getString("email"));
        admin.setHashedPassword(rs.getString("password"));
        admin.setRole(Role.valueOf(rs.getString("role")));
        return admin;
    }

    private Student mapStudentRow(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setFirstName(rs.getString("first_name"));
        s.setLastName(rs.getString("last_name"));
        s.setEmail(rs.getString("email"));
        s.setPhone(rs.getString("phone"));
        s.setDepartment(rs.getString("department"));
        s.setUsername(rs.getString("username"));
        s.setHashedPassword(rs.getString("password"));
        s.setStatus(Status.valueOf(rs.getString("status")));
        Date dob = rs.getDate("date_of_birth");
        if (dob != null)
            s.setDateOfBirth(dob.toLocalDate());
        return s;
    }
}