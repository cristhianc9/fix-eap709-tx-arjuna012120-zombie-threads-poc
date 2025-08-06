package com.example.poc.batch.service;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.UUID;

/**
 * Alternativa A: Implementa el control de errores manual en el batchlet.
 * Captura y maneja localmente cualquier excepción lanzada por los métodos con
 * REQUIRES_NEW,
 * evitando que la excepción se propague y cause un rollback global.
 * Permite guardar el estado y reintentar solo la operación fallida.
 */
@Dependent
public class ManualErrorHandlingBatchletService {

    @Inject
    private ClaseBatchFinal claseBatchFinal;

    public String ejecutarProcesoBatch() {
        String transactionId = UUID.randomUUID().toString();
        System.out.println(
                "ManualErrorHandlingBatchletService - Iniciando proceso batch con Transaction ID: " + transactionId);
        try {
            claseBatchFinal.iniciarProceso(transactionId);
            System.out.println("ManualErrorHandlingBatchletService - Proceso batch completado exitosamente.");
            return "COMPLETED";
        } catch (Exception e) {
            System.err.println("ManualErrorHandlingBatchletService - Error controlado: " + e.getMessage());
            // Aquí se podría guardar el estado para reintento
            return "FAILED";
        }
    }
}
