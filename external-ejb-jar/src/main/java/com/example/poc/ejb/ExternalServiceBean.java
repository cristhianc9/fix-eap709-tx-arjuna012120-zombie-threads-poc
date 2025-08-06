package com.example.poc.ejb;

import com.example.poc.entity.LogEntry;
import com.example.poc.entity.LogEntryXA; // Importar LogEntryXA
import com.example.poc.exception.CustomCheckedException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.SystemException;
import javax.annotation.Resource;
import javax.ejb.EJB;

@Stateless
public class ExternalServiceBean implements ExternalService {

    @PersistenceContext(unitName = "primary")
    private EntityManager emPrimary;

    @PersistenceContext(unitName = "secondary")
    private EntityManager emSecondary;

    @EJB
    private ExternalService self; // Inyección del propio EJB para llamadas transaccionales

    private static final int FAIL_AT_COUNTER = 3; // Simular fallo en la tercera iteración

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void metodoA(int counter, String transactionId) throws SystemException {
        System.out.println("metodoA() invocado con counter: " + counter + ", Transaction ID: " + transactionId);

        try {
            // Persistir un log en el datasource primario (no-XA)
            LogEntry logEntryPrimary = new LogEntry("Operación " + counter + " en metodoA() - Primary", transactionId);
            emPrimary.persist(logEntryPrimary);
            emPrimary.flush(); // Forzar la escritura a la base de datos
            System.out.println("Persistido (Primary): " + logEntryPrimary);

            // Persistir un log en el datasource secundario (XA)
            LogEntryXA logEntrySecondary = new LogEntryXA("Operación " + counter + " en metodoA() - Secondary (XA)", transactionId);
            emSecondary.persist(logEntrySecondary);
            emSecondary.flush(); // Forzar la escritura a la base de datos
            System.out.println("Persistido (Secondary XA): " + logEntrySecondary);

            // Retardo para simular una operación larga y forzar un posible timeout de transacción/hilo
            try {
                Thread.sleep(5000); // 5 segundos
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SystemException("Interrupción durante el retardo: " + e.getMessage());
            }

            if (counter == FAIL_AT_COUNTER) {
                System.out.println("Simulando fallo en metodoA() en la iteración " + counter);
                throw new CustomCheckedException("Fallo simulado en metodoA() en la iteración " + counter);
            }

            // Simular el avance del flujo de tareas
            if (counter < 5) { // Limitar la recursión para evitar StackOverflow
                self.metodoA(counter + 1, transactionId); // Llamada recursiva a través del proxy EJB
            }
        } catch (CustomCheckedException e) {
            System.out.println("Excepción controlada capturada en metodoA(): " + e.getMessage());
            // @ApplicationException(rollback=true) en CustomCheckedException asegura el rollback.
            throw new SystemException("Fallo en metodoA() que causa rollback de la transacción actual: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error inesperado en metodoA(): " + e.getMessage());
            throw new SystemException("Error inesperado en metodoA(): " + e.getMessage());
        }
    }
}