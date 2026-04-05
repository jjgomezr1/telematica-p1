CC = gcc
CFLAGS = -Wall -Wextra -pthread -g
SRC = src/main.c src/logger.c src/protocol.c src/server.c src/client_handler.c src/sensor_list.c src/operator_list.c src/alert_history.c
OUT = server

all:
	$(CC) $(CFLAGS) -o $(OUT) $(SRC)

clean:
	rm -f $(OUT)