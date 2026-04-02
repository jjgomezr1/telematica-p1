# sensor_humedad.py — Sensor de humedad relativa simulado.
# Rango: 20–95 % | Sin alertas definidas

import random
from base_sensor import BaseSensor


class SensorHumedad(BaseSensor):
    # Humedad con valores uniformes, sin umbral de alerta
    sensor_id = "sensor_humidity_01"
    tipo      = "humidity"

    def generar_valor(self) -> float:
        return round(random.uniform(20.0, 95.0), 2)
