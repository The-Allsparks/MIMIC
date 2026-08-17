package org.allsparks.mimic.log;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable time-correlated mechanism event for offline analysis. */
public final class MimicEvent {
    private final long timestampNanos;
    private final MimicEventType type;
    private final String message;
    private final Map<String, String> fields;

    public MimicEvent(long timestampNanos, MimicEventType type, String message, Map<String, String> fields) {
        this.timestampNanos = timestampNanos;
        this.type = Objects.requireNonNull(type, "type");
        this.message = message == null ? "" : message;
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(
                fields == null ? Collections.<String, String>emptyMap() : fields));
    }

    public long timestampNanos() {
        return timestampNanos;
    }

    public MimicEventType type() {
        return type;
    }

    public String message() {
        return message;
    }

    public Map<String, String> fields() {
        return fields;
    }

    /** CSV-friendly single line (timestamp,type,message,k=v;...). */
    public String toExportLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(timestampNanos).append(',').append(type.name()).append(',');
        sb.append(escape(message)).append(',');
        boolean first = true;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (!first) {
                sb.append(';');
            }
            first = false;
            sb.append(escape(entry.getKey())).append('=').append(escape(entry.getValue()));
        }
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\\', '/').replace(',', '_').replace(';', '_').replace('\n', ' ');
    }
}
