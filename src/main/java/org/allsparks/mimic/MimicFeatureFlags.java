package org.allsparks.mimic;

/**
 * Central feature flags. Any flag that could command hardware defaults to
 * {@code false}.
 *
 * Missing or invalid measurements must fail safe: active control remains off
 * and sensing is reported as invalid rather than inventing values.
 */
public final class MimicFeatureFlags {

    private final boolean phase0Contracts;
    private final boolean phase1PassiveObservation;
    private final boolean phase2Calibration;
    private final boolean phase3Limits;
    private final boolean phase4ProfiledControl;
    private final boolean phase5Synchronization;
    private final boolean phase6States;
    private final boolean phase7Interlocks;
    private final boolean phase8Faults;
    private final boolean phase9Amper;
    private final boolean phase10Simulation;

    private MimicFeatureFlags(Builder builder) {
        this.phase0Contracts = builder.phase0Contracts;
        this.phase1PassiveObservation = builder.phase1PassiveObservation;
        this.phase2Calibration = builder.phase2Calibration;
        this.phase3Limits = builder.phase3Limits;
        this.phase4ProfiledControl = builder.phase4ProfiledControl;
        this.phase5Synchronization = builder.phase5Synchronization;
        this.phase6States = builder.phase6States;
        this.phase7Interlocks = builder.phase7Interlocks;
        this.phase8Faults = builder.phase8Faults;
        this.phase9Amper = builder.phase9Amper;
        this.phase10Simulation = builder.phase10Simulation;
    }

    /** Safe defaults: Phase 0 on; all actuation off. */
    public static MimicFeatureFlags defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Phase 0 + Phase 1 only. No motor or servo output. */
    public static MimicFeatureFlags passiveObservation() {
        return builder().phase1PassiveObservation(true).build();
    }

    public boolean isPhase0Contracts() {
        return phase0Contracts;
    }

    public boolean isPhase1PassiveObservation() {
        return phase1PassiveObservation;
    }

    public boolean isPhase2Calibration() {
        return phase2Calibration;
    }

    public boolean isPhase3Limits() {
        return phase3Limits;
    }

    public boolean isPhase4ProfiledControl() {
        return phase4ProfiledControl;
    }

    public boolean isPhase5Synchronization() {
        return phase5Synchronization;
    }

    public boolean isPhase6States() {
        return phase6States;
    }

    public boolean isPhase7Interlocks() {
        return phase7Interlocks;
    }

    public boolean isPhase8Faults() {
        return phase8Faults;
    }

    public boolean isPhase9Amper() {
        return phase9Amper;
    }

    public boolean isPhase10Simulation() {
        return phase10Simulation;
    }

    /** True if any feature that may command motors or servos is enabled. */
    public boolean isAnyActuationEnabled() {
        return phase2Calibration
                || phase3Limits
                || phase4ProfiledControl
                || phase5Synchronization
                || phase6States
                || phase7Interlocks
                || phase8Faults
                || phase9Amper
                || phase10Simulation;
    }

    public static final class Builder {
        private boolean phase0Contracts = true;
        private boolean phase1PassiveObservation = false;
        private boolean phase2Calibration = false;
        private boolean phase3Limits = false;
        private boolean phase4ProfiledControl = false;
        private boolean phase5Synchronization = false;
        private boolean phase6States = false;
        private boolean phase7Interlocks = false;
        private boolean phase8Faults = false;
        private boolean phase9Amper = false;
        private boolean phase10Simulation = false;

        public Builder phase0Contracts(boolean value) {
            this.phase0Contracts = value;
            return this;
        }

        public Builder phase1PassiveObservation(boolean value) {
            this.phase1PassiveObservation = value;
            return this;
        }

        public Builder phase2Calibration(boolean value) {
            this.phase2Calibration = value;
            return this;
        }

        public Builder phase3Limits(boolean value) {
            this.phase3Limits = value;
            return this;
        }

        public Builder phase4ProfiledControl(boolean value) {
            this.phase4ProfiledControl = value;
            return this;
        }

        public Builder phase5Synchronization(boolean value) {
            this.phase5Synchronization = value;
            return this;
        }

        public Builder phase6States(boolean value) {
            this.phase6States = value;
            return this;
        }

        public Builder phase7Interlocks(boolean value) {
            this.phase7Interlocks = value;
            return this;
        }

        public Builder phase8Faults(boolean value) {
            this.phase8Faults = value;
            return this;
        }

        public Builder phase9Amper(boolean value) {
            this.phase9Amper = value;
            return this;
        }

        public Builder phase10Simulation(boolean value) {
            this.phase10Simulation = value;
            return this;
        }

        public MimicFeatureFlags build() {
            return new MimicFeatureFlags(this);
        }
    }
}
