package com.campus.cms.grading;

public class Grade {
    private int gradeId;
    private int assignmentId;
    private int studentId;
    private double marks;
    private String feedback;
    private int published; // 0 = Draft, 1 = Published

    public Grade() {}

    public Grade(int gradeId, int assignmentId, int studentId, double marks, String feedback, int published) {
        this.gradeId = gradeId;
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.marks = marks;
        this.feedback = feedback;
        this.published = published;
    }

    // Getters and Setters
    public int getGradeId() { return gradeId; }
    public void setGradeId(int gradeId) { this.gradeId = gradeId; }

    public int getAssignmentId() { return assignmentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public double getMarks() { return marks; }
    public void setMarks(double marks) { this.marks = marks; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public int getPublished() { return published; }
    public void setPublished(int published) { this.published = published; }
}