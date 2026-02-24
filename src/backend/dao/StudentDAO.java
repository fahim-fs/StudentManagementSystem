package backend.dao;

import backend.database.DatabaseConnection;
import backend.model.Role;
import backend.model.Status;
import backend.model.Student;

import java.sql.*;

public class StudentDAO {

    public boolean insertStudent(Student student) {
        String sql = """
                INSERT INTO users
                (first_name, last_name, father_name, mother_name, date_of_birth,
                 phone, address, gender, session, department, username, password, role, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, student.getFirstName());
            ps.setString(2, student.getLastName());
            ps.setString(3, student.getFatherName());
            ps.setString(4, student.getMotherName());
            ps.setDate(5, student.getDateOfBirth() != null
                    ? Date.valueOf(student.getDateOfBirth()) : null);
            ps.setString(6, student.getPhone());
            ps.setString(7, student.getAddress());
            ps.setString(8, student.getGender());
            ps.setString(9, student.getSession());
            ps.setString(10, student.getDepartment());
            ps.setString(11, student.getUsername());
            ps.setString(12, student.getPassword()); // already hashed
            ps.setString(13, student.getRole().name());
            ps.setString(14, student.getStatus().name());

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Student getStudentByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
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

    public Student getStudentByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    private Student mapRow(ResultSet rs) throws SQLException {
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
        s.setRole(Role.valueOf(rs.getString("role")));
        Date dob = rs.getDate("date_of_birth");
        if (dob != null) s.setDateOfBirth(dob.toLocalDate());
        return s;
    }
}