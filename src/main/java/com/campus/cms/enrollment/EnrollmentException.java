package com.campus.cms.enrollment;

/** A rule was broken or something failed. The message is safe to show to the user. */
public class EnrollmentException extends Exception {
    public EnrollmentException(String message) {
        super(message);
    }

    public EnrollmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
