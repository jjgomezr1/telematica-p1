"""
main.py — Punto de entrada del simulador de sensores IoT.
Instancia y lanza todos los sensores en hilos independientes.

Uso:
    python main.py

Variables de entorno:
    SERVER_HOST  → host o dominio del servidor  (default: localhost)
    SERVER_PORT  → puerto TCP del servidor      (default: 9000)
"""

import time
from sensor_temperatura import SensorTemperatura
from sensor_vibracion   import SensorVibracion
from sensor_energia     import SensorEnergia
from sensor_humedad     import SensorHumedad
from sensor_estado      import SensorEstado
from base_sensor        import SERVER_HOST, SERVER_PORT


SENSORES = [
    SensorTemperatura(),
    SensorVibracion(),
    SensorEnergia(),
    SensorHumedad(),
    SensorEstado(),
]


def main():
    print("=" * 50)
    print("  IoT Sensor Simulator — Persona 2")
    print(f"  Servidor : {SERVER_HOST}:{SERVER_PORT}")
    print(f"  Sensores : {len(SENSORES)}")
    print("=" * 50)

    for sensor in SENSORES:
        sensor.start()
        time.sleep(0.3)  # pequeño delay para no saturar el servidor al inicio

    print("\nTodos los sensores activos. Presiona Ctrl+C para detener.\n")

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nDeteniendo sensores...")
        for sensor in SENSORES:
            sensor.stop()
        for sensor in SENSORES:
            sensor.join(timeout=3)
        print("Sensores detenidos.")


if __name__ == "__main__":
    main()