# sensor_estado.py — Sensor de estado operacional simulado.
# Valores: 1.0 (OK) o 0.0 (FALLA) | 95% OK, 5% FALLA

import random
from base_sensor import BaseSensor


class SensorEstado(BaseSensor):
    # Estado del sistema: 1=OK, 0=FALLA
    sensor_id = "sensor_status_01"
    tipo      = "status"

    def generar_valor(self) -> float:
        # 95% de que esté en estado OK (1), 5% en estado FALLA (0)
        return 1.0 if random.random() > 0.05 else 0.0
