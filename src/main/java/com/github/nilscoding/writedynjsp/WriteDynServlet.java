package com.github.nilscoding.writedynjsp;

import com.github.nilscoding.writedynjsp.utils.DatabaseUtils;
import com.github.nilscoding.writedynjsp.utils.JspFileEntry;
import com.github.nilscoding.writedynjsp.utils.StringUtils;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
        Connection conn = DatabaseUtils.getDatabaseConnection(cfgDbSourceName);
        
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
            DatabaseUtils.close(conn);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        String requestedFilename = requestUri.substring(tmpPath.length() + 1); // plus 1 to remove / at beginning
        
        // get jsp data from database
        //   statement is expected to return the data in following order with bind parameter on filename
        //   - filename: String
        //   - jsp data: String
        //   - last change: Date
        JspFileEntry jspEntry = DatabaseUtils.readJspFileEntryFromDatabase(conn, cfgDbStatement, requestedFilename);
        DatabaseUtils.close(conn);
        
        if (jspEntry == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // this is the path to the temporary jsp folder
        String completeTempPath = request.getServletContext().getRealPath(cfgTempPath);
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
        request.getRequestDispatcher(targetUri).forward(request, response);
        
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

}
