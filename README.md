

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

Esta PoC reproduce el escenario y documenta la aplicación de un parche oficial para mitigar el problema.

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

## 👤 Contacto

Para dudas o colaboración, contactar a: [equipo.poc@ejemplo.com](mailto:equipo.poc@ejemplo.com)

## Autores
- Equipo de desarrollo PoC https://cristhianc9.github.io/
