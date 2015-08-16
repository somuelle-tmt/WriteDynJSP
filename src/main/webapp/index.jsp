<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>WriteDynJSP</title>
    </head>
    <body>
        <h1>WriteDynJSP</h1>
        <p>
            This is a sample project for a servlet which dynamically deploys JSP files from database to web application.
        </p>
        <p>
            It uses a container-provided database connection to select the JSP content, check modification dates, writes the JSP to disk and then forwards to that JSP.
        </p>
        <p>
            For configuration details see sample <code>context.xml</code> and <code>web.xml</code> in this project.
        </p>
        <h2>Notice</h2>
        <p>
            This is a proof-of-concept application which is not intended for production usage.
        </p>
        <p>
            It is also not fully tested with different web-application servers and configurations.
        </p>
        <p>
            Feel free to fork and extend it at <a href="https://github.com/NilsCoding/WriteDynJSP">GitHub</a>
        </p>
    </body>
</html>
