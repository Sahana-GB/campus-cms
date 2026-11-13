package com.campus.cms.grading;

public class Material {
    private int assignmentId;
    private int courseId;
    private String title;
    private String materialPath;

    public Material() {}

    public Material(int assignmentId, int courseId, String title, String materialPath) {
        this.assignmentId = assignmentId;
        this.courseId = courseId;
        this.title = title;
        this.materialPath = materialPath;
    }

    public static Material fromAssignment(Assignment assignment) {
        return new Material(
                assignment.getAssignmentId(),
                assignment.getCourseId(),
                assignment.getTitle(),
                assignment.getMaterialPath()
        );
    }

    public int getAssignmentId() { return assignmentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMaterialPath() { return materialPath; }
    public void setMaterialPath(String materialPath) { this.materialPath = materialPath; }

    public boolean hasAttachment() {
        return materialPath != null && !materialPath.isBlank();
    }
}
