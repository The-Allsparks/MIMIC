package org.allsparks.mimic.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.allsparks.mimic.templates.MechanismBlueprint;
import org.allsparks.mimic.templates.MechanismConstruct;
import org.allsparks.mimic.templates.MechanismFamily;
import org.allsparks.mimic.templates.MechanismMotionKind;

/**
 * Immutable mechanism instance metadata. Validated on {@link #build()}. Never
 * contains FTC hardware objects and never writes motors or servos.
 *
 * Join this to a {@code MechanismObserver} with the same {@code mechanismId}.
 * Optional observer extras may use the same names and roles; this type stays
 * metadata and does not read or write hardware.
 */
public final class MechanismConfiguration {
    private final String mechanismId;
    private final ConstructDescriptor construct;
    private final ActuatorTopology actuators;
    private final List<SensorDeclaration> sensors;
    private final EnumSet<Capability> capabilities;
    private final Map<SensorRole, DegradedBehavior> degradedBehaviors;
    private final CalibrationStrategy calibrationStrategy;
    private final CalibrationContract calibrationContract;
    private final ControlDomain controlDomain;
    private final LimitPolicy limitPolicy;
    private final LimitContract limitContract;
    private final SyncContract syncContract;
    private final List<String> namedStates;

    private MechanismConfiguration(Builder builder) {
        this.mechanismId = builder.mechanismId;
        this.construct = builder.construct;
        this.actuators = builder.actuators;
        this.sensors = Collections.unmodifiableList(new ArrayList<>(builder.sensors));
        this.capabilities =
                builder.capabilities.isEmpty()
                        ? EnumSet.noneOf(Capability.class)
                        : EnumSet.copyOf(builder.capabilities);
        this.degradedBehaviors = Collections.unmodifiableMap(new EnumMap<>(builder.degradedBehaviors));
        this.calibrationStrategy = builder.calibrationStrategy;
        this.calibrationContract = builder.calibrationContract;
        this.controlDomain = builder.controlDomain;
        this.limitPolicy = builder.limitPolicy;
        this.limitContract = builder.limitContract;
        this.syncContract = builder.syncContract;
        this.namedStates = Collections.unmodifiableList(new ArrayList<>(builder.namedStates));
    }

    public static Builder builder(String mechanismId) {
        return new Builder(mechanismId);
    }

    public String mechanismId() {
        return mechanismId;
    }

    public ConstructDescriptor construct() {
        return construct;
    }

    public MechanismFamily family() {
        return construct == null ? null : construct.family();
    }

    public MechanismMotionKind motionKind() {
        return construct == null ? null : construct.motionKind();
    }

    public ActuatorTopology actuators() {
        return actuators;
    }

    public List<SensorDeclaration> sensors() {
        return sensors;
    }

    public Set<Capability> capabilities() {
        return Collections.unmodifiableSet(capabilities);
    }

    public Map<SensorRole, DegradedBehavior> degradedBehaviors() {
        return degradedBehaviors;
    }

    public DegradedBehavior degradedBehavior(SensorRole role) {
        DegradedBehavior behavior = degradedBehaviors.get(role);
        return behavior == null ? DegradedBehavior.MARK_DEGRADED : behavior;
    }

    public CalibrationStrategy calibrationStrategy() {
        return calibrationStrategy;
    }

    /**
     * Optional homing bounds. Absent by default. Presence does not run
     * homing and does not write hardware.
     */
    public Optional<CalibrationContract> calibrationContract() {
        return Optional.ofNullable(calibrationContract);
    }

    public ControlDomain controlDomain() {
        return controlDomain;
    }

    public LimitPolicy limitPolicy() {
        return limitPolicy;
    }

    /**
     * Optional limit bounds. Absent by default. Presence does not enforce
     * limits and does not write hardware.
     */
    public Optional<LimitContract> limitContract() {
        return Optional.ofNullable(limitContract);
    }

    /**
     * Optional independent-actuator disagreement limit. Absent by default.
     * Presence does not synchronize motors, does not apply side correction,
     * and is unused by {@code MimicSession}.
     */
    public Optional<SyncContract> syncContract() {
        return Optional.ofNullable(syncContract);
    }

    /**
     * Declared pose or cycle names for this instance. Empty when unused. The
     * list is metadata: it does not schedule motion or write hardware.
     */
    public List<String> namedStates() {
        return namedStates;
    }

    public Optional<MechanismBlueprint> toBlueprint() {
        if (construct == null || !construct.isStandard()) {
            return Optional.empty();
        }
        return Optional.of(MechanismBlueprint.of(mechanismId, construct.standardConstruct()));
    }

    public ValidationResult validate() {
        return ConfigurationValidator.validate(this);
    }

