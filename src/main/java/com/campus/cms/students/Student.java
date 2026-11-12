package com.campus.cms.students;

import java.util.Objects;

/**
 * Model representing a Student entity linked to a User account.
 */
public class Student {

    private final int studentId;
    private final int userId;
    private final String rollNumber;
    private final String name;
    private final String branch;
    private final int year;
    private final String email;
    private final String status;
    private final String username;

    public Student(int studentId, int userId, String rollNumber, String name,
                   String branch, int year, String email, String status) {
        this(studentId, userId, rollNumber, name, branch, year, email, status, null);
    }

    public Student(int studentId, int userId, String rollNumber, String name,
                   String branch, int year, String email, String status, String username) {
        this.studentId = studentId;
        this.userId = userId;
        this.rollNumber = rollNumber;
        this.name = name;
        this.branch = branch;
        this.year = year;
        this.email = email;
        this.status = status != null ? status : "ACTIVE";
        this.username = username;
    }

    public int getStudentId() {
        return studentId;
    }

    public int getUserId() {
        return userId;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public String getName() {
        return name;
    }

    public String getBranch() {
        return branch;
    }

    public int getYear() {
        return year;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public String getUsername() {
        return username;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return studentId == student.studentId &&
                userId == student.userId &&
                year == student.year &&
                Objects.equals(rollNumber, student.rollNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, userId, rollNumber, year);
    }

    @Override
    public String toString() {
        return "Student{" +
                "studentId=" + studentId +
                ", userId=" + userId +
                ", rollNumber='" + rollNumber + '\'' +
                ", name='" + name + '\'' +
                ", branch='" + branch + '\'' +
                ", year=" + year +
                ", email='" + email + '\'' +
                ", status='" + status + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
