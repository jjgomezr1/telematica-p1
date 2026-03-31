#include "server.h"
#include "logger.h"
#include "sensor_list.h"

int main() {
    logger_init("logs/server.log");
    sensor_list_init();
    server_run(9000);
    logger_close();
    return 0;
}