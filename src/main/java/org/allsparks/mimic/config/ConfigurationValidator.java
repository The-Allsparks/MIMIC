package org.allsparks.mimic.config;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.allsparks.mimic.templates.MechanismFamily;

/**
 * Pure validation of {@link MechanismConfiguration}. Never writes hardware.
 */
public final class ConfigurationValidator {
    public static final String EMPTY_ID = "EMPTY_ID";
    public static final String MISSING_CONSTRUCT = "MISSING_CONSTRUCT";
    public static final String MISSING_TOPOLOGY = "MISSING_TOPOLOGY";
    public static final String EMPTY_SENSOR_NAME = "EMPTY_SENSOR_NAME";
    public static final String DUPLICATE_SENSOR_NAME = "DUPLICATE_SENSOR_NAME";
    public static final String HOMING_WITHOUT_REFERENCE = "HOMING_WITHOUT_REFERENCE";
    public static final String SOFT_LIMITS_WITHOUT_POSITION = "SOFT_LIMITS_WITHOUT_POSITION";
    public static final String SOFT_LIMITS_WITHOUT_CALIBRATION = "SOFT_LIMITS_WITHOUT_CALIBRATION";
    public static final String READY_AT_SPEED_WITHOUT_VELOCITY = "READY_AT_SPEED_WITHOUT_VELOCITY";
    public static final String SYNC_REQUIRES_INDEPENDENT_SIDES = "SYNC_REQUIRES_INDEPENDENT_SIDES";
    public static final String SYNC_INSUFFICIENT_FEEDBACK = "SYNC_INSUFFICIENT_FEEDBACK";
    public static final String PIECE_COUNTING_WITHOUT_EVIDENCE = "PIECE_COUNTING_WITHOUT_EVIDENCE";
    public static final String ACTIVE_DOMAIN_WITHOUT_ACTUATOR = "ACTIVE_DOMAIN_WITHOUT_ACTUATOR";
    public static final String SERVO_COMMAND_AS_MEASURED_POSITION = "SERVO_COMMAND_AS_MEASURED_POSITION";
    public static final String LINKED_MOTORS_WITH_INDEPENDENT_SYNC = "LINKED_MOTORS_WITH_INDEPENDENT_SYNC";
    public static final String REQUIRED_SENSOR_IGNORE_OPTIONAL = "REQUIRED_SENSOR_IGNORE_OPTIONAL";
    public static final String EMPTY_NAMED_STATE = "EMPTY_NAMED_STATE";
    public static final String DUPLICATE_NAMED_STATE = "DUPLICATE_NAMED_STATE";
    public static final String CALIBRATION_TIMEOUT_REQUIRED = "CALIBRATION_TIMEOUT_REQUIRED";
    public static final String CALIBRATION_MAX_TRAVEL_REQUIRED = "CALIBRATION_MAX_TRAVEL_REQUIRED";
    public static final String CALIBRATION_STRATEGY_MISMATCH = "CALIBRATION_STRATEGY_MISMATCH";
    public static final String LIMIT_POLICY_MISMATCH = "LIMIT_POLICY_MISMATCH";
    public static final String LIMIT_POLICY_REQUIRED = "LIMIT_POLICY_REQUIRED";
    public static final String LIMIT_SOFT_BOUNDS_REQUIRED = "LIMIT_SOFT_BOUNDS_REQUIRED";
    public static final String LIMIT_WRAP_PERIOD_REQUIRED = "LIMIT_WRAP_PERIOD_REQUIRED";
    public static final String LIMIT_INVALID_STOPPING_MARGIN = "LIMIT_INVALID_STOPPING_MARGIN";
    public static final String LIMIT_LINEAR_BOUNDS_ORDER = "LIMIT_LINEAR_BOUNDS_ORDER";
    public static final String SYNC_MAX_DISAGREEMENT_REQUIRED = "SYNC_MAX_DISAGREEMENT_REQUIRED";

    private ConfigurationValidator() {}

    public static ValidationResult validate(MechanismConfiguration configuration) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (configuration == null) {
            issues.add(new ValidationIssue(MISSING_CONSTRUCT, "configuration", "configuration is required"));
            return ValidationResult.of(issues);
        }
        if (configuration.mechanismId() == null || configuration.mechanismId().trim().isEmpty()) {
            issues.add(new ValidationIssue(EMPTY_ID, "mechanismId", "mechanismId must be non-empty"));
        }
        if (configuration.construct() == null) {
            issues.add(new ValidationIssue(MISSING_CONSTRUCT, "construct", "construct is required"));
        }
        if (configuration.actuators() == null) {
            issues.add(new ValidationIssue(MISSING_TOPOLOGY, "actuators", "actuator topology is required"));
            return ValidationResult.of(issues);
        }

