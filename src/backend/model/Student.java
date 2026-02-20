package backend.model;

public class Student {

    // personal details
    private int id; // auto generated
    private String FirstName;
    private String LastName;
    private String email;
    private String phone;
    private String department;
    private int level;
    private int term;

    // student status--Role and Status is defined in Role.java and Status.java
    // respectively
    private Role role;
    private Status status;
    private String password;

    // ---------constructor--------------
    public Student() {
        this.id = 0;
        this.FirstName = "";
        this.LastName = "";
        this.phone = "";
        this.email = "";
        this.password = "";
        this.department = "";
        this.level = 0;
        this.term = 0;
        this.role = Role.STUDENT; // default role
        this.status = Status.PENDING; // default status
    }

    public Student(String id, String FirstName, String LastName, String phone, String email,
            String department, int level, int term) {

        this.id = id;
        this.FirstName = FirstName;
        this.LastName = LastName;
        this.phone = phone;
        this.email = email;
        this.department = department;
        this.level = level;
        this.term = term;

        this.role = Role.STUDENT; // default role
        this.status = Status.PENDING; // default status

    }

    public Student(String id, String FirstName, String LastName, String phone, String email,
            String department, int level, int term, Status status) {

        this.id = id;
        this.FirstName = FirstName;
        this.LastName = LastName;
        this.phone = phone;
        this.email = email;
        this.department = department;
        this.level = level;
        this.term = term;

        this.role = Role.STUDENT; // default role
        this.status = status;

    }

    // ---------getter/setter--------------------
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return FirstName;
    }

    public void setFirstName(String FirstName) {
        this.FirstName = FirstName;
    }

    public String getLastName() {
        return LastName;
    }

    public void setLastName(String LastName) {
        this.LastName = LastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;

    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }


    public getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    //---------other utility methods---------------

    






    // ---------Password Encryption------------------
    // Do not modify this method
    public void setPassword(String newPass) {
        String hashedPassword = PasswordUtil.hashPassword(newPass);
        this.password = hashedPassword;

    }

    public String getPassword() {
        return password; // hashed password
    }

    // ----------Debugging utility--------------------
    @Override // only for debugging perpose
    public String toString() {
        return "Student{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", department='" + department + '\'' + '}';
    }

}
