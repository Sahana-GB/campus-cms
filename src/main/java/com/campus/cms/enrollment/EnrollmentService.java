package com.campus.cms.enrollment;

import com.campus.cms.core.Database;
import com.campus.cms.core.Role;
import com.campus.cms.core.Session;
import com.campus.cms.enrollment.EnrollmentDAO.BrowseRow;
import com.campus.cms.enrollment.EnrollmentDAO.CourseInfo;
import com.campus.cms.enrollment.EnrollmentDAO.CourseOption;
import com.campus.cms.reports.ReportTable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * All enrollment rules. Screens call this class and never touch SQL.
 *
 * Every method checks who is calling, so a hidden button is never the only protection.
 * Rule violations are thrown as EnrollmentException with a message that is safe to show.
 */
public class EnrollmentService {

    /** Result of Auto-allocate: how many were approved and how many are still waiting. */
    public record AllocationResult(int approved, int stillPending) { }

    private final ConnectionFactory factory;
    private final EnrollmentDAO dao = new EnrollmentDAO();
    private final CgpaCalculator cgpaCalculator = new CgpaCalculator();

    public EnrollmentService() {
        this(Database::getConnection);
    }

    public EnrollmentService(ConnectionFactory factory) {
        this.factory = factory;
    }

    // ---- The only two lines that depend on Person A's Session. Change them if its API differs. ----
    protected Role currentRole() {
        return Session.getRole();
    }

    protected int currentStudentId() {
        return Session.getStudentId();
    }
    // -------------------------------------------------------------------------------------------------

    // =========================================================== student actions

