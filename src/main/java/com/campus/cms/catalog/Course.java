package com.campus.cms.catalog;

import java.util.Objects;

/**
 * Model representing a course offered by the campus.
 * facultyName is an optional display field populated by joined DAO queries.
 *
 * The slot field is retained to match the current SQLite schema. It is treated
 * as a course label only; this class does not implement timetable logic.
 */
public class Course {

    public static final String ACTIVE = "ACTIVE";
    public static final String ARCHIVED = "ARCHIVED";

    private final int courseId;
    private final String code;
    private final String title;
    private final int credits;
    private final int capacity;
    private final String semester;
    private final String slot;
    private final Integer facultyId;
    private final String status;
    private final String facultyName;

    public Course(int courseId, String code, String title, int credits,
                  int capacity, String semester, String slot,
                  Integer facultyId, String status) {
        this(courseId, code, title, credits, capacity, semester, slot,
                facultyId, status, null);
    }

    public Course(int courseId, String code, String title, int credits,
                  int capacity, String semester, String slot,
                  Integer facultyId, String status, String facultyName) {
        this.courseId = courseId;
        this.code = code;
        this.title = title;
        this.credits = credits;
        this.capacity = capacity;
        this.semester = semester;
        this.slot = slot;
        this.facultyId = facultyId;
        this.status = status != null ? status : ACTIVE;
        this.facultyName = facultyName;
    }

    public int getCourseId() { return courseId; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public int getCredits() { return credits; }
    public int getCapacity() { return capacity; }
    public String getSemester() { return semester; }
    public String getSlot() { return slot; }
    public Integer getFacultyId() { return facultyId; }
    public String getStatus() { return status; }
    public String getFacultyName() { return facultyName; }

    public boolean isActive() {
        return ACTIVE.equalsIgnoreCase(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return courseId == course.courseId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseId);
    }

    @Override
    public String toString() {
        return "Course{" +
                "courseId=" + courseId +
                ", code='" + code + '\'' +
                ", title='" + title + '\'' +
                ", credits=" + credits +
                ", capacity=" + capacity +
                ", semester='" + semester + '\'' +
                ", slot='" + slot + '\'' +
                ", facultyId=" + facultyId +
                ", status='" + status + '\'' +
                ", facultyName='" + facultyName + '\'' +
                '}';
    }
}
