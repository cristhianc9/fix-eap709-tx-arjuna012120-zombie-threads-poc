package com.example.poc.batch;

import com.example.poc.batch.service.ClaseBatchFinal;

import javax.batch.api.AbstractBatchlet;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.SystemException;
import java.util.UUID;

@Dependent
@Named("transactionBatchlet")
public class TransactionBatchlet extends AbstractBatchlet {

    @Inject
    private ClaseBatchFinal claseBatchFinal;

    @Override
    public String process() throws Exception {
        String transactionId = UUID.randomUUID().toString();
        System.out.println("TransactionBatchlet.process() - Iniciando el proceso batch con Transaction ID: " + transactionId);
        try {
            claseBatchFinal.iniciarProceso(transactionId);
            System.out.println("TransactionBatchlet.process() - Proceso batch completado exitosamente.");
            return "COMPLETED";
        } catch (SystemException e) {
            System.err.println("TransactionBatchlet.process() - Error durante el proceso batch: " + e.getMessage());
            return "FAILED";
        }
    }
}