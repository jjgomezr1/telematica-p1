#include "sensor_list.h"
#include <string.h>
#include <pthread.h>

static SensorEntry sensores[MAX_SENSORS];
static int         total = 0;
static pthread_mutex_t lista_mutex = PTHREAD_MUTEX_INITIALIZER;

void sensor_list_init() {
    pthread_mutex_lock(&lista_mutex);
    memset(sensores, 0, sizeof(sensores));
    total = 0;
    pthread_mutex_unlock(&lista_mutex);
}

void sensor_list_register(const char *id, const char *tipo) {
    pthread_mutex_lock(&lista_mutex);

    // verificar si ya existe
    for (int i = 0; i < total; i++) {
        if (sensores[i].activo && strcmp(sensores[i].id, id) == 0) {
            pthread_mutex_unlock(&lista_mutex);
            return;
        }
    }

    // agregar si hay espacio
    if (total < MAX_SENSORS) {
        strncpy(sensores[total].id,   id,   MAX_ID_LEN - 1);
        strncpy(sensores[total].tipo, tipo, MAX_TIPO_LEN - 1);
        sensores[total].ultimo_valor = 0.0;
        sensores[total].activo = 1;
        total++;
    }

    pthread_mutex_unlock(&lista_mutex);
}

void sensor_list_update(const char *id, const char *tipo, double valor) {
    pthread_mutex_lock(&lista_mutex);

    for (int i = 0; i < total; i++) {
        if (sensores[i].activo && strcmp(sensores[i].id, id) == 0) {
            strncpy(sensores[i].tipo, tipo, MAX_TIPO_LEN - 1);
            sensores[i].ultimo_valor = valor;
            pthread_mutex_unlock(&lista_mutex);
            return;
        }
    }

    pthread_mutex_unlock(&lista_mutex);
}

void sensor_list_remove(const char *id) {
    pthread_mutex_lock(&lista_mutex);

    for (int i = 0; i < total; i++) {
        if (sensores[i].activo && strcmp(sensores[i].id, id) == 0) {
            sensores[i].activo = 0;
            pthread_mutex_unlock(&lista_mutex);
            return;
        }
    }

    pthread_mutex_unlock(&lista_mutex);
}

int sensor_list_get_all(SensorEntry *destino, int max) {
    pthread_mutex_lock(&lista_mutex);

    int count = 0;
    for (int i = 0; i < total && count < max; i++) {
        if (sensores[i].activo) {
            destino[count++] = sensores[i];
        }
    }

    pthread_mutex_unlock(&lista_mutex);
    return count;
}