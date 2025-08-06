package com.example.poc.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "LogEntryXA") // Nombre de tabla diferente para evitar conflictos
public class LogEntryXA implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;
    private Date timestamp;
    private String transactionId;

    public LogEntryXA() {
    }

    public LogEntryXA(String message, String transactionId) {
        this.message = message;
        this.timestamp = new Date();
        this.transactionId = transactionId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public String toString() {
        return "LogEntryXA{" +
               "id=" + id +
               ", message='" + message + '\'' +
               ", timestamp=" + timestamp +
               ", transactionId='" + transactionId + '\'' +
               '}';
    }
}