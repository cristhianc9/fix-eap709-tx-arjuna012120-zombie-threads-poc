package com.example.poc.batch.service;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;

import com.example.poc.ejb.ExternalService;

/**
 * Bean CDI @ApplicationScoped que actúa como intermediario para invocar métodos
 * con REQUIRES_NEW.
 * Permite lograr separación transaccional real entre el batchlet y el EJB.
 */
@ApplicationScoped
public class RequiresNewDelegatorBean {

    @EJB
    private ExternalService externalService;

    public void ejecutarMetodoA(int counter, String transactionId) throws Exception {
        externalService.metodoA(counter, transactionId);
    }
}
