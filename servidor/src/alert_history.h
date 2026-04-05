#ifndef ALERT_HISTORY_H
#define ALERT_HISTORY_H

#include <pthread.h>

#define MAX_HISTORIAL 20
#define MAX_ALERT_LEN 128

typedef struct {
    char alertas[MAX_HISTORIAL][MAX_ALERT_LEN];
    int  inicio;
    int  fin;
    int  cantidad;
    pthread_mutex_t mutex;
} AlertHistory;

void alert_history_init();
void alert_history_add(const char *alerta);
int  alert_history_get_all(char *destino, int max_size);

#endif
