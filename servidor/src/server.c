#include "server.h"
#include "logger.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/in.h>

void *handle_client(void *arg);

typedef struct {
    int  fd;
    char ip[INET_ADDRSTRLEN];
    int  port;
} ClientInfo;

void server_run(int puerto) {
    int server_fd;
    struct sockaddr_in direccion;

    // crear socket TCP
    server_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (server_fd < 0) {
        perror("Error creando socket");
        exit(1);
    }

    // permite reusar el puerto si el servidor se reinicia rapido
    int opt = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    // configurar la direccion
    memset(&direccion, 0, sizeof(direccion));
    direccion.sin_family      = AF_INET;
    direccion.sin_addr.s_addr = INADDR_ANY;  // escuchar en todas las interfaces
    direccion.sin_port        = htons(puerto);

    // bind: asociar socket al puerto
    if (bind(server_fd, (struct sockaddr *)&direccion, sizeof(direccion)) < 0) {
        perror("Error en bind");
        exit(1);
    }

    // listen: ponerse a escuchar
    if (listen(server_fd, 10) < 0) {
        perror("Error en listen");
        exit(1);
    }

    printf("Servidor escuchando en puerto %d\n", puerto);
    log_event("servidor", 0, "INICIO", "escuchando");

    // accept loop: aceptar clientes indefinidamente
    while (1) {
        struct sockaddr_in cliente_addr;
        socklen_t cliente_len = sizeof(cliente_addr);

        int cliente_fd = accept(server_fd,
                                (struct sockaddr *)&cliente_addr,
                                &cliente_len);

        if (cliente_fd < 0) {
            perror("Error en accept");
            continue;  // no salir del loop, intentar con el siguiente
        }

        // obtener IP y puerto del cliente
        char cliente_ip[INET_ADDRSTRLEN];
        inet_ntop(AF_INET, &cliente_addr.sin_addr, cliente_ip, INET_ADDRSTRLEN);
        int cliente_port = ntohs(cliente_addr.sin_port);

        log_event(cliente_ip, cliente_port, "CONEXION", "cliente conectado");

        // empaquetar info del cliente para pasarla al hilo
        ClientInfo *info = malloc(sizeof(ClientInfo));
        info->fd   = cliente_fd;
        info->port = cliente_port;
        strncpy(info->ip, cliente_ip, INET_ADDRSTRLEN);

        // lanzar hilo para este cliente
        pthread_t hilo;
        pthread_create(&hilo, NULL, handle_client, info);
        pthread_detach(hilo);
    }
}