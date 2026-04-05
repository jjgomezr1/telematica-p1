# Dockerfile para los sensores Python
FROM python:3.12-slim

WORKDIR /app

# Copiar el código fuente
COPY . .

# Variables de entorno por defecto
ENV SERVER_HOST=iot-server
ENV SERVER_PORT=9000

# Ejecutar el simulador
CMD ["python", "main.py"]
