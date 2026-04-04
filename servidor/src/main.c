#include "server.h"
#include "logger.h"
#include "sensor_list.h"

#include <stdlib.h>
#include <stdio.h>

#include "operator_list.h"
#include "alert_history.h"

int main(int argc, char *argv[]) {
    if (argc < 3) {
        fprintf(stderr, "Uso: %s <puerto> <archivoDeLogs>\n", argv[0]);
        return 1;
    }

    int puerto = atoi(argv[1]);
    const char *ruta_log = argv[2];

    logger_init(ruta_log);
    sensor_list_init();
    operator_list_init();
    alert_history_init();
    server_run(puerto);
    logger_close();
    return 0;
}