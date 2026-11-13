package com.campus.cms.catalog;

import java.util.Objects;

/**
 * Model representing a Faculty member linked to a User login account.
 */
public class Faculty {

    private final int facultyId;
    private final int userId;
    private final String name;
    private final String department;
    private final String email;
    private final String status;
    private final String username;

    public Faculty(int facultyId, int userId, String name, String department,
                   String email, String status) {
        this(facultyId, userId, name, department, email, status, null);
    }

    public Faculty(int facultyId, int userId, String name, String department,
                   String email, String status, String username) {
        this.facultyId = facultyId;
        this.userId = userId;
        this.name = name;
        this.department = department;
        this.email = email;
        this.status = status != null ? status : "ACTIVE";
        this.username = username;
    }

    public int getFacultyId() { return facultyId; }
    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
    public String getUsername() { return username; }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Faculty faculty = (Faculty) o;
        return facultyId == faculty.facultyId && userId == faculty.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(facultyId, userId);
    }

    @Override
    public String toString() {
        return "Faculty{" +
                "facultyId=" + facultyId +
                ", userId=" + userId +
                ", name='" + name + '\'' +
                ", department='" + department + '\'' +
                ", email='" + email + '\'' +
                ", status='" + status + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
