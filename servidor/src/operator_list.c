#include "operator_list.h"
#include <string.h>
#include <pthread.h>
#include <sys/socket.h>

static int operators[MAX_OPERATORS];
static int op_count = 0;
static pthread_mutex_t op_mutex = PTHREAD_MUTEX_INITIALIZER;

void operator_list_init(void) {
    pthread_mutex_lock(&op_mutex);
    for (int i = 0; i < MAX_OPERATORS; i++) {
        operators[i] = -1;
    }
    op_count = 0;
    pthread_mutex_unlock(&op_mutex);
}

void operator_list_add(int fd) {
    pthread_mutex_lock(&op_mutex);
    if (op_count < MAX_OPERATORS) {
        for (int i = 0; i < MAX_OPERATORS; i++) {
            if (operators[i] == -1) {
                operators[i] = fd;
                op_count++;
                break;
            }
        }
    }
    pthread_mutex_unlock(&op_mutex);
}

void operator_list_remove(int fd) {
    pthread_mutex_lock(&op_mutex);
    for (int i = 0; i < MAX_OPERATORS; i++) {
        if (operators[i] == fd) {
            operators[i] = -1;
            op_count--;
            break;
        }
    }
    pthread_mutex_unlock(&op_mutex);
}

void operator_list_broadcast(const char *mensaje) {
    pthread_mutex_lock(&op_mutex);
    size_t len = strlen(mensaje);
    for (int i = 0; i < MAX_OPERATORS; i++) {
        if (operators[i] != -1) {
            send(operators[i], mensaje, len, 0);
        }
    }
    pthread_mutex_unlock(&op_mutex);
}
