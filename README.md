# Sensores IoT - Sistema de Simulación

**Componente: Persona 2 - Clientes Sensores + Protocolo**

Este directorio contiene la implementación de sensores IoT simulados en Python que se comunican con el servidor de monitoreo implementado en C.

## 📁 Estructura

```
sensores/
├── PROTOCOLO.md              # Especificación completa del protocolo
├── base_sensor.py            # Clase base para todos los sensores
├── sensor_temperatura.py     # Sensor de temperatura (°C)
├── sensor_vibracion.py       # Sensor de vibración (mm/s)
├── sensor_energia.py         # Sensor de consumo energético (W)
├── sensor_humedad.py         # Sensor de humedad relativa (%)
├── sensor_estado.py          # Sensor de estado operativo (OK/Error)
├── test_sensores.py          # Script de prueba integral
└── requirements.txt          # Dependencias de Python
```

## 🚀 Requisitos Previos

- **Python 3.7+** instalado
- **Servidor ejecutándose** en `localhost:9000`
- El servidor debe estar compilado y corriendo: `cd servidor && make run`

## 📊 Sensores Disponibles

| Sensor | Archivo | Tipo | Rango Normal | Umbral Alerta |
|--------|---------|------|--------------|---------------|
| Temperatura | `sensor_temperatura.py` | `temp` | 20-28°C | > 90°C |
| Vibración | `sensor_vibracion.py` | `vibration` | 0.5-2.5 mm/s | > 5.0 |
| Energía | `sensor_energia.py` | `energy` | 100-400 W | > 500 W |
| Humedad | `sensor_humedad.py` | `humidity` | 40-70% | N/A |
| Estado | `sensor_estado.py` | `status` | 1=OK, 0=Error | N/A |

## 🧪 Cómo Ejecutar

### Opción 1: Prueba Integral (RECOMENDADO)

Ejecuta todos los sensores en paralelo:

```bash
python3 test_sensores.py
```

**Salida esperada:**
```
======== PRUEBA INTEGRAL DE SENSORES IoT ========
✓ temp_sensor_01: Conectado a localhost:9000
✓ temp_sensor_01: Registrado correctamente
  → temp_sensor_01 temp=22.5 [OK]
  → temp_sensor_01 temp=24.3 [OK]
  ...
⚠️  temp_sensor_01: ALERTA - ALERT HIGH_TEMP 95.0
```

### Opción 2: Prueba Individual

Ejecuta un sensor específico:

```bash
# Probar solo temperatura
python3 sensor_temperatura.py

# Probar solo vibración
python3 sensor_vibracion.py

# Y así con los demás...
```

## 📈 ¿Qué Verifica la Prueba?

✓ **Conectividad**: Cada sensor se conecta al servidor en puerto 9000  
✓ **Registro**: Los sensores se registran correctamente con su ID  
✓ **Mediciones**: Los sensores envían mediciones periódicas  
✓ **Alertas**: El sistema genera alertas cuando se superan umbrales  
✓ **Concurrencia**: 6 sensores funcionan en paralelo sin conflictos  

## 🔍 Verificar Resultados

Después de ejecutar la prueba, verifica:

1. **Logs del servidor:**
   ```bash
   tail -f servidor/logs/server.log
   ```

2. **Conexiones activas:**
   ```bash
   # En otro terminal
   netstat -an | grep 9000
   ```

3. **Sensores registrados:** Usa el cliente operador (Persona 3) para ver:
   - Lista de sensores activos
   - Mediciones recientes
   - Alertas generadas

## 🛠️ Personalización

### Cambiar Host/Puerto

```python
sensor = SensorTemperatura("temp_sensor_01", host="192.168.1.100", puerto=9001)
```

### Ajustar Cantidad de Mediciones

```python
sensor.simular_mediciones(cantidad=20, intervalo=0.5)
```

### Crear Nuevo Sensor

1. Hereda de `BaseSensor`
2. Implementa `generar_medicion()`
3. Define el `tipo_sensor`

Ejemplo:

```python
from base_sensor import BaseSensor

class SensorPresion(BaseSensor):
    def __init__(self, sensor_id):
        super().__init__(sensor_id)
        self.tipo_sensor = "pressure"
    
    def generar_medicion(self):
        return random.uniform(1000, 1025)  # Presión en hPa
    
    def simular_mediciones(self, cantidad=10, intervalo=2):
        # ... similar a otros sensores
```

## ✨ Características Implementadas

- ✓ Clase base reutilizable para crear nuevos sensores
- ✓ 5 sensores simulados funcionando correctamente
- ✓ Protocolo basado en texto (TCP/IP)
- ✓ Manejo de errores de conexión
- ✓ Ejecución concurrente con hilos
- ✓ Detección automática de alertas
- ✓ Logs detallados de cada operación
- ✓ Compatible con el servidor C

## 📚 Especificación del Protocolo

Ver [PROTOCOLO.md](PROTOCOLO.md) para la especificación completa de mensajes.

### Formato de Mensajes

```
Registro:   REGISTER SENSOR <id>
Medición:   MEASURE <id> <tipo> <valor>
Respuesta:  OK | ALERT <tipo> <valor>
```

**Ejemplo:**
```
→ REGISTER SENSOR temp_sensor_01
← OK registered temp_sensor_01

→ MEASURE temp_sensor_01 temp 25.5
← OK

→ MEASURE temp_sensor_01 temp 95.0
← ALERT HIGH_TEMP 95.0
```

## 🐛 Troubleshooting

### Error: "Conexión rechazada"
```
✗ temp_sensor_01: Conexión rechazada - ¿el servidor está corriendo?
```
**Solución:** Verifica que el servidor esté ejecutándose en puerto 9000

### Error: "Timeout"
- El servidor está lento o no responde
- Aumenta el timeout en `base_sensor.py`
- Verifica la salud del servidor

### Algunos sensores fallan
- Verifica que la red esté disponible
- Los hilos reintentan automáticamente
- Revisar logs del servidor para detalles

## 📝 Notas para la Sustentación

- Este código es la **Persona 2** del proyecto
- Trabaja en paralelo con el servidor (**Persona 1**)
- Define el protocolo en colaboración con Persona 1
- Los sensores demuestran comunicación TCP fiable
- Uso correcto de hilos para concurrencia

---

**Implementado por: Persona 2**  
**Versión:** 1.0  
**Fecha:** Marzo 2026
