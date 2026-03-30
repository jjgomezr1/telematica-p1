CC = gcc
CFLAGS = -Wall -Wextra -pthread -g
SRC = src/main.c src/server.c src/client_handler.c src/protocol.c src/logger.c
OUT = server

all:
	$(CC) $(CFLAGS) -o $(OUT) $(SRC)

clean:
	rm -f $(OUT)