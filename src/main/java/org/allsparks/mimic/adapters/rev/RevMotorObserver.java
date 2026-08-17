package org.allsparks.mimic.adapters.rev;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import org.allsparks.mimic.clock.MimicClock;
import org.allsparks.mimic.observe.MechanismObserver;
import org.allsparks.mimic.units.MechanismUnits;

/**
 * Passive REV Control Hub / Expansion Hub motor observer.
 *
 * Depends on functional suppliers so unit tests and desktop builds do not
 * require the FTC SDK on the classpath. On-robot, wire suppliers to
 * {@code DcMotorEx#getCurrentPosition()}, {@code getVelocity()},
 * {@code getCurrent()}, and the last commanded power.
 *
 * This adapter never commands motors or servos.
 */
public final class RevMotorObserver {
    private final MechanismObserver observer;

    public RevMotorObserver(MechanismObserver observer) {
        this.observer = Objects.requireNonNull(observer, "observer");
    }

    public static RevMotorObserver create(
            String mechanismId,
            MimicClock clock,
            MechanismUnits units,
            DoubleSupplier ticks,
            DoubleSupplier ticksPerSecond,
            DoubleSupplier commandedEffort) {
        MechanismObserver observer = MechanismObserver.builder(mechanismId, clock, units)
                .ticks(ticks)
                .ticksPerSecond(ticksPerSecond)
                .requestedOutput(commandedEffort)
                .appliedOutput(commandedEffort)
                .build();
        return new RevMotorObserver(observer);
    }

    public MechanismObserver observer() {
        return observer;
    }

    /**
     * Optional current supplier. {@code DcMotorEx#getCurrent} is not bulk-read
     * on current REV hubs; teams should measure loop overhead before polling
     * every motor every cycle.
     */
    public static DoubleSupplier currentAmps(DoubleSupplier amps) {
        return amps;
    }

    public static BooleanSupplier digitalChannel(BooleanSupplier rawState) {
        return rawState;
    }
}
