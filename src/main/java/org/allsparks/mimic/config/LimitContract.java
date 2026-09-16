package org.allsparks.mimic.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.allsparks.mimic.observe.LimitSwitchSample;
import org.allsparks.mimic.observe.MeasurementValidity;

/**
 * Immutable declaration of soft/hard bounds, stopping margin, wrap policy,
 * and the missing-switch rule. Phase 0 does not enforce limits: a contract
 * is not an {@code ActuatorSafetyGate}, does not clamp output, and does not
 * write motors or servos.
 *
 * Students should keep three ideas apart: a <em>target</em> is where you want
 * to be; a <em>soft limit</em> is a software bound plus stopping margin; a
 * <em>hard limit</em> is a physical switch or stop that must not be driven
 * further into.
 *
 * <p>Wrap-aware rotary bounds: {@link WrapPolicy#WRAP_AWARE} treats
 * {@code min} and {@code max} as an allowed arc on a circle of
 * {@link #wrapPeriod()}. Linear mechanisms must use {@link WrapPolicy#NONE}
 * so the allowed set is the closed interval {@code [min, max]}. See
 * {@link WrapPolicy} for the min-greater-than-max (crosses-zero) case.
 *
 * <p>Missing switch is not clear: a {@link LimitSwitchSample} that is
 * {@link MeasurementValidity#MISSING} must not authorize travel into that
 * limit. {@link #permitsMotion()} is always false.
 */
public final class LimitContract {
    private final LimitPolicy policy;
    private final Double min;
    private final Double max;
    private final double stoppingMargin;
    private final WrapPolicy wrapPolicy;
    private final Double wrapPeriod;
    private final MissingSwitchRule missingSwitchRule;

    private LimitContract(Builder builder) {
        this.policy = builder.policy;
        this.min = builder.min;
        this.max = builder.max;
        this.stoppingMargin = builder.stoppingMargin;
        this.wrapPolicy = builder.wrapPolicy == null ? WrapPolicy.NONE : builder.wrapPolicy;
        this.wrapPeriod = builder.wrapPeriod;
        this.missingSwitchRule =
                builder.missingSwitchRule == null
                        ? MissingSwitchRule.NEVER_TREAT_AS_CLEAR
                        : builder.missingSwitchRule;
    }

    /**
     * Soft-only bounds. Sketch:
     * {@code LimitContract.soft(min, max).stoppingMargin(...)}.
     */
    public static Builder soft(double min, double max) {
        return new Builder(LimitPolicy.SOFT_ONLY).bounds(min, max);
    }

    /** Hard-switch policy with no software interval. */
    public static Builder hard() {
        return new Builder(LimitPolicy.HARD_ONLY);
    }

    /** Soft interval plus hard-switch rule. */
    public static Builder softAndHard(double min, double max) {
        return new Builder(LimitPolicy.SOFT_AND_HARD).bounds(min, max);
    }

    public static Builder of(LimitPolicy policy) {
        return new Builder(policy);
    }

    public LimitPolicy policy() {
        return policy;
    }

    public Double min() {
        return min;
    }

    public Double max() {
        return max;
    }

    public double stoppingMargin() {
        return stoppingMargin;
    }

    public WrapPolicy wrapPolicy() {
        return wrapPolicy;
    }

    public Double wrapPeriod() {
        return wrapPeriod;
    }

    public MissingSwitchRule missingSwitchRule() {
        return missingSwitchRule;
    }

    public boolean hasSoftBounds() {
        return policy == LimitPolicy.SOFT_ONLY || policy == LimitPolicy.SOFT_AND_HARD;
    }

    public boolean hasHardLimits() {
        return policy == LimitPolicy.HARD_ONLY || policy == LimitPolicy.SOFT_AND_HARD;
    }

    /**
     * Always false. Declaring bounds does not authorize actuation or enable
     * the Phase 3 gate.
     */
    public boolean permitsMotion() {
        return false;
    }

