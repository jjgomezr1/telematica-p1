# Servidor IoT en C

Este directorio contiene el servidor principal del sistema IoT. Está escrito en C y escucha conexiones TCP de sensores y operadores para registrar dispositivos, recibir mediciones, emitir alertas y entregar información del estado actual del sistema.

## Propósito

El servidor actúa como el punto central de comunicación entre:

- Sensores, que se registran y envían mediciones.
- Operadores, que reciben alertas en tiempo real y pueden consultar el estado del sistema.
- El servicio de identidad, usado para validar credenciales en el comando `AUTH`.

Además de atender clientes, el servidor mantiene:

- Una lista de sensores activos y su último valor reportado.
- Una lista de operadores conectados para hacer broadcast de alertas.
- Un historial circular de alertas recientes.
- Un registro de eventos en consola y en archivo.

## Estructura del módulo

- `src/main.c`: punto de entrada del binario.
- `src/server.c`: crea el socket TCP, escucha conexiones y lanza un hilo por cliente.
- `src/client_handler.c`: lee comandos de cada cliente y decide cómo responder.
- `src/protocol.c`: convierte texto crudo en mensajes parseados.
- `src/sensor_list.c`: administra sensores registrados y sus lecturas.
- `src/operator_list.c`: administra operadores conectados y reenvía alertas.
- `src/alert_history.c`: guarda el historial de alertas en una cola circular.
- `src/logger.c`: imprime y persiste logs con marca de tiempo.

## Flujo de ejecución

### 1. Arranque

El proceso comienza en `main()`:

1. Valida los argumentos de entrada.
2. Inicializa el logger.
3. Inicializa las estructuras globales del sistema.
4. Llama a `server_run(puerto)` para empezar a aceptar conexiones.

Formato de uso:

```bash
./server <puerto> <archivoDeLogs>
```

Ejemplo:

```bash
./server 9000 logs/server.log
```

### 2. Escucha de conexiones

`server_run()` crea un socket TCP, activa `SO_REUSEADDR`, hace `bind()` sobre el puerto indicado y luego entra en un ciclo infinito de `accept()`.

Cada cliente aceptado se registra con su IP y puerto de origen, se envía a un hilo independiente mediante `pthread_create()` y el hilo se separa con `pthread_detach()`.

### 3. Atención por cliente

`handle_client()` recibe los bytes del socket, acumula fragmentos hasta encontrar saltos de línea y procesa cada comando completo con `parse_message()`.

Este diseño permite soportar mensajes que lleguen partidos en varias lecturas de `recv()`.

## Protocolo soportado

Los comandos que entiende el servidor están definidos en `protocol.h` y se parsean en `parse_message()`.

### `REGISTER`

Registra un cliente en el sistema.

Uso esperado:

```text
REGISTER SENSOR <id>
REGISTER OPERATOR <id>
```

Comportamiento:

- Si el cliente es un sensor, se guarda en `sensor_list`.
- Si el cliente es un operador, se agrega a `operator_list`.
- Responde con `OK registered <id>`.

### `MEASURE`

Permite a un sensor enviar una lectura.

Uso esperado:

```text
MEASURE <id> <tipo> <valor>
```

Ejemplos de tipos usados por el código:

- `temp`
- `vibration`
- `energy`

Comportamiento:

- Solo se acepta desde sensores registrados.
- Actualiza el último valor del sensor.
- Si supera ciertos umbrales, genera una alerta.
- Si no supera el umbral, responde `OK`.

Umbrales actuales:

- Temperatura: mayor que `90.0` genera `ALERT HIGH_TEMP`.
- Vibración: mayor que `5.0` genera `ALERT HIGH_VIBRATION`.
- Energía: mayor que `300.0` genera `ALERT HIGH_ENERGY`.

Cuando se genera una alerta:

1. Se envía a todos los operadores conectados.
2. Se guarda en el historial de alertas.
3. El sensor recibe igualmente `OK` para no interrumpir su flujo.

### `LIST`

Devuelve la lista de sensores activos con su estado actual.

Salida general:

```text
SENSORS <n>
<id> <tipo> <ultimo_valor>
...
END
```

### `STATUS`

Retorna un mensaje simple de salud:

```text
STATUS OK
```

### `AUTH`

