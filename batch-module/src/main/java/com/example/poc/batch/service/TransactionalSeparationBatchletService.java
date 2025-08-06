package com.example.poc.batch.service;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.UUID;

/**
 * Alternativa B: Implementa separación transaccional real usando un bean
 * CDI @ApplicationScoped.
 * El batchlet invoca un bean intermediario que a su vez llama al EJB con
 * REQUIRES_NEW,
 * asegurando que la transacción REQUIRES_NEW esté completamente separada de la
 * transacción principal.
 * Permite que los commits previos se mantengan aunque ocurra un error
 * posterior.
 */
@Dependent
public class TransactionalSeparationBatchletService {

    @Inject
    private RequiresNewDelegatorBean requiresNewDelegatorBean;

    public String ejecutarProcesoBatch() {
        String transactionId = UUID.randomUUID().toString();
        System.out.println("TransactionalSeparationBatchletService - Iniciando proceso batch con Transaction ID: "
                + transactionId);
        try {
            // Simulación de varias operaciones con separación transaccional
            for (int i = 1; i <= 3; i++) {
                try {
                    requiresNewDelegatorBean.ejecutarMetodoA(i, transactionId);
                    System.out.println("TransactionalSeparationBatchletService - Operación " + i + " confirmada.");
                } catch (Exception e) {
                    System.err.println("TransactionalSeparationBatchletService - Error controlado en operación " + i
                            + ": " + e.getMessage());
                    // Aquí se podría guardar el estado para reintento
                    break;
                }
            }
            return "COMPLETED";
        } catch (Exception e) {
            System.err.println("TransactionalSeparationBatchletService - Error inesperado: " + e.getMessage());
            return "FAILED";
        }
    }
}
