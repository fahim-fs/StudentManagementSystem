package common;

import backend.model.Student;

public class SessionManager {

    private static Student loggedInStudent = null;

    public static void setStudent(Student student) {
        loggedInStudent = student;
    }

    public static Student getStudent() {
        return loggedInStudent;
    }

    public static void clear() {
        loggedInStudent = null;
    }
}