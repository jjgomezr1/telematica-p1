#include "logger.h"

int main() {
    logger_init("logs/test.log");

    log_event("127.0.0.1", 5000, "REGISTER SENSOR temp s01", "OK registered s01");
    log_event("127.0.0.1", 5001, "MEASURE s01 temp 87.3", "OK");
    log_error("192.168.1.5", 6000, "conexion rechazada");

    logger_close();
    return 0;
}