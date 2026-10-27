package org.allsparks.mimic.control;

import java.util.Objects;

/**
 * What the safety gate allowed, and why. {@link #passed()} means the
 * requested command was written unchanged.
 */
public final class GateDecision {
    private final ActuatorCommand command;
    private final String reason;

    public GateDecision(ActuatorCommand command, String reason) {
        this.command = Objects.requireNonNull(command, "command");
        if (reason == null || reason.isEmpty()) {
            throw new IllegalArgumentException("reason must be non-empty");
        }
        this.reason = reason;
    }

    public ActuatorCommand command() {
        return command;
    }

    public String reason() {
        return reason;
    }

    /** True when the sink receives the command the law asked for. */
    public boolean passed() {
        return ActuatorSafetyGate.PASSED.equals(reason);
    }
}
