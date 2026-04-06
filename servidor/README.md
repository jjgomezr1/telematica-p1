# Servidor IoT
Este directorio contiene el backend TCP del sistema IoT. El servidor recibe conexiones de sensores y operadores, procesa comandos del protocolo de texto y mantiene estado en memoria para sensores, operadores y alertas.

## Estructura del servidor

- `src/main.c`: arranque del proceso e inicializacion global.
- `src/server.c`: socket TCP, `bind`, `listen`, `accept` y creacion de hilos por cliente.
- `src/client_handler.c`: ciclo de lectura por cliente y logica de comandos.
- `src/protocol.c`: parser del protocolo de texto.
- `src/sensor_list.c`: inventario de sensores activos y ultimo valor.
- `src/operator_list.c`: lista de operadores conectados para broadcast de alertas.
- `src/alert_history.c`: historial circular de alertas (ultimas `MAX_HISTORIAL`).
- `src/logger.c`: logs por consola y archivo.

## Flujo principal

1. `main()` valida argumentos (`puerto` y `archivoDeLogs`).
2. Inicializa servicios compartidos:
	 - `logger_init()`
	 - `sensor_list_init()`
	 - `operator_list_init()`
	 - `alert_history_init()`
3. Ejecuta `server_run(puerto)`.
4. `server_run()` acepta conexiones y crea un hilo por cliente con `handle_client()`.
5. Cada hilo parsea lineas entrantes y ejecuta `procesar()`.

## Protocolo soportado (actual)

Todos los mensajes son lineas de texto terminadas en `\n`.

### 1) REGISTER

Formato:

```text
REGISTER SENSOR <id>
REGISTER OPERATOR <id>
```

Comportamiento:

- Registra el tipo de cliente.
- Si es sensor, lo agrega a `sensor_list`.
- Si es operador, guarda su socket en `operator_list`.
- Respuesta: `OK registered <id>`.

### 2) MEASURE

Formato:

```text
MEASURE <id> <tipo> <valor>
```

Reglas:

- Solo clientes tipo sensor pueden enviar mediciones.
- Actualiza el valor del sensor con `sensor_list_update()`.
- Evalua umbrales:
	- `temp > 90.0` -> `ALERT HIGH_TEMP`
	- `vibration > 5.0` -> `ALERT HIGH_VIBRATION`
	- `energy > 300.0` -> `ALERT HIGH_ENERGY`

Si hay alerta:

- Se hace broadcast a operadores con `operator_list_broadcast()`.
- Se guarda en historial con `alert_history_add()`.
- Al sensor se le responde `OK` para no cortar su flujo.

### 3) LIST

Retorna sensores activos:

```text
SENSORS <n>
<id> <tipo> <ultimo_valor>
...
END
```

### 4) STATUS

Respuesta de salud:

```text
STATUS OK
```

### 5) AUTH

Formato:

```text
AUTH <usuario> <clave>
```

Flujo:

- Resuelve `identity-svc`.
- Conecta por TCP al puerto `5001`.
- Reenvia `AUTH usuario clave` al servicio de identidad.
- Responde al cliente:
	- `OK auth accepted` si identidad responde OK.
	- `ERROR auth denied` o mensaje de error de conectividad en otros casos.

### 6) ALERTS

Devuelve historial de alertas en memoria:

```text
ALERTS_START
<alerta_1>
...
ALERTS_END
```

## Funciones clave por modulo

### `src/main.c`

- `main(int argc, char *argv[])`: punto de entrada del programa.
	- Valida argumentos de ejecucion (`puerto` y `archivoDeLogs`).
	- Inicializa logger, lista de sensores, lista de operadores e historial de alertas.
	- Arranca el loop principal de red con `server_run(...)`.

### `src/server.c`

- `server_run(int puerto)`: ciclo de vida del servidor TCP.
	- Crea y configura socket (`socket`, `setsockopt`, `bind`, `listen`).
	- Acepta conexiones entrantes con `accept`.
	- Obtiene IP/puerto del cliente para logging.
	- Crea un hilo detached por cliente para delegar la atencion sin bloquear nuevas conexiones.

### `src/client_handler.c`

- `handle_client(void *arg)`: atiende una conexion individual.
	- Mantiene un loop de `recv` hasta desconexion/error.
	- Recompone mensajes por linea para soportar paquetes fragmentados.
	- Parsea cada linea con `parse_message` y delega en `procesar`.
	- Limpia estado al desconectar (sensor u operador).
- `procesar(...)`: nucleo de negocio del protocolo.
	- Ejecuta comandos `REGISTER`, `MEASURE`, `LIST`, `STATUS`, `AUTH` y `ALERTS`.
	- Actualiza estructuras en memoria y dispara alertas cuando aplica.
	- Construye la respuesta de texto y registra el evento en logs.
- `responder(int fd, const char *msg)`: envia la respuesta final al socket cliente.

### `src/protocol.c`

- `parse_message(const char *raw)`: parser de mensajes de una linea.
	- Identifica el tipo de comando (`CMD_*`).
	- Extrae argumentos y los guarda en `ParsedMessage` con limites de longitud.

### `src/sensor_list.c`

