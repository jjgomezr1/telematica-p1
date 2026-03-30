#include "logger.h"
#include <stdio.h>
#include <time.h>
#include <pthread.h>

static FILE *log_file = NULL;
static pthread_mutex_t log_mutex = PTHREAD_MUTEX_INITIALIZER;

void logger_init(const char *filepath) {
    log_file = fopen(filepath, "a");

    if(!log_file) {
        perror("No se pudo abrir archivo de logs");
    }
}

static void obtener_timestamp(char *buf, int size) {
    time_t ahora = time(NULL);
    struct tm *t = localtime(&ahora);
    strftime(buf, size, "%Y-%m-%d %H:%M:%S", t);
}

void log_event(const char *ip, int port, const char *mensaje, const char *respuesta) {
    char ts[32];
    obtener_timestamp(ts, sizeof(ts));
    pthread_mutex_lock(&log_mutex);
    printf("[%s] %s:%d | MSG: %s | RSP: %s\n", ts, ip, port, mensaje, respuesta);
    
    if (log_file){
        fprintf(log_file, "[%s] %s:%d | MSG: %s | RSP: %s\n", ts, ip, port, mensaje, respuesta);
        fflush(log_file);
    }
    pthread_mutex_unlock(&log_mutex);
}

void log_error(const char *ip, int port, const char *descripcion) {
    char ts[32];
    obtener_timestamp(ts, sizeof(ts));
    pthread_mutex_lock(&log_mutex);
    printf("[%s] ERROR %s:%d | %s\n", ts, ip, port, descripcion);

    if (log_file) {
        fprintf(log_file, "[%s] ERROR %s:%d | %s\n", ts, ip, port, descripcion);
        fflush(log_file);
    }

    pthread_mutex_unlock(&log_mutex);
}

void logger_close() {
    if (log_file) fclose(log_file);
}