package backend.service;

import common.User;

/**
 * UserService — Backend partner এই class implement করবে।
 * RegisterController শুধু register() call করে একটা User object দিয়ে।
 */
public class UserService {

    /**
     * নতুন user database এ save করে।
     * @param user  RegisterController থেকে আসা populated User object
     * @return true if registration successful, false otherwise
     */
    public static boolean register(User user) {
        // TODO (backend partner):
        //  1. user.getPassword() কে BCrypt দিয়ে hash করো
        //  2. Username already exists কিনা check করো
        //  3. Database এ INSERT করো (JDBC / Hibernate / যেটা ব্যবহার করছো)
        //  4. Success হলে true, failure তে false return করো

        System.out.println("UserService.register() called for: " + user);
        return true; // placeholder
    }

    /**
     * Username ও password দিয়ে user authenticate করে।
     * @param username  Login form থেকে আসা username
     * @param password  Login form থেকে আসা plain-text password
     * @return true if credentials are valid, false otherwise
     */
    public static boolean login(String username, String password) {
        // TODO (backend partner):
        //  1. Database থেকে username দিয়ে user খোঁজো
        //  2. User না পেলে false return করো
        //  3. BCrypt দিয়ে password verify করো
        //  4. Match হলে true, না হলে false return করো

        System.out.println("UserService.login() called for username: " + username);
        return true; // placeholder
    }
}
