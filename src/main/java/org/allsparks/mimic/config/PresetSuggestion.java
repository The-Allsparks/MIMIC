package org.allsparks.mimic.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Suggested defaults for a standard construct. Suggestions are not wiring and
 * do not enable a capability on a robot.
 */
public final class PresetSuggestion {
    private final ActuatorTopology topology;
    private final EnumSet<Capability> capabilities;
    private final List<SensorDeclaration> exampleSensors;
    private final ControlDomain controlDomain;
    private final CalibrationStrategy calibrationStrategy;
    private final LimitPolicy limitPolicy;
    private final String notes;

    public PresetSuggestion(
            ActuatorTopology topology,
            Set<Capability> capabilities,
            List<SensorDeclaration> exampleSensors,
            ControlDomain controlDomain,
            CalibrationStrategy calibrationStrategy,
            LimitPolicy limitPolicy,
            String notes) {
        this.topology = topology == null ? ActuatorTopology.none() : topology;
        this.capabilities =
                capabilities == null || capabilities.isEmpty()
                        ? EnumSet.noneOf(Capability.class)
                        : EnumSet.copyOf(capabilities);
        this.exampleSensors =
                exampleSensors == null
                        ? Collections.emptyList()
                        : Collections.unmodifiableList(new ArrayList<>(exampleSensors));
        this.controlDomain = controlDomain == null ? ControlDomain.PASSIVE_OBSERVATION : controlDomain;
        this.calibrationStrategy =
                calibrationStrategy == null ? CalibrationStrategy.NONE : calibrationStrategy;
        this.limitPolicy = limitPolicy == null ? LimitPolicy.NONE : limitPolicy;
        this.notes = notes == null ? "" : notes;
    }

    public ActuatorTopology topology() {
        return topology;
    }

    public Set<Capability> capabilities() {
        return Collections.unmodifiableSet(capabilities);
    }

    /**
     * Example sensor declarations that make a valid configuration for tests and
     * docs. They are not a claim that the robot has those sensors.
     */
    public List<SensorDeclaration> exampleSensors() {
        return exampleSensors;
    }

    public ControlDomain controlDomain() {
        return controlDomain;
    }

    public CalibrationStrategy calibrationStrategy() {
        return calibrationStrategy;
    }

    public LimitPolicy limitPolicy() {
        return limitPolicy;
    }

    public String notes() {
        return notes;
    }
}
