FROM ubuntu:22.04

RUN apt-get update && apt-get install -y \
    gcc \
    make \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY src/ ./src/
COPY Makefile .

RUN make

RUN mkdir -p logs

EXPOSE 9000

CMD ["./server", "9000", "logs/server.log"]