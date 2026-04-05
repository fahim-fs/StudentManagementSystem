package common;

import java.time.LocalDate;

/**
 * Intermediate model class — Registration form data is packed into this object
 * and passed to the backend UserService for database storage.
 */
public class User {

    private String    firstName;
    private String    lastName;
    private String    fatherName;
    private String    motherName;
    private LocalDate dateOfBirth;
    private String    phoneNumber;
    private String    address;
    private String    gender;
    private String    session;
    private String    department;
    private String    username;
    private String    password;   // backend should hash this before storing

    //  Constructor
    public User(String firstName, String lastName, String fatherName, String motherName,
                LocalDate dateOfBirth, String phoneNumber, String address, String gender,
                String session, String department, String username, String password) {
        this.firstName   = firstName;
        this.lastName    = lastName;
        this.fatherName  = fatherName;
        this.motherName  = motherName;
        this.dateOfBirth = dateOfBirth;
        this.phoneNumber = phoneNumber;
        this.address     = address;
        this.gender      = gender;
        this.session     = session;
        this.department  = department;
        this.username    = username;
        this.password    = password;
    }

    // Getters
    public String    getFirstName()   { return firstName;   }
    public String    getLastName()    { return lastName;    }
    public String    getFatherName()  { return fatherName;  }
    public String    getMotherName()  { return motherName;  }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String    getPhoneNumber() { return phoneNumber; }
    public String    getAddress()     { return address;     }
    public String    getGender()      { return gender;      }
    public String    getSession()     { return session;     }
    public String    getDepartment()  { return department;  }
    public String    getUsername()    { return username;    }
    public String    getPassword()    { return password;    }

    //  Setters (optional, backend may need these)
    public void setFirstName(String firstName)     { this.firstName   = firstName;   }
    public void setLastName(String lastName)       { this.lastName    = lastName;    }
    public void setFatherName(String fatherName)   { this.fatherName  = fatherName;  }
    public void setMotherName(String motherName)   { this.motherName  = motherName;  }
    public void setDateOfBirth(LocalDate dob)      { this.dateOfBirth = dob;         }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setAddress(String address)         { this.address     = address;     }
    public void setGender(String gender)           { this.gender      = gender;      }
    public void setSession(String session)         { this.session     = session;     }
    public void setDepartment(String department)   { this.department  = department;  }
    public void setUsername(String username)       { this.username    = username;    }
    public void setPassword(String password)       { this.password    = password;    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + firstName + " " + lastName + '\'' +
                ", username='" + username + '\'' +
                ", department='" + department + '\'' +
                ", session='" + session + '\'' +
                '}';
    }
}
