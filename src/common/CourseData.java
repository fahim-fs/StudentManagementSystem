package common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CourseData {

    public static class Course {
        public final String code;
        public final String name;
        public final double credit;
        public final String faculty;

        public Course(String code, String name, double credit, String faculty) {
            this.code    = code;
            this.name    = name;
            this.credit  = credit;
            this.faculty = faculty;
        }

        public String getKey() { return level(code) + "_" + term(code); }

        private String level(String c) { return ""; }
        private String term(String c)  { return ""; }

        @Override
        public String toString() {
            return code + " | " + name + " | " + credit + " cr | " + faculty;
        }
    }

    // Key format: "L1T1", "L1T2", "L2T1" ...
    private static final Map<String, List<Course>> COURSES = new HashMap<>();

    static {
        //Level 1, Term 1
        COURSES.put("L1T1", Arrays.asList(
            new Course("CSE101", "Introduction to Programming",     3.0, "Dr. Rahman"),
            new Course("CSE102", "Discrete Mathematics",            3.0, "Dr. Islam"),
            new Course("CSE103", "Digital Logic Design",            3.0, "Dr. Hossain"),
            new Course("ENG101", "English Communication",           2.0, "Mr. Karim"),
            new Course("PHY101", "Physics I",                       3.0, "Dr. Ahmed"),
            new Course("PHY102", "Physics I Lab",                   1.5, "Dr. Ahmed")
        ));

        // Level 1, Term 2
        COURSES.put("L1T2", Arrays.asList(
            new Course("CSE111", "Object Oriented Programming",     3.0, "Dr. Alam"),
            new Course("CSE112", "Data Structures",                 3.0, "Dr. Chowdhury"),
            new Course("MATH111","Calculus",                        3.0, "Dr. Begum"),
            new Course("ENG111", "Technical Writing",               2.0, "Mr. Karim"),
            new Course("PHY111", "Physics II",                      3.0, "Dr. Siddiqui"),
            new Course("PHY112", "Physics II Lab",                  1.5, "Dr. Siddiqui")
        ));

        //  Level 2, Term 1
        COURSES.put("L2T1", Arrays.asList(
            new Course("CSE201", "Algorithms",                      3.0, "Dr. Rahman"),
            new Course("CSE202", "Database Systems",                3.0, "Dr. Islam"),
            new Course("CSE203", "Computer Architecture",           3.0, "Dr. Noor"),
            new Course("MATH201","Linear Algebra",                  3.0, "Dr. Begum"),
            new Course("CSE204", "Database Lab",                    1.5, "Dr. Islam"),
            new Course("HUM201", "Economics",                       2.0, "Mr. Khan")
        ));

        //  Level 2, Term 2
        COURSES.put("L2T2", Arrays.asList(
            new Course("CSE211", "Operating Systems",               3.0, "Dr. Hossain"),
            new Course("CSE212", "Computer Networks",               3.0, "Dr. Alam"),
            new Course("CSE213", "Software Engineering",            3.0, "Dr. Chowdhury"),
            new Course("MATH211","Statistics & Probability",        3.0, "Dr. Begum"),
            new Course("CSE214", "Networks Lab",                    1.5, "Dr. Alam"),
            new Course("HUM211", "Sociology",                       2.0, "Mr. Khan")
        ));

        //  Level 3, Term 1
        COURSES.put("L3T1", Arrays.asList(
            new Course("CSE301", "Artificial Intelligence",         3.0, "Dr. Siddiqui"),
            new Course("CSE302", "Compiler Design",                 3.0, "Dr. Noor"),
            new Course("CSE303", "Computer Graphics",               3.0, "Dr. Rahman"),
            new Course("CSE304", "AI Lab",                          1.5, "Dr. Siddiqui"),
            new Course("CSE305", "Theory of Computation",           3.0, "Dr. Islam"),
            new Course("HUM301", "Engineering Ethics",              2.0, "Mr. Karim")
        ));

        //  Level 3, Term 2
        COURSES.put("L3T2", Arrays.asList(
            new Course("CSE311", "Machine Learning",                3.0, "Dr. Alam"),
            new Course("CSE312", "Information Security",            3.0, "Dr. Chowdhury"),
            new Course("CSE313", "Mobile Application Development",  3.0, "Dr. Hossain"),
            new Course("CSE314", "ML Lab",                          1.5, "Dr. Alam"),
            new Course("CSE315", "Distributed Systems",             3.0, "Dr. Noor"),
            new Course("HUM311", "Project Management",              2.0, "Mr. Khan")
        ));

        //  Level 4, Term 1
        COURSES.put("L4T1", Arrays.asList(
            new Course("CSE401", "Thesis/Project Part I",           6.0, "Dr. Rahman"),
            new Course("CSE402", "Cloud Computing",                 3.0, "Dr. Islam"),
            new Course("CSE403", "Big Data Analytics",              3.0, "Dr. Siddiqui"),
            new Course("CSE404", "IoT & Embedded Systems",          3.0, "Dr. Noor")
        ));

        //  Level 4, Term 2
        COURSES.put("L4T2", Arrays.asList(
            new Course("CSE411", "Thesis/Project Part II",          6.0, "Dr. Rahman"),
            new Course("CSE412", "Natural Language Processing",     3.0, "Dr. Alam"),
            new Course("CSE413", "Blockchain Technology",           3.0, "Dr. Chowdhury"),
            new Course("CSE414", "Entrepreneurship in Tech",        2.0, "Mr. Karim")
        ));
    }

    public static List<Course> getCourses(int level, int term) {
        return COURSES.getOrDefault("L" + level + "T" + term, List.of());
    }

    public static final double MAX_CREDIT = 20.0;
}
