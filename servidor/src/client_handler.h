#ifndef CLIENT_HANDLER_H
#define CLIENT_HANDLER_H

#include <netinet/in.h>

typedef struct {
    int  fd;
    char ip[INET_ADDRSTRLEN];
    int  port;
} ClientInfo;

void *handle_client(void *arg);

#endif