import socket
import threading
import urllib.parse
import os
import re

# Configuración desde variables de entorno
IOT_HOST = os.environ.get('IOT_SERVER_HOST', 'iot-server')
IOT_PORT = int(os.environ.get('IOT_SERVER_PORT', 9000))
IDENTITY_HOST = os.environ.get('IDENTITY_SVC_HOST', 'identity-svc')
IDENTITY_PORT = int(os.environ.get('IDENTITY_SVC_PORT', 5001))

def send_response(conn, status, content_type="text/html", body="", headers=None):
    if headers is None:
        headers = []
    
    body_bytes = body.encode('utf-8')
    response = f"HTTP/1.1 {status}\r\n"
    response += f"Content-Type: {content_type}\r\n"
    response += f"Content-Length: {len(body_bytes)}\r\n"
    for h in headers:
        response += f"{h}\r\n"
    response += "\r\n"
    
    conn.sendall(response.encode('utf-8') + body_bytes)

def parse_request(data):
    lines = data.split('\r\n')
    if len(lines) == 0 or not lines[0]:
        return None, None, {}, ""
    
    parts = lines[0].split(' ')
    if len(parts) < 3:
        return None, None, {}, ""
    
    method, path, _ = parts
    headers = {}
    i = 1
    while i < len(lines) and lines[i]:
        if ": " in lines[i]:
            k, v = lines[i].split(": ", 1)
            headers[k] = v
        i += 1
    
    body = "\n".join(lines[i+1:]) if i + 1 < len(lines) else ""
    return method, path, headers, body

def check_identity(username, password):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(3)
        s.connect((IDENTITY_HOST, IDENTITY_PORT))
        s.sendall(f"AUTH {username} {password}\n".encode())
        res = s.recv(1024).decode().strip()
        s.close()
        if res.startswith("OK"):
            parts = res.split(" ")
            role = parts[1] if len(parts) > 1 else "usuario"
            return role
        return None
    except Exception as e:
        print(f"Error conectando a identity-svc: {e}")
        return None

def load_template(filename):
    filepath = os.path.join(os.path.dirname(__file__), 'templates', filename)
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            return f.read()
    except Exception as e:
        return f"<html><body>Error loading {filename}: {e}</body></html>"

def get_sensors_list():
    names_map = {
        "sensor_temp_01": "Sensor Temperatura",
        "sensor_vib_01": "Sensor Vibración",
        "sensor_energy_01": "Sensor Energía",
        "sensor_humidity_01": "Sensor Humedad",
        "sensor_status_01": "Sensor Estado"
    }
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(3)
        s.connect((IOT_HOST, IOT_PORT))
        s.sendall(b"LIST\n")
        
        data = b""
        while b"END\n" not in data:
            chunk = s.recv(512)
            if not chunk:
                break
            data += chunk
        s.close()
        
        lines = data.decode().split('\n')
        sensores_html = ""
        for line in lines:
            line = line.strip()
            if line.startswith("SENSORS") or line == "END" or not line:
                continue
            parts = line.split(" ")
            if len(parts) >= 3:
                sensor_name = names_map.get(parts[0], parts[0])
                sensores_html += f'<div class="log-entry" style="display:flex; justify-content:space-between;"><span><b>{sensor_name}</b> <span style="color:var(--text-muted); font-size:0.85em;">[{parts[1]}]</span></span><span style="font-weight:600; color:var(--success);">{parts[2]}</span></div>\n'
        
        if not sensores_html:
            sensores_html = '<div style="color: var(--text-muted);">No hay sensores activos reportando datos...</div>'
            
        return sensores_html
    except Exception as e:
        return f'<div class="log-entry" style="color:red;">Error conectando al servidor IoT: {e}</div>'

def get_system_status():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(2)
        s.connect((IOT_HOST, IOT_PORT))
        s.sendall(b"STATUS\n")
        res = s.recv(1024).decode().strip()
        s.close()
        return "Activo" if "OK" in res else "Error"
    except:
        return "Desconectado"

def get_alerts_list():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(2)
        s.connect((IOT_HOST, IOT_PORT))
        s.sendall(b"ALERTS\n")
        
        data = b""
        while b"ALERTS_END\n" not in data:
            chunk = s.recv(512)
            if not chunk: break
            data += chunk
        s.close()
        
        content = data.decode()
        if "ALERTS_START\n" in content:
            alerts_text = content.split("ALERTS_START\n")[1].split("ALERTS_END\n")[0]
            lines = alerts_text.strip().split("\n")
            html = ""
            for line in reversed(lines): # Mostrar más recientes primero
                if line.strip():
                    html += f'<div class="log-entry" style="color:#fca5a5;">⚠️ {line}</div>'
            return html if html else '<div style="color:var(--text-muted);">Sin alertas recientes.</div>'
        return "Error parseando alertas"
    except:
        return "Error consultando alertas"

