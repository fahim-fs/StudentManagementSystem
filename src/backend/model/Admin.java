package backend.model;

import backend.model.*;
import java.util.List;
import java.util.ArrayList;

public class Admin {

    private int id;
    private String adminId; // e.g. 02100, 02101, ...
    private String firstName;
    private String lastName;
    private String email;
    private String password; // hashed
    private Role role;
    private List<Student> pendingStudents;

    public Admin() {
        this.role = Role.ADMIN;
        this.pendingStudents = new ArrayList<>();
    }

    public Admin(String firstName, String lastName, String email, String hashedPassword, String adminId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = hashedPassword;
        this.adminId = adminId;
        this.role = Role.ADMIN;
        this.pendingStudents = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setHashedPassword(String hashedPassword) {
        this.password = hashedPassword;
    }

    public void setPassword(String plainPassword) {
        this.password = backend.util.PasswordUtil.hashPassword(plainPassword);
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<Student> getPendingStudents() {
        return pendingStudents;
    }

    public void setPendingStudents(List<Student> pendingStudents) {
        this.pendingStudents = pendingStudents;
    }


    /**
     * Loads unapproved (PENDING) students from the database into the local list.
     * Delegates to AdminDAO.
     */
    public void fetchPendingStudents() {
        backend.dao.AdminDAO adminDAO = new backend.dao.AdminDAO();
        this.pendingStudents = adminDAO.getPendingStudents();
    }

    // * Sets the approval status of a student.

    public void setStudentStatus(Student student, Status status) {
        backend.dao.AdminDAO adminDAO = new backend.dao.AdminDAO();
        boolean success = adminDAO.updateStudentStatus(student.getId(), status);
        if (success) {
            student.setStatus(status);
            if (status == Status.APPROVED) {
                pendingStudents.remove(student);
            }
        }
    }

    /**
     * Creates a Faculty object and persists it to the database.
     * Only admins can call this.
     * 
     * @param faculty the Faculty object to add
     * @return true if successfully added
     */
    /*
     * public boolean addFaculty(Faculty faculty) {
     * backend.dao.FacultyDAO facultyDAO = new backend.dao.FacultyDAO();
     * return facultyDAO.insertFaculty(faculty);
     * }
     */

    /**
     * Builds a Faculty object from raw details and persists it.
     * Convenience method — admin provides plain details directly.
     */
    /*
     * public boolean createAndAddFaculty(String firstName, String lastName, String
     * email,
     * String plainPassword, String department) {
     * Faculty faculty = new Faculty();
     * faculty.setFirstName(firstName);
     * faculty.setLastName(lastName);
     * faculty.setEmail(email);
     * faculty.setPassword(plainPassword); // hashed internally
     * faculty.setDepartment(department);
     * faculty.setRole(Role.FACULTY);
     * faculty.setStatus(Status.APPROVED); // faculty is active upon creation
     * 
     * backend.dao.FacultyDAO facultyDAO = new backend.dao.FacultyDAO();
     * return facultyDAO.insertFaculty(faculty);
     * }
     */

    @Override
    public String toString() {
        return "Admin{adminId='" + adminId + "', name='" + firstName + " " + lastName
                + "', email='" + email + "'}";
    }
}