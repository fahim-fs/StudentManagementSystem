import backend.dao.AdminDAO;

public class Main {
    public static void main(String[] args) {
    AdminDAO.addAdminManually("John", "Doe", "admin@example.com", "securepassword");
}
}
