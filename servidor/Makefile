CC = gcc
CFLAGS = -Wall -Wextra -pthread -g
SRC = src/main.c src/logger.c src/protocol.c src/server.c src/client_handler.c src/sensor_list.c
OUT = server

all:
	$(CC) $(CFLAGS) -o $(OUT) $(SRC)

clean:
	rm -f $(OUT)