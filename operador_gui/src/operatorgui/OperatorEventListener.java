package operatorgui;

import java.util.List;

public interface OperatorEventListener {
    void onConnectionStateChanged(String status, boolean connected);

    void onRegistrationAcknowledged(String message);

    void onSensorListReceived(List<SensorSnapshot> sensors);

    void onStatusReceived(String message);

    void onAlertReceived(String message);

    void onInfoMessage(String message);

    void onErrorMessage(String message);
}
