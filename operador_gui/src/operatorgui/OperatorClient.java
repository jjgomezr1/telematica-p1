package operatorgui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OperatorClient {
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int RECONNECT_DELAY_MS = 5000;
    private static final int LIST_REFRESH_MS = 4000;
    private static final int STATUS_REFRESH_EVERY = 3;

    private final OperatorEventListener listener;
    private final Object writerLock = new Object();

    private volatile boolean running;
    private volatile boolean connected;
    private volatile boolean manualDisconnectRequested;

    private volatile String host = "localhost";
    private volatile int port = 9000;
    private volatile String operatorId = "operator_01";

    private volatile Socket socket;
    private volatile BufferedReader reader;
    private volatile PrintWriter writer;
    private volatile Thread connectionThread;
    private volatile Thread pollingThread;

    public OperatorClient(OperatorEventListener listener) {
        this.listener = listener;
    }

    public synchronized void connect(String host, int port, String operatorId) {
        if (connectionThread != null && connectionThread.isAlive()) {
            manualDisconnectRequested = true;
            running = false;
            connectionThread.interrupt();
            try { connectionThread.join(1000); } catch (InterruptedException ignored) {}
        }

        this.host = host;
        this.port = port;
        this.operatorId = operatorId;
        this.running = true;
        this.connected = false;
        this.manualDisconnectRequested = false;

        connectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                connectionLoop();
            }
        }, "operator-connection");
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    public synchronized void disconnect() {
        manualDisconnectRequested = true;
        running = false;
        connected = false;
        stopPolling();
        closeSocket();

        if (connectionThread != null) {
            connectionThread.interrupt();
        } else {
            listener.onConnectionStateChanged("Desconectado por el operador.", false);
        }
    }

    public void requestRefresh() {
        sendCommand("LIST", false);
        sendCommand("STATUS", false);
    }

    private void connectionLoop() {
        while (running) {
            listener.onConnectionStateChanged(
                String.format("Conectando con %s:%d...", host, port),
                false
            );

            try {
                openSocket();
                connected = true;
                listener.onConnectionStateChanged(
                    String.format("Conectado a %s:%d", host, port),
                    true
                );

                sendCommand("REGISTER OPERATOR " + operatorId, true);
                sendCommand("STATUS", true);
                startPolling();
                readLoop();
            } catch (IOException e) {
                if (running) {
                    listener.onErrorMessage("Fallo de red: " + e.getMessage());
                }
            } finally {
                boolean wasConnected = connected;
                connected = false;
                stopPolling();
                closeSocket();

                if (running) {
                    if (wasConnected) {
                        listener.onConnectionStateChanged(
                            "Conexion perdida. Reintentando en 5 segundos...",
                            false
                        );
                    } else {
                        listener.onConnectionStateChanged(
                            "No fue posible conectarse. Reintentando en 5 segundos...",
                            false
                        );
                    }
                }
            }

            if (running) {
                sleepQuietly(RECONNECT_DELAY_MS);
            }
        }

        if (manualDisconnectRequested) {
            listener.onConnectionStateChanged("Desconectado por el operador.", false);
        } else {
            listener.onConnectionStateChanged("Desconectado.", false);
        }
    }

    private void openSocket() throws IOException {
        Socket newSocket = new Socket();
        newSocket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        newSocket.setKeepAlive(true);

        socket = newSocket;
        reader = new BufferedReader(
            new InputStreamReader(newSocket.getInputStream(), StandardCharsets.UTF_8)
        );
        writer = new PrintWriter(
            new OutputStreamWriter(newSocket.getOutputStream(), StandardCharsets.UTF_8),
            false
        );
    }

    private void readLoop() throws IOException {
        BufferedReader currentReader = reader;
        List<SensorSnapshot> sensorBuffer = null;
        String line;

        while (running && currentReader != null && (line = currentReader.readLine()) != null) {
            if (!running) break;

            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (sensorBuffer != null) {
                if ("END".equals(line)) {
                    listener.onSensorListReceived(
                        Collections.unmodifiableList(new ArrayList<SensorSnapshot>(sensorBuffer))
                    );
                    sensorBuffer = null;
                    continue;
                }

                if (line.startsWith("ALERT ")) {
                    listener.onAlertReceived(line);
                    continue;
                }

                SensorSnapshot snapshot = parseSensorLine(line);
                if (snapshot != null) {
                    sensorBuffer.add(snapshot);
                    continue;
                }

                dispatchSingleLine(line);
                continue;
            }

            if (line.startsWith("SENSORS ")) {
                sensorBuffer = new ArrayList<SensorSnapshot>();
                continue;
            }

            dispatchSingleLine(line);
        }

        if (running) {
            throw new IOException("El servidor cerro la conexion.");
        }
    }

    private void dispatchSingleLine(String line) {
        if (line.startsWith("OK registered")) {
            listener.onRegistrationAcknowledged(line);
        } else if (line.startsWith("STATUS")) {
            listener.onStatusReceived(line);
        } else if (line.startsWith("ALERT")) {
            listener.onAlertReceived(line);
        } else if (line.startsWith("ERROR")) {
            listener.onErrorMessage(line);
        } else {
            listener.onInfoMessage(line);
        }
    }

    private SensorSnapshot parseSensorLine(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 3) {
            return null;
        }

        try {
            double value = Double.parseDouble(parts[2]);
            return new SensorSnapshot(parts[0], parts[1], value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void startPolling() {
        stopPolling();

        pollingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int cycles = 0;

                while (running && connected) {
                    sendCommand("LIST", true);
                    if (cycles % STATUS_REFRESH_EVERY == 0) {
                        sendCommand("STATUS", true);
                    }

                    cycles++;
                    if (!sleepQuietly(LIST_REFRESH_MS)) {
                        return;
                    }
                }
            }
        }, "operator-polling");

        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    private void stopPolling() {
        Thread currentPollingThread = pollingThread;
        pollingThread = null;

        if (currentPollingThread != null) {
            currentPollingThread.interrupt();
        }
    }

    private boolean sendCommand(String command, boolean silent) {
        synchronized (writerLock) {
            PrintWriter currentWriter = writer;
            if (!connected || currentWriter == null) {
                if (!silent) {
                    listener.onErrorMessage("No hay conexion activa para enviar: " + command);
                }
                return false;
            }

            currentWriter.print(command);
            currentWriter.print('\n');
            currentWriter.flush();

            if (currentWriter.checkError()) {
                if (!silent) {
                    listener.onErrorMessage("No fue posible enviar el comando: " + command);
                }
                return false;
            }

            if (!silent) {
                listener.onInfoMessage("Enviado -> " + command);
            }
            return true;
        }
    }

    private boolean sleepQuietly(int millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void closeSocket() {
        synchronized (writerLock) {
            try { if (reader != null) reader.close(); } catch (IOException ignored) {}
            try { if (writer != null) writer.close(); } catch (Exception ignored) {}
            try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
            reader = null;
            writer = null;
            socket = null;
        }
    }
}
