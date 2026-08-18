package org.allsparks.mimic.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.allsparks.mimic.observe.MechanismSnapshot;

/**
 * Records time-correlated mechanism events. Exportable for offline analysis.
 * Does not command hardware. Field names are TRACE-compatible (stable keys).
 * Observation export keeps {@code pos} / {@code vel} and additively includes
 * {@code posValid} / {@code velValid} as {@link org.allsparks.mimic.observe.MeasurementValidity}
 * enum names.
 */
public final class MimicEventLogger {
    private final int capacity;
    private final List<MimicEvent> events;
    private long dropped;

    public MimicEventLogger(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.capacity = capacity;
        this.events = new ArrayList<>(capacity);
    }

    public void record(MimicEvent event) {
        Objects.requireNonNull(event, "event");
        if (events.size() >= capacity) {
            events.remove(0);
            dropped++;
        }
        events.add(event);
    }

    public void recordObservation(MechanismSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("id", snapshot.mechanismId());
        fields.put("pos", format(snapshot.position()));
        fields.put("posValid", snapshot.positionSample().validity().name());
        fields.put("unit", snapshot.positionUnitSymbol());
        fields.put("vel", format(snapshot.velocity()));
        fields.put("velValid", snapshot.velocitySample().validity().name());
        fields.put("acc", format(snapshot.acceleration()));
        fields.put("reqOut", format(snapshot.requestedOutput()));
        fields.put("appOut", format(snapshot.appliedOutput()));
        fields.put("amps", format(snapshot.currentAmps()));
        fields.put("lower", Boolean.toString(snapshot.lowerLimit().asserted()));
        fields.put("upper", Boolean.toString(snapshot.upperLimit().asserted()));
        fields.put("sensorValid", Boolean.toString(snapshot.sensorValid()));
        fields.put("disagree", format(snapshot.disagreement()));
        fields.put("loopNs", Long.toString(snapshot.loopDurationNanos()));
        MimicEventType type = snapshot.sensorValid() ? MimicEventType.LOOP_SAMPLE : MimicEventType.SENSOR_INVALID;
        record(new MimicEvent(snapshot.timestampNanos(), type, "observation", fields));
    }

    public List<MimicEvent> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public long droppedCount() {
        return dropped;
    }

    public void clear() {
        events.clear();
        dropped = 0;
    }

    public String exportCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("timestampNanos,type,message,fields\n");
        for (MimicEvent event : events) {
            sb.append(event.toExportLine()).append('\n');
        }
        return sb.toString();
    }

    private static String format(double value) {
        if (Double.isNaN(value)) {
            return "NaN";
        }
        return String.format(Locale.US, "%.4f", value);
    }
}
