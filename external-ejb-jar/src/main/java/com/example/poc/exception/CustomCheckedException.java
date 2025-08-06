package com.example.poc.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class CustomCheckedException extends Exception {
    public CustomCheckedException(String message) {
        super(message);
    }
}