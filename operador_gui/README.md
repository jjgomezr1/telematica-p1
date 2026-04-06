# Cliente Operador con GUI

## Que hace

- Se conecta al servidor usando `host`, `puerto` e `id` de operador configurables.
- Envia `REGISTER OPERATOR <id>`.
- Consulta `LIST` y `STATUS`.
- Muestra una GUI con:
  - sensores activos
  - panel de mediciones recientes
  - panel de alertas
  - estado de conexion y estado general del sistema
- Mantiene escucha continua del socket para aceptar notificaciones `push` como `ALERT ...`.
- Hace refresco periodico de `LIST` para mantener visibles las mediciones recientes.
- Reintenta conexion automaticamente si la red falla.

## Estructura

- `src/operatorgui/OperatorMain.java`
- `src/operatorgui/OperatorFrame.java`
- `src/operatorgui/OperatorClient.java`
- `src/operatorgui/OperatorEventListener.java`
- `src/operatorgui/SensorSnapshot.java`

## Requisitos

- **JDK 8 o superior** para compilar.
- Java Runtime para ejecutar.

## Compilar

Desde la raiz del proyecto:

```powershell
cd .\operador_gui\
javac -d out src\operatorgui\OperatorMain.java src\operatorgui\OperatorFrame.java src\operatorgui\OperatorClient.java src\operatorgui\OperatorEventListener.java src\operatorgui\SensorSnapshot.java
```

## Ejecutar

```powershell
cd .\operador_gui\
java -cp out operatorgui.OperatorMain localhost 9000 operator_sebas
```

Parametros:

- argumento 1: `host`
- argumento 2: `puerto`
- argumento 3: `id_operador`

Si no envias argumentos, la aplicacion usa:

- host: `localhost`
- puerto: `9000`
- operador: `operator_persona3`

## Uso esperado

1. Inicia el servidor del proyecto.
2. Abre el cliente operador.
3. Pulsa `Conectar`.
4. Observa la lista de sensores activos y sus ultimas mediciones.
5. Revisa el panel de alertas para eventos anomalos enviados por el servidor.

## Nota importante

Este cliente ya queda preparado para recibir mensajes `push` del servidor en tiempo real.  
Si el servidor solo responde a consultas y no empuja alertas por si mismo, la GUI sigue mostrando sensores y mediciones mediante consultas periodicas de `LIST`.
