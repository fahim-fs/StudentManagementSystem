package backend.util;

import backend.util.PasswordUtil;

//imports for student authentication

import backend.model.Role;
import backend.model.Student;
import backend.dao.StudentDao;

//imports for admin aauthentication

//imports for faculty authentication

public class AuthUtil {

    public static Object authenticate(String email, String password, UserRole role) {

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

    // student authentication
    private static Student authenticateStudent(String email, String password) {

        StudentDAO studentDAO = new StudentDAO(); // is not implemented yet
        Student student = StudentDao.getStudentByEmail(email); // search in database using email

        if (student == null) {
            return null;
        }

        // Only approved students can login
        if (!student.getStatus().name().equals("APPROVED")) {
            return null;
        }

        boolean passwordMatch = PasswordUtil.checkPassword(password, student.getPassword());

        return passwordMatch ? student : null;

    }

}