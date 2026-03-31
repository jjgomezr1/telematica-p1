"""
sensor_humedad.py — Sensor de humedad simulado.
Rango: 20–95 %
El servidor no tiene umbral de alerta para humedad (sensor extra).
"""

import random
from base_sensor import BaseSensor


class SensorHumedad(BaseSensor):

    sensor_id = "sensor_humidity_01"
    tipo      = "humidity"

    def generar_valor(self) -> float:
        return round(random.uniform(20.0, 95.0), 2)
