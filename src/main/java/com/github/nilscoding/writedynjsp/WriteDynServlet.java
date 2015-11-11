package com.github.nilscoding.writedynjsp;

import com.github.nilscoding.writedynjsp.utils.DatabaseUtils;
import com.github.nilscoding.writedynjsp.utils.JspFileEntry;
import com.github.nilscoding.writedynjsp.utils.StringUtils;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 * Servlet to write JSP from database to filesystem and forward request
 * @author NilsCoding
 */
public class WriteDynServlet extends HttpServlet {
    private static final long serialVersionUID = 6772931418775372910L;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String cfgDbSourceName = this.getInitParameter("db.source");
        String cfgDbStatement = this.getInitParameter("db.sql");
        String cfgTempPath = this.getInitParameter("jsp.temppath");
        if (StringUtils.isEmpty(cfgDbSourceName) || StringUtils.isEmpty(cfgDbStatement) || StringUtils.isEmpty(cfgTempPath)) {
            // send 404 if database settings are missing
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String cfgReqPathMapping = this.getInitParameter("req.pattern");
        Connection conn = getDatabaseConnection(cfgDbSourceName);
        
        if (conn == null) {
            // send 404 if no database connection is available
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // http path information
        String contextPath = request.getContextPath();
        String servletPath = request.getServletPath();
        String requestUri = request.getRequestURI();
        
        // get requested file name
        String tmpPath = contextPath + servletPath;
        if (requestUri.startsWith(tmpPath) == false) {
            // something went wrong with the path
            close(conn);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        String pageIncludedAs = (String)request.getAttribute("page_included_as");
        String requestedFilename = null;
        if (StringUtils.isEmpty(pageIncludedAs, true) == false) {
            //System.out.println("included as: " + pageIncludedAs);
            requestedFilename = pageIncludedAs;
        } else if (requestUri.length() >= tmpPath.length() + 1) {
            requestedFilename = requestUri.substring(tmpPath.length() + 1); // plus 1 to remove / at beginning
            //System.out.println("via request uri: " + requestedFilename);
        } else {
            close(conn);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // get jsp data from database
        //   statement is expected to return the data in following order with bind parameter on filename
        //   - filename: String
        //   - jsp data: String
        //   - last change: Date
        if (StringUtils.isEmpty(cfgReqPathMapping, true) == false) {
            String tmpReqFilename = StringUtils.findRegexGroup(requestedFilename, cfgReqPathMapping, 1);
            if (StringUtils.isEmpty(tmpReqFilename, true) == false) {
                requestedFilename = tmpReqFilename;
            }
        }
        JspFileEntry jspEntry = readJspFileEntryFromDatabase(conn, cfgDbStatement, requestedFilename);
        close(conn);
        
        if (jspEntry == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // this is the path to the temporary jsp folder
        String completeTempPath = this.getServletContext().getRealPath(cfgTempPath);
        if (StringUtils.isEmpty(completeTempPath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (completeTempPath.endsWith(File.separator) == false) {
            completeTempPath = completeTempPath + File.separator;
        }
        
        String jspFilename = requestedFilename;
        if (jspFilename.endsWith(".jsp") == false) {
            // add .jsp extension if not already present
            jspFilename = jspFilename + ".jsp";
        }
        // this is the complete path to the temporary jsp file on disk
        String jspFullFilename = completeTempPath + jspFilename;
        
        boolean needWrite = false;
        File jspFileObj = new File(jspFullFilename);
        if (jspFileObj.exists() == false) {
            // check if file exists
            needWrite = true;
        } else if (jspEntry.getFiledate() == null) {
            // check if file date from database is set
            needWrite = true;
        } else if (jspFileObj.lastModified() <= jspEntry.getFiledate().getTime()) {
            // check if file date from database is newer than date of present file
            needWrite = true;
        }
        
        if (needWrite == true) {
            // if the file needs to be written, then write it locally
            try {
                // make sure that the directory exists
                File parentDir = jspFileObj.getParentFile();
                if ((parentDir != null) && (parentDir.exists() == false)) {
                    parentDir.mkdirs();
                }
                // write data
                PrintWriter fileWriter = new PrintWriter(jspFileObj);
                fileWriter.write(jspEntry.getFiledata());
                fileWriter.flush();
                fileWriter.close();
            } catch (Exception ex) {
            }
        }
        
        // forward internally to jsp file
        String targetUri = "/" + cfgTempPath + jspFilename;
//        request.getRequestDispatcher(targetUri).forward(request, response);
        request.getRequestDispatcher(targetUri).include(request, response);
        
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. ">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "WriteDynServlet";
    }// </editor-fold>

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
