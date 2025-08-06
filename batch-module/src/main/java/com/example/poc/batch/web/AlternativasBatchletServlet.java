package com.example.poc.batch.web;

import com.example.poc.batch.service.ManualErrorHandlingBatchletService;
import com.example.poc.batch.service.TransactionalSeparationBatchletService;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Servlet para probar y comparar las alternativas de manejo transaccional en el
 * batchlet.
 * Permite invocar el escenario original, la alternativa A (control de errores
 * manual)
 * y la alternativa B (separación transaccional real con bean CDI).
 *
 * Endpoints:
 * /alternativas-batchlet/original - Ejecuta el flujo original (con error)
 * /alternativas-batchlet/alternativaA - Ejecuta el flujo con control de errores
 * manual
 * /alternativas-batchlet/alternativaB - Ejecuta el flujo con separación
 * transaccional
 */
@WebServlet(urlPatterns = { "/alternativas-batchlet/original", "/alternativas-batchlet/alternativaA",
        "/alternativas-batchlet/alternativaB" })
public class AlternativasBatchletServlet extends HttpServlet {

    @Inject
    private ManualErrorHandlingBatchletService manualErrorHandlingBatchletService;

    @Inject
    private TransactionalSeparationBatchletService transactionalSeparationBatchletService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        String resultado;
        switch (path) {
            case "/alternativas-batchlet/original":
                try {
                    javax.batch.operations.JobOperator jobOperator = javax.batch.runtime.BatchRuntime.getJobOperator();
                    long executionId = jobOperator.start("simpleJob", new java.util.Properties());
                    resultado = "Batch Job 'simpleJob' iniciado. Execution ID: " + executionId
                            + "\nRevisa los logs de JBoss EAP para ver el progreso y el comportamiento transaccional.";
                } catch (Exception e) {
                    resultado = "Error al iniciar el Batch Job: " + e.getMessage();
                }
                break;
            case "/alternativas-batchlet/alternativaA":
                resultado = manualErrorHandlingBatchletService.ejecutarProcesoBatch();
                break;
            case "/alternativas-batchlet/alternativaB":
                resultado = transactionalSeparationBatchletService.ejecutarProcesoBatch();
                break;
            default:
                resultado = "Ruta no reconocida.";
        }
        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
        out.println("Resultado: " + resultado);
    }
}
