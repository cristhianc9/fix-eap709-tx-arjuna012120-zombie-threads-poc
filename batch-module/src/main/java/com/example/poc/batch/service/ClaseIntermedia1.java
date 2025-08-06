package com.example.poc.batch.service;

import com.example.poc.ejb.ExternalService;

import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import javax.transaction.SystemException;

@Dependent
public class ClaseIntermedia1 {

    @EJB
    private ExternalService externalService;

    public void ejecutarMetodoA(int counter, String transactionId) throws SystemException {
        System.out.println("ClaseIntermedia1.ejecutarMetodoA() - Llamando a metodoA() del EJB.");
        externalService.metodoA(counter, transactionId);
    }
}