        EnumSet<SensorRole> roles = EnumSet.noneOf(SensorRole.class);
        List<String> names = new ArrayList<>();
        for (SensorDeclaration sensor : configuration.sensors()) {
            if (sensor.name() == null || sensor.name().trim().isEmpty()) {
                issues.add(new ValidationIssue(EMPTY_SENSOR_NAME, "sensors", "sensor name must be non-empty"));
            } else if (names.contains(sensor.name())) {
                issues.add(new ValidationIssue(
                        DUPLICATE_SENSOR_NAME, sensor.name(), "duplicate sensor name " + sensor.name()));
            } else {
                names.add(sensor.name());
            }
            roles.add(sensor.role());
            DegradedBehavior degraded = configuration.degradedBehavior(sensor.role());
            if (sensor.required() && degraded == DegradedBehavior.IGNORE_OPTIONAL) {
                issues.add(new ValidationIssue(
                        REQUIRED_SENSOR_IGNORE_OPTIONAL,
                        sensor.name(),
                        "required sensor " + sensor.name() + " cannot use IGNORE_OPTIONAL"));
            }
        }

        if (configuration.capabilities().contains(Capability.HOMING)
                && !hasHomingReference(configuration, roles)) {
            issues.add(new ValidationIssue(
                    HOMING_WITHOUT_REFERENCE,
                    "homing",
                    "HOMING requires a home/index/limit/absolute reference or a known-pose calibration strategy"));
        }

        CalibrationContract contract = configuration.calibrationContract().orElse(null);
        if (contract != null) {
            if (contract.strategy() != configuration.calibrationStrategy()) {
                issues.add(new ValidationIssue(
                        CALIBRATION_STRATEGY_MISMATCH,
                        "calibrationContract",
                        "calibration contract strategy must match calibrationStrategy"));
            }
            contract.collectBoundIssues(issues);
        }

        LimitContract limitContract = configuration.limitContract().orElse(null);
        if (limitContract != null) {
            if (limitContract.policy() != configuration.limitPolicy()) {
                issues.add(
                        new ValidationIssue(
                                LIMIT_POLICY_MISMATCH,
                                "limitContract",
                                "limit contract policy must match limitPolicy"));
            }
            limitContract.collectBoundIssues(issues);
        }

        boolean wantsSoft = configuration.capabilities().contains(Capability.SOFT_LIMITS)
                || configuration.limitPolicy() == LimitPolicy.SOFT_ONLY
                || configuration.limitPolicy() == LimitPolicy.SOFT_AND_HARD;
        if (wantsSoft && !hasPositionSource(roles)) {
            issues.add(new ValidationIssue(
                    SOFT_LIMITS_WITHOUT_POSITION,
                    "softLimits",
                    "soft limits require a position source"));
        }
        if (wantsSoft && configuration.calibrationStrategy() == CalibrationStrategy.NONE) {
            issues.add(new ValidationIssue(
                    SOFT_LIMITS_WITHOUT_CALIBRATION,
                    "calibrationStrategy",
                    "soft limits require a calibration strategy other than NONE"));
        }

        if (configuration.capabilities().contains(Capability.READY_AT_SPEED)
                && !hasRole(roles, SensorRole.VELOCITY)) {
            issues.add(new ValidationIssue(
                    READY_AT_SPEED_WITHOUT_VELOCITY,
                    "readyAtSpeed",
                    "READY_AT_SPEED requires a VELOCITY sensor role"));
        }

        if (configuration.capabilities().contains(Capability.MULTI_ACTUATOR_SYNCHRONIZATION)) {
            if (configuration.actuators().isMechanicallyLinked()) {
                issues.add(new ValidationIssue(
                        LINKED_MOTORS_WITH_INDEPENDENT_SYNC,
                        "actuators",
                        "mechanically linked motors must not enable independent synchronization"));
            } else if (!configuration.actuators().isIndependentlySensedMotors()
                    || configuration.actuators().actuatorCount() < 2) {
                issues.add(new ValidationIssue(
                        SYNC_REQUIRES_INDEPENDENT_SIDES,
                        "actuators",
                        "MULTI_ACTUATOR_SYNCHRONIZATION requires independently sensed motors (count >= 2)"));
            }
            if (countPositionFeedback(roles) < 2) {
                issues.add(new ValidationIssue(
                        SYNC_INSUFFICIENT_FEEDBACK,
                        "sensors",
                        "independently synchronized actuators require at least two position measurements"));
            }
        }

        SyncContract syncContract = configuration.syncContract().orElse(null);
        if (syncContract != null) {
            syncContract.collectBoundIssues(issues);
            if (configuration.actuators().isMechanicallyLinked()) {
                issues.add(
                        new ValidationIssue(
                                LINKED_MOTORS_WITH_INDEPENDENT_SYNC,
                                "syncContract",
                                "mechanically linked motors must not declare independent synchronization"));
            }
        }

