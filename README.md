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

## Despliegue en la Nube (AWS)

### Entorno en Vivo (Producción)
Actualmente el sistema se encuentra desplegado y accesible públicamente en la infraestructura AWS del administrador del equipo. Evaluadores o integradores pueden interactuar con el sistema sin requerir configuración adicional a través de los siguientes puntos formales:

- **Interfaz Web (Dashboard):** [http://telematica-iot.duckdns.org:5000](http://telematica-iot.duckdns.org:5000)
- **Servidor TCP (Para Sensores/Aplicaciones):** Host `telematica-iot.duckdns.org` | Puerto `9000`

---

*Nota para administradores: Si se requiere desplegar la arquitectura desde cero en un nuevo entorno de AWS, siga las siguientes instrucciones técnicas:*

Para poner el sistema en producción y accesible desde internet desde cero, se utiliza una instancia de Amazon EC2 y resolución de nombres de dominio en estricto cumplimiento con los requerimientos técnicos.

### 1. Configuración de Infraestructura
1. Desplegar una instancia **EC2 (Ubuntu)**.
2. Asociar una **Elastic IP** a la instancia para mantener la dirección IPv4 pública estática frente a reinicios.
3. Configurar los **Security Groups** para permitir tráfico entrante (Inbound rules):
   - `TCP 22` (Acceso seguro por administración SSH)
   - `TCP 9000` (Comunicación del Servidor con los sensores y el operador final)
   - `TCP 5000` (Interfaz Web de Monitoreo de logs HTTP)

### 2. Puesta en Marcha en el Servidor Remoto
Conéctese vía SSH a la instancia EC2 y ejecute el despliegue nativo mediante Docker Compose:

```bash
# 1. Instalar dependencias esenciales
sudo apt update && sudo apt install git docker.io docker-compose-v2 -y

# 2. Clonar el repositorio
git clone https://github.com/jjgomezr1/telematica-p1.git
cd telematica-p1

# 3. Construir e iniciar backend, sensores simulados y la web externa
sudo docker compose up --build -d
```

### 3. Resolución de Nombres (DNS)
El sistema evita utilizar direcciones IP codificadas en duro (hardcoded). En la nube, la configuración de DNS se estructuró en dos partes para satisfacer la arquitectura y facilitar la sustentación:

1. **Configuración Tecnológica (AWS Route 53):** Se configuró la arquitectura nativa exigida habilitando una Zona Hospedada en Route 53 con el dominio interno `sistema-iot-telematica-p1.com` configurando su respectivo registro de tipo 'A' hacia la IP de la instancia.
2. **Demostración Global (Entorno en Vivo):** Dado que el dominio de Route 53 no fue comprado a la ICANN (por restricciones de laboratorio estudiantil), se vinculó simultáneamente la IP a un servicio de resolución dinámica gratuito (`http://telematica-iot.duckdns.org:5000`) para posibilitar accesos mundiales inmediatos durante la evaluación sin modificar archivos de host.

### 4. Cliente Operador de Escritorio (Java GUI)
El proyecto incluye además un cliente nativo en Java (Desarrollado en `operador_gui`) para tener monitoreo en tiempo real desde estaciones de escritorio. Como el servidor se encuentra desplegado publicamente en la nube, cualquier máquina con Java puede ejecutar la GUI remotamente conectándose así:

```bash
# Compilar los archivos (Desde la raíz del proyecto)
cd operador_gui
javac -d out src/operatorgui/*.java

# Ejecutar conectándose a la arquitectura en la nube de AWS
java -cp out operatorgui.OperatorMain telematica-iot.duckdns.org 9000 admin
```
