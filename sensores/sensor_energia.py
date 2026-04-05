# sensor_energia.py — Sensor de consumo energético simulado.
# Rango normal: 50–290 W | Alerta: valor > 300 W → ALERT HIGH_ENERGY

import random
from base_sensor import BaseSensor


class SensorEnergia(BaseSensor):
    # Energía con 6% de probabilidad de generar alerta
    sensor_id = "sensor_energy_01"
    tipo      = "energy"

    def generar_valor(self) -> float:
        # 6% de probabilidad de superar el umbral de alerta (300 W)
        if random.random() < 0.06:
            return round(random.uniform(301.0, 450.0), 2)
        return round(random.uniform(50.0, 290.0), 2)
