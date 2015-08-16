package com.github.nilscoding.writedynjsp.utils;

import java.io.Serializable;
import java.util.Date;

/**
 * JSP file entry (from database)
 * @author NilsCoding
 */
public class JspFileEntry implements Serializable {
    private static final long serialVersionUID = -9106719409280452182L;
    
    protected String filename;
    protected String filedata;
    protected Date filedate;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFiledata() {
        return filedata;
    }

    public void setFiledata(String filedata) {
        this.filedata = filedata;
    }

    public Date getFiledate() {
        return filedate;
    }

    public void setFiledate(Date filedate) {
        this.filedate = filedate;
    }
    
}