Valida credenciales contra el servicio `identity-svc`.

Uso esperado:

```text
AUTH <usuario> <clave>
```

Comportamiento:

- Resuelve `identity-svc` por DNS interno del entorno Docker.
- Se conecta al puerto `5001`.
- Envía `AUTH usuario clave` al servicio de identidad.
- Responde `OK auth accepted` o `ERROR auth denied`.

Si el servicio no está disponible, devuelve un error explicando el fallo de conexión.

### `ALERTS`

Devuelve el historial reciente de alertas guardadas en memoria.

Formato:

```text
ALERTS_START
...
ALERTS_END
```

## Funciones principales

### `main()`

Archivo: `src/main.c`

Responsabilidad:

- Recibir puerto y ruta del archivo de logs.
- Inicializar logger, listas y historial.
- Ejecutar el servidor.
- Cerrar el logger al terminar.

### `server_run(int puerto)`

Archivo: `src/server.c`

Responsabilidad:

- Crear y configurar el socket TCP.
- Escuchar en todas las interfaces.
- Aceptar conexiones infinitamente.
- Construir la estructura `ClientInfo` para cada cliente.
- Lanzar un hilo por conexión.

### `handle_client(void *arg)`

Archivo: `src/client_handler.c`

Responsabilidad:

- Recibir datos del socket.
- Reensamblar mensajes por línea.
- Parsear cada comando.
- Llamar a `procesar()` para aplicar la lógica.
- Limpiar el estado del cliente al desconectarse.

### `parse_message(const char *raw)`

Archivo: `src/protocol.c`

Responsabilidad:

- Separar el comando principal y sus argumentos.
- Identificar el tipo de mensaje.
- Copiar los argumentos en una estructura segura de tamaño fijo.

### `sensor_list_*()`

Archivo: `src/sensor_list.c`

Responsabilidad:

- Inicializar la lista.
- Registrar sensores nuevos.
- Actualizar el tipo y último valor.
- Marcar sensores como inactivos al desconectarse.
- Entregar una copia de la lista para el comando `LIST`.

La implementación usa un mutex global para proteger accesos concurrentes desde varios hilos.

### `operator_list_*()`

Archivo: `src/operator_list.c`

Responsabilidad:

- Inicializar la lista de operadores.
- Agregar o quitar sockets activos.
- Reenviar alertas a todos los operadores conectados.

### `alert_history_*()`

Archivo: `src/alert_history.c`

Responsabilidad:

- Inicializar el historial circular.
- Guardar alertas nuevas sobreescribiendo las más antiguas cuando se llena.
- Exportar el historial acumulado para el comando `ALERTS`.

### `log_event()` y `log_error()`

Archivo: `src/logger.c`

Responsabilidad:

- Imprimir eventos con timestamp.
- Persistirlos en el archivo configurado.
- Proteger la escritura con mutex para evitar mezclas entre hilos.

## Construcción

El archivo `Makefile` genera el binario `server`.

```bash
make
```

Para limpiar el binario:

```bash
make clean
```

## Uso local

### Compilación nativa

Desde esta carpeta:

```bash
make
./server 9000 logs/server.log
```

### Contenedor Docker

El `Dockerfile` compila el binario dentro de `ubuntu:22.04`, crea `logs/` y deja el servidor escuchando en el puerto `9000`.

Comando que usa el contenedor:

```bash
./server 9000 logs/server.log
```

## Sincronización con el resto del sistema

Este servidor está pensado para trabajar con el resto de la arquitectura del proyecto:

- Los sensores del directorio `sensores/` envían registros y mediciones.
- La interfaz web de `auth_web/` consulta el historial y el estado del servidor.
- El servicio `identidad_svc/` valida credenciales para `AUTH`.

En el `docker-compose.yml` del proyecto raíz, este servicio expone el puerto `9000` y comparte la carpeta `servidor/logs` como volumen para conservar los logs fuera del contenedor.

## Observaciones técnicas

- El servidor trabaja con hilos por cliente y usa mutexes en las estructuras compartidas.
- Los comandos `LIST` y `STATUS` generan respuestas multilinea, por eso el logger marca esos casos para evitar registrar contenido largo en forma completa.
- El manejo de alertas está orientado a mantener informados a los operadores en tiempo real sin bloquear la recepción de sensores.