    /**
     * Whether travel <em>into</em> {@code side} is authorized given pose and
     * the switch sample for that side. Motion away from a violated bound may
     * still be described as authorized for the opposite side; that is not a
     * motor command.
     *
     * A {@link MeasurementValidity#MISSING} sample never authorizes travel
     * into that limit. {@link MeasurementValidity#UNSUPPORTED} never
     * authorizes when hard limits are in the policy. Only a usable
     * {@link MeasurementValidity#VALID} sample that is not asserted can pass
     * the switch check. Soft-only may proceed on pose when the channel is
     * {@code UNSUPPORTED} (no switch wired).
     *
     * This method does not write hardware.
     */
    public boolean authorizesTravelInto(LimitSide side, double pose, LimitSwitchSample sample) {
        if (side == null || policy == LimitPolicy.NONE) {
            return false;
        }
        if (!switchAllowsTravelInto(sample)) {
            return false;
        }
        if (!hasSoftBounds()) {
            return true;
        }
        if (!hasUsableSoftBounds() || !Double.isFinite(pose)) {
            return false;
        }
        if (wrapPolicy == WrapPolicy.WRAP_AWARE) {
            if (!hasUsableWrapPeriod()) {
                return false;
            }
            return authorizesWrapAware(side, pose);
        }
        return authorizesLinear(side, pose);
    }

    public ValidationResult validate() {
        List<ValidationIssue> issues = new ArrayList<>();
        collectBoundIssues(issues);
        return ValidationResult.of(issues);
    }

    void collectBoundIssues(List<ValidationIssue> issues) {
        if (policy == LimitPolicy.NONE) {
            issues.add(
                    new ValidationIssue(
                            ConfigurationValidator.LIMIT_POLICY_REQUIRED,
                            "policy",
                            "limit contract requires SOFT_ONLY, HARD_ONLY, or SOFT_AND_HARD"));
        }
        if (!Double.isFinite(stoppingMargin) || stoppingMargin < 0.0) {
            issues.add(
                    new ValidationIssue(
                            ConfigurationValidator.LIMIT_INVALID_STOPPING_MARGIN,
                            "stoppingMargin",
                            "stopping margin must be finite and >= 0"));
        }
        if (hasSoftBounds()) {
            if (!hasUsableSoftBounds()) {
                issues.add(
                        new ValidationIssue(
                                ConfigurationValidator.LIMIT_SOFT_BOUNDS_REQUIRED,
                                "bounds",
                                "soft limit contract requires finite min and max"));
            } else if (wrapPolicy != WrapPolicy.WRAP_AWARE && min > max) {
                issues.add(
                        new ValidationIssue(
                                ConfigurationValidator.LIMIT_LINEAR_BOUNDS_ORDER,
                                "bounds",
                                "linear soft bounds require min <= max"));
            }
        }
        if (wrapPolicy == WrapPolicy.WRAP_AWARE && !hasUsableWrapPeriod()) {
            issues.add(
                    new ValidationIssue(
                            ConfigurationValidator.LIMIT_WRAP_PERIOD_REQUIRED,
                            "wrapPeriod",
                            "WRAP_AWARE rotary bounds require a positive finite period"));
        }
    }

    private boolean switchAllowsTravelInto(LimitSwitchSample sample) {
        if (sample == null) {
            return false;
        }
        if (sample.missing()
                || sample.validity() == MeasurementValidity.STALE
                || sample.validity() == MeasurementValidity.OUT_OF_RANGE
                || sample.validity() == MeasurementValidity.DISAGREEING) {
            return false;
        }
        if (sample.unsupported()) {
            return !hasHardLimits();
        }
        if (!sample.isUsable()) {
            return false;
        }
        return !sample.asserted();
    }

    private boolean authorizesLinear(LimitSide side, double pose) {
        if (side == LimitSide.MIN) {
            return pose > min + stoppingMargin;
        }
        return pose < max - stoppingMargin;
    }

