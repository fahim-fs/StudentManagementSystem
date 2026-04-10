# Student Management System

A comprehensive JavaFX-based desktop application for managing student records, course registrations, attendance tracking, and academic performance evaluation in educational institutions.

![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange)
![Maven](https://img.shields.io/badge/Maven-3.8.5-red)
![License](https://img.shields.io/badge/License-MIT-green)

## 📋 Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Database Setup](#database-setup)
- [Running the Application](#running-the-application)
- [Project Structure](#project-structure)
- [User Roles & Features](#user-roles--features)
- [Screenshots](#screenshots)
- [Default Credentials](#default-credentials)


---

## ✨ Features

### 🔐 Multi-Role Authentication System
- **Student Portal** - Self-registration, course management, attendance tracking
- **Faculty Portal** - Marks entry, attendance management, student lists
- **Admin Portal** - Complete system control and user management

### 📚 Core Functionality

#### For Students:
- ✅ Self-registration with approval workflow
- 📖 Course registration with credit limit validation (20 credits/term)
- 📊 Real-time attendance tracking and visualization
- 📈 View CT marks, quiz marks, and overall performance
- 📢 Access to university notices and announcements
- 💰 Fee payment tracking and due amount monitoring

#### For Faculty:
- ✏️ Attendance entry for enrolled students
- 📝 CT marks entry (Best 3 of 4 counted)
- 📋 Quiz marks entry with custom full marks
- 👥 View enrolled student lists per course
- 📊 Course-wise performance tracking

#### For Administrators:
- ✅ Student registration approval/rejection
- 📋 Course registration approval
- 👨‍🏫 Add and manage faculty members
- 📚 Create and assign courses
- 🔗 Assign courses to faculty
- 📢 Post notices to students and faculty
- 🔍 Search and view detailed student information
- 🔑 Reset user credentials (username/password)

---

## 🛠️ Technology Stack

### Frontend
- **JavaFX 21** - Modern UI framework
- **FXML** - Declarative UI design
- **CSS** - Custom styling and theming

### Backend
- **Java 21** - Core programming language
- **JDBC** - Database connectivity
- **BCrypt** - Password hashing and security

### Database
- **MySQL 8.0** - Relational database management

### Build Tool
- **Maven 3.8.5** - Dependency management and build automation

### Libraries & Dependencies
- `mysql-connector-j` - MySQL database driver
- `jbcrypt` - Password encryption
- `javafx-controls` - UI controls
- `javafx-fxml` - FXML support

---

## 📦 Prerequisites

Before running this application, ensure you have the following installed:

1. **Java Development Kit (JDK) 21 or higher**
   - Download: [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
   - Verify installation: `java -version`

2. **Apache Maven 3.8+**
   - Download: [Maven Official Site](https://maven.apache.org/download.cgi)
   - Verify installation: `mvn -version`

3. **MySQL Server 8.0+**
   - Download: [MySQL Community Server](https://dev.mysql.com/downloads/mysql/)
   - Verify installation: `mysql --version`

4. **IDE (Optional but Recommended)**
   - IntelliJ IDEA, Eclipse, or NetBeans with JavaFX plugin

---

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/student-management-system.git
cd student-management-system
```

### 2. Configure Database Connection

Open `src/backend/database/DatabaseConnection.java` and update the credentials:

```java
private static final String URL      = "jdbc:mysql://localhost:3306/student_management";
private static final String USER     = "your_mysql_username";
private static final String PASSWORD = "your_mysql_password";
```

### 3. Install Dependencies

```bash
mvn clean install
```

---

## 🗄️ Database Setup

### Step 1: Create Database

Open MySQL command line or MySQL Workbench and execute:

```sql
CREATE DATABASE student_management;
USE student_management;
```

### Step 2: Create Tables

Execute the following SQL scripts in order:

#### 1. Users Table (Students, Faculty, Admin)

```sql
CREATE TABLE users (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    admin_id        VARCHAR(10),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    father_name     VARCHAR(100),
    mother_name     VARCHAR(100),
    date_of_birth   DATE,
    phone           VARCHAR(20),
    email           VARCHAR(100) UNIQUE,
    address         TEXT,
    gender          VARCHAR(10),
    session         VARCHAR(20),
    department      VARCHAR(100),
    username        VARCHAR(50) UNIQUE NOT NULL,
    password        VARCHAR(255) NOT NULL,
    level           INT DEFAULT 1,
    term            INT DEFAULT 1,
    role            ENUM('STUDENT', 'FACULTY', 'ADMIN') NOT NULL,
    status          ENUM('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED') DEFAULT 'PENDING',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 2. Courses Table

```sql
CREATE TABLE courses (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(20) UNIQUE NOT NULL,
    name        VARCHAR(200) NOT NULL,
    credit      DOUBLE NOT NULL,
    level       INT NOT NULL,
    term        INT NOT NULL,
    faculty_id  INT,
    faculty     VARCHAR(100),
    FOREIGN KEY (faculty_id) REFERENCES users(id) ON DELETE SET NULL
);
```

#### 3. Course Registrations Table

```sql
CREATE TABLE course_registrations (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    level       INT NOT NULL,
    term        INT NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    course_name VARCHAR(200),
    credit      DOUBLE NOT NULL,
    faculty     VARCHAR(100),
    status      ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_registration (user_id, course_code, level, term),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

#### 4. Attendance Table

```sql
CREATE TABLE attendance (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL,
    course_code     VARCHAR(20) NOT NULL,
    total_held      INT DEFAULT 0,
    total_attended  INT DEFAULT 0,
    UNIQUE KEY uq_attendance (user_id, course_code),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

#### 5. CT Marks Table

```sql
CREATE TABLE ct_marks (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    ct_number   INT NOT NULL,
    marks       DOUBLE NOT NULL,
    entered_by  INT,
    entered_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_ct (user_id, course_code, ct_number),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (entered_by) REFERENCES users(id) ON DELETE SET NULL
);
```

#### 6. Quiz Marks Table

```sql
CREATE TABLE quiz_marks (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    quiz_number INT NOT NULL,
    marks       DOUBLE NOT NULL,
    full_marks  DOUBLE NOT NULL,
    entered_by  INT,
    entered_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_quiz (user_id, course_code, quiz_number),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (entered_by) REFERENCES users(id) ON DELETE SET NULL
);
```

#### 7. Notices Table

```sql
CREATE TABLE notices (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    body        TEXT NOT NULL,
    audience    VARCHAR(50) DEFAULT 'All',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 8. Results Tables (Optional - for future use)

```sql
-- Execute the result_migration.sql file for complete result system
SOURCE /path/to/result_migration.sql;
```

### Step 3: Create Admin User

**Important:** Admin users cannot self-register. You must manually create the first admin:

```java
// Run this code once to create an admin user
// Open src/backend/dao/AdminDAO.java and run the addAdminManually method

public static void main(String[] args) {
    AdminDAO.addAdminManually(
        "Admin",              // First Name
        "User",               // Last Name
        "admin@university.edu", // Email
        "admin123"            // Password (will be hashed)
    );
}
```

Or execute this SQL directly:

```sql
INSERT INTO users 
(admin_id, first_name, last_name, email, username, password, role, status)
VALUES 
('02100', 'Admin', 'User', 'admin@university.edu', 'admin', 
 '$2a$12$yourHashedPasswordHere', 'ADMIN', 'APPROVED');
```

**Note:** Use BCrypt to hash the password. You can use online BCrypt generators or run the Java PasswordUtil class.

---

## ▶️ Running the Application

### Method 1: Using Maven

```bash
mvn clean javafx:run
```

### Method 2: Using IDE

1. Open the project in your IDE (IntelliJ IDEA recommended)
2. Navigate to `src/com/youruniversity/sms/Main.java`
3. Right-click and select "Run Main.main()"

### Method 3: Build JAR and Run

```bash
# Build the JAR
mvn clean package

# Run the JAR
java -jar target/StudentManagementSystem-1.0-SNAPSHOT.jar
```

---

## 📁 Project Structure



---

## 👥 User Roles & Features

### 🎓 Student Role

**Login:** Use username and password after admin approval

**Dashboard Features:**
- 📊 View overall attendance percentage
- 💰 Check due amounts and payment status
- 📢 View new notices
- 📚 See enrolled courses count

**Course Registration:**
- Browse available courses by level and term
- Register for courses (max 20 credits per term)
- Drop pending courses
- View registration status (Pending/Approved)

**Attendance:**
- View overall attendance summary
- Course-wise attendance breakdown
- Visual progress bars for each course
- Regular/Irregular status indicators

**Marks & Results:**
- View CT marks and quiz scores
- Track academic performance
- Download result sheets (coming soon)

---

### 👨‍🏫 Faculty Role

**Login:** Username and password (created by admin)

**Dashboard Features:**
- 👥 Total approved students count
- 📚 Total active courses count
- 📅 Quick access to attendance and marks entry

**Attendance Entry:**
- Select course from dropdown
- Enter classes held and attended for each student
- Real-time percentage calculation
- Attendance status indicators (Regular ≥75%)

**CT Marks Entry:**
- Select course and CT number (1-4)
- Enter marks out of 20
- Automatic Best-3 calculation
- Individual student tracking

**Quiz Marks Entry:**
- Enter custom quiz number and full marks
- Record quiz scores for students
- Flexible marking system

**Student List:**
- View all enrolled students per course
- Student details and status

---

### ⚙️ Admin Role

**Login:** Manually created admin credentials

**Dashboard Overview:**
- 📊 System-wide statistics
- ⏳ Pending approvals counter
- 👥 Student and faculty counts
- 📚 Course management overview

**Student Management:**
- ✅ Approve/Reject new registrations
- 🔍 Search and view student details
- 📋 Course registration approval
- 🔑 Reset student credentials

**Faculty Management:**
- ➕ Add new faculty members
- 👨‍🏫 View faculty list
- ❌ Remove faculty accounts
- 🔑 Reset faculty credentials

**Course Management:**
- ➕ Create new courses
- 📝 Edit course details (code, name, credit, level, term)
- 🔗 Assign courses to faculty
- ❌ Delete courses

**Notice Board:**
- 📢 Post announcements
- 🎯 Target specific audiences (All/Students/Faculty)
- 🗑️ Delete old notices

**Credential Reset:**
- 🔑 Reset any user's password
- ✏️ Update usernames
- 🔍 Search users by name or username

---

## 🖼️ Screenshots

### Login Screen
Modern, gradient-themed login interface with username/password authentication and registration link.

### Student Dashboard
Clean, card-based dashboard showing:
- Overall attendance (with color-coded status)
- Due amount
- Notice count
- Enrolled courses
- Course-wise attendance selector

### Admin Dashboard
Comprehensive admin panel with:
- Summary cards for system statistics
- Quick action buttons
- Approval queues
- Management panels

### Faculty Dashboard
Streamlined interface for:
- Attendance entry tables
- Marks entry forms
- Student lists per course

---

## 🔑 Default Credentials

### Admin
**Username:** `admin`  
**Password:** `admin123`  
*(Change after first login)*

### Test Student (if created)
**Username:** `student1`  
**Password:** `password123`

### Test Faculty (if created)
**Username:** `faculty1`  
**Password:** `password123`

**Security Note:** Always change default passwords in production environments.

---

## 🎨 UI Design Principles

### Color Scheme
- **Primary:** `#2a9d8f` (Teal)
- **Success:** `#27ae60` (Green)
- **Danger:** `#e74c3c` (Red)
- **Warning:** `#e67e22` (Orange)
- **Info:** `#3498db` (Blue)
- **Dark:** `#2c3e50`
- **Background:** Gradient `#e0f7fa` to `#b2ebf2`

### Styling Features
- Rounded corners (8-12px border-radius)
- Soft shadows for depth
- Hover effects on interactive elements
- Consistent spacing and padding
- Responsive color indicators for status

---

## 🔧 Configuration

### Database Configuration
Edit `src/backend/database/DatabaseConnection.java`:

```java
private static final String URL      = "jdbc:mysql://localhost:3306/student_management";
private static final String USER     = "root";
private static final String PASSWORD = "your_password";
```

### Window Settings
Edit `src/com/youruniversity/sms/Main.java`:

```java
primaryStage.setTitle("Student Management System");
primaryStage.setResizable(false); // Set to true for resizable window
```

---

## 🐛 Troubleshooting

### Common Issues

**1. "Unable to connect to database"**
- Check MySQL server is running
- Verify database credentials in `DatabaseConnection.java`
- Ensure database `student_management` exists

**2. "JavaFX runtime components are missing"**
- Ensure JDK 21 with JavaFX is installed
- Run with: `mvn clean javafx:run`

**3. "No suitable driver found for jdbc:mysql"**
- Check `mysql-connector-j` dependency in `pom.xml`
- Run: `mvn clean install`

**4. "Scene is null" or "Location is not set"**
- Verify FXML file paths in controllers
- Check resource folder structure

**5. "Admin cannot login"**
- Verify admin user exists in database
- Check password is properly hashed with BCrypt

---

## 👨‍💻 Authors

**Auritro**
- GitHub: [@yourusername](https://github.com/yourusername)

---

## 🙏 Acknowledgments

- JavaFX community for excellent documentation
- MySQL for robust database management
- BCrypt for secure password hashing
- Maven for dependency management

---


<div align="center">


[⬆ Back to Top](#student-management-system)

</div>
