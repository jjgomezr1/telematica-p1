import socket
import threading

USERS = {
    "admin": {"pw": "admin123", "rol": "administrador"},
    "operador": {"pw": "operador123", "rol": "operador"}
}

def handle_client(conn, addr):
    print(f"[Identity] Conexión de {addr}")
    try:
        data = conn.recv(1024).decode('utf-8').strip()
        if data.startswith("AUTH "):
            parts = data.split(" ")
            if len(parts) == 3:
                _, username, password = parts
                user_info = USERS.get(username)
                if user_info and user_info["pw"] == password:
                    conn.sendall(f"OK {user_info['rol']}\n".encode())
                else:
                    conn.sendall(b"DENIED\n")
            else:
                conn.sendall(b"ERROR invalid format\n")
        else:
            conn.sendall(b"ERROR unknown command\n")
    except Exception as e:
        print(f"[Identity] Error con {addr}: {e}")
    finally:
        conn.close()

def main():
    host = '0.0.0.0'
    port = 5001
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((host, port))
    server.listen(5)
    print(f"[Identity] Escuchando en {host}:{port}")

    try:
        while True:
            conn, addr = server.accept()
            thread = threading.Thread(target=handle_client, args=(conn, addr))
            thread.daemon = True
            thread.start()
    except KeyboardInterrupt:
        print("\n[Identity] Apagando...")
    finally:
        server.close()

if __name__ == '__main__':
    main()
