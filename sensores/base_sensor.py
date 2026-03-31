"""
base_sensor.py — Clase base para todos los sensores IoT simulados.
Maneja: conexión TCP, registro, envío de mediciones y reconexión automática.
"""

import socket
import threading
import logging
import time
import os

SERVER_HOST      = os.environ.get("SERVER_HOST", "localhost")
SERVER_PORT      = int(os.environ.get("SERVER_PORT", 9000))
RECONNECT_DELAY  = 5   # segundos entre intentos de reconexión
MEASURE_INTERVAL = 3   # segundos entre mediciones

logging.basicConfig(
    level=logging.INFO,
    format="[%(asctime)s] %(levelname)s [%(name)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)


class BaseSensor(threading.Thread):
    """
    Clase base para sensores IoT simulados.
    Las subclases deben implementar:
      - self.sensor_id  (str)  → identificador único del sensor
      - self.tipo       (str)  → tipo de medición (temp, vibration, energy, humidity)
      - generar_valor() (float)→ valor simulado a enviar
    """

    def __init__(self):
        super().__init__(name=self.sensor_id, daemon=True)
        self.log         = logging.getLogger(self.sensor_id)
        self._stop_event = threading.Event()

    def generar_valor(self) -> float:
        raise NotImplementedError("Cada sensor debe implementar generar_valor()")

    def stop(self):
        self._stop_event.set()

    # ── Métodos de red ──────────────────────────────────────────────────

    def _conectar(self) -> socket.socket | None:
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(10)
            sock.connect((SERVER_HOST, SERVER_PORT))
            self.log.info(f"Conectado a {SERVER_HOST}:{SERVER_PORT}")
            return sock
        except (socket.error, OSError) as e:
            self.log.warning(f"No se pudo conectar: {e}")
            return None

    def _recibir_linea(self, sock: socket.socket) -> str | None:
        try:
            data = b""
            while not data.endswith(b"\n"):
                chunk = sock.recv(256)
                if not chunk:
                    return None
                data += chunk
            return data.decode().strip()
        except (socket.error, OSError):
            return None

    def _registrar(self, sock: socket.socket) -> bool:
        try:
            sock.sendall(f"REGISTER SENSOR {self.sensor_id}\n".encode())
            respuesta = self._recibir_linea(sock)
            if respuesta and respuesta.startswith("OK"):
                self.log.info(f"Registrado → {respuesta}")
                return True
            self.log.error(f"Registro fallido: {respuesta}")
            return False
        except (socket.error, OSError) as e:
            self.log.error(f"Error al registrar: {e}")
            return False

    def _enviar_medicion(self, sock: socket.socket) -> bool:
        try:
            valor   = self.generar_valor()
            mensaje = f"MEASURE {self.sensor_id} {self.tipo} {valor}\n"
            sock.sendall(mensaje.encode())

            respuesta = self._recibir_linea(sock)
            if respuesta is None:
                return False

            if respuesta.startswith("ALERT"):
                self.log.warning(f"⚠️  ALERTA: {respuesta}  (valor={valor})")
            else:
                self.log.info(f"{self.tipo}={valor} → {respuesta}")
            return True
        except (socket.error, OSError) as e:
            self.log.error(f"Error enviando medición: {e}")
            return False

    # ── Loop principal ──────────────────────────────────────────────────

    def run(self):
        self.log.info(f"Sensor iniciado. Tipo: {self.tipo}")

        while not self._stop_event.is_set():
            sock = self._conectar()
            if sock is None:
                self._stop_event.wait(RECONNECT_DELAY)
                continue

            if not self._registrar(sock):
                sock.close()
                self._stop_event.wait(RECONNECT_DELAY)
                continue

            try:
                while not self._stop_event.is_set():
                    if not self._enviar_medicion(sock):
                        self.log.warning("Conexión perdida. Reconectando...")
                        break
                    self._stop_event.wait(MEASURE_INTERVAL)
            finally:
                sock.close()

            if not self._stop_event.is_set():
                self._stop_event.wait(RECONNECT_DELAY)

        self.log.info("Sensor detenido.")