package backend.util;

import backend.model.Role;
import backend.model.Student;
import backend.dao.StudentDAO;
import backend.util.PasswordUtil;


public class AuthUtil {

    public static Object authenticate(String email, String password, Role role) {

        switch (role) {

            case STUDENT:
                return authenticateStudent(email, password);

            case ADMIN:
                // return authenticateAdmin(email, password);
                return null;

            case FACULTY:
                // return authenticateFaculty(email, password);
                return null;

            default:
                return null;
        }
    }

    private static Student authenticateStudent(String email, String password) {

        StudentDAO studentDAO = new StudentDAO();
        Student student = studentDAO.getStudentByEmail(email); // instance method

        if (student == null) return null;

        if (!student.getStatus().name().equals("APPROVED")) return null;

        boolean passwordMatch = PasswordUtil.checkPassword(password, student.getPassword());

        return passwordMatch ? student : null;
    }
}