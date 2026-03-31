# Especificación del Protocolo de Aplicación - IoT Monitoring System

---

## 1. Visión General

Este protocolo define la comunicación entre:
- **Sensores IoT**: dispositivos que envían mediciones periodicamente
- **Operadores**: clientes que supervisan el sistema
- **Servidor Central**: recibe, procesa y distribuye información

El protocolo está basado en **texto** usando **TCP/IP** con formato de mensajes basado en líneas (`\n`).

---

## 2. Especificación de Mensajes

### 2.1 Registro de Sensores

**Comando:** `REGISTER`

```
REGISTER SENSOR <id_sensor>
```

**Descripción:** Un sensor se registra en el servidor.

**Parámetros:**
- `id_sensor`: identificador único del sensor (ej: `temp_sensor_01`)

**Respuesta esperada:**
```
OK registered <id_sensor>
```

**Ejemplo:**
```
→ REGISTER SENSOR temp_sensor_01
← OK registered temp_sensor_01
```

---

### 2.2 Envío de Mediciones

**Comando:** `MEASURE`

```
MEASURE <id_sensor> <tipo> <valor>
```

**Descripción:** El sensor envía una medición al servidor.

**Parámetros:**
- `id_sensor`: identificador del sensor
- `tipo`: tipo de medición (`temp`, `vibration`, `energy`, `humidity`, etc.)
- `valor`: valor numérico (float)

**Respuesta esperada:**
```
OK
```
o en caso de alerta:
```
ALERT <tipo_alerta> <valor>
```

**Ejemplo sin alerta:**
```
→ MEASURE temp_sensor_01 temp 25.5
← OK
```

**Ejemplo con alerta:**
```
→ MEASURE temp_sensor_01 temp 95.0
← ALERT HIGH_TEMP 95.0
```

---

### 2.3 Consultar Lista de Sensores

**Comando:** `LIST`

```
LIST
```

**Descripción:** Obtiene la lista de todos los sensores registrados.

**Respuesta esperada:**
```
SENSORS <cantidad>
<id> <tipo> <valor>
<id> <tipo> <valor>
...
END
```

**Ejemplo:**
```
→ LIST
← SENSORS 2
← temp_sensor_01 temp 25.5
← vibration_sensor_01 vibration 1.2
← END
```

---

### 2.4 Consultar Estado del Sistema

**Comando:** `STATUS`

```
STATUS
```

**Descripción:** Obtiene el estado actual del sistema.

**Respuesta esperada:**
```
STATUS OK
```

**Ejemplo:**
```
→ STATUS
← STATUS OK
```

---

### 2.5 Registro de Operador

**Comando:** `REGISTER OPERATOR`

```
REGISTER OPERATOR <id_operador>
```

**Descripción:** Un operador se registra para recibir notificaciones.

**Parámetros:**
- `id_operador`: identificador del operador

**Respuesta esperada:**
```
OK registered <id_operador>
```

**Ejemplo:**
```
→ REGISTER OPERATOR operator_juan
← OK registered operator_juan
```

---

### 2.6 Autenticación

**Comando:** `AUTH`

```
AUTH <credencial>
```

**Descripción:** Autentica un cliente con credenciales.

**Parámetros:**
- `credencial`: token o credencial de autenticación

**Respuesta esperada:**
```
OK auth <credencial>
```

**Ejemplo:**
```
→ AUTH token_12345
← OK auth token_12345
```

---

## 3. Alertas

El sistema genera alertas automáticas en los siguientes casos:

| Tipo de Sensor | Condición | Mensaje |
|---|---|---|
| `temp` | valor > 90°C | `ALERT HIGH_TEMP <valor>` |
| `vibration` | valor > 5.0 | `ALERT HIGH_VIBRATION <valor>` |
| `energy` | valor > 300 W | `ALERT HIGH_ENERGY <valor>` |

---

## 4. Códigos de Error

| Error | Descripción |
|---|---|
| `ERROR faltan argumentos` | Comando incompleto |
| `ERROR tipo invalido` | Tipo de cliente desconocido |
| `ERROR solo sensores pueden enviar mediciones` | Operador intentó enviar MEASURE |
| `ERROR conexión perdida` | Cliente se desconectó |

---

## 5. Secuencia Típica de Operación

### Flujo de un Sensor

```
1. Sensor se conecta al servidor (TCP)
2. Sensor envía: REGISTER SENSOR <id>
3. Servidor responde: OK registered <id>
4. Sensor entra en bucle de envío:
   a. Sensor genera medición
   b. Sensor envía: MEASURE <id> <tipo> <valor>
   c. Servidor responde: OK (o ALERT si aplica)
   d. Esperar X segundos antes de siguiente medición
5. Sensor se desconecta (conexión se cierra)
```

### Flujo de un Operador

```
1. Operador se conecta al servidor (TCP)
2. Operador envía: REGISTER OPERATOR <id>
3. Servidor responde: OK registered <id>
4. Operador puede:
   a. Enviar LIST para ver sensores activos
   b. Enviar STATUS para ver estado
   c. Recibir notificaciones de alertas en tiempo real
5. Operador se desconecta
```

---

## 6. Características de Implementación

- **Protocolo:** TCP/IP (SOCK_STREAM)
- **Puerto:** 9000 (por defecto)
- **Formato:** Texto delimitado por `\n`
- **Encoding:** UTF-8
- **Terminación de línea:** `\n` (LF)
- **Timeout:** 30 segundos por defecto
- **Concurrencia:** El servidor se permite usar hilos
- **Reconexión:** Los sensores deben reintentar conexión si fallan

---

## 7. Ejemplo de Sesión Completa

```
[Sensor 1 se conecta]
REGISTER SENSOR temp_sensor_01
OK registered temp_sensor_01

MEASURE temp_sensor_01 temp 22.5
OK

MEASURE temp_sensor_01 temp 23.0
OK

MEASURE temp_sensor_01 temp 92.0
ALERT HIGH_TEMP 92.0

[Operador se conecta]
REGISTER OPERATOR operator_01
OK registered operator_01

LIST
SENSORS 1
temp_sensor_01 temp 92.0
END

STATUS
STATUS OK
```

---

## 8. Consideraciones de Robustez

1. **Reconexión automática:** Si un sensor pierde conexión, debe reintentar cada 5 segundos
2. **Manejo de errores:** Los clientes deben manejar excepciones de red
3. **Validación:** El servidor valida todos los mensajes recibidos
4. **Logging:** Todos los mensajes se registran (IP, puerto, contenido)
5. **Limpieza:** Sensores desconectados se marcan como inactivos inmediatamente cuando se cierra la conexión
6. **Compatibilidad:** El servidor soporta múltiples conexiones simultáneas usando hilos independientes
7. **Timeout:** La conexión se puede desconectar por inactividad (depende de la configuración del socket)

---