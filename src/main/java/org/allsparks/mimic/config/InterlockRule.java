package org.allsparks.mimic.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.allsparks.mimic.api.GoalDisposition;
import org.allsparks.mimic.api.GoalResult;

/**
 * Named cross-mechanism constraint. Sketch:
 * {@code InterlockRule.when("feeder").requires("launcher", READY).onFail(REJECT)}.
 *
 * Phase 0 evaluates tables only. A satisfied requirement returns
 * {@link GoalDisposition#ACCEPTED}. A failed requirement uses the explicit
 * {@link InterlockOutcome}. This is not {@code InterlockManager}, not a
 * Command / Subsystem / Scheduler, and does not write motors or servos.
 * {@link org.allsparks.mimic.MimicSession} must not register rules. The
 * engine that would command hardware remains Phase 7 / issue #19.
 *
 * Generated {@link InterlockOutcome#INTERMEDIATE} goals must not cycle:
 * an intermediate must not be the source mechanism, and two rules must
 * not replace each other's source. {@link #generatedIntermediatesCycle}
 * reports that data-model hazard. It does not schedule either goal.
 */
public final class InterlockRule {
    public static final String READY = "READY";

    public static final String SATISFIED = "INTERLOCK_SATISFIED";
    public static final String REJECTED = "INTERLOCK_REJECTED";
    public static final String DEFERRED = "INTERLOCK_DEFERRED";
    public static final String CLAMPED = "INTERLOCK_CLAMPED";
    public static final String INTERMEDIATE_GOAL = "INTERLOCK_INTERMEDIATE";
    public static final String CONFIRM_REQUIRED = "INTERLOCK_CONFIRM_REQUIRED";

    private final String name;
    private final String source;
    private final String requiredMechanism;
    private final String requiredState;
    private final InterlockOutcome onFail;
    private final String clampTo;
    private final String intermediate;

    private InterlockRule(Builder builder) {
        this.source = trimToEmpty(builder.source);
        this.requiredMechanism = trimToEmpty(builder.requiredMechanism);
        this.requiredState = trimToEmpty(builder.requiredState);
        this.onFail = builder.onFail;
        this.clampTo = emptyToNull(builder.clampTo);
        this.intermediate = emptyToNull(builder.intermediate);
        this.name = emptyToNull(builder.name) == null ? defaultName() : builder.name.trim();
    }

    /**
     * Start a rule for goals requested on {@code sourceMechanism}.
     * Example: {@code when("feeder")} or {@code when("intake")}.
     */
    public static Builder when(String sourceMechanism) {
        return new Builder(sourceMechanism);
    }

    public String name() {
        return name;
    }

    public String source() {
        return source;
    }

    public String requiredMechanism() {
        return requiredMechanism;
    }

    public String requiredState() {
        return requiredState;
    }

    public InterlockOutcome onFail() {
        return onFail;
    }

    public String clampTo() {
        return clampTo;
    }

    public String intermediate() {
        return intermediate;
    }

    /**
     * Mechanism ids this rule reads: the source and the required other.
     * Order is source then required. Immutable.
     */
    public List<String> sources() {
        List<String> ids = new ArrayList<>(2);
        if (!source.isEmpty()) {
            ids.add(source);
        }
        if (!requiredMechanism.isEmpty()) {
            ids.add(requiredMechanism);
        }
        return Collections.unmodifiableList(ids);
    }

    /**
     * Always false. Declaring a named rule does not authorize actuation
     * or enable Phase 7 output.
     */
    public boolean permitsMotion() {
        return false;
    }

    /**
     * Table evaluation for a goal on {@link #source()}. Missing evidence
     * is not a pass: an omitted ready bit or named state fails the
     * requirement. Does not write hardware.
     */
    public GoalResult evaluate(InterlockInputs inputs) {
        Objects.requireNonNull(inputs, "inputs");
        if (!validate().valid()) {
            return new GoalResult(false, GoalDisposition.REJECTED, REJECTED);
        }
        if (requirementMet(inputs)) {
            return GoalResult.accepted(SATISFIED);
        }
        return new GoalResult(false, onFail.disposition(), failReason());
    }