    /**
     * Wrap-aware arc test. Pose is reduced modulo {@link #wrapPeriod()}. The
     * allowed set is the forward arc from {@code min} to {@code max}. Travel
     * into {@link LimitSide#MAX} is positive along that circle; travel into
     * {@link LimitSide#MIN} is negative. When pose is already in the
     * forbidden complementary arc (the wrap stop), travel further into the
     * nearer bound is refused and travel toward the interior may still be
     * described as authorized for the opposite side.
     */
    private boolean authorizesWrapAware(LimitSide side, double pose) {
        double period = wrapPeriod;
        if (!inAllowedArc(pose, min, max, period)) {
            double fromMax = forwardDistance(max, pose, period);
            double toMin = forwardDistance(pose, min, period);
            if (fromMax == toMin) {
                return false;
            }
            boolean pastMax = fromMax < toMin;
            if (side == LimitSide.MAX) {
                return !pastMax;
            }
            return pastMax;
        }
        if (side == LimitSide.MAX) {
            return forwardDistance(pose, max, period) > stoppingMargin;
        }
        return forwardDistance(min, pose, period) > stoppingMargin;
    }

    private boolean hasUsableSoftBounds() {
        return min != null && max != null && Double.isFinite(min) && Double.isFinite(max);
    }

    private boolean hasUsableWrapPeriod() {
        return wrapPeriod != null && Double.isFinite(wrapPeriod) && wrapPeriod > 0.0;
    }

    static boolean inAllowedArc(double pose, double min, double max, double period) {
        double span = forwardDistance(min, max, period);
        return forwardDistance(min, pose, period) <= span;
    }

    /**
     * Forward distance from {@code from} to {@code to} on a circle of
     * {@code period}. Both values are wrapped into {@code [0, period)} first.
     */
    static double forwardDistance(double from, double to, double period) {
        double a = wrap(from, period);
        double b = wrap(to, period);
        double distance = b - a;
        if (distance < 0.0) {
            distance += period;
        }
        return distance;
    }

    static double wrap(double value, double period) {
        double remainder = value % period;
        if (remainder < 0.0) {
            remainder += period;
        }
        return remainder;
    }

    public static final class Builder {
        private final LimitPolicy policy;
        private Double min;
        private Double max;
        private double stoppingMargin;
        private WrapPolicy wrapPolicy = WrapPolicy.NONE;
        private Double wrapPeriod;
        private MissingSwitchRule missingSwitchRule = MissingSwitchRule.NEVER_TREAT_AS_CLEAR;

        private Builder(LimitPolicy policy) {
            this.policy = policy == null ? LimitPolicy.NONE : policy;
        }

        public Builder bounds(double min, double max) {
            this.min = min;
            this.max = max;
            return this;
        }

        public Builder stoppingMargin(double stoppingMargin) {
            this.stoppingMargin = stoppingMargin;
            return this;
        }

        public Builder wrapPolicy(WrapPolicy wrapPolicy) {
            this.wrapPolicy = wrapPolicy == null ? WrapPolicy.NONE : wrapPolicy;
            return this;
        }

        /**
         * Rotary allowed-arc mode. {@code period} is the circle length in the
         * same units as {@code min}/{@code max} (degrees or radians). This
         * does not command a turret.
         */
        public Builder wrapAware(double period) {
            this.wrapPolicy = WrapPolicy.WRAP_AWARE;
            this.wrapPeriod = period;
            return this;
        }

        public Builder wrapPeriod(double wrapPeriod) {
            this.wrapPeriod = wrapPeriod;
            return this;
        }

        public Builder missingSwitchRule(MissingSwitchRule missingSwitchRule) {
            this.missingSwitchRule =
                    missingSwitchRule == null
                            ? MissingSwitchRule.NEVER_TREAT_AS_CLEAR
                            : missingSwitchRule;
            return this;
        }

        public ValidationResult validate() {
            return new LimitContract(this).validate();
        }

        public LimitContract build() {
            return new LimitContract(this);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LimitContract)) {
            return false;
        }
        LimitContract that = (LimitContract) other;
        return Double.compare(that.stoppingMargin, stoppingMargin) == 0
                && policy == that.policy
                && wrapPolicy == that.wrapPolicy
                && missingSwitchRule == that.missingSwitchRule
                && Objects.equals(min, that.min)
                && Objects.equals(max, that.max)
                && Objects.equals(wrapPeriod, that.wrapPeriod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                policy, min, max, stoppingMargin, wrapPolicy, wrapPeriod, missingSwitchRule);
    }
}