    /** Student asks to join a course. Returns the new status (PENDING). */
    public String requestEnrollment(int studentId, int courseId) throws EnrollmentException {
        requireStudent(studentId);
        try (Connection c = factory.open()) {
            if (!"ACTIVE".equals(dao.studentStatus(c, studentId))) {
                throw new EnrollmentException("Your account is not active.");
            }
            CourseInfo course = dao.findCourse(c, courseId);
            if (course == null) {
                throw new EnrollmentException("Course not found.");
            }
            if (!"ACTIVE".equals(course.status())) {
                throw new EnrollmentException("This course is archived and not open for enrollment.");
            }

            Enrollment existing = dao.find(c, studentId, courseId);
            if (existing == null) {
                try {
                    dao.insertPending(c, studentId, courseId);
                } catch (SQLException e) {
                    if (isUniqueViolation(e)) {
                        throw new EnrollmentException("You have already requested this course.");
                    }
                    throw e;
                }
                return Enrollment.PENDING;
            }

            String status = existing.getStatus();
            if (Enrollment.DROPPED.equals(status)) {
                dao.reopen(c, existing.getEnrollmentId());
                return Enrollment.PENDING;
            }
            if (Enrollment.PENDING.equals(status)) {
                throw new EnrollmentException("Your request for this course is already pending.");
            }
            if (Enrollment.APPROVED.equals(status)) {
                throw new EnrollmentException("You are already enrolled in this course.");
            }
            if (Enrollment.REJECTED.equals(status)) {
                throw new EnrollmentException(
                        "Your earlier request for this course was rejected. Please contact the admin.");
            }
            throw new EnrollmentException("You have already completed this course.");
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public List<BrowseRow> browseCourses(int studentId) throws EnrollmentException {
        requireStudent(studentId);
        try (Connection c = factory.open()) {
            return dao.findBrowseRows(c, studentId);
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public List<Enrollment> myEnrollments(int studentId) throws EnrollmentException {
        requireStudent(studentId);
        try (Connection c = factory.open()) {
            return dao.findByStudent(c, studentId);
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    /** The student's APPROVED courses ordered by slot (slots are labels, not clock times). */
    public List<Enrollment> myTimetable(int studentId) throws EnrollmentException {
        List<Enrollment> approved = new ArrayList<>();
        for (Enrollment e : myEnrollments(studentId)) {
            if (Enrollment.APPROVED.equals(e.getStatus())) {
                approved.add(e);
            }
        }
        approved.sort(Comparator.comparing(Enrollment::getSlot).thenComparing(Enrollment::getCourseCode));
        return approved;
    }

    /**
     * Drop a PENDING or APPROVED enrollment. A student can drop only their own;
     * an admin can drop any (that is the admin "Remove" button). The row is kept as DROPPED.
     */
    public void drop(int enrollmentId) throws EnrollmentException {
        Role role = currentRole();
        if (role != Role.STUDENT && role != Role.ADMIN) {
            throw new EnrollmentException("You are not allowed to do this.");
        }
        try (Connection c = factory.open()) {
            Enrollment e = dao.findById(c, enrollmentId);
            if (e == null) {
                throw new EnrollmentException("Enrollment not found.");
            }
            if (role == Role.STUDENT && e.getStudentId() != currentStudentId()) {
                throw new EnrollmentException("You can only drop your own enrollments.");
            }
            if (!Enrollment.PENDING.equals(e.getStatus()) && !Enrollment.APPROVED.equals(e.getStatus())) {
                throw new EnrollmentException(
                        "Only pending or approved enrollments can be dropped (this one is " + e.getStatus() + ").");
            }
            dao.updateStatus(c, enrollmentId, Enrollment.DROPPED);
        } catch (SQLException ex) {
            throw wrap(ex);
        }
    }

    // ============================================================ admin actions

    public void approve(int enrollmentId) throws EnrollmentException {
        requireAdmin();
        try (Connection c = factory.open()) {
            if (dao.approveIfSeatAvailable(c, enrollmentId)) {
                return;
            }
            // It did not go through: work out why, so the admin sees a clear message.
            Enrollment e = dao.findById(c, enrollmentId);
            if (e == null) {
                throw new EnrollmentException("Enrollment not found.");
            }
            if (!Enrollment.PENDING.equals(e.getStatus())) {
                throw new EnrollmentException(
                        "Only PENDING requests can be approved (this one is " + e.getStatus() + ").");
            }
            CourseInfo course = dao.findCourse(c, e.getCourseId());
            if (course == null || !"ACTIVE".equals(course.status())) {
                throw new EnrollmentException("This course is archived.");
            }
            if (!"ACTIVE".equals(dao.studentStatus(c, e.getStudentId()))) {
                throw new EnrollmentException("This student is inactive.");
            }
            throw new EnrollmentException("Course is full (" + course.capacity() + " seats).");
        } catch (SQLException ex) {
            throw wrap(ex);
        }
    }

    public void reject(int enrollmentId) throws EnrollmentException {
        requireAdmin();
        try (Connection c = factory.open()) {
            Enrollment e = dao.findById(c, enrollmentId);
            if (e == null) {
                throw new EnrollmentException("Enrollment not found.");
            }
            if (!Enrollment.PENDING.equals(e.getStatus())) {
                throw new EnrollmentException(
                        "Only PENDING requests can be rejected (this one is " + e.getStatus() + ").");
            }
            dao.updateStatus(c, enrollmentId, Enrollment.REJECTED);
        } catch (SQLException ex) {
            throw wrap(ex);
        }
    }

    /**
     * Fill the free seats of one course from its PENDING requests, highest CGPA first
     * (ties broken by roll number). Requests that do not fit stay PENDING.
     */
    public AllocationResult autoAllocate(int courseId) throws EnrollmentException {
        requireAdmin();
        try (Connection c = factory.open()) {
            CourseInfo course = dao.findCourse(c, courseId);
            if (course == null) {
                throw new EnrollmentException("Course not found.");
            }
            if (!"ACTIVE".equals(course.status())) {
                throw new EnrollmentException("This course is archived.");
            }
            List<Enrollment> pending = dao.search(c, courseId, Enrollment.PENDING);
            for (Enrollment p : pending) {
                p.setCgpa(cgpaCalculator.compute(c, p.getStudentId()));
            }
            pending.sort(Comparator.<Enrollment>comparingDouble(Enrollment::getCgpa).reversed()
                    .thenComparing(Enrollment::getRollNumber));

            int approved = 0;
            for (Enrollment p : pending) {
                if (dao.approveIfSeatAvailable(c, p.getEnrollmentId())) {
                    approved++;
                } else if (dao.countApproved(c, courseId) >= course.capacity()) {
                    break;   // course is full, nobody else can be approved
                }
            }
            return new AllocationResult(approved, pending.size() - approved);
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    /** Admin table. A null courseId or status means "any". With withCgpa, each row gets its CGPA filled in. */
    public List<Enrollment> listEnrollments(Integer courseId, String status, boolean withCgpa)
            throws EnrollmentException {
        requireAdmin();
        try (Connection c = factory.open()) {
            List<Enrollment> rows = dao.search(c, courseId, status);
            if (withCgpa) {
                for (Enrollment e : rows) {
                    e.setCgpa(cgpaCalculator.compute(c, e.getStudentId()));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public List<CourseOption> courseOptions() throws EnrollmentException {
        requireAdmin();
        try (Connection c = factory.open()) {
            return dao.findCourseOptions(c);
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    // ================================================= small helpers for Person A's dashboards

    /** Seats still free in a course: capacity minus APPROVED enrollments. */
    public int seatsLeft(int courseId) throws EnrollmentException {
        try (Connection c = factory.open()) {
            CourseInfo course = dao.findCourse(c, courseId);
            if (course == null) {
                throw new EnrollmentException("Course not found.");
            }
            return Math.max(0, course.capacity() - dao.countApproved(c, courseId));
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    /** Admin card: number of enrollments with this status (e.g. APPROVED, PENDING). */
    public int countByStatus(String status) throws EnrollmentException {
        requireAdmin();
        try (Connection c = factory.open()) {
            return dao.countByStatus(c, status);
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    /** Student card: how many of my enrollments have this status. */
    public int countMine(int studentId, String status) throws EnrollmentException {
        requireStudent(studentId);
        try (Connection c = factory.open()) {
            return dao.countForStudent(c, studentId, status);
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    /** CGPA (0 to 10) from published grades. A student sees only their own; an admin can see any. */
    public double cgpa(int studentId) throws EnrollmentException {
        if (currentRole() != Role.ADMIN) {
            requireStudent(studentId);
        }
        try (Connection c = factory.open()) {
            return cgpaCalculator.compute(c, studentId);
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    // ===================================================================== reports

    public ReportTable enrollmentCountsReport() throws EnrollmentException {
        requireAdmin();
        try (Connection c = factory.open()) {
            return dao.enrollmentCountsReport(c);
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public ReportTable courseRosterReport(int courseId) throws EnrollmentException {
        requireAdmin();
        try (Connection c = factory.open()) {
            CourseInfo course = dao.findCourse(c, courseId);
            if (course == null) {
                throw new EnrollmentException("Course not found.");
            }
            return dao.courseRosterReport(c, courseId, "Course roster: " + course.code());
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public ReportTable studentHistoryReport(int studentId) throws EnrollmentException {
        if (currentRole() != Role.ADMIN) {
            requireStudent(studentId);
        }
        try (Connection c = factory.open()) {
            return dao.studentHistoryReport(c, studentId);
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    // ===================================================================== checks

    private void requireAdmin() throws EnrollmentException {
        if (currentRole() != Role.ADMIN) {
            throw new EnrollmentException("Only an admin can do this.");
        }
    }

    private void requireStudent(int studentId) throws EnrollmentException {
        if (currentRole() != Role.STUDENT || currentStudentId() != studentId) {
            throw new EnrollmentException("You can only manage your own enrollments.");
        }
    }

    private static boolean isUniqueViolation(SQLException e) {
        String m = e.getMessage();
        return m != null && m.contains("UNIQUE");
    }

    private static EnrollmentException wrap(SQLException e) {
        return new EnrollmentException("Database error: " + e.getMessage(), e);
    }
}
