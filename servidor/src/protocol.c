#include "protocol.h"
#include <string.h>
#include <stdlib.h>

ParsedMessage parse_message(const char *raw) {
    ParsedMessage msg;
    msg.type = CMD_UNKNOWN;
    msg.argc = 0;

    // copiar el string porque strtok modifica el original
    char buffer[512];
    strncpy(buffer, raw, sizeof(buffer) - 1);
    buffer[sizeof(buffer) - 1] = '\0';

    // primer token: el comando
    char *token = strtok(buffer, " \n\r");
    if (!token) return msg;

    if      (strcmp(token, "REGISTER") == 0) msg.type = CMD_REGISTER;
    else if (strcmp(token, "MEASURE")  == 0) msg.type = CMD_MEASURE;
    else if (strcmp(token, "LIST")     == 0) msg.type = CMD_LIST;
    else if (strcmp(token, "STATUS")   == 0) msg.type = CMD_STATUS;
    else if (strcmp(token, "AUTH")     == 0) msg.type = CMD_AUTH;
    else if (strcmp(token, "ALERTS")   == 0) msg.type = CMD_ALERTS;

    //resto de tokens: argumentos
    while ((token = strtok(NULL, " \n\r")) && msg.argc < MAX_ARGS) {
        strncpy(msg.args[msg.argc], token, MAX_ARG_LEN - 1);
        msg.args[msg.argc][MAX_ARG_LEN - 1] = '\0';
        msg.argc++;
    }

    return msg;
}

