package com.campus.cms.grading;

public class Assignment {
    private int assignmentId;
    private int courseId;
    private String title;
    private String type; // 'ASSIGNMENT' or 'QUIZ'
    private String description;
    private String dueDate; // YYYY-MM-DD
    private double maxMarks;
    private double weight;
    private String status; // 'OPEN' or 'CLOSED'
    private String materialPath;

    public Assignment() {}

    public Assignment(int assignmentId, int courseId, String title, String type, 
                      String description, String dueDate, double maxMarks, 
                      double weight, String status, String materialPath) {
        this.assignmentId = assignmentId;
        this.courseId = courseId;
        this.title = title;
        this.type = type;
        this.description = description;
        this.dueDate = dueDate;
        this.maxMarks = maxMarks;
        this.weight = weight;
        this.status = status;
        this.materialPath = materialPath;
    }

    public int getAssignmentId() { return assignmentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public double getMaxMarks() { return maxMarks; }
    public void setMaxMarks(double maxMarks) { this.maxMarks = maxMarks; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMaterialPath() { return materialPath; }
    public void setMaterialPath(String materialPath) { this.materialPath = materialPath; }
}