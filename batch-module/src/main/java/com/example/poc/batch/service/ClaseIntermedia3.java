package com.example.poc.batch.service;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.SystemException;

@Dependent
public class ClaseIntermedia3 {

    @Inject
    private ClaseIntermedia2 claseIntermedia2;

    public void delegarProcesamiento(int counter, String transactionId) throws SystemException {
        System.out.println("ClaseIntermedia3.delegarProcesamiento() - Llamando a ClaseIntermedia2.");
        claseIntermedia2.procesar(counter, transactionId);
    }
}