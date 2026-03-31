#ifndef PROTOCOL_H
#define PROTOCOL_H

#define MAX_ARGS 80
#define MAX_ARG_LEN 64

// Tipos de comando que el servidor entiende
typedef enum {
    CMD_REGISTER,
    CMD_MEASURE,
    CMD_LIST,
    CMD_STATUS,
    CMD_AUTH,
    CMD_UNKNOWN
} CommandType;

// Estructura de mensaje parseado
typedef struct {
    CommandType type;
    char args[MAX_ARGS][MAX_ARG_LEN];
    int argc;
} ParsedMessage;

// Parsear mensaje
ParsedMessage parse_message(const char *raw);

#endif