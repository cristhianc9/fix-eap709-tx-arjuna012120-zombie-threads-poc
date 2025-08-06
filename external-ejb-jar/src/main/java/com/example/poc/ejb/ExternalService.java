package com.example.poc.ejb;

import javax.ejb.Remote;
import javax.transaction.SystemException;

@Remote
public interface ExternalService {
    void metodoA(int counter, String transactionId) throws SystemException;
}