def handle_client(conn):
    try:
        data = conn.recv(4096).decode('utf-8')
        if not data:
            return
        
        method, path, headers, body = parse_request(data)
        if not method:
            return

        cookies = headers.get('Cookie', '')
        session = ''
        role = ''
        if 'session=' in cookies:
            session = cookies.split('session=')[1].split(';')[0]
        if 'role=' in cookies:
            role = cookies.split('role=')[1].split(';')[0]

        if path == '/':
            if session:
                send_response(conn, "302 Found", headers=["Location: /dashboard"])
            else:
                send_response(conn, "302 Found", headers=["Location: /login"])
                
        elif path == '/login' and method == 'GET':
            html = load_template('login.html')
            html = re.sub(r'\{%\s*if\s+error\s*%\}.*?\{%\s*endif\s*%\}', '', html, flags=re.DOTALL)
            send_response(conn, "200 OK", body=html)
            
        elif path == '/login' and method == 'POST':
            params = dict(urllib.parse.parse_qsl(body))
            username = params.get('username', '')
            password = params.get('password', '')
            
            user_role = check_identity(username, password)
            if user_role:
                send_response(conn, "302 Found", headers=[
                    "Location: /dashboard", 
                    f"Set-Cookie: session={username}",
                    f"Set-Cookie: role={user_role}"
                ])
            else:
                html = load_template('login.html')
                error_html = '<div class="error">Credenciales invalidas o servicio inalcanzable.</div>'
                html = re.sub(r'\{%\s*if\s+error\s*%\}.*?\{%\s*endif\s*%\}', error_html, html, flags=re.DOTALL)
                send_response(conn, "401 Unauthorized", body=html)
                
        elif path == '/dashboard' and method == 'GET':
            if not session:
                send_response(conn, "302 Found", headers=["Location: /login"])
                return
                
            sensores_html = get_sensors_list()
            status_real = get_system_status()
            alertas_html = get_alerts_list()
            
            html = load_template('dashboard.html')
            html = html.replace('{{ username }}', f"{session} [{role.capitalize()}]")
            html = html.replace("{{ url_for('logout') }}", "/logout")
            
            # Badge de estado dinámico
            status_class = "success" if status_real == "Activo" else "error"
            status_badge = f'<span class="status-badge" style="background-color:{"rgba(34,197,94,0.2)" if status_real=="Activo" else "rgba(239,68,68,0.2)"}; color:{"#22c55e" if status_real=="Activo" else "#ef4444"};">Servidor {status_real}</span>'
            html = html.replace('<span class="status-badge">Servidor Activo</span>', status_badge)

            # Bloque de sensores
            html = re.sub(r'\{%\s*if\s+logs\s*%\}.*?\{%\s*endif\s*%\}', sensores_html, html, flags=re.DOTALL)
            
            # Bloque de alertas (usaremos el mismo mecanismo de reemplazo si existe o inyectamos)
            # Como dashboard.html no tiene bloque de alertas, lo inyectaremos en una nueva card ficticia o lo pasaremos por logs
            # Mejor: Reemplazamos un placeholder que añadiremos al HTML
            html = html.replace('<!-- ALERTAS_PLACEHOLDER -->', alertas_html)

            # Final cleanup of any lingering Jinja-like tags
            html = re.sub(r'\{%.*?%\}', '', html, flags=re.DOTALL)
            html = re.sub(r'\{\{.*?\}\}', '', html)
            
            send_response(conn, "200 OK", body=html)
            
        elif path == '/logout':
            send_response(conn, "302 Found", headers=["Location: /login", "Set-Cookie: session=; Max-Age=0"])
            
        else:
            send_response(conn, "404 Not Found", body="<h1>404 Not Found</h1>")
            
    except Exception as e:
        print("Error handling client:", e)
    finally:
        conn.close()

def main():
    host = '0.0.0.0'
    port = 5000
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((host, port))
    server.listen(10)
    print(f"[{port}] Web Server HTTP Básico Corriendo...")
    
    while True:
        conn, addr = server.accept()
        thread = threading.Thread(target=handle_client, args=(conn,))
        thread.daemon = True
        thread.start()

if __name__ == '__main__':
    main()
