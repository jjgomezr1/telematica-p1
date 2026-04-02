# IoT Services Monitoring - Proyecto 1

Este repositorio contiene la arquitectura cliente-servidor para un sistema de monitoreo IoT (Internet of Things).

## Despliegue con Docker (Persona 4)

Todo el despliegue del sistema (Servidor C, Sensores Python y el servicio Web/Auth) está orquestado mediante Docker Compose.

### Requisitos previos
- Entorno Linux (o WSL) con `docker` y `docker-compose` instalados.
- Entorno Windows con `docker` y `docker-compose` instalados.

### Instrucciones de despliegue local

1. Clona el repositorio e ingresa a la raíz del proyecto (`telematica-p1`).
2. Levanta los contenedores en segundo plano:
   ```bash
   docker compose up --build -d
   ```
3. Verifica en Docker Desktop o corriendo `docker ps` que los siguientes contenedores estén en ejecución:
   - `telematica-p1-iot-server-1` (Puerto 9000)
   - `telematica-p1-iot-sensors-1`
   - `telematica-p1-auth-web-1` (Puerto 5000)

### Acceso a la Interfaz Web (Operador)
Abre tu navegador web e ingresa a [http://localhost:5000](http://localhost:5000)

**Usuarios de prueba:**
- `admin` / `admin123`
- `operador` / `operador123`

### Acceso a los Logs Crudos
Los logs globales del servidor se sincronizan mediante contenedores. Puedes ver el historial crudo en tu entorno local ingresando a:
`servidor/logs/server.log`

4. Para detener los contenedores:
   ```bash
   docker compose down
   ```
