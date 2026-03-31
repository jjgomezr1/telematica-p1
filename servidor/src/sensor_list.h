#ifndef SENSOR_LIST_H
#define SENSOR_LIST_H

#define MAX_SENSORS  64
#define MAX_ID_LEN   64
#define MAX_TIPO_LEN 32

typedef struct {
    char id[MAX_ID_LEN];
    char tipo[MAX_TIPO_LEN];
    double ultimo_valor;
    int activo;
} SensorEntry;

// inicializar la lista
void sensor_list_init();

// agregar o actualizar un sensor
void sensor_list_register(const char *id, const char *tipo);

// actualizar el ultimo valor de un sensor
void sensor_list_update(const char *id, const char *tipo, double valor);

// eliminar un sensor (cuando se desconecta)
void sensor_list_remove(const char *id);

// copiar la lista actual para enviarla (retorna cuantos hay)
int sensor_list_get_all(SensorEntry *destino, int max);

#endif