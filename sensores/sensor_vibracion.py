# sensor_vibracion.py — Sensor de vibración mecánica simulado.
# Rango normal: 0.1–4.5 | Alerta: valor > 5.0 → ALERT HIGH_VIBRATION

import random
from base_sensor import BaseSensor


class SensorVibracion(BaseSensor):
    # Vibración con 8% de probabilidad de generar alerta
    sensor_id = "sensor_vib_01"
    tipo      = "vibration"

    def generar_valor(self) -> float:
        # 8% de probabilidad de superar el umbral de alerta (5.0)
        if random.random() < 0.08:
            return round(random.uniform(5.1, 9.5), 2)
        return round(random.uniform(0.1, 4.5), 2)
