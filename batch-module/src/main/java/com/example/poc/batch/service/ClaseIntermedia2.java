package com.example.poc.batch.service;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.SystemException;

@Dependent
public class ClaseIntermedia2 {

    @Inject
    private ClaseIntermedia1 claseIntermedia1;

    public void procesar(int counter, String transactionId) throws SystemException {
        System.out.println("ClaseIntermedia2.procesar() - Llamando a ClaseIntermedia1.");
        claseIntermedia1.ejecutarMetodoA(counter, transactionId);
    }
}