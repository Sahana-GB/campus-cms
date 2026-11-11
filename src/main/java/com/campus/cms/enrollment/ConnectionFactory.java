package com.campus.cms.enrollment;

import java.sql.Connection;
import java.sql.SQLException;

/** Opens a fresh database connection. Lets tests plug in a temporary database. */
@FunctionalInterface
public interface ConnectionFactory {
    Connection open() throws SQLException;
}
