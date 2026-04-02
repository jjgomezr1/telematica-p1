# sensor_temperatura.py — Sensor de temperatura simulado.
# Rango normal: 20–85 °C | Alerta: valor > 90 °C → ALERT HIGH_TEMP

import random
from base_sensor import BaseSensor


class SensorTemperatura(BaseSensor):
    # Temperatura con 5% de probabilidad de generar alerta
    sensor_id = "sensor_temp_01"
    tipo      = "temp"

    def generar_valor(self) -> float:
        # 5% de probabilidad de superar el umbral de alerta (90 °C)
        if random.random() < 0.05:
            return round(random.uniform(91.0, 110.0), 2)
        return round(random.uniform(20.0, 85.0), 2)