    public static final class Builder {
        private final String mechanismId;
        private ConstructDescriptor construct;
        private ActuatorTopology actuators = ActuatorTopology.none();
        private final List<SensorDeclaration> sensors = new ArrayList<>();
        private final EnumSet<Capability> capabilities = EnumSet.noneOf(Capability.class);
        private final EnumMap<SensorRole, DegradedBehavior> degradedBehaviors =
                new EnumMap<>(SensorRole.class);
        private CalibrationStrategy calibrationStrategy = CalibrationStrategy.NONE;
        private CalibrationContract calibrationContract;
        private ControlDomain controlDomain = ControlDomain.PASSIVE_OBSERVATION;
        private LimitPolicy limitPolicy = LimitPolicy.NONE;
        private LimitContract limitContract;
        private SyncContract syncContract;
        private final List<String> namedStates = new ArrayList<>();

        private Builder(String mechanismId) {
            this.mechanismId = mechanismId;
        }

        public Builder construct(ConstructDescriptor construct) {
            this.construct = construct;
            return this;
        }

        public Builder construct(MechanismConstruct construct) {
            this.construct = ConstructDescriptor.standard(construct);
            return this;
        }

        public Builder actuators(ActuatorTopology actuators) {
            this.actuators = actuators;
            return this;
        }

        public Builder sensor(SensorDeclaration declaration) {
            this.sensors.add(Objects.requireNonNull(declaration, "declaration"));
            return this;
        }

        public Builder sensor(String name, SensorRole role) {
            this.sensors.add(SensorDeclaration.optional(name, role));
            return this;
        }

        public Builder requiredSensor(String name, SensorRole role) {
            this.sensors.add(SensorDeclaration.required(name, role));
            return this;
        }

        public Builder enable(Capability capability) {
            this.capabilities.add(Objects.requireNonNull(capability, "capability"));
            return this;
        }

        public Builder degradedBehavior(SensorRole role, DegradedBehavior behavior) {
            this.degradedBehaviors.put(
                    Objects.requireNonNull(role, "role"), Objects.requireNonNull(behavior, "behavior"));
            return this;
        }

        public Builder calibrationStrategy(CalibrationStrategy calibrationStrategy) {
            this.calibrationStrategy =
                    calibrationStrategy == null ? CalibrationStrategy.NONE : calibrationStrategy;
            return this;
        }

        /**
         * Attach an optional {@link CalibrationContract}. {@code null} leaves
         * the contract absent. A non-null contract copies its strategy onto
         * {@link #calibrationStrategy(CalibrationStrategy)} so existing
         * strategy-only builders stay valid.
         */
        public Builder calibrationContract(CalibrationContract calibrationContract) {
            this.calibrationContract = calibrationContract;
            if (calibrationContract != null) {
                this.calibrationStrategy = calibrationContract.strategy();
            }
            return this;
        }

        public Builder controlDomain(ControlDomain controlDomain) {
            this.controlDomain =
                    controlDomain == null ? ControlDomain.PASSIVE_OBSERVATION : controlDomain;
            return this;
        }

        public Builder limitPolicy(LimitPolicy limitPolicy) {
            this.limitPolicy = limitPolicy == null ? LimitPolicy.NONE : limitPolicy;
            return this;
        }

        /**
         * Attach an optional {@link LimitContract}. {@code null} leaves the
         * contract absent. A non-null contract copies its policy onto
         * {@link #limitPolicy(LimitPolicy)} so existing policy-only builders
         * stay valid.
         */
        public Builder limitContract(LimitContract limitContract) {
            this.limitContract = limitContract;
            if (limitContract != null) {
                this.limitPolicy = limitContract.policy();
            }
            return this;
        }

        /**
         * Attach an optional {@link SyncContract}. {@code null} leaves the
         * contract absent. Presence does not enable Phase 5 synchronization
         * and does not write hardware.
         */
        public Builder syncContract(SyncContract syncContract) {
            this.syncContract = syncContract;
            return this;
        }

        /**
         * Replace the declared named-state set. Names do not move hardware and
         * are not a scheduler.
         */
        public Builder namedStates(String... names) {
            this.namedStates.clear();
            if (names != null) {
                Collections.addAll(this.namedStates, names);
            }
            return this;
        }

        /** Validate without throwing. Does not write hardware. */
        public ValidationResult validate() {
            return ConfigurationValidator.validate(new MechanismConfiguration(this));
        }

        /**
         * Build an immutable configuration. Throws if validation fails. Does not
         * write hardware.
         */
        public MechanismConfiguration build() {
            MechanismConfiguration configuration = new MechanismConfiguration(this);
            ValidationResult result = ConfigurationValidator.validate(configuration);
            if (!result.valid()) {
                throw new InvalidMechanismConfigurationException(result);
            }
            return configuration;
        }
    }
}
