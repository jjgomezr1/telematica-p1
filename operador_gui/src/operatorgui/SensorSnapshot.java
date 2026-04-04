package operatorgui;

import java.util.Locale;

public final class SensorSnapshot {
    private final String id;
    private final String type;
    private final double value;

    public SensorSnapshot(String id, String type, double value) {
        this.id = id;
        this.type = type;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    public String getFormattedValue() {
        return String.format(Locale.US, "%.2f", value);
    }
}
