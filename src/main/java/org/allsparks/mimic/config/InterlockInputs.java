package org.allsparks.mimic.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.allsparks.mimic.observe.Readiness;

/**
 * Immutable evaluation table for {@link InterlockRule}: named states and
 * readiness bits keyed by mechanism id. Reuses {@code namedStates} names
 * and {@link Readiness#ready()} as inputs only. Not a snapshot of
 * hardware, not a scheduler, and does not write motors or servos.
 */
public final class InterlockInputs {
    private final Map<String, String> namedStates;
    private final Map<String, Boolean> ready;

    private InterlockInputs(Map<String, String> namedStates, Map<String, Boolean> ready) {
        this.namedStates = Collections.unmodifiableMap(new LinkedHashMap<>(namedStates));
        this.ready = Collections.unmodifiableMap(new LinkedHashMap<>(ready));
    }

    public static InterlockInputs empty() {
        return new InterlockInputs(Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * Record the current named state for {@code mechanismId}. Names are
     * data, not motion commands.
     */
    public InterlockInputs withNamedState(String mechanismId, String namedState) {
        requireId(mechanismId, "mechanismId");
        requireId(namedState, "namedState");
        Map<String, String> next = new LinkedHashMap<>(namedStates);
        next.put(mechanismId, namedState);
        return new InterlockInputs(next, ready);
    }

    /**
     * Record whether {@code mechanismId} is ready. Typical source is
     * {@link Readiness#ready()}; this table stores the bit only.
     */
    public InterlockInputs withReady(String mechanismId, boolean readyBit) {
        requireId(mechanismId, "mechanismId");
        Map<String, Boolean> next = new LinkedHashMap<>(ready);
        next.put(mechanismId, readyBit);
        return new InterlockInputs(namedStates, next);
    }

    /**
     * Copy {@link Readiness#ready()} into the table. Does not feed the
     * evaluator, does not spin a launcher, and does not write hardware.
     */
    public InterlockInputs withReadiness(String mechanismId, Readiness readiness) {
        Objects.requireNonNull(readiness, "readiness");
        return withReady(mechanismId, readiness.ready());
    }

    public Optional<String> namedState(String mechanismId) {
        return Optional.ofNullable(namedStates.get(mechanismId));
    }

    public Optional<Boolean> ready(String mechanismId) {
        return Optional.ofNullable(ready.get(mechanismId));
    }

    public Map<String, String> namedStates() {
        return namedStates;
    }

    public Map<String, Boolean> readyBits() {
        return ready;
    }

    private static void requireId(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must be non-empty");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof InterlockInputs)) {
            return false;
        }
        InterlockInputs that = (InterlockInputs) other;
        return namedStates.equals(that.namedStates) && ready.equals(that.ready);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namedStates, ready);
    }
}
