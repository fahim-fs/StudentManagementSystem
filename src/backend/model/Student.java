package backend.model;

import backend.util.PasswordUtil;

import java.time.LocalDate;

public class Student {

    private int id;
    private String firstName;
    private String lastName;
    private String fatherName;
    private String motherName;
    private String email;
    private String phone;
    private String address;
    private String gender;
    private String session;
    private String department;
    private String username;
    private int level;
    private int term;
    private Role role;
    private Status status;
    private String password;
    private java.time.LocalDate dateOfBirth;

    // ── Constructors ──────────────────────────────────────────────────────────
    public Student() {
        this.id = 0;
        this.firstName = "";
        this.lastName = "";
        this.phone = "";
        this.email = "";
        this.password = "";
        this.department = "";
        this.level = 0;
        this.term = 0;
        this.role = Role.STUDENT;
        this.status = Status.PENDING;
        this.dateOfBirth = null;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String v) {
        this.firstName = v;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String v) {
        this.lastName = v;
    }

    public String getFatherName() {
        return fatherName;
    }

    public void setFatherName(String v) {
        this.fatherName = v;
    }

    public String getMotherName() {
        return motherName;
    }

    public void setMotherName(String v) {
        this.motherName = v;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        this.email = v;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String v) {
        this.phone = v;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String v) {
        this.address = v;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String v) {
        this.gender = v;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String v) {
        this.session = v;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String v) {
        this.department = v;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String v) {
        this.username = v;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int v) {
        this.level = v;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int v) {
        this.term = v;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role v) {
        this.role = v;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status v) {
        this.status = v;
    }

    // ── Password (hashed) ─────────────────────────────────────────────────────
    public void setPassword(String plainPassword) {
        this.password = PasswordUtil.hashPassword(plainPassword);
    }

    public void setHashedPassword(String hashedPassword) {
        this.password = hashedPassword;
    }

    public String getPassword() {
        return password;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    // ── Debug ─────────────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return "Student{id=" + id + ", name='" + firstName + " " + lastName
                + "', department='" + department + "'}";
    }
}