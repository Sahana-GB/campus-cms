package com.campus.cms.grading;

import java.util.List;

public class GradeService {
    private final AssignmentDAO assignmentDAO = new AssignmentDAO();
    private final GradeDAO gradeDAO = new GradeDAO();

    public boolean createAssignment(Assignment assignment) {
        return assignmentDAO.createAssignment(assignment);
    }

    public List<Assignment> getAssignmentsByCourse(int courseId) {
        return assignmentDAO.getAssignmentsByCourse(courseId);
    }

    public List<Grade> getGradesByAssignment(int assignmentId) {
        return gradeDAO.getGradesForAssignment(assignmentId);
    }

    public boolean saveOrUpdateGrade(Grade grade) {
        return gradeDAO.saveOrUpdateGrade(grade);
    }

    public boolean publishGrades(int assignmentId, boolean published) {
        if (published) {
            return gradeDAO.publishGrades(assignmentId);
        }
        return false;
    }
}
