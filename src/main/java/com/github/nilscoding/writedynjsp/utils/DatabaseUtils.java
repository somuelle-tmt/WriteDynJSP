package com.github.nilscoding.writedynjsp.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Database utils
 * @author NilsCoding
 */
public class DatabaseUtils {
    
    private DatabaseUtils() { }
    
    /**
     * Returns a named JDBC connection from container
     * @param name  connection name, e.g. "jdbc/MyDatabase"
     * @return  database connection or null on error
     */
    public static Connection getDatabaseConnection(String name) {
        try {
            Context initContext = new InitialContext();
            Context envContext = (Context) initContext.lookup("java:comp/env");
            DataSource ds = (DataSource) envContext.lookup(name);
            Connection conn = ds.getConnection();
            return conn;
        } catch (Exception ex) {
            return null;
        }
    }
    
    /**
     * Silently closes a statement
     * @param stmt  statement to close
     */
    public static void close(Statement stmt) {
        if (stmt == null) {
            return;
        }
        try {
            stmt.close();
        } catch (Exception ex) {
        }
    }

    /**
     * Silently closes a result set
     * @param rs    result set to close
     */
    public static void close(ResultSet rs) {
        if (rs == null) {
            return;
        }
        try {
            rs.close();
        } catch (Exception ex) {
        }
    }
    
    /**
     * Silently closes a connection
     * @param conn  connection to close
     */
    public static void close(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.close();
        } catch (Exception ex) {
        }
    }
    
    /**
     * Reads the JSP file entry from database
     * @param conn  database connection
     * @param stmt  select statement
     * @param filename  filename
     * @return jsp entry or null if not found
     */
    public static JspFileEntry readJspFileEntryFromDatabase(Connection conn, String stmt, String filename) {
        JspFileEntry jspEntry = null;
        PreparedStatement pStmt = null;
        ResultSet rs = null;
        try {
            pStmt = conn.prepareStatement(stmt);
            pStmt.setString(1, filename);
            rs = pStmt.executeQuery();
            if (rs.next()) {
                jspEntry = new JspFileEntry();
                jspEntry.setFilename(rs.getString(1));
                jspEntry.setFiledata(rs.getString(2));
                Object dateObj = rs.getObject(3);
                if (dateObj instanceof java.sql.Timestamp) {
                    jspEntry.setFiledate(new Date(((java.sql.Timestamp)dateObj).getTime()));
                } else if (dateObj instanceof Number) {
                    jspEntry.setFiledate(new Date(((Number)dateObj).longValue()));
                } else {
                    jspEntry.setFiledate(new Date());
                }
            }
        } catch (Exception ex) {
            jspEntry = null;
        }
        close(pStmt);
        close(rs);
        return jspEntry;
    }
    
}
