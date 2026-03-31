"""
sensor_estado.py — Sensor de estado operativo simulado.
Simula el estado de un equipamiento: 1=OK, 0=FALLA
Rango: 0.0 o 1.0
El servidor no tiene umbral de alerta específico para estado.
"""

import random
from base_sensor import BaseSensor


class SensorEstado(BaseSensor):

    sensor_id = "sensor_status_01"
    tipo      = "status"

    def generar_valor(self) -> float:
        # 95% de que esté en estado OK (1), 5% en estado FALLA (0)
        return 1.0 if random.random() > 0.05 else 0.0
