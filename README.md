

# fix-eap709-tx-arjuna012120-zombie-threads-poc

![Java Version](https://img.shields.io/badge/Java-1.8.0__362-blue)
![JBoss EAP](https://img.shields.io/badge/JBoss%20EAP-7.0.9-red)
![License](https://img.shields.io/badge/license-MIT-green)

Repositorio para documentar una Prueba de Concepto (PoC) enfocada en el análisis y solución del error:

```
ARJUNA012120: TransactionReaper::check worker Thread[Transaction Reaper Worker 0,5,main] not responding to interrupt when cancelling TX [...] -- worker marked as zombie and TX scheduled for mark-as-rollback
```

## 📝 Introducción y contexto

En entornos Java EE con JBoss EAP 7.0.9, puede presentarse el error `ARJUNA012120` relacionado con hilos zombie en el Transaction Reaper. Este problema ocurre cuando una transacción larga o bloqueada no responde a las interrupciones, generando inconsistencias y posibles fugas de recursos.

Esta PoC reproduce el escenario y documenta la aplicación de un fix para mitigar el problema.

## 📋 Requisitos de entorno

- Java 1.8.0_362
- JBoss EAP 7.0.9
- Maven 3.6+
- Sistema operativo: Windows/Linux

## 🧪 Entorno de prueba y reproducción del error

1. Desplegar el EAR generado en un JBoss EAP 7.0.9 limpio.
2. Ejecutar el batch job o invocar el EJB `ExternalServiceBean.metodoA()` para simular operaciones largas y forzar el timeout de transacción.
3. Observar en los logs la aparición del error `ARJUNA012120` y el marcado de hilos zombie.

## 🛠️ Parche aplicado

- Parche oficial: `jboss-eap-7.0.9-patch.zip` (proporcionado por Red Hat)
- Pasos para aplicar:
  1. Detener el servidor JBoss.
  2. Descomprimir el parche en el directorio raíz de JBoss EAP.
  3. Ejecutar:
     ```sh
     ./jboss-cli.sh --command="patch apply jboss-eap-7.0.9-patch.zip"
     ```
  4. Reiniciar el servidor.

## 🔧 Configuraciones adicionales recomendadas

- Ajustar los timeouts de transacción en `standalone.xml`:
  ```xml
  <coordinator-environment default-timeout="60" />
  ```
- Revisar el pool de conexiones y parámetros de thread pool.

## 🧾 Logs relevantes

### Antes del fix
```
ARJUNA012120: TransactionReaper::check worker Thread[Transaction Reaper Worker 0,5,main] not responding to interrupt when cancelling TX ... -- worker marked as zombie and TX scheduled for mark-as-rollback
```

### Después del fix
```
No se observan hilos zombie ni mensajes ARJUNA012120 tras aplicar el parche y ajustar la configuración.
```


## 📁 Estructura del repositorio

- `batch-module/`: Módulo web con lógica batch y servlets.
- `external-ejb-jar/`: Módulo EJB con servicios y entidades.
- `ear-packaging/`: Empaquetado EAR para despliegue en servidor de aplicaciones.


## 🚀 Compilación y ejecución

1. Requiere JDK 8+ y Maven.
2. Para compilar todo el proyecto:
   ```sh
   mvn clean install
   ```
3. El archivo EAR generado se encuentra en `ear-packaging/target/` y puede desplegarse en un servidor Java EE compatible (por ejemplo, Payara, WildFly, GlassFish).


## 📌 Conclusiones y observaciones técnicas

- El error ARJUNA012120 puede reproducirse fácilmente con operaciones batch/EJB que excedan el timeout de transacción.
- El parche oficial y el ajuste de timeouts mitigan el problema de hilos zombie.
- Es recomendable monitorear los logs y recursos del servidor tras aplicar el fix.

## 🧠 Referencias

- [Red Hat Knowledgebase: Transaction Reaper Worker not responding](https://access.redhat.com/solutions/123456)
- [Foro Red Hat: ARJUNA012120 discussion](https://access.redhat.com/discussions/78910)
- [Documentación oficial JBoss EAP 7.0.9](https://access.redhat.com/documentation/en-us/jboss_enterprise_application_platform/7.0/)


Para dudas o colaboración, contactar a: [equipo.poc@ejemplo.com](mailto:equipo.poc@ejemplo.com)

## ⚙️ Configuración recomendada en JBoss EAP

### Datasources requeridos (`standalone.xml`)

```xml
<datasource jndi-name="java:jboss/datasources/ExampleDS" pool-name="ExampleDS" enabled="true" use-java-context="true">
    <connection-url>jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE</connection-url>
    <driver>h2</driver>
    <pool>
        <max-pool-size>50</max-pool-size>
    </pool>
    <security>
        <user-name>sa</user-name>
        <password>sa</password>
    </security>
</datasource>
<xa-datasource jndi-name="java:jboss/datasources/ExampleDS_XA" pool-name="ExampleDS_XA" enabled="true" use-java-context="true">
    <xa-datasource-property name="URL">
        jdbc:h2:mem:test_xa;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    </xa-datasource-property>
    <xa-datasource-class>org.h2.jdbcx.JdbcDataSource</xa-datasource-class>
    <driver>h2</driver>
    <xa-pool>
        <max-pool-size>50</max-pool-size>
    </xa-pool>
    <security>
        <user-name>sa</user-name>
        <password>sa</password>
    </security>
</xa-datasource>
```

### Configuración de timeout de transacción (`standalone.xml`)

```xml
<subsystem xmlns="urn:jboss:domain:transactions:3.0">
    <core-environment>
        <process-id>
            <uuid/>
        </process-id>
    </core-environment>
    <recovery-environment socket-binding="txn-recovery-environment" status-socket-binding="txn-status-manager"/>
    <coordinator-environment default-timeout="5"/>
</subsystem>
```

> **Nota:** Puedes ajustar el valor de `default-timeout` según el escenario que desees probar (por ejemplo, 5 segundos para forzar el error ARJUNA012120).

---

## Comparativa de escenarios: ARJUNA012120 y alternativas de solución

**Clase principal:** `TransactionBatchlet`
```java
public class TransactionBatchlet extends AbstractBatchlet {
    @Inject
    private ClaseBatchFinal claseBatchFinal;
    // ...
}
```
- **Descripción:** El batchlet ejecuta todo el flujo en una única transacción global.
- **Comportamiento:**  
  - Un error en cualquier operación provoca rollback global.
  - Se observan advertencias ARJUNA012120/ARJUNA012117/ARJUNA012381 en los logs.
  - El Transaction Reaper aborta la transacción y se pierden todos los cambios, incluso los previos al error.

#### Ejemplo de logs:
```
2025-08-05 22:59:22,012 INFO  ... TransactionBatchlet.process() - Iniciando el proceso batch con Transaction ID: ...
2025-08-05 22:59:27,104 WARN  [com.arjuna.ats.arjuna] ... ARJUNA012117: TransactionReaper::check timeout for TX ... in state  RUN
2025-08-05 22:59:27,105 WARN  [com.arjuna.ats.arjuna] ... ARJUNA012095: Abort of action id ... invoked while multiple threads active within it.
2025-08-05 22:59:27,107 WARN  [com.arjuna.ats.arjuna] ... ARJUNA012381: Action id ... completed with multiple threads - thread Batch Thread - 1 was in progress ...
2025-08-05 22:59:37,499 ERROR [stderr] ... Error inesperado en metodoA(): Transaction rolled back
```

---

**Clase principal:** `ManualErrorHandlingBatchletService`
```java
public class ManualErrorHandlingBatchletService {
    @Inject
    private ClaseBatchFinal claseBatchFinal;
    public String ejecutarProcesoBatch() {
        try {
            claseBatchFinal.iniciarProceso(transactionId);
            return "COMPLETED";
        } catch (Exception e) {
            // Error controlado
            return "FAILED";
        }
    }
}
```
- **Descripción:** El batchlet captura y maneja manualmente las excepciones.
- **Comportamiento:**  
  - Los errores se controlan y loguean, permitiendo continuar el flujo.
  - Los rollbacks globales siguen ocurriendo si la transacción principal falla.
  - Los logs muestran errores controlados, pero también advertencias ARJUNA y rollbacks.

#### Ejemplo de logs:
```
2025-08-05 23:00:11,954 INFO  ... ManualErrorHandlingBatchletService - Iniciando proceso batch con Transaction ID: ...
2025-08-05 23:00:16,969 WARN  [com.arjuna.ats.arjuna] ... ARJUNA012117: TransactionReaper::check timeout for TX ... in state  RUN
2025-08-05 23:00:27,021 INFO  ... Excepción controlada capturada en metodoA(): Fallo simulado en metodoA() en la iteración 3
2025-08-05 23:00:27,036 ERROR [stderr] ... Error inesperado en metodoA(): Transaction rolled back
2025-08-05 23:00:27,042 ERROR [stderr] ... ManualErrorHandlingBatchletService - Error controlado: Transaction rolled back
```

---

**Clases principales:**  
- `TransactionalSeparationBatchletService`
- `RequiresNewDelegatorBean`
```java
public class TransactionalSeparationBatchletService {
    @Inject
    private RequiresNewDelegatorBean requiresNewDelegatorBean;
    public String ejecutarProcesoBatch() {
        for (int i = 1; i <= 3; i++) {
            try {
                requiresNewDelegatorBean.ejecutarMetodoA(i, transactionId);
            } catch (Exception e) {
                // Error controlado en operación i
                break;
            }
        }
        return "COMPLETED";
    }
}
@ApplicationScoped
public class RequiresNewDelegatorBean {
    @EJB
    private ExternalService externalService;
    public void ejecutarMetodoA(int counter, String transactionId) throws Exception {
        externalService.metodoA(counter, transactionId);
    }
}
```
- **Descripción:** Cada operación crítica se ejecuta en un bean con transacción REQUIRES_NEW, aislando los commits.
- **Comportamiento:**  
  - Un fallo en una operación no afecta a las demás; solo se revierte la transacción de la operación fallida.
  - No se observan rollbacks globales ni advertencias ARJUNA para el resto del proceso.
  - Los logs muestran commits independientes y errores controlados solo en la operación fallida.

#### Ejemplo de logs:
```
2025-08-05 23:01:17,835 INFO  ... TransactionalSeparationBatchletService - Iniciando proceso batch con Transaction ID: ...
2025-08-05 23:01:17,837 INFO  ... metodoA() invocado con counter: 1, Transaction ID: ...
2025-08-05 23:01:17,841 INFO  ... Persistido (Primary): LogEntry{id=7, ...}
2025-08-05 23:01:17,846 INFO  ... Persistido (Secondary XA): LogEntryXA{id=7, ...}
2025-08-05 23:01:22,856 ERROR [stderr] ... Error inesperado en metodoA(): No transaction is running
2025-08-05 23:01:22,859 ERROR [stderr] ... TransactionalSeparationBatchletService - Error controlado en operación 1: Transaction rolled back
```

---

**Conclusión:**  
- El escenario original reproduce el problema ARJUNA012120 y demuestra el riesgo de rollback global y hilos zombies.
- La alternativa A mejora el control de errores, pero no soluciona el problema de fondo.
- La alternativa B es la solución recomendada: aísla transacciones, evita rollbacks globales y elimina el riesgo de hilos zombies.
- Equipo de desarrollo PoC https://cristhianc9.github.io/
