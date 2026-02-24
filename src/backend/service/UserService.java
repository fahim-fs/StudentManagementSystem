package backend.service;

import backend.dao.StudentDAO;
import backend.model.Student;
import backend.util.PasswordUtil;
import common.User;

public class UserService {

    private static final StudentDAO studentDAO = new StudentDAO();

    public static boolean register(User user) {
        Student student = new Student();
        student.setFirstName(user.getFirstName());
        student.setLastName(user.getLastName());
        student.setFatherName(user.getFatherName());
        student.setMotherName(user.getMotherName());
        student.setDateOfBirth(user.getDateOfBirth());
        student.setPhone(user.getPhoneNumber());
        student.setAddress(user.getAddress());
        student.setGender(user.getGender());
        student.setSession(user.getSession());
        student.setDepartment(user.getDepartment());
        student.setUsername(user.getUsername());
        student.setPassword(user.getPassword());

        return studentDAO.insertStudent(student);
    }

    public static Student login(String username, String password) {
        Student student = studentDAO.getStudentByUsername(username);
        if (student == null) return null;
        if (!student.getStatus().name().equals("APPROVED")) return null;
        boolean match = PasswordUtil.checkPassword(password, student.getPassword());
        return match ? student : null;
    }
}