- `sensor_list_init()`: limpia la estructura global de sensores al arranque.
- `sensor_list_register(const char *id, const char *tipo)`: agrega un sensor nuevo si no existe.
- `sensor_list_update(const char *id, const char *tipo, double valor)`: actualiza tipo y ultimo valor reportado.
- `sensor_list_remove(const char *id)`: marca un sensor como inactivo al desconectarse.
- `sensor_list_get_all(SensorEntry *destino, int max)`: devuelve una copia de sensores activos para comandos como `LIST`.

Notas:

- Este modulo usa mutex para proteger concurrencia entre hilos.
- `total` refleja sensores registrados historicamente; el campo `activo` indica si siguen conectados.

### `src/operator_list.c`

- `operator_list_init()`: inicializa la tabla de sockets de operadores.
- `operator_list_add(int fd)`: registra el socket de un operador conectado.
- `operator_list_remove(int fd)`: elimina el socket cuando el operador se desconecta.
- `operator_list_broadcast(const char *mensaje)`: envia una alerta a todos los operadores activos.

Notas:

- La lista tiene capacidad fija (`MAX_OPERATORS`).
- Las operaciones estan protegidas por mutex para evitar carreras.

### `src/alert_history.c`

- `alert_history_init()`: inicializa la cola circular y su mutex.
- `alert_history_add(const char *alerta)`: inserta una alerta nueva en memoria.
- `alert_history_get_all(char *destino, int max_size)`: serializa el historial completo a texto para responder `ALERTS`.

El historial es circular: cuando se llena (`MAX_HISTORIAL = 20`), sobrescribe alertas antiguas.

### `src/logger.c`

- `logger_init(const char *filepath)`: abre el archivo de log en modo append.
- `log_event(const char *ip, int port, const char *mensaje, const char *respuesta)`: registra eventos normales en consola y archivo con timestamp.
- `log_error(const char *ip, int port, const char *descripcion)`: registra errores operativos de red/procesamiento.
- `logger_close()`: cierra el archivo al finalizar el proceso.

## Concurrencia y seguridad basica

- Hay un hilo por cliente (`pthread`).
- Estructuras compartidas usan mutex:
	- sensores (`sensor_list`)
	- operadores (`operator_list`)
	- historial (`alert_history`)
	- logger (`logger`)

## Compilacion

Desde este directorio:

```bash
make
```

Genera el binario `server` con:

```make
src/main.c src/logger.c src/protocol.c src/server.c src/client_handler.c src/sensor_list.c src/operator_list.c src/alert_history.c
```

Limpiar:

```bash
make clean
```

## Ejecucion

### Nativo

```bash
./server 9000 logs/server.log
```

### Docker (segun `Dockerfile`)

- Base: `ubuntu:22.04`
- Instala: `gcc`, `make`
- Compila con `make`
- Expone: `9000`
- Comando final:

```bash
./server 9000 logs/server.log
```

## Integracion con el proyecto

El servidor es el eje de comunicacion entre los demas componentes. Esta es la interaccion real por flujo:

### Flujo 1: Sensores -> Servidor -> Operadores

1. Un sensor del modulo `sensores/` abre socket TCP hacia el servidor (`iot-server:9000`).
2. El sensor envia `REGISTER SENSOR <id>` para quedar registrado.
3. Envia periodicamente `MEASURE <id> <tipo> <valor>`.
4. El servidor actualiza `sensor_list`.
5. Si el valor supera umbral, el servidor:
	- genera una alerta,
	- la guarda en `alert_history`,
	- y la reenvia a todos los operadores activos por `operator_list_broadcast`.

Resultado: los operadores ven alertas en tiempo real sin hacer polling constante.

### Flujo 2: Operador (GUI/Web) -> Servidor

1. Un cliente operador (Java GUI en `operador_gui/` o frontend en `auth_web/`) se conecta al puerto 9000.
2. Envia `REGISTER OPERATOR <id>` para suscribirse a alertas push.
3. Puede consultar estado bajo demanda con:
	- `LIST` para sensores activos y ultimo valor,
	- `STATUS` para health-check,
	- `ALERTS` para recuperar historial reciente.

Resultado: el operador combina snapshot actual (`LIST`) + stream de alertas + historial.

### Flujo 3: Operador -> Servidor -> Identity Service

1. El operador inicia autenticacion con `AUTH <usuario> <clave>`.
2. El servidor abre una conexion saliente a `identity-svc:5001`.
3. Reenvia la solicitud `AUTH` y espera respuesta.
4. Devuelve al cliente `OK auth accepted` o `ERROR auth denied`.

Resultado: la validacion de credenciales queda centralizada en `identidad_svc/`, no en el servidor TCP.

### Orquestacion en Docker

- En `docker-compose.yml`, el servicio del servidor se publica como `iot-server` y expone `9000:9000`.
- Sensores y web usan DNS interno de Docker para resolver `iot-server` e `identity-svc`.
- `servidor/logs` se monta como volumen para persistir `server.log` fuera del contenedor.

Esto permite levantar toda la arquitectura en conjunto y mantener separacion clara de responsabilidades entre captura (sensores), procesamiento/transporte (servidor), autenticacion (identity-svc) y visualizacion (web/gui).
