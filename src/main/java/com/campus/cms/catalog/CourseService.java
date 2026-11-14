package com.campus.cms.catalog;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Application-level operations for the course catalogue.
 *
 * The DAO remains responsible for persistence and database constraints. This
 * service is the boundary for UI code and provides one place to add catalogue
 * business rules without putting them into Swing components. Timetable and
 * prerequisite rules are intentionally not implemented.
 */
public class CourseService {

    private final CourseDAO courseDAO;

    public CourseService() {
        this(new CourseDAO());
    }

    public CourseService(CourseDAO courseDAO) {
        this.courseDAO = Objects.requireNonNull(courseDAO, "courseDAO");
    }

    public Course createCourse(String code, String title, int credits, int capacity,
                               String semester, String slot, Integer facultyId) throws SQLException {
        return courseDAO.createCourse(code, title, credits, capacity, semester, slot, facultyId);
    }

    public Optional<Course> findById(int courseId) throws SQLException {
        return courseDAO.findById(courseId);
    }

    public Optional<Course> findByCode(String code) throws SQLException {
        return courseDAO.findByCode(code);
    }

    public List<Course> findAll() throws SQLException {
        return courseDAO.findAll();
    }

    public List<Course> search(String query, String status, Integer facultyId) throws SQLException {
        return courseDAO.search(query, status, facultyId);
    }

    public boolean update(int courseId, String code, String title, int credits, int capacity,
                          String semester, String slot, Integer facultyId) throws SQLException {
        return courseDAO.update(courseId, code, title, credits, capacity, semester, slot, facultyId);
    }

    public boolean assignFaculty(int courseId, Integer facultyId) throws SQLException {
        return courseDAO.assignFaculty(courseId, facultyId);
    }

    public boolean archive(int courseId) throws SQLException {
        return courseDAO.archive(courseId);
    }

    public boolean activate(int courseId) throws SQLException {
        return courseDAO.activate(courseId);
    }
}
