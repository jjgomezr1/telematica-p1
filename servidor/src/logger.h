#ifndef LOGGER_H
#define LOGGER_H

void logger_init(const char *filepath);
void log_event(const char *ip, int port, const char *mensaje, const char *respuesta);
void log_error(const char *ip, int port, const char *descripcion);
void logger_close();

#endif