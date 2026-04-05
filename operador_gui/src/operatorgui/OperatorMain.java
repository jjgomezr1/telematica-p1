package operatorgui;

import javax.swing.SwingUtilities;

public final class OperatorMain {
    private OperatorMain() {
    }

    public static void main(String[] args) {
        final String host = args.length > 0 ? args[0] : "localhost";
        final int port = args.length > 1 ? parsePort(args[1]) : 9000;
        final String operatorId = args.length > 2 ? args[2] : "operator_persona3";

        OperatorFrame.configureLookAndFeel();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                OperatorFrame frame = new OperatorFrame(host, port, operatorId);
                frame.setVisible(true);
            }
        });
    }

    private static int parsePort(String rawPort) {
        try {
            return Integer.parseInt(rawPort);
        } catch (NumberFormatException e) {
            return 9000;
        }
    }
}
