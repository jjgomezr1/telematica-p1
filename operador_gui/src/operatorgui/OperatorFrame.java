package operatorgui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class OperatorFrame extends JFrame implements OperatorEventListener {
    private static final long serialVersionUID = 1L;

    private static final Color BG = new Color(12, 18, 28);
    private static final Color PANEL = new Color(20, 29, 45);
    private static final Color PANEL_ALT = new Color(16, 24, 37);
    private static final Color TEXT = new Color(226, 232, 240);
    private static final Color MUTED = new Color(148, 163, 184);
    private static final Color ACCENT = new Color(56, 189, 248);
    private static final Color SUCCESS = new Color(34, 197, 94);
    private static final Color WARNING = new Color(249, 115, 22);
    private static final Color BORDER = new Color(51, 65, 85);

    private final OperatorClient client;
    private final JTextField hostField;
    private final JTextField portField;
    private final JTextField operatorField;
    private final JButton connectButton;
    private final JButton disconnectButton;
    private final JButton refreshButton;
    private final JLabel connectionLabel;
    private final JLabel systemLabel;
    private final JLabel activeSensorsLabel;
    private final JLabel lastUpdateLabel;
    private final JTextArea measurementsArea;
    private final JTextArea alertsArea;
    private final DefaultTableModel sensorTableModel;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private Map<String, SensorSnapshot> previousSensors = new LinkedHashMap<String, SensorSnapshot>();

    public OperatorFrame(String defaultHost, int defaultPort, String defaultOperatorId) {
        super("Operador IoT - Persona 3");
        this.client = new OperatorClient(this);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setSize(1200, 780);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(new EmptyBorder(18, 18, 18, 18));
        root.setBackground(BG);
        setContentPane(root);

        hostField = createField(defaultHost, 14);
        portField = createField(String.valueOf(defaultPort), 7);
        operatorField = createField(defaultOperatorId, 14);

        connectButton = createButton("Conectar", ACCENT, new Color(12, 74, 110));
        disconnectButton = createButton("Desconectar", WARNING, new Color(124, 45, 18));
        refreshButton = createButton("Actualizar", SUCCESS, new Color(21, 128, 61));

        disconnectButton.setEnabled(false);
        refreshButton.setEnabled(false);

        connectionLabel = createValueLabel("Sin conexion", MUTED);
        systemLabel = createValueLabel("Sin datos", MUTED);
        activeSensorsLabel = createValueLabel("0 sensores", TEXT);
        lastUpdateLabel = createValueLabel("Sin actualizacion", MUTED);

        measurementsArea = createTextArea();
        alertsArea = createTextArea();

        sensorTableModel = new DefaultTableModel(
            new Object[]{"Sensor", "Tipo", "Ultimo valor"},
            0
        ) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable sensorTable = new JTable(sensorTableModel);
        sensorTable.setRowHeight(24);
        sensorTable.setFillsViewportHeight(true);
        sensorTable.setBackground(PANEL_ALT);
        sensorTable.setForeground(TEXT);
        sensorTable.setGridColor(BORDER);
        sensorTable.setSelectionBackground(new Color(14, 116, 144));
        sensorTable.setSelectionForeground(TEXT);
        sensorTable.getTableHeader().setBackground(new Color(15, 23, 42));
        sensorTable.getTableHeader().setForeground(TEXT);
        sensorTable.getTableHeader().setFont(sensorTable.getTableHeader().getFont().deriveFont(Font.BOLD));

        root.add(buildHeaderPanel(), BorderLayout.NORTH);
        root.add(buildBodyPanel(sensorTable), BorderLayout.CENTER);
        root.add(buildFooterPanel(), BorderLayout.SOUTH);

        wireActions();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                client.disconnect();
            }
        });
    }

    private JPanel buildHeaderPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(12, 12));
        wrapper.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Centro de Operacion IoT");
        title.setForeground(TEXT);
        title.setFont(new Font("Segoe UI Semibold", Font.BOLD, 28));

        JLabel subtitle = new JLabel(
            "Cliente operador con GUI para supervisar sensores activos, mediciones y alertas."
        );
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        titlePanel.add(title);
        titlePanel.add(subtitle);

        JPanel connectionPanel = createPanelCard();
        connectionPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        connectionPanel.add(createInlineLabel("Host"));
        connectionPanel.add(hostField);
        connectionPanel.add(createInlineLabel("Puerto"));
        connectionPanel.add(portField);
        connectionPanel.add(createInlineLabel("Operador"));
        connectionPanel.add(operatorField);
        connectionPanel.add(connectButton);
        connectionPanel.add(disconnectButton);
        connectionPanel.add(refreshButton);

        wrapper.add(titlePanel, BorderLayout.NORTH);
        wrapper.add(wrapSection("Conexion del Operador", connectionPanel), BorderLayout.CENTER);
        return wrapper;
    }

    private JSplitPane buildBodyPanel(JTable sensorTable) {
        JPanel summaryPanel = new JPanel(new GridLayout(1, 2, 12, 12));
        summaryPanel.setOpaque(false);
        summaryPanel.add(createSummaryCard("Sensores reportados", activeSensorsLabel));
        summaryPanel.add(createSummaryCard("Ultima actualizacion", lastUpdateLabel));

        JPanel sensorsContent = new JPanel(new BorderLayout(12, 12));
        sensorsContent.setOpaque(false);
        sensorsContent.add(summaryPanel, BorderLayout.NORTH);
        sensorsContent.add(createScrollPane(sensorTable), BorderLayout.CENTER);

        JPanel measurementsContent = new JPanel(new BorderLayout());
        measurementsContent.setOpaque(false);
        measurementsContent.add(createScrollPane(measurementsArea), BorderLayout.CENTER);

        JPanel alertsContent = new JPanel(new BorderLayout());
        alertsContent.setOpaque(false);
        alertsContent.add(createScrollPane(alertsArea), BorderLayout.CENTER);

        JSplitPane rightSplit = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            wrapSection("Mediciones Recientes", measurementsContent),
            wrapSection("Alertas", alertsContent)
        );
        rightSplit.setResizeWeight(0.6);
        rightSplit.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane mainSplit = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            wrapSection("Sensores Activos", sensorsContent),
            rightSplit
        );
        mainSplit.setResizeWeight(0.52);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());
        return mainSplit;
    }

    private JPanel buildFooterPanel() {
        JPanel footer = new JPanel(new GridLayout(1, 2, 12, 12));
        footer.setOpaque(false);
        footer.add(createSummaryCard("Estado de conexion", connectionLabel));
        footer.add(createSummaryCard("Estado del sistema", systemLabel));
        return footer;
    }

    private JPanel wrapSection(String title, JComponent content) {
        JPanel panel = createPanelCard();
        panel.setLayout(new BorderLayout(0, 12));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT);
        titleLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 18));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPanelCard() {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(14, 14, 14, 14)
        ));
        return panel;
    }

    private JPanel createSummaryCard(String title, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(PANEL_ALT);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(MUTED);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane createScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(PANEL_ALT);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        return scrollPane;
    }

    private JScrollPane createScrollPane(JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.getViewport().setBackground(PANEL_ALT);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        return scrollPane;
    }

    private JTextField createField(String value, int columns) {
        JTextField field = new JTextField(value, columns);
        field.setMargin(new Insets(8, 10, 8, 10));
        field.setBackground(PANEL_ALT);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setBorder(BorderFactory.createLineBorder(BORDER));
        return field;
    }

    private JButton createButton(String text, Color foreground, Color background) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setForeground(TEXT);
        button.setBackground(background);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(foreground),
            new EmptyBorder(8, 14, 8, 14)
        ));
        return button;
    }

    private JLabel createInlineLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return label;
    }

    private JLabel createValueLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Segoe UI Semibold", Font.BOLD, 15));
        return label;
    }

    private JTextArea createTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(PANEL_ALT);
        area.setForeground(TEXT);
        area.setCaretColor(TEXT);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setBorder(new EmptyBorder(10, 10, 10, 10));
        return area;
    }

    private void wireActions() {
        connectButton.addActionListener(e -> connect());
        disconnectButton.addActionListener(e -> client.disconnect());
        refreshButton.addActionListener(e -> client.requestRefresh());
    }

    private void connect() {
        String host = hostField.getText().trim();
        String portText = portField.getText().trim();
        String operatorId = operatorField.getText().trim();

        if (host.isEmpty()) {
            onErrorMessage("Debes ingresar un host valido.");
            return;
        }

        if (operatorId.isEmpty()) {
            onErrorMessage("Debes ingresar un identificador de operador.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            onErrorMessage("El puerto debe ser numerico.");
            return;
        }

        client.connect(host, port, operatorId);
    }

    @Override
    public void onConnectionStateChanged(final String status, final boolean connected) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                boolean retrying = status.startsWith("Conectando")
                    || status.contains("Reintentando");

                connectionLabel.setText(status);
                connectionLabel.setForeground(connected ? SUCCESS : (retrying ? WARNING : MUTED));
                connectButton.setEnabled(!connected && !retrying);
                disconnectButton.setEnabled(connected || retrying);
                refreshButton.setEnabled(connected);
            }
        });
    }

    @Override
    public void onRegistrationAcknowledged(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                appendMeasurement("Sesion iniciada: " + message);
            }
        });
    }

    @Override
    public void onSensorListReceived(final List<SensorSnapshot> sensors) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Map<String, SensorSnapshot> nextSensors = new LinkedHashMap<String, SensorSnapshot>();
                sensorTableModel.setRowCount(0);

                for (SensorSnapshot sensor : sensors) {
                    sensorTableModel.addRow(new Object[]{
                        sensor.getId(),
                        sensor.getType(),
                        sensor.getFormattedValue()
                    });
                    nextSensors.put(sensor.getId(), sensor);

                    SensorSnapshot previous = previousSensors.get(sensor.getId());
                    if (previous == null
                        || Double.compare(previous.getValue(), sensor.getValue()) != 0
                        || !previous.getType().equals(sensor.getType())) {
                        appendMeasurement(
                            sensor.getId() + " | " + sensor.getType() + " = " + sensor.getFormattedValue()
                        );
                    }
                }

                for (String previousId : previousSensors.keySet()) {
                    if (!nextSensors.containsKey(previousId)) {
                        appendMeasurement(previousId + " dejo de reportarse como sensor activo.");
                    }
                }

                previousSensors = nextSensors;
                activeSensorsLabel.setText(sensors.size() + " sensores");
                lastUpdateLabel.setText(timestamp());
            }
        });
    }

    @Override
    public void onStatusReceived(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                systemLabel.setText(message);
                systemLabel.setForeground(SUCCESS);
            }
        });
    }

    @Override
    public void onAlertReceived(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                appendAlert(message);
            }
        });
    }

    @Override
    public void onInfoMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                appendMeasurement("INFO: " + message);
            }
        });
    }

    @Override
    public void onErrorMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                appendAlert("ERROR: " + message);
            }
        });
    }

    private void appendMeasurement(String text) {
        measurementsArea.append("[" + timestamp() + "] " + text + "\n");
        measurementsArea.setCaretPosition(measurementsArea.getDocument().getLength());
    }

    private void appendAlert(String text) {
        alertsArea.append("[" + timestamp() + "] " + text + "\n");
        alertsArea.setCaretPosition(alertsArea.getDocument().getLength());
    }

    private String timestamp() {
        return timeFormat.format(new Date());
    }

    public static void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }
}