        if (configuration.capabilities().contains(Capability.PIECE_COUNTING)
                && !hasPiecePassage(roles)) {
            issues.add(new ValidationIssue(
                    PIECE_COUNTING_WITHOUT_EVIDENCE,
                    "pieceCounting",
                    "PIECE_COUNTING requires PIECE_ENTRY, PIECE_EXIT, or PIECE_COUNT evidence"));
        }

        boolean activeDomain = configuration.controlDomain() != ControlDomain.PASSIVE_OBSERVATION;
        boolean passiveFamily =
                configuration.construct() != null
                        && configuration.construct().family() == MechanismFamily.PASSIVE;
        if (activeDomain && configuration.actuators().isPassive() && !passiveFamily) {
            issues.add(new ValidationIssue(
                    ACTIVE_DOMAIN_WITHOUT_ACTUATOR,
                    "actuators",
                    "active control domain requires at least one actuator"));
        }

        if (configuration.actuators().isPositionalServo() && claimsUnmeasuredServoPose(roles)) {
            issues.add(new ValidationIssue(
                    SERVO_COMMAND_AS_MEASURED_POSITION,
                    "sensors",
                    "positional servo command is not measured position; declare EXTERNAL_SERVO_FEEDBACK"));
        }

        List<String> seenNamedStates = new ArrayList<>();
        for (String name : configuration.namedStates()) {
            if (name == null || name.trim().isEmpty()) {
                issues.add(new ValidationIssue(
                        EMPTY_NAMED_STATE, "namedStates", "named state must be non-empty"));
            } else if (seenNamedStates.contains(name)) {
                issues.add(new ValidationIssue(
                        DUPLICATE_NAMED_STATE, name, "duplicate named state " + name));
            } else {
                seenNamedStates.add(name);
            }
        }

        return ValidationResult.of(issues);
    }

    /**
     * True when the configuration already names a homing reference. A
     * {@link CalibrationContract} is not itself a reference and does not
     * enable {@link CalibrationStrategy#HARD_STOP_CURRENT} without current
     * sensing.
     */
    public static boolean hasHomingReference(MechanismConfiguration configuration) {
        if (configuration == null) {
            return false;
        }
        EnumSet<SensorRole> roles = EnumSet.noneOf(SensorRole.class);
        for (SensorDeclaration sensor : configuration.sensors()) {
            roles.add(sensor.role());
        }
        return hasHomingReference(configuration, roles);
    }

    static boolean hasHomingReference(
            MechanismConfiguration configuration, EnumSet<SensorRole> roles) {
        if (configuration == null) {
            return false;
        }
        CalibrationStrategy strategy = configuration.calibrationStrategy();
        if (strategy == CalibrationStrategy.KNOWN_STARTUP_POSE
                || strategy == CalibrationStrategy.ABSOLUTE_SENSOR
                || strategy == CalibrationStrategy.HOME_SWITCH
                || strategy == CalibrationStrategy.INDEX_PULSE
                || strategy == CalibrationStrategy.MANUAL
                || strategy == CalibrationStrategy.RETAINED_WITH_VALIDATION) {
            return true;
        }
        if (strategy == CalibrationStrategy.HARD_STOP_CURRENT
                && hasRole(roles, SensorRole.ACTUATOR_CURRENT)) {
            return true;
        }
        for (SensorRole role : roles) {
            if (role.isHomingReference()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPositionSource(EnumSet<SensorRole> roles) {
        for (SensorRole role : roles) {
            if (role.isPositionSource()) {
                return true;
            }
        }
        return false;
    }

    private static int countPositionFeedback(EnumSet<SensorRole> roles) {
        int count = 0;
        for (SensorRole role : roles) {
            if (role.isPositionSource() || role == SensorRole.REDUNDANT_POSITION) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasPiecePassage(EnumSet<SensorRole> roles) {
        return hasRole(roles, SensorRole.PIECE_ENTRY)
                || hasRole(roles, SensorRole.PIECE_EXIT)
                || hasRole(roles, SensorRole.PIECE_COUNT);
    }

    private static boolean claimsUnmeasuredServoPose(EnumSet<SensorRole> roles) {
        if (hasRole(roles, SensorRole.EXTERNAL_SERVO_FEEDBACK)) {
            return false;
        }
        return hasRole(roles, SensorRole.RELATIVE_POSITION)
                || hasRole(roles, SensorRole.ABSOLUTE_POSITION)
                || hasRole(roles, SensorRole.ACTUATOR_SIDE_POSITION);
    }

    private static boolean hasRole(EnumSet<SensorRole> roles, SensorRole role) {
        return roles.contains(role);
    }
}
