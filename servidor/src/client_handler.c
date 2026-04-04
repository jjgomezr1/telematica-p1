#include "client_handler.h"
#include "protocol.h"
#include "logger.h"
#include "sensor_list.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>

#define BUFFER_SIZE 1024
#define TIPO_DESCONOCIDO 0
#define TIPO_SENSOR      1
#define TIPO_OPERADOR    2

static void responder(int fd, const char *msg) {
    send(fd, msg, strlen(msg), 0);
}

static void procesar(int fd, ParsedMessage *msg, const char *ip, int port,
                     int *tipo_cliente, char *id_cliente) {
    char respuesta[2048] = "";

    switch (msg->type) {

        case CMD_REGISTER:
            if (msg->argc < 2) {
                snprintf(respuesta, sizeof(respuesta), "ERROR faltan argumentos\n");
                break;
            }
            if (strcmp(msg->args[0], "SENSOR") == 0) {
                *tipo_cliente = TIPO_SENSOR;
                strncpy(id_cliente, msg->args[1], MAX_ARG_LEN - 1);
                sensor_list_register(msg->args[1], "desconocido");
            } else if (strcmp(msg->args[0], "OPERATOR") == 0) {
                *tipo_cliente = TIPO_OPERADOR;
            } else {
                snprintf(respuesta, sizeof(respuesta), "ERROR tipo invalido\n");
                break;
            }
            strncpy(id_cliente, msg->args[1], MAX_ARG_LEN - 1);
            snprintf(respuesta, sizeof(respuesta), "OK registered %s\n", id_cliente);
            break;

        case CMD_MEASURE:
            if (msg->argc < 3) {
                snprintf(respuesta, sizeof(respuesta), "ERROR faltan argumentos\n");
                break;
            }
            if (*tipo_cliente != TIPO_SENSOR) {
                snprintf(respuesta, sizeof(respuesta), "ERROR solo sensores pueden enviar mediciones\n");
                break;
            }
            double valor = atof(msg->args[2]);
            sensor_list_update(msg->args[0], msg->args[1], valor);

            if (strcmp(msg->args[1], "temp") == 0 && valor > 90.0) {
                snprintf(respuesta, sizeof(respuesta), "ALERT HIGH_TEMP %.1f\n", valor);
            } else if (strcmp(msg->args[1], "vibration") == 0 && valor > 5.0) {
                snprintf(respuesta, sizeof(respuesta), "ALERT HIGH_VIBRATION %.1f\n", valor);
            } else if (strcmp(msg->args[1], "energy") == 0 && valor > 300.0) {
                snprintf(respuesta, sizeof(respuesta), "ALERT HIGH_ENERGY %.1f\n", valor);
            } else {
                snprintf(respuesta, sizeof(respuesta), "OK\n");
            }
            break;

        case CMD_LIST: {
            SensorEntry lista[MAX_SENSORS];
            int n = sensor_list_get_all(lista, MAX_SENSORS);
            char linea[128];
            snprintf(respuesta, sizeof(respuesta), "SENSORS %d\n", n);
            for (int i = 0; i < n; i++) {
                snprintf(linea, sizeof(linea), "%s %s %.1f\n",
                         lista[i].id, lista[i].tipo, lista[i].ultimo_valor);
                strncat(respuesta, linea, sizeof(respuesta) - strlen(respuesta) - 1);
            }
            strncat(respuesta, "END\n", sizeof(respuesta) - strlen(respuesta) - 1);
            break;
        }

        case CMD_STATUS:
            snprintf(respuesta, sizeof(respuesta), "STATUS OK\n");
            break;

        case CMD_AUTH:
            if (msg->argc < 2) {
                snprintf(respuesta, sizeof(respuesta), "ERROR faltan argumentos\n");
            } else {
                snprintf(respuesta, sizeof(respuesta), "OK auth %s\n", msg->args[0]);
            }
            break;

        default:
            snprintf(respuesta, sizeof(respuesta), "ERROR comando desconocido\n");
            break;
    }

    responder(fd, respuesta);

    char msg_log[256] = "";
    switch (msg->type) {
        case CMD_REGISTER: snprintf(msg_log, sizeof(msg_log), "REGISTER %s %s", msg->args[0], msg->argc > 1 ? msg->args[1] : ""); break;
        case CMD_MEASURE:  snprintf(msg_log, sizeof(msg_log), "MEASURE %s %s %s", msg->args[0], msg->argc > 1 ? msg->args[1] : "", msg->argc > 2 ? msg->args[2] : ""); break;
        case CMD_LIST:     snprintf(msg_log, sizeof(msg_log), "LIST"); break;
        case CMD_STATUS:   snprintf(msg_log, sizeof(msg_log), "STATUS"); break;
        case CMD_AUTH:     snprintf(msg_log, sizeof(msg_log), "AUTH %s", msg->args[0]); break;
        default:           snprintf(msg_log, sizeof(msg_log), "UNKNOWN"); break;
    }
    
    if (msg->type == CMD_LIST || msg->type == CMD_STATUS) {
        log_event(ip, port, msg_log, "OMITIDO_MULTILINEA (Dashboard Safegaurd)");
    } else {
        log_event(ip, port, msg_log, respuesta);
    }
}

void *handle_client(void *arg) {
    ClientInfo *info = (ClientInfo *)arg;
    int  fd   = info->fd;
    char ip[INET_ADDRSTRLEN];
    int  port = info->port;
    strncpy(ip, info->ip, INET_ADDRSTRLEN);
    free(info);

    int  tipo_cliente = TIPO_DESCONOCIDO;
    char id_cliente[MAX_ARG_LEN] = "desconocido";

    char acumulador[BUFFER_SIZE * 2] = "";
    int  acumulado = 0;
    char buffer[BUFFER_SIZE];

    while (1) {
        memset(buffer, 0, BUFFER_SIZE);
        int bytes = recv(fd, buffer, BUFFER_SIZE - 1, 0);

        if (bytes == 0) {
            log_event(ip, port, "DESCONEXION", id_cliente);
            if (tipo_cliente == TIPO_SENSOR)
                sensor_list_remove(id_cliente);
            break;
        }
        if (bytes < 0) {
            log_error(ip, port, "error en recv");
            if (tipo_cliente == TIPO_SENSOR)
                sensor_list_remove(id_cliente);
            break;
        }

        memcpy(acumulador + acumulado, buffer, bytes);
        acumulado += bytes;
        acumulador[acumulado] = '\0';

        char *inicio = acumulador;
        char *fin;

        while ((fin = strchr(inicio, '\n')) != NULL) {
            *fin = '\0';
            if (strlen(inicio) > 0) {
                ParsedMessage msg = parse_message(inicio);
                procesar(fd, &msg, ip, port, &tipo_cliente, id_cliente);
            }
            inicio = fin + 1;
        }

        int restante = acumulado - (inicio - acumulador);
        memmove(acumulador, inicio, restante);
        acumulado = restante;
        acumulador[acumulado] = '\0';
    }

    close(fd);
    return NULL;
}