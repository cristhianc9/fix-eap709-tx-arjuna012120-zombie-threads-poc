package com.example.poc.batch.web;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

@WebServlet("/startBatch")
public class BatchStarterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.println("<html><body>");
        out.println("<h1>Iniciando Batch Job</h1>");

        try {
            JobOperator jobOperator = BatchRuntime.getJobOperator();
            long executionId = jobOperator.start("simpleJob", new Properties());
            out.println("<p>Batch Job 'simpleJob' iniciado. Execution ID: " + executionId + "</p>");
            out.println("<p>Revisa los logs de JBoss EAP para ver el progreso y el comportamiento transaccional.</p>");
        } catch (Exception e) {
            out.println("<p style='color:red;'>Error al iniciar el Batch Job: " + e.getMessage() + "</p>");
            e.printStackTrace(out);
        }

        out.println("</body></html>");
    }
}