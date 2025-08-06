package com.example.poc.batch.service;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.SystemException;

@Dependent
public class ClaseBatchFinal {

    @Inject
    private ClaseIntermedia3 claseIntermedia3;

    public void iniciarProceso(String transactionId) throws SystemException {
        System.out.println("ClaseBatchFinal.iniciarProceso() - Iniciando el flujo.");
        claseIntermedia3.delegarProcesamiento(1, transactionId); // Inicia el contador en 1
    }
}