from flask import Flask, render_template, request, redirect, url_for, session
import os

app = Flask(__name__)
app.secret_key = os.environ.get("SECRET_KEY", "dev-key-iot-123")

USERS = {
    "admin": "admin123",
    "operador": "operador123"
}

@app.route('/')
def index():
    if 'username' in session:
        return redirect(url_for('dashboard'))
    return redirect(url_for('login'))

@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']
        if username in USERS and USERS[username] == password:
            session['username'] = username
            return redirect(url_for('dashboard'))
        return render_template('login.html', error="Credenciales inválidas")
    return render_template('login.html')

@app.route('/dashboard')
def dashboard():
    if 'username' not in session:
        return redirect(url_for('login'))
    log_content = []
    try:
        log_path = "/app/logs/server.log"
        if os.path.exists(log_path):
            with open(log_path, "r") as f:
                log_content = f.readlines()[-15:]
    except Exception as e:
        log_content = [f"Error leyendo logs: {e}"]
    return render_template('dashboard.html', username=session['username'], logs=log_content)

@app.route('/logout')
def logout():
    session.pop('username', None)
    return redirect(url_for('login'))

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