    public ValidationResult validate() {
        List<ValidationIssue> issues = new ArrayList<>();
        if (source.isEmpty()) {
            issues.add(
                    new ValidationIssue(
                            ConfigurationValidator.INTERLOCK_EMPTY_SOURCE,
                            "source",
                            "interlock source mechanism must be non-empty"));
        }
        if (requiredMechanism.isEmpty() || requiredState.isEmpty()) {
            issues.add(
                    new ValidationIssue(
                            ConfigurationValidator.INTERLOCK_EMPTY_REQUIREMENT,
                            "requires",
                            "interlock requires a non-empty mechanism and state"));
        }
        if (onFail == null) {
            issues.add(
                    new ValidationIssue(
                            ConfigurationValidator.INTERLOCK_MISSING_FAIL_OUTCOME,
                            "onFail",
                            "interlock requires an explicit fail outcome"));
        }
        if (onFail == InterlockOutcome.CLAMP && clampTo == null) {
            issues.add(
                    new ValidationIssue(
                            ConfigurationValidator.INTERLOCK_CLAMP_TARGET_REQUIRED,
                            "clampTo",
                            "clamp outcome requires a named clamp target"));
        }
        if (onFail == InterlockOutcome.INTERMEDIATE && intermediate == null) {
            issues.add(
                    new ValidationIssue(
                            ConfigurationValidator.INTERLOCK_INTERMEDIATE_TARGET_REQUIRED,
                            "intermediate",
                            "intermediate outcome requires a generated goal name"));
        }
        if (onFail == InterlockOutcome.INTERMEDIATE
                && intermediate != null
                && intermediate.equals(source)) {
            issues.add(
                    new ValidationIssue(
                            ConfigurationValidator.INTERLOCK_INTERMEDIATE_CYCLE,
                            "intermediate",
                            "generated intermediate must not equal the source mechanism"));
        }
        return ValidationResult.of(issues);
    }

    /**
     * True when two INTERMEDIATE rules would replace each other's source.
     * That pair is a deadlock in the data model. This method does not
     * command either mechanism.
     */
    public static boolean generatedIntermediatesCycle(InterlockRule first, InterlockRule second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.onFail != InterlockOutcome.INTERMEDIATE
                || second.onFail != InterlockOutcome.INTERMEDIATE) {
            return false;
        }
        if (first.intermediate == null || second.intermediate == null) {
            return false;
        }
        if (first.intermediate.equals(first.source) || second.intermediate.equals(second.source)) {
            return true;
        }
        return first.intermediate.equals(second.source) && second.intermediate.equals(first.source);
    }

    private boolean requirementMet(InterlockInputs inputs) {
        if (READY.equals(requiredState)) {
            Boolean ready = inputs.ready(requiredMechanism).orElse(null);
            if (Boolean.TRUE.equals(ready)) {
                return true;
            }
        }
        String named = inputs.namedState(requiredMechanism).orElse(null);
        return named != null && named.equals(requiredState);
    }

    private String failReason() {
        switch (onFail) {
            case REJECT:
                return REJECTED;
            case DEFER:
                return DEFERRED;
            case CLAMP:
                return CLAMPED;
            case INTERMEDIATE:
                return INTERMEDIATE_GOAL;
            case CONFIRM:
                return CONFIRM_REQUIRED;
            default:
                return REJECTED;
        }
    }

    private String defaultName() {
        return source + "-requires-" + requiredMechanism + "-" + requiredState;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static final class Builder {
        private final String source;
        private String name;
        private String requiredMechanism;
        private String requiredState;
        private InterlockOutcome onFail;
        private String clampTo;
        private String intermediate;

        private Builder(String source) {
            this.source = source;
        }

        /**
         * Student-facing rule id, for example
         * {@code "intake-may-run-only-when-deployed"}.
         */
        public Builder named(String name) {
            this.name = name;
            return this;
        }

        /**
         * Other mechanism plus required named state or {@link #READY}.
         * {@code READY} may be satisfied by a named state {@code READY}
         * or by a {@link org.allsparks.mimic.observe.Readiness#ready()}
         * bit in {@link InterlockInputs}.
         */
        public Builder requires(String mechanismId, String namedStateOrReady) {
            this.requiredMechanism = mechanismId;
            this.requiredState = namedStateOrReady;
            return this;
        }

        /**
         * Named state the source goal is clamped to when the requirement
         * fails. Declaration only; not a motor command.
         */
        public Builder clampTo(String namedState) {
            this.clampTo = namedState;
            return this;
        }

        /**
         * Generated intermediate goal name when the requirement fails.
         * Must not equal {@link #source()} and must not cycle with
         * another INTERMEDIATE rule.
         */
        public Builder intermediate(String generatedGoal) {
            this.intermediate = generatedGoal;
            return this;
        }

        public ValidationResult validate() {
            return new InterlockRule(this).validate();
        }

        /**
         * Finish the rule with an explicit fail action. Sketch:
         * {@code .onFail(InterlockOutcome.REJECT)}.
         */
        public InterlockRule onFail(InterlockOutcome outcome) {
            this.onFail = outcome;
            return new InterlockRule(this);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof InterlockRule)) {
            return false;
        }
        InterlockRule that = (InterlockRule) other;
        return Objects.equals(name, that.name)
                && Objects.equals(source, that.source)
                && Objects.equals(requiredMechanism, that.requiredMechanism)
                && Objects.equals(requiredState, that.requiredState)
                && onFail == that.onFail
                && Objects.equals(clampTo, that.clampTo)
                && Objects.equals(intermediate, that.intermediate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name, source, requiredMechanism, requiredState, onFail, clampTo, intermediate);
    }
}
