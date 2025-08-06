

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

- [Red Hat Knowledgebase: Transaction Reaper Worker not responding](https://access.redhat.com/solutions/18425)
- [Foro Red Hat: ARJUNA012120 discussion](https://access.redhat.com/solutions/167033)
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


#### Ejemplo de logs (real):
```
2025-08-05 23:25:26,290 INFO  [stdout] (default task-6) TransactionalSeparationBatchletService - Iniciando proceso batch con Transaction ID: 732dda52-677f-4419-ab8d-d2107e432043
2025-08-05 23:25:26,291 INFO  [stdout] (default task-6) metodoA() invocado con counter: 1, Transaction ID: 732dda52-677f-4419-ab8d-d2107e432043
2025-08-05 23:25:26,295 INFO  [stdout] (default task-6) Persistido (Primary): LogEntry{id=14, message='Operación 1 en metodoA() - Primary', ...}
2025-08-05 23:25:26,298 INFO  [stdout] (default task-6) Persistido (Secondary XA): LogEntryXA{id=14, message='Operación 1 en metodoA() - Secondary (XA)', ...}
2025-08-05 23:25:31,294 WARN  [com.arjuna.ats.arjuna] (Transaction Reaper) ARJUNA012117: TransactionReaper::check timeout for TX ... in state  RUN
2025-08-05 23:25:31,310 INFO  [stdout] (default task-6) metodoA() invocado con counter: 2, Transaction ID: 732dda52-677f-4419-ab8d-d2107e432043
2025-08-05 23:25:31,312 INFO  [stdout] (default task-6) Hibernate: 
2025-08-05 23:25:31,312 INFO  [stdout] (default task-6)     insert 
2025-08-05 23:25:31,312 INFO  [stdout] (default task-6)     into
2025-08-05 23:25:31,312 INFO  [stdout] (default task-6)         LogEntry
2025-08-05 23:25:31,312 INFO  [stdout] (default task-6)         (id, message, timestamp, transactionId) 
2025-08-05 23:25:31,313 INFO  [stdout] (default task-6)     values
2025-08-05 23:25:31,313 INFO  [stdout] (default task-6)         (null, ?, ?, ?)
2025-08-05 23:25:31,314 INFO  [stdout] (default task-6) Persistido (Primary): LogEntry{id=15, message='Operación 2 en metodoA() - Primary', timestamp=Tue Aug 05 23:25:31 COT 2025, transactionId='732dda52-677f-4419-ab8d-d2107e432043'}
2025-08-05 23:25:31,314 INFO  [stdout] (default task-6) Hibernate: 
2025-08-05 23:25:31,315 INFO  [stdout] (default task-6)     insert 
2025-08-05 23:25:31,315 INFO  [stdout] (default task-6)     into
2025-08-05 23:25:31,315 INFO  [stdout] (default task-6)         LogEntryXA
2025-08-05 23:25:31,315 INFO  [stdout] (default task-6)         (id, message, timestamp, transactionId) 
2025-08-05 23:25:31,315 INFO  [stdout] (default task-6)     values
2025-08-05 23:25:31,315 INFO  [stdout] (default task-6)         (null, ?, ?, ?)
2025-08-05 23:25:31,316 INFO  [stdout] (default task-6) Persistido (Secondary XA): LogEntryXA{id=15, message='Operación 2 en metodoA() - Secondary (XA)', timestamp=Tue Aug 05 23:25:31 COT 2025, transactionId='732dda52-677f-4419-ab8d-d2107e432043'}
2025-08-05 23:25:36,325 WARN  [com.arjuna.ats.arjuna] (Transaction Reaper) ARJUNA012117: TransactionReaper::check timeout for TX 0:ffffac1c9001:-585a8e34:6892d2a8:d0 in state  RUN
2025-08-05 23:25:36,327 WARN  [com.arjuna.ats.arjuna] (Transaction Reaper Worker 0) ARJUNA012095: Abort of action id 0:ffffac1c9001:-585a8e34:6892d2a8:d0 invoked while multiple threads active within it.
2025-08-05 23:25:36,329 WARN  [com.arjuna.ats.arjuna] (Transaction Reaper Worker 0) ARJUNA012381: Action id 0:ffffac1c9001:-585a8e34:6892d2a8:d0 completed with multiple threads - thread default task-6 was in progress with com.arjuna.ats.arjuna.coordinator.BasicAction.removeChildThread(BasicAction.java:662)
com.arjuna.ats.internal.arjuna.thread.ThreadActionData.purgeActions(ThreadActionData.java:245)
com.arjuna.ats.internal.arjuna.thread.ThreadActionData.purgeActions(ThreadActionData.java:221)
com.arjuna.ats.arjuna.AtomicAction.suspend(AtomicAction.java:334)
com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple.suspend(TransactionManagerImple.java:71)
com.arjuna.ats.jbossatx.BaseTransactionManagerDelegate.suspend(BaseTransactionManagerDelegate.java:194)
org.jboss.as.ejb3.tx.CMTTxInterceptor.requiresNew(CMTTxInterceptor.java:342)
org.jboss.as.ejb3.tx.CMTTxInterceptor.processInvocation(CMTTxInterceptor.java:241)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext$Invocation.proceed(InterceptorContext.java:437)
org.jboss.weld.ejb.AbstractEJBRequestScopeActivationInterceptor.aroundInvoke(AbstractEJBRequestScopeActivationInterceptor.java:64)
org.jboss.as.weld.ejb.EjbRequestScopeActivationInterceptor.processInvocation(EjbRequestScopeActivationInterceptor.java:83)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.remote.EJBRemoteTransactionPropagatingInterceptor.processInvocation(EJBRemoteTransactionPropagatingInterceptor.java:80)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor.processInvocation(CurrentInvocationContextInterceptor.java:41)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.invocationmetrics.WaitTimeInterceptor.processInvocation(WaitTimeInterceptor.java:43)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.security.SecurityContextInterceptor.processInvocation(SecurityContextInterceptor.java:100)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.deployment.processors.StartupAwaitInterceptor.processInvocation(StartupAwaitInterceptor.java:22)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.ShutDownInterceptorFactory$1.processInvocation(ShutDownInterceptorFactory.java:64)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.deployment.processors.EjbSuspendInterceptor.processInvocation(EjbSuspendInterceptor.java:44)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.LoggingInterceptor.processInvocation(LoggingInterceptor.java:66)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ee.component.NamespaceContextInterceptor.processInvocation(NamespaceContextInterceptor.java:50)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.AdditionalSetupInterceptor.processInvocation(AdditionalSetupInterceptor.java:54)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.ContextClassLoaderInterceptor.processInvocation(ContextClassLoaderInterceptor.java:64)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext.run(InterceptorContext.java:356)
org.wildfly.security.manager.WildFlySecurityManager.doChecked(WildFlySecurityManager.java:632)
org.jboss.invocation.AccessCheckingInterceptor.processInvocation(AccessCheckingInterceptor.java:61)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext.run(InterceptorContext.java:356)
org.jboss.invocation.PrivilegedWithCombinerInterceptor.processInvocation(PrivilegedWithCombinerInterceptor.java:80)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.ChainedInterceptor.processInvocation(ChainedInterceptor.java:61)
org.jboss.as.ee.component.ViewService$View.invoke(ViewService.java:198)
org.jboss.as.ejb3.remote.LocalEjbReceiver.processInvocation(LocalEjbReceiver.java:261)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:184)
org.jboss.ejb.client.EJBObjectInterceptor.handleInvocation(EJBObjectInterceptor.java:58)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.EJBHomeInterceptor.handleInvocation(EJBHomeInterceptor.java:83)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.TransactionInterceptor.handleInvocation(TransactionInterceptor.java:42)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.ReceiverInterceptor.handleInvocation(ReceiverInterceptor.java:138)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.EJBInvocationHandler.sendRequestWithPossibleRetries(EJBInvocationHandler.java:255)
org.jboss.ejb.client.EJBInvocationHandler.doInvoke(EJBInvocationHandler.java:200)
org.jboss.ejb.client.EJBInvocationHandler.doInvoke(EJBInvocationHandler.java:183)
org.jboss.ejb.client.EJBInvocationHandler.invoke(EJBInvocationHandler.java:146)
com.sun.proxy.$Proxy60.metodoA(Unknown Source)
com.example.poc.ejb.ExternalServiceBean.metodoA(ExternalServiceBean.java:64)
sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
java.lang.reflect.Method.invoke(Method.java:498)
org.jboss.as.ee.component.ManagedReferenceMethodInterceptor.processInvocation(ManagedReferenceMethodInterceptor.java:52)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext$Invocation.proceed(InterceptorContext.java:437)
org.jboss.as.weld.ejb.Jsr299BindingsInterceptor.doMethodInterception(Jsr299BindingsInterceptor.java:82)
org.jboss.as.weld.ejb.Jsr299BindingsInterceptor.processInvocation(Jsr299BindingsInterceptor.java:93)
org.jboss.as.ee.component.interceptors.UserInterceptorFactory$1.processInvocation(UserInterceptorFactory.java:63)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.invocationmetrics.ExecutionTimeInterceptor.processInvocation(ExecutionTimeInterceptor.java:43)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.jpa.interceptor.SBInvocationInterceptor.processInvocation(SBInvocationInterceptor.java:47)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ee.concurrent.ConcurrentContextInterceptor.processInvocation(ConcurrentContextInterceptor.java:45)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InitialInterceptor.processInvocation(InitialInterceptor.java:21)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.ChainedInterceptor.processInvocation(ChainedInterceptor.java:61)
org.jboss.as.ee.component.interceptors.ComponentDispatcherInterceptor.processInvocation(ComponentDispatcherInterceptor.java:52)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.pool.PooledInstanceInterceptor.processInvocation(PooledInstanceInterceptor.java:51)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.tx.CMTTxInterceptor.invokeInOurTx(CMTTxInterceptor.java:275)
org.jboss.as.ejb3.tx.CMTTxInterceptor.requiresNew(CMTTxInterceptor.java:344)
org.jboss.as.ejb3.tx.CMTTxInterceptor.processInvocation(CMTTxInterceptor.java:241)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext$Invocation.proceed(InterceptorContext.java:437)
org.jboss.weld.ejb.AbstractEJBRequestScopeActivationInterceptor.aroundInvoke(AbstractEJBRequestScopeActivationInterceptor.java:64)
org.jboss.as.weld.ejb.EjbRequestScopeActivationInterceptor.processInvocation(EjbRequestScopeActivationInterceptor.java:83)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.remote.EJBRemoteTransactionPropagatingInterceptor.processInvocation(EJBRemoteTransactionPropagatingInterceptor.java:80)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor.processInvocation(CurrentInvocationContextInterceptor.java:41)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.invocationmetrics.WaitTimeInterceptor.processInvocation(WaitTimeInterceptor.java:43)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.security.SecurityContextInterceptor.processInvocation(SecurityContextInterceptor.java:100)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.deployment.processors.StartupAwaitInterceptor.processInvocation(StartupAwaitInterceptor.java:22)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.ShutDownInterceptorFactory$1.processInvocation(ShutDownInterceptorFactory.java:64)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.deployment.processors.EjbSuspendInterceptor.processInvocation(EjbSuspendInterceptor.java:44)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.LoggingInterceptor.processInvocation(LoggingInterceptor.java:66)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ee.component.NamespaceContextInterceptor.processInvocation(NamespaceContextInterceptor.java:50)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.AdditionalSetupInterceptor.processInvocation(AdditionalSetupInterceptor.java:54)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.ContextClassLoaderInterceptor.processInvocation(ContextClassLoaderInterceptor.java:64)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext.run(InterceptorContext.java:356)
org.wildfly.security.manager.WildFlySecurityManager.doChecked(WildFlySecurityManager.java:632)
org.jboss.invocation.AccessCheckingInterceptor.processInvocation(AccessCheckingInterceptor.java:61)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext.run(InterceptorContext.java:356)
org.jboss.invocation.PrivilegedWithCombinerInterceptor.processInvocation(PrivilegedWithCombinerInterceptor.java:80)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.ChainedInterceptor.processInvocation(ChainedInterceptor.java:61)
org.jboss.as.ee.component.ViewService$View.invoke(ViewService.java:198)
org.jboss.as.ejb3.remote.LocalEjbReceiver.processInvocation(LocalEjbReceiver.java:261)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:184)
org.jboss.ejb.client.EJBObjectInterceptor.handleInvocation(EJBObjectInterceptor.java:58)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.EJBHomeInterceptor.handleInvocation(EJBHomeInterceptor.java:83)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.TransactionInterceptor.handleInvocation(TransactionInterceptor.java:42)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.ReceiverInterceptor.handleInvocation(ReceiverInterceptor.java:138)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.EJBInvocationHandler.sendRequestWithPossibleRetries(EJBInvocationHandler.java:255)
org.jboss.ejb.client.EJBInvocationHandler.doInvoke(EJBInvocationHandler.java:200)
org.jboss.ejb.client.EJBInvocationHandler.doInvoke(EJBInvocationHandler.java:183)
org.jboss.ejb.client.EJBInvocationHandler.invoke(EJBInvocationHandler.java:146)
com.sun.proxy.$Proxy60.metodoA(Unknown Source)
com.example.poc.ejb.ExternalServiceBean.metodoA(ExternalServiceBean.java:64)
sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
java.lang.reflect.Method.invoke(Method.java:498)
org.jboss.as.ee.component.ManagedReferenceMethodInterceptor.processInvocation(ManagedReferenceMethodInterceptor.java:52)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext$Invocation.proceed(InterceptorContext.java:437)
org.jboss.as.weld.ejb.Jsr299BindingsInterceptor.doMethodInterception(Jsr299BindingsInterceptor.java:82)
org.jboss.as.weld.ejb.Jsr299BindingsInterceptor.processInvocation(Jsr299BindingsInterceptor.java:93)
org.jboss.as.ee.component.interceptors.UserInterceptorFactory$1.processInvocation(UserInterceptorFactory.java:63)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.invocationmetrics.ExecutionTimeInterceptor.processInvocation(ExecutionTimeInterceptor.java:43)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.jpa.interceptor.SBInvocationInterceptor.processInvocation(SBInvocationInterceptor.java:47)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ee.concurrent.ConcurrentContextInterceptor.processInvocation(ConcurrentContextInterceptor.java:45)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InitialInterceptor.processInvocation(InitialInterceptor.java:21)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.ChainedInterceptor.processInvocation(ChainedInterceptor.java:61)
org.jboss.as.ee.component.interceptors.ComponentDispatcherInterceptor.processInvocation(ComponentDispatcherInterceptor.java:52)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.pool.PooledInstanceInterceptor.processInvocation(PooledInstanceInterceptor.java:51)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.tx.CMTTxInterceptor.invokeInOurTx(CMTTxInterceptor.java:275)
org.jboss.as.ejb3.tx.CMTTxInterceptor.requiresNew(CMTTxInterceptor.java:344)
org.jboss.as.ejb3.tx.CMTTxInterceptor.processInvocation(CMTTxInterceptor.java:241)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext$Invocation.proceed(InterceptorContext.java:437)
org.jboss.weld.ejb.AbstractEJBRequestScopeActivationInterceptor.aroundInvoke(AbstractEJBRequestScopeActivationInterceptor.java:64)
org.jboss.as.weld.ejb.EjbRequestScopeActivationInterceptor.processInvocation(EjbRequestScopeActivationInterceptor.java:83)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.remote.EJBRemoteTransactionPropagatingInterceptor.processInvocation(EJBRemoteTransactionPropagatingInterceptor.java:80)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor.processInvocation(CurrentInvocationContextInterceptor.java:41)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.invocationmetrics.WaitTimeInterceptor.processInvocation(WaitTimeInterceptor.java:43)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.security.SecurityContextInterceptor.processInvocation(SecurityContextInterceptor.java:100)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.deployment.processors.StartupAwaitInterceptor.processInvocation(StartupAwaitInterceptor.java:22)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.ShutDownInterceptorFactory$1.processInvocation(ShutDownInterceptorFactory.java:64)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.deployment.processors.EjbSuspendInterceptor.processInvocation(EjbSuspendInterceptor.java:44)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.LoggingInterceptor.processInvocation(LoggingInterceptor.java:66)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ee.component.NamespaceContextInterceptor.processInvocation(NamespaceContextInterceptor.java:50)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.AdditionalSetupInterceptor.processInvocation(AdditionalSetupInterceptor.java:54)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.ContextClassLoaderInterceptor.processInvocation(ContextClassLoaderInterceptor.java:64)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext.run(InterceptorContext.java:356)
org.wildfly.security.manager.WildFlySecurityManager.doChecked(WildFlySecurityManager.java:632)
org.jboss.invocation.AccessCheckingInterceptor.processInvocation(AccessCheckingInterceptor.java:61)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext.run(InterceptorContext.java:356)
org.jboss.invocation.PrivilegedWithCombinerInterceptor.processInvocation(PrivilegedWithCombinerInterceptor.java:80)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.ChainedInterceptor.processInvocation(ChainedInterceptor.java:61)
org.jboss.as.ee.component.ViewService$View.invoke(ViewService.java:198)
org.jboss.as.ejb3.remote.LocalEjbReceiver.processInvocation(LocalEjbReceiver.java:261)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:184)
org.jboss.ejb.client.EJBObjectInterceptor.handleInvocation(EJBObjectInterceptor.java:58)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.EJBHomeInterceptor.handleInvocation(EJBHomeInterceptor.java:83)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.TransactionInterceptor.handleInvocation(TransactionInterceptor.java:42)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.ReceiverInterceptor.handleInvocation(ReceiverInterceptor.java:138)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.EJBInvocationHandler.sendRequestWithPossibleRetries(EJBInvocationHandler.java:255)
org.jboss.ejb.client.EJBInvocationHandler.doInvoke(EJBInvocationHandler.java:200)
org.jboss.ejb.client.EJBInvocationHandler.doInvoke(EJBInvocationHandler.java:183)
org.jboss.ejb.client.EJBInvocationHandler.invoke(EJBInvocationHandler.java:146)
com.sun.proxy.$Proxy60.metodoA(Unknown Source)
com.example.poc.batch.service.RequiresNewDelegatorBean.ejecutarMetodoA(RequiresNewDelegatorBean.java:20)
com.example.poc.batch.service.RequiresNewDelegatorBean$Proxy$_$$_WeldClientProxy.ejecutarMetodoA(Unknown Source)
com.example.poc.batch.service.TransactionalSeparationBatchletService.ejecutarProcesoBatch(TransactionalSeparationBatchletService.java:31)
com.example.poc.batch.web.AlternativasBatchletServlet.doGet(AlternativasBatchletServlet.java:58)
javax.servlet.http.HttpServlet.service(HttpServlet.java:687)
javax.servlet.http.HttpServlet.service(HttpServlet.java:790)
io.undertow.servlet.handlers.ServletHandler.handleRequest(ServletHandler.java:85)
io.undertow.servlet.handlers.security.ServletSecurityRoleHandler.handleRequest(ServletSecurityRoleHandler.java:62)
io.undertow.servlet.handlers.ServletDispatchingHandler.handleRequest(ServletDispatchingHandler.java:36)
org.wildfly.extension.undertow.security.SecurityContextAssociationHandler.handleRequest(SecurityContextAssociationHandler.java:78)
io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43)
io.undertow.servlet.handlers.security.SSLInformationAssociationHandler.handleRequest(SSLInformationAssociationHandler.java:131)
io.undertow.servlet.handlers.security.ServletAuthenticationCallHandler.handleRequest(ServletAuthenticationCallHandler.java:57)
io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43)
io.undertow.security.handlers.AbstractConfidentialityHandler.handleRequest(AbstractConfidentialityHandler.java:46)
io.undertow.servlet.handlers.security.ServletConfidentialityConstraintHandler.handleRequest(ServletConfidentialityConstraintHandler.java:64)
io.undertow.security.handlers.AuthenticationMechanismsHandler.handleRequest(AuthenticationMechanismsHandler.java:60)
io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler.handleRequest(CachedAuthenticatedSessionHandler.java:77)
io.undertow.security.handlers.NotificationReceiverHandler.handleRequest(NotificationReceiverHandler.java:50)
io.undertow.security.handlers.AbstractSecurityContextAssociationHandler.handleRequest(AbstractSecurityContextAssociationHandler.java:43)
io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43)
org.wildfly.extension.undertow.security.jacc.JACCContextIdHandler.handleRequest(JACCContextIdHandler.java:61)
io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43)
io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43)
io.undertow.servlet.handlers.ServletInitialHandler.handleFirstRequest(ServletInitialHandler.java:285)
io.undertow.servlet.handlers.ServletInitialHandler.dispatchRequest(ServletInitialHandler.java:264)
io.undertow.servlet.handlers.ServletInitialHandler.access$000(ServletInitialHandler.java:81)
io.undertow.servlet.handlers.ServletInitialHandler$1.handleRequest(ServletInitialHandler.java:175)
io.undertow.server.Connectors.executeRootHandler(Connectors.java:324)
io.undertow.server.HttpServerExchange$1.run(HttpServerExchange.java:803)
java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
java.lang.Thread.run(Thread.java:750)
2025-08-05 23:25:36,333 WARN  [com.arjuna.ats.arjuna] (Transaction Reaper Worker 0) ARJUNA012108: CheckedAction::check - atomic action 0:ffffac1c9001:-585a8e34:6892d2a8:d0 aborting with 1 threads active!
2025-08-05 23:25:36,339 WARN  [org.hibernate.resource.transaction.backend.jta.internal.synchronization.SynchronizationCallbackCoordinatorTrackingImpl] (Transaction Reaper Worker 0) HHH000451: Transaction afterCompletion called by a background thread; delaying afterCompletion processing until the original thread can handle it. [status=4]
2025-08-05 23:25:36,343 WARN  [org.hibernate.resource.transaction.backend.jta.internal.synchronization.SynchronizationCallbackCoordinatorTrackingImpl] (Transaction Reaper Worker 0) HHH000451: Transaction afterCompletion called by a background thread; delaying afterCompletion processing until the original thread can handle it. [status=4]
2025-08-05 23:25:36,343 INFO  [stdout] (default task-6) metodoA() invocado con counter: 3, Transaction ID: 732dda52-677f-4419-ab8d-d2107e432043
2025-08-05 23:25:36,344 WARN  [com.arjuna.ats.arjuna] (Transaction Reaper Worker 0) ARJUNA012121: TransactionReaper::doCancellations worker Thread[Transaction Reaper Worker 0,5,main] successfully canceled TX 0:ffffac1c9001:-585a8e34:6892d2a8:d0
2025-08-05 23:25:36,350 INFO  [stdout] (default task-6) Hibernate: 
2025-08-05 23:25:36,350 INFO  [stdout] (default task-6)     insert 
2025-08-05 23:25:36,351 INFO  [stdout] (default task-6)     into
2025-08-05 23:25:36,351 INFO  [stdout] (default task-6)         LogEntry
2025-08-05 23:25:36,351 INFO  [stdout] (default task-6)         (id, message, timestamp, transactionId) 
2025-08-05 23:25:36,351 INFO  [stdout] (default task-6)     values
2025-08-05 23:25:36,351 INFO  [stdout] (default task-6)         (null, ?, ?, ?)
2025-08-05 23:25:36,352 INFO  [stdout] (default task-6) Persistido (Primary): LogEntry{id=16, message='Operación 3 en metodoA() - Primary', timestamp=Tue Aug 05 23:25:36 COT 2025, transactionId='732dda52-677f-4419-ab8d-d2107e432043'}
2025-08-05 23:25:36,353 INFO  [stdout] (default task-6) Hibernate: 
2025-08-05 23:25:36,353 INFO  [stdout] (default task-6)     insert 
2025-08-05 23:25:36,353 INFO  [stdout] (default task-6)     into
2025-08-05 23:25:36,353 INFO  [stdout] (default task-6)         LogEntryXA
2025-08-05 23:25:36,353 INFO  [stdout] (default task-6)         (id, message, timestamp, transactionId) 
2025-08-05 23:25:36,353 INFO  [stdout] (default task-6)     values
2025-08-05 23:25:36,353 INFO  [stdout] (default task-6)         (null, ?, ?, ?)
2025-08-05 23:25:36,354 INFO  [stdout] (default task-6) Persistido (Secondary XA): LogEntryXA{id=16, message='Operación 3 en metodoA() - Secondary (XA)', timestamp=Tue Aug 05 23:25:36 COT 2025, transactionId='732dda52-677f-4419-ab8d-d2107e432043'}
2025-08-05 23:25:41,343 WARN  [com.arjuna.ats.arjuna] (Transaction Reaper) ARJUNA012117: TransactionReaper::check timeout for TX 0:ffffac1c9001:-585a8e34:6892d2a8:d9 in state  RUN
2025-08-05 23:25:41,345 WARN  [com.arjuna.ats.arjuna] (Transaction Reaper Worker 0) ARJUNA012095: Abort of action id 0:ffffac1c9001:-585a8e34:6892d2a8:d9 invoked while multiple threads active within it.
2025-08-05 23:25:41,348 WARN  [com.arjuna.ats.arjuna] (Transaction Reaper Worker 0) ARJUNA012381: Action id 0:ffffac1c9001:-585a8e34:6892d2a8:d9 completed with multiple threads - thread default task-6 was in progress with java.lang.Thread.sleep(Native Method)
com.example.poc.ejb.ExternalServiceBean.metodoA(ExternalServiceBean.java:51)
sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
java.lang.reflect.Method.invoke(Method.java:498)
org.jboss.as.ee.component.ManagedReferenceMethodInterceptor.processInvocation(ManagedReferenceMethodInterceptor.java:52)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext$Invocation.proceed(InterceptorContext.java:437)
org.jboss.as.weld.ejb.Jsr299BindingsInterceptor.doMethodInterception(Jsr299BindingsInterceptor.java:82)
org.jboss.as.weld.ejb.Jsr299BindingsInterceptor.processInvocation(Jsr299BindingsInterceptor.java:93)
org.jboss.as.ee.component.interceptors.UserInterceptorFactory$1.processInvocation(UserInterceptorFactory.java:63)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.invocationmetrics.ExecutionTimeInterceptor.processInvocation(ExecutionTimeInterceptor.java:43)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.jpa.interceptor.SBInvocationInterceptor.processInvocation(SBInvocationInterceptor.java:47)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ee.concurrent.ConcurrentContextInterceptor.processInvocation(ConcurrentContextInterceptor.java:45)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InitialInterceptor.processInvocation(InitialInterceptor.java:21)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.ChainedInterceptor.processInvocation(ChainedInterceptor.java:61)
org.jboss.as.ee.component.interceptors.ComponentDispatcherInterceptor.processInvocation(ComponentDispatcherInterceptor.java:52)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.pool.PooledInstanceInterceptor.processInvocation(PooledInstanceInterceptor.java:51)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.tx.CMTTxInterceptor.invokeInOurTx(CMTTxInterceptor.java:275)
org.jboss.as.ejb3.tx.CMTTxInterceptor.requiresNew(CMTTxInterceptor.java:344)
org.jboss.as.ejb3.tx.CMTTxInterceptor.processInvocation(CMTTxInterceptor.java:241)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext$Invocation.proceed(InterceptorContext.java:437)
org.jboss.weld.ejb.AbstractEJBRequestScopeActivationInterceptor.aroundInvoke(AbstractEJBRequestScopeActivationInterceptor.java:64)
org.jboss.as.weld.ejb.EjbRequestScopeActivationInterceptor.processInvocation(EjbRequestScopeActivationInterceptor.java:83)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.remote.EJBRemoteTransactionPropagatingInterceptor.processInvocation(EJBRemoteTransactionPropagatingInterceptor.java:80)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor.processInvocation(CurrentInvocationContextInterceptor.java:41)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.invocationmetrics.WaitTimeInterceptor.processInvocation(WaitTimeInterceptor.java:43)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.security.SecurityContextInterceptor.processInvocation(SecurityContextInterceptor.java:100)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.deployment.processors.StartupAwaitInterceptor.processInvocation(StartupAwaitInterceptor.java:22)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.ShutDownInterceptorFactory$1.processInvocation(ShutDownInterceptorFactory.java:64)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.deployment.processors.EjbSuspendInterceptor.processInvocation(EjbSuspendInterceptor.java:44)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.LoggingInterceptor.processInvocation(LoggingInterceptor.java:66)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ee.component.NamespaceContextInterceptor.processInvocation(NamespaceContextInterceptor.java:50)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.AdditionalSetupInterceptor.processInvocation(AdditionalSetupInterceptor.java:54)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.ContextClassLoaderInterceptor.processInvocation(ContextClassLoaderInterceptor.java:64)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext.run(InterceptorContext.java:356)
org.wildfly.security.manager.WildFlySecurityManager.doChecked(WildFlySecurityManager.java:632)
org.jboss.invocation.AccessCheckingInterceptor.processInvocation(AccessCheckingInterceptor.java:61)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext.run(InterceptorContext.java:356)
org.jboss.invocation.PrivilegedWithCombinerInterceptor.processInvocation(PrivilegedWithCombinerInterceptor.java:80)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.ChainedInterceptor.processInvocation(ChainedInterceptor.java:61)
org.jboss.as.ee.component.ViewService$View.invoke(ViewService.java:198)
org.jboss.as.ejb3.remote.LocalEjbReceiver.processInvocation(LocalEjbReceiver.java:261)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:184)
org.jboss.ejb.client.EJBObjectInterceptor.handleInvocation(EJBObjectInterceptor.java:58)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.EJBHomeInterceptor.handleInvocation(EJBHomeInterceptor.java:83)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.TransactionInterceptor.handleInvocation(TransactionInterceptor.java:42)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.ReceiverInterceptor.handleInvocation(ReceiverInterceptor.java:138)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.EJBInvocationHandler.sendRequestWithPossibleRetries(EJBInvocationHandler.java:255)
org.jboss.ejb.client.EJBInvocationHandler.doInvoke(EJBInvocationHandler.java:200)
org.jboss.ejb.client.EJBInvocationHandler.doInvoke(EJBInvocationHandler.java:183)
org.jboss.ejb.client.EJBInvocationHandler.invoke(EJBInvocationHandler.java:146)
com.sun.proxy.$Proxy60.metodoA(Unknown Source)
com.example.poc.ejb.ExternalServiceBean.metodoA(ExternalServiceBean.java:64)
sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
java.lang.reflect.Method.invoke(Method.java:498)
org.jboss.as.ee.component.ManagedReferenceMethodInterceptor.processInvocation(ManagedReferenceMethodInterceptor.java:52)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext$Invocation.proceed(InterceptorContext.java:437)
org.jboss.as.weld.ejb.Jsr299BindingsInterceptor.doMethodInterception(Jsr299BindingsInterceptor.java:82)
org.jboss.as.weld.ejb.Jsr299BindingsInterceptor.processInvocation(Jsr299BindingsInterceptor.java:93)
org.jboss.as.ee.component.interceptors.UserInterceptorFactory$1.processInvocation(UserInterceptorFactory.java:63)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.invocationmetrics.ExecutionTimeInterceptor.processInvocation(ExecutionTimeInterceptor.java:43)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.jpa.interceptor.SBInvocationInterceptor.processInvocation(SBInvocationInterceptor.java:47)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ee.concurrent.ConcurrentContextInterceptor.processInvocation(ConcurrentContextInterceptor.java:45)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InitialInterceptor.processInvocation(InitialInterceptor.java:21)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.ChainedInterceptor.processInvocation(ChainedInterceptor.java:61)
org.jboss.as.ee.component.interceptors.ComponentDispatcherInterceptor.processInvocation(ComponentDispatcherInterceptor.java:52)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.pool.PooledInstanceInterceptor.processInvocation(PooledInstanceInterceptor.java:51)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.tx.CMTTxInterceptor.invokeInOurTx(CMTTxInterceptor.java:275)
org.jboss.as.ejb3.tx.CMTTxInterceptor.requiresNew(CMTTxInterceptor.java:344)
org.jboss.as.ejb3.tx.CMTTxInterceptor.processInvocation(CMTTxInterceptor.java:241)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext$Invocation.proceed(InterceptorContext.java:437)
org.jboss.weld.ejb.AbstractEJBRequestScopeActivationInterceptor.aroundInvoke(AbstractEJBRequestScopeActivationInterceptor.java:64)
org.jboss.as.weld.ejb.EjbRequestScopeActivationInterceptor.processInvocation(EjbRequestScopeActivationInterceptor.java:83)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.remote.EJBRemoteTransactionPropagatingInterceptor.processInvocation(EJBRemoteTransactionPropagatingInterceptor.java:80)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor.processInvocation(CurrentInvocationContextInterceptor.java:41)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.invocationmetrics.WaitTimeInterceptor.processInvocation(WaitTimeInterceptor.java:43)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.security.SecurityContextInterceptor.processInvocation(SecurityContextInterceptor.java:100)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.deployment.processors.StartupAwaitInterceptor.processInvocation(StartupAwaitInterceptor.java:22)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.ShutDownInterceptorFactory$1.processInvocation(ShutDownInterceptorFactory.java:64)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.deployment.processors.EjbSuspendInterceptor.processInvocation(EjbSuspendInterceptor.java:44)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.LoggingInterceptor.processInvocation(LoggingInterceptor.java:66)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ee.component.NamespaceContextInterceptor.processInvocation(NamespaceContextInterceptor.java:50)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.as.ejb3.component.interceptors.AdditionalSetupInterceptor.processInvocation(AdditionalSetupInterceptor.java:54)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.ContextClassLoaderInterceptor.processInvocation(ContextClassLoaderInterceptor.java:64)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext.run(InterceptorContext.java:356)
org.wildfly.security.manager.WildFlySecurityManager.doChecked(WildFlySecurityManager.java:632)
org.jboss.invocation.AccessCheckingInterceptor.processInvocation(AccessCheckingInterceptor.java:61)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.InterceptorContext.run(InterceptorContext.java:356)
org.jboss.invocation.PrivilegedWithCombinerInterceptor.processInvocation(PrivilegedWithCombinerInterceptor.java:80)
org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
org.jboss.invocation.ChainedInterceptor.processInvocation(ChainedInterceptor.java:61)
org.jboss.as.ee.component.ViewService$View.invoke(ViewService.java:198)
org.jboss.as.ejb3.remote.LocalEjbReceiver.processInvocation(LocalEjbReceiver.java:261)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:184)
org.jboss.ejb.client.EJBObjectInterceptor.handleInvocation(EJBObjectInterceptor.java:58)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.EJBHomeInterceptor.handleInvocation(EJBHomeInterceptor.java:83)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.TransactionInterceptor.handleInvocation(TransactionInterceptor.java:42)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.ReceiverInterceptor.handleInvocation(ReceiverInterceptor.java:138)
org.jboss.ejb.client.EJBClientInvocationContext.sendRequest(EJBClientInvocationContext.java:186)
org.jboss.ejb.client.EJBInvocationHandler.sendRequestWithPossibleRetries(EJBInvocationHandler.java:255)
org.jboss.ejb.client.EJBInvocationHandler.doInvoke(EJBInvocationHandler.java:200)
org.jboss.ejb.client.EJBInvocationHandler.doInvoke(EJBInvocationHandler.java:183)
org.jboss.ejb.client.EJBInvocationHandler.invoke(EJBInvocationHandler.java:146)
com.sun.proxy.$Proxy60.metodoA(Unknown Source)
com.example.poc.batch.service.RequiresNewDelegatorBean.ejecutarMetodoA(RequiresNewDelegatorBean.java:20)
com.example.poc.batch.service.RequiresNewDelegatorBean$Proxy$_$$_WeldClientProxy.ejecutarMetodoA(Unknown Source)
com.example.poc.batch.service.TransactionalSeparationBatchletService.ejecutarProcesoBatch(TransactionalSeparationBatchletService.java:31)
com.example.poc.batch.web.AlternativasBatchletServlet.doGet(AlternativasBatchletServlet.java:58)
javax.servlet.http.HttpServlet.service(HttpServlet.java:687)
javax.servlet.http.HttpServlet.service(HttpServlet.java:790)
io.undertow.servlet.handlers.ServletHandler.handleRequest(ServletHandler.java:85)
io.undertow.servlet.handlers.security.ServletSecurityRoleHandler.handleRequest(ServletSecurityRoleHandler.java:62)
io.undertow.servlet.handlers.ServletDispatchingHandler.handleRequest(ServletDispatchingHandler.java:36)
org.wildfly.extension.undertow.security.SecurityContextAssociationHandler.handleRequest(SecurityContextAssociationHandler.java:78)
io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43)
io.undertow.servlet.handlers.security.SSLInformationAssociationHandler.handleRequest(SSLInformationAssociationHandler.java:131)
io.undertow.servlet.handlers.security.ServletAuthenticationCallHandler.handleRequest(ServletAuthenticationCallHandler.java:57)
io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43)
io.undertow.security.handlers.AbstractConfidentialityHandler.handleRequest(AbstractConfidentialityHandler.java:46)
io.undertow.servlet.handlers.security.ServletConfidentialityConstraintHandler.handleRequest(ServletConfidentialityConstraintHandler.java:64)
io.undertow.security.handlers.AuthenticationMechanismsHandler.handleRequest(AuthenticationMechanismsHandler.java:60)
io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler.handleRequest(CachedAuthenticatedSessionHandler.java:77)
io.undertow.security.handlers.NotificationReceiverHandler.handleRequest(NotificationReceiverHandler.java:50)
io.undertow.security.handlers.AbstractSecurityContextAssociationHandler.handleRequest(AbstractSecurityContextAssociationHandler.java:43)
io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43)
org.wildfly.extension.undertow.security.jacc.JACCContextIdHandler.handleRequest(JACCContextIdHandler.java:61)
io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43)
io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43)
io.undertow.servlet.handlers.ServletInitialHandler.handleFirstRequest(ServletInitialHandler.java:285)
io.undertow.servlet.handlers.ServletInitialHandler.dispatchRequest(ServletInitialHandler.java:264)
io.undertow.servlet.handlers.ServletInitialHandler.access$000(ServletInitialHandler.java:81)
io.undertow.servlet.handlers.ServletInitialHandler$1.handleRequest(ServletInitialHandler.java:175)
io.undertow.server.Connectors.executeRootHandler(Connectors.java:324)
io.undertow.server.HttpServerExchange$1.run(HttpServerExchange.java:803)
java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
java.lang.Thread.run(Thread.java:750)
2025-08-05 23:25:41,350 WARN  [com.arjuna.ats.arjuna] (Transaction Reaper Worker 0) ARJUNA012108: CheckedAction::check - atomic action 0:ffffac1c9001:-585a8e34:6892d2a8:d9 aborting with 1 threads active!
2025-08-05 23:25:41,352 WARN  [org.hibernate.resource.transaction.backend.jta.internal.synchronization.SynchronizationCallbackCoordinatorTrackingImpl] (Transaction Reaper Worker 0) HHH000451: Transaction afterCompletion called by a background thread; delaying afterCompletion processing until the original thread can handle it. [status=4]
2025-08-05 23:25:41,353 WARN  [org.hibernate.resource.transaction.backend.jta.internal.synchronization.SynchronizationCallbackCoordinatorTrackingImpl] (Transaction Reaper Worker 0) HHH000451: Transaction afterCompletion called by a background thread; delaying afterCompletion processing until the original thread can handle it. [status=4]
2025-08-05 23:25:41,355 WARN  [com.arjuna.ats.arjuna] (Transaction Reaper Worker 0) ARJUNA012121: TransactionReaper::doCancellations worker Thread[Transaction Reaper Worker 0,5,main] successfully canceled TX 0:ffffac1c9001:-585a8e34:6892d2a8:d9
2025-08-05 23:25:41,360 INFO  [stdout] (default task-6) Simulando fallo en metodoA() en la iteración 3
2025-08-05 23:25:41,360 INFO  [stdout] (default task-6) Excepción controlada capturada en metodoA(): Fallo simulado en metodoA() en la iteración 3
2025-08-05 23:25:41,360 WARN  [com.arjuna.ats.arjuna] (default task-6) ARJUNA012077: Abort called on already aborted atomic action 0:ffffac1c9001:-585a8e34:6892d2a8:d9
2025-08-05 23:25:41,361 ERROR [org.jboss.as.ejb3.invocation] (default task-6) WFLYEJB0034: EJB Invocation failed on component ExternalServiceBean for method public abstract void com.example.poc.ejb.ExternalService.metodoA(int,java.lang.String) throws javax.transaction.SystemException: javax.ejb.EJBTransactionRolledbackException: Transaction rolled back
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.handleEndTransactionException(CMTTxInterceptor.java:137)
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.endTransaction(CMTTxInterceptor.java:117)
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.invokeInOurTx(CMTTxInterceptor.java:279)
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.requiresNew(CMTTxInterceptor.java:344)
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.processInvocation(CMTTxInterceptor.java:241)
    at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
    ...
Caused by: javax.transaction.RollbackException: WFLYEJB0447: Transaction 'TransactionImple < ac, BasicAction: 0:ffffac1c9001:-585a8e34:6892d2a8:d9 status: ActionStatus.ABORTED >' was already rolled back
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.endTransaction(CMTTxInterceptor.java:98)
    ... 245 more
2025-08-05 23:25:41,366 ERROR [stderr] (default task-6) Error inesperado en metodoA(): Transaction rolled back
2025-08-05 23:25:41,366 WARN  [com.arjuna.ats.arjuna] (default task-6) ARJUNA012077: Abort called on already aborted atomic action 0:ffffac1c9001:-585a8e34:6892d2a8:d0
2025-08-05 23:25:41,367 ERROR [org.jboss.as.ejb3.invocation] (default task-6) WFLYEJB0034: EJB Invocation failed on component ExternalServiceBean for method public abstract void com.example.poc.ejb.ExternalService.metodoA(int,java.lang.String) throws javax.transaction.SystemException: javax.ejb.EJBTransactionRolledbackException: Transaction rolled back
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.handleEndTransactionException(CMTTxInterceptor.java:137)
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.endTransaction(CMTTxInterceptor.java:117)
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.invokeInOurTx(CMTTxInterceptor.java:279)
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.requiresNew(CMTTxInterceptor.java:344)
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.processInvocation(CMTTxInterceptor.java:241)
    at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
    ...
Caused by: javax.transaction.RollbackException: WFLYEJB0447: Transaction 'TransactionImple < ac, BasicAction: 0:ffffac1c9001:-585a8e34:6892d2a8:d0 status: ActionStatus.ABORTED >' was already rolled back
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.endTransaction(CMTTxInterceptor.java:98)
    ... 166 more
2025-08-05 23:25:41,369 ERROR [stderr] (default task-6) Error inesperado en metodoA(): Transaction rolled back
2025-08-05 23:25:41,369 WARN  [com.arjuna.ats.arjuna] (default task-6) ARJUNA012077: Abort called on already aborted atomic action 0:ffffac1c9001:-585a8e34:6892d2a8:ca
2025-08-05 23:25:41,370 ERROR [org.jboss.as.ejb3.invocation] (default task-6) WFLYEJB0034: EJB Invocation failed on component ExternalServiceBean for method public abstract void com.example.poc.ejb.ExternalService.metodoA(int,java.lang.String) throws javax.transaction.SystemException: javax.ejb.EJBTransactionRolledbackException: Transaction rolled back
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.handleEndTransactionException(CMTTxInterceptor.java:137)
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.endTransaction(CMTTxInterceptor.java:117)
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.invokeInOurTx(CMTTxInterceptor.java:279)
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.requiresNew(CMTTxInterceptor.java:349)
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.processInvocation(CMTTxInterceptor.java:241)
    at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
    ...
Caused by: javax.transaction.RollbackException: WFLYEJB0447: Transaction 'TransactionImple < ac, BasicAction: 0:ffffac1c9001:-585a8e34:6892d2a8:ca status: ActionStatus.ABORTED >' was already rolled back
    at org.jboss.as.ejb3.tx.CMTTxInterceptor.endTransaction(CMTTxInterceptor.java:98)
    ... 87 more
2025-08-05 23:25:41,372 ERROR [stderr] (default task-6) TransactionalSeparationBatchletService - Error controlado en operación 1: Transaction rolled back
```

---


**Análisis adicional sobre advertencias ARJUNA en la alternativa B:**

A pesar de que la alternativa B elimina el error crítico `ARJUNA012120` y el riesgo de rollback global, en los logs pueden seguir apareciendo otras advertencias de Arjuna, como:

- `ARJUNA012117`: TransactionReaper::check timeout for TX ... in state RUN
- `ARJUNA012095`: Abort of action id ... invoked while multiple threads active within it.
- `ARJUNA012381`: Action id ... completed with multiple threads ...
- `ARJUNA012121`: TransactionReaper::doCancellations worker ... successfully canceled TX ...
- `ARJUNA012108`: CheckedAction::check - atomic action ... aborting with N threads active!
- `ARJUNA012077`: Abort called on already aborted atomic action ...

Estas advertencias reflejan el manejo interno de timeouts, abortos y concurrencia en la gestión de transacciones por parte del Transaction Manager de JBoss/Arjuna. En la alternativa B, estos mensajes suelen estar asociados únicamente a la transacción puntual de la operación fallida (por ejemplo, un commit o insert que excede el timeout), pero **no afectan a las demás operaciones ni provocan rollback global**.

**Puntos clave:**
- Estas advertencias son esperadas cuando se fuerzan timeouts o abortos en operaciones individuales.
- No implican hilos zombie ni inconsistencias globales si se aíslan correctamente las transacciones con `REQUIRES_NEW`.
- El sistema sigue siendo robusto y los commits previos al error se mantienen.
- Solo la operación fallida es revertida y logueada con detalle.

**Conclusión:**  
- El escenario original reproduce el problema ARJUNA012120 y demuestra el riesgo de rollback global y hilos zombies.
- La alternativa A mejora el control de errores, pero no soluciona el problema de fondo.
- La alternativa B es la solución recomendada: aísla transacciones, evita rollbacks globales y elimina el riesgo de hilos zombies. Las advertencias adicionales de Arjuna son normales en abortos individuales y no representan un riesgo sistémico.
- Equipo de desarrollo PoC https://cristhianc9.github.io/
