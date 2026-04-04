#include "alert_history.h"
#include <string.h>
#include <stdio.h>

static AlertHistory history;

void alert_history_init() {
    memset(&history, 0, sizeof(AlertHistory));
    history.inicio = 0;
    history.fin = 0;
    history.cantidad = 0;
    pthread_mutex_init(&history.mutex, NULL);
}

void alert_history_add(const char *alerta) {
    pthread_mutex_lock(&history.mutex);

    // Guardar en la posición fin
    strncpy(history.alertas[history.fin], alerta, MAX_ALERT_LEN - 1);
    history.alertas[history.fin][MAX_ALERT_LEN - 1] = '\0';

    // Avanzar fin circularmente
    history.fin = (history.fin + 1) % MAX_HISTORIAL;

    if (history.cantidad < MAX_HISTORIAL) {
        history.cantidad++;
    } else {
        // El inicio también avanza si ya estamos llenos (el más viejo se sobreescribe)
        history.inicio = (history.inicio + 1) % MAX_HISTORIAL;
    }

    pthread_mutex_unlock(&history.mutex);
}

int alert_history_get_all(char *destino, int max_size) {
    pthread_mutex_lock(&history.mutex);
    
    destino[0] = '\0';
    int offset = 0;
    
    // Recorrer desde inicio hasta fin (teniendo en cuenta la circularidad)
    for (int i = 0; i < history.cantidad; i++) {
        int idx = (history.inicio + i) % MAX_HISTORIAL;
        int len = strlen(history.alertas[idx]);
        
        if (offset + len + 2 < max_size) {
            strcat(destino, history.alertas[idx]);
            // Asegurar que termine en newline si no la tiene
            if (history.alertas[idx][len-1] != '\n') {
                strcat(destino, "\n");
            }
            offset = strlen(destino);
        } else {
            break;
        }
    }

    pthread_mutex_unlock(&history.mutex);
    return history.cantidad;